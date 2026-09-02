-- P77：工具通用意图路由元数据。
ALTER TABLE tool_definition
    ADD COLUMN intent_codes JSON NULL COMMENT '工具可处理的意图编码JSON数组' AFTER response_schema,
    ADD COLUMN routing_examples JSON NULL COMMENT '工具自然语言路由示例JSON数组' AFTER intent_codes,
    ADD COLUMN required_entities JSON NULL COMMENT '工具执行前必填实体名称JSON数组' AFTER routing_examples,
    COMMENT = '工具定义表';

-- 为内置演示工具补充可读的路由元数据，业务规则仍由数据库配置表达。
UPDATE tool_definition
SET intent_codes = JSON_ARRAY('order.query', 'order.summary', 'logistics.track'),
    routing_examples = JSON_ARRAY('查询订单状态', '我有多少订单', '订单列表', '订单到哪里了'),
    required_entities = JSON_ARRAY(),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE tool_code = 'demo_order_status_rest';
