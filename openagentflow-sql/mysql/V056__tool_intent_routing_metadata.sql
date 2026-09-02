-- P77：工具通用意图路由元数据。
ALTER TABLE tool_definition
    ADD COLUMN intent_codes JSON NULL COMMENT '工具可处理的意图编码JSON数组' AFTER response_schema,
    ADD COLUMN routing_examples JSON NULL COMMENT '工具自然语言路由示例JSON数组' AFTER intent_codes,
    ADD COLUMN required_entities JSON NULL COMMENT '工具执行前必填实体名称JSON数组' AFTER routing_examples,
    COMMENT = '工具定义表';

