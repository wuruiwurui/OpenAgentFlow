import { API_BASE_URL, applyAuthHeaders, request } from './http';
import type { KnowledgeSource } from './knowledge';
import type { MemoryRecallItem } from './memories';

export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

export interface ChatCompletionRequest {
  agentId?: string;
  modelId?: string;
  sessionId?: string;
  input: string;
  history: ChatMessage[];
  temperature?: number;
  maxTokens?: number;
}

export interface IntentRoutePlan {
  intents?: string[];
  entities?: Record<string, string>;
  selectedToolNames?: string[];
  uncoveredIntents?: string[];
  missingEntities?: string[];
  needTool?: boolean;
  needRag?: boolean;
  directAnswer?: boolean;
  needsClarification?: boolean;
  confidence?: number;
  reason?: string;
  clarificationQuestion?: string;
}

export interface ChatCompletionResponse {
  runId: string;
  sessionId?: string;
  content: string;
  providerName: string;
  modelName: string;
  status: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  latencyMs: number;
  errorMessage?: string;
  memories?: MemoryRecallItem[];
  sources?: KnowledgeSource[];
  trustedAnswer?: TrustedAnswerStatus;
  toolResults?: Record<string, unknown>[];
  intentRoute?: IntentRoutePlan;
  enhancedQueries?: string[];
  rerankMode?: string;
  rerankModelId?: string;
  rerankLatencyMs?: number;
  rerankErrorMessage?: string;
}

export interface TrustedAnswerStatus {
  enabled: boolean;
  answerable: boolean;
  citationRequired?: boolean;
  minCitationCount?: number;
  confidenceScore?: number;
  rejectReason?: string;
  qualityAdvice?: string;
}

export interface StreamHandlers {
  onMeta?: (data: Record<string, unknown>) => void;
  onDelta?: (content: string) => void;
  onTool?: (data: Record<string, unknown>) => void;
  onDone?: (data: Record<string, unknown>) => void;
  onError?: (message: string) => void;
}

export interface StreamResult {
  doneReceived: boolean;
  errorReceived: boolean;
  aborted?: boolean;
}

interface StreamCursor {
  runId?: string;
  lastEventId: number;
  reconnectCount: number;
  resumable: boolean;
}

export async function completeChat(payload: ChatCompletionRequest) {
  return request<ChatCompletionResponse>('/chat/completions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function streamChat(
  payload: ChatCompletionRequest,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<StreamResult> {
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');
  applyAuthHeaders(headers);

  const response = await fetch(`${API_BASE_URL}/chat/completions/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok || !response.body) {
    throw new Error('流式对话请求失败');
  }

  return readSseStream(response.body, handlers, signal, {
    lastEventId: 0,
    reconnectCount: 0,
    resumable: true,
  });
}

export async function readSseStream(
  body: ReadableStream<Uint8Array>,
  handlers: StreamHandlers,
  signal?: AbortSignal,
  cursor: StreamCursor = { lastEventId: 0, reconnectCount: 0, resumable: false },
): Promise<StreamResult> {
  const reader = body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let doneReceived = false;
  let errorReceived = false;
  let aborted = false;
  const abortReader = () => {
    aborted = true;
    void reader.cancel();
  };
  signal?.addEventListener('abort', abortReader, { once: true });

  try {
    while (true) {
      if (signal?.aborted) {
        aborted = true;
        break;
      }
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split('\n\n');
      buffer = events.pop() ?? '';
      for (const eventText of events) {
        const event = dispatchSseEvent(eventText, handlers);
        const eventName = event.name;
        cursor.lastEventId = Math.max(cursor.lastEventId, event.id ?? 0);
        if (event.runId) cursor.runId = event.runId;
        doneReceived ||= eventName === 'done';
        errorReceived ||= eventName === 'error';
      }
    }
  } catch (error) {
    if (signal?.aborted || (error instanceof DOMException && error.name === 'AbortError')) {
      aborted = true;
      return { doneReceived, errorReceived, aborted };
    }
    // 已收到 done 说明业务调用成功，忽略 SSE 连接收尾阶段的读流异常。
    if (!doneReceived) {
      if (cursor.resumable && cursor.runId && cursor.reconnectCount < 3) {
        await new Promise((resolve) => window.setTimeout(resolve, 300 * (cursor.reconnectCount + 1)));
        return resumeSseStream(cursor, handlers, signal);
      }
      throw error;
    }
  } finally {
    signal?.removeEventListener('abort', abortReader);
  }

  if (!aborted && buffer.trim()) {
    const event = dispatchSseEvent(buffer, handlers);
    const eventName = event.name;
    cursor.lastEventId = Math.max(cursor.lastEventId, event.id ?? 0);
    if (event.runId) cursor.runId = event.runId;
    doneReceived ||= eventName === 'done';
    errorReceived ||= eventName === 'error';
  }

  if (!aborted && !doneReceived && !errorReceived && cursor.resumable && cursor.runId && cursor.reconnectCount < 3) {
    return resumeSseStream(cursor, handlers, signal);
  }

  return { doneReceived, errorReceived, aborted };
}

function dispatchSseEvent(eventText: string, handlers: StreamHandlers): { name?: string; id?: number; runId?: string } {
  const lines = eventText.split(/\r?\n/);
  const eventName = lines.find((line) => line.startsWith('event:'))?.slice(6).trim() ?? 'message';
  const eventId = Number(lines.find((line) => line.startsWith('id:'))?.slice(3).trim() ?? 0);
  const dataText = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())
    .join('\n');
  if (!dataText) {
    return {};
  }

  let data: Record<string, unknown>;
  try {
    data = JSON.parse(dataText);
  } catch {
    data = { content: dataText };
  }

  if (eventName === 'meta') {
    handlers.onMeta?.(data);
  } else if (eventName === 'delta') {
    handlers.onDelta?.(String(data.content ?? ''));
  } else if (eventName === 'tool') {
    handlers.onTool?.(data);
  } else if (eventName === 'done') {
    handlers.onDone?.(data);
  } else if (eventName === 'error') {
    handlers.onError?.(String(data.message ?? '模型调用失败'));
  }
  return {
    name: eventName,
    id: Number.isFinite(eventId) ? eventId : undefined,
    runId: typeof data.runId === 'string' ? data.runId : undefined,
  };
}

async function resumeSseStream(cursor: StreamCursor, handlers: StreamHandlers, signal?: AbortSignal) {
  const headers = new Headers();
  applyAuthHeaders(headers);
  headers.set('Last-Event-ID', String(cursor.lastEventId));
  const response = await fetch(
    `${API_BASE_URL}/runs/${encodeURIComponent(cursor.runId ?? '')}/events/stream?after=${cursor.lastEventId}`,
    { headers, signal },
  );
  if (!response.ok || !response.body) throw new Error('流式对话续传失败');
  return readSseStream(response.body, handlers, signal, {
    ...cursor,
    reconnectCount: cursor.reconnectCount + 1,
  });
}
