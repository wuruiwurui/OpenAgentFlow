import { request } from './http';

export interface ToolDefinitionSummary {
  id: string;
  toolCode: string;
  toolName: string;
  toolType: string;
  description?: string;
  requestMethod?: string;
  endpointUrl?: string;
  authType?: string;
  authConfig?: string;
  headers?: string;
  requestSchema?: string;
  responseSchema?: string;
  intentCodes?: string[];
  routingExamples?: string[];
  requiredEntities?: string[];
  timeoutMs?: number;
  retryCount?: number;
  riskLevel: string;
  riskLabel: string;
  requireConfirm: boolean;
  enabled: boolean;
  sourceType?: string;
  mcpServerId?: string;
  mcpToolName?: string;
  status: string;
  invocationCount: number;
  successRate: number;
  updatedAt?: string;
}

export interface ToolDefinitionRequest {
  toolCode?: string;
  toolName: string;
  toolType: string;
  description?: string;
  requestMethod?: string;
  endpointUrl?: string;
  authType?: string;
  authConfig?: string;
  headers?: string;
  requestSchema?: string;
  responseSchema?: string;
  intentCodes?: string[];
  routingExamples?: string[];
  requiredEntities?: string[];
  timeoutMs?: number;
  retryCount?: number;
  riskLevel?: string;
  requireConfirm?: boolean;
  enabled?: boolean;
  mcpServerId?: string;
  mcpToolName?: string;
  status?: string;
}

export interface ToolExecutionResult {
  success: boolean;
  statusCode: number;
  latencyMs: number;
  responseBody?: string;
  errorMessage?: string;
  confirmationRequired?: boolean;
  confirmationId?: string;
}

export interface AgentToolBindingSummary {
  agentId: string;
  toolId: string;
  toolCode: string;
  toolName: string;
  toolType: string;
  riskLevel: string;
  requireConfirm: boolean;
  enabled: boolean;
}

export async function fetchTools() {
  return request<ToolDefinitionSummary[]>('/tools');
}

export async function fetchTool(id: string) {
  return request<ToolDefinitionSummary>(`/tools/${id}`);
}

export async function createTool(payload: ToolDefinitionRequest) {
  return request<ToolDefinitionSummary>('/tools', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateTool(id: string, payload: ToolDefinitionRequest) {
  return request<ToolDefinitionSummary>(`/tools/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteTool(id: string) {
  return request<void>(`/tools/${id}`, { method: 'DELETE' });
}

export async function testTool(id: string, inputParams: Record<string, unknown>) {
  return request<ToolExecutionResult>(`/tools/${id}/test`, {
    method: 'POST',
    body: JSON.stringify({ inputParams }),
  });
}

export async function fetchAgentToolBindings(agentId: string) {
  return request<AgentToolBindingSummary[]>(`/agents/${agentId}/tools`);
}

export async function saveAgentToolBindings(agentId: string, toolIds: string[]) {
  return request<AgentToolBindingSummary[]>(`/agents/${agentId}/tools`, {
    method: 'PUT',
    body: JSON.stringify({ toolIds }),
  });
}
