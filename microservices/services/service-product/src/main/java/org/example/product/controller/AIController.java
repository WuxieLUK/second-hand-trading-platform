package org.example.product.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.POJO.AiChatMessage;
import org.example.ai.POJO.AiChatSession;
import org.example.ai.VO.AiMessageVO;
import org.example.ai.VO.AiSessionVO;
import org.example.common.Result;
import org.example.product.service.AiSessionService;
import org.example.product.agent.CampusAssistant;
import org.example.product.service.AiService;
import org.example.product.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 服务控制器
 * <p>
 * 整合了：
 * - 流式对话（无工具）
 * - 商品描述生成
 * - 智能体对话（含工具调用）
 * - AI 对话会话管理（支持长对话记忆）
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {

    private final AiService aiService;
    private final CampusAssistant campusAssistant;
    private final AiSessionService aiSessionService;
    private final HttpServletRequest request;

    public AIController(AiService aiService,
                        CampusAssistant campusAssistant,
                        AiSessionService aiSessionService,
                        HttpServletRequest request) {
        this.aiService = aiService;
        this.campusAssistant = campusAssistant;
        this.aiSessionService = aiSessionService;
        this.request = request;
    }

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     */
    @PostMapping("/session/create")
    public Result<AiSessionVO> createSession(@RequestBody(required = false) Map<String, Object> params) {
        Integer userId = extractUserId();
        String title = (params != null) ? (String) params.getOrDefault("title", "新对话") : "新对话";
        AiChatSession session = aiSessionService.createSession(userId, title);
        AiSessionVO vo = buildSessionVO(session);
        return Result.success(vo);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/session/list")
    public Result<List<AiSessionVO>> listSessions() {
        Integer userId = extractUserId();
        List<AiSessionVO> sessions = aiSessionService.listSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        Integer userId = extractUserId();
        aiSessionService.deleteSession(sessionId, userId);
        return Result.success(null);
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<AiMessageVO>> getSessionMessages(@PathVariable String sessionId) {
        List<AiMessageVO> messages = aiSessionService.getMessages(sessionId);
        return Result.success(messages);
    }

    // ==================== 流式对话 ====================

    /**
     * 流式 AI 客服对话（无工具，纯对话）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody String message) {
        return aiService.getAiResponse(message, "");
    }

    /**
     * AI 生成商品描述
     */
    @PostMapping("/generateGoodsDesc")
    public Result<String> generateGoodsDesc(@RequestBody Map<String, Object> params) {
        try {
            String keywords = params.getOrDefault("keywords", "").toString();
            String goodsName = params.getOrDefault("goodsName", "").toString();
            Integer isNew = Integer.parseInt(params.getOrDefault("isNew", "0").toString());
            BigDecimal sellPrice = new BigDecimal(params.getOrDefault("sellPrice", "0").toString());

            if (keywords.isBlank() || goodsName.isBlank()) {
                return Result.error("关键词和商品名称不能为空");
            }
            String desc = aiService.generateGoodsDesc(keywords, goodsName, isNew, sellPrice);
            return Result.success(desc);
        } catch (Exception e) {
            log.error("AI生成商品描述异常", e);
            return Result.error("生成失败，请重试");
        }
    }

    // ==================== 智能体对话（含会话管理） ====================

    /**
     * 智能体对话（同步，含工具调用）
     */
    @PostMapping("/agent/chat")
    public Result<String> agentChat(@RequestBody(required = false) Map<String, String> requestBody) {
        String message = requestBody != null ? requestBody.get("message") : null;
        if (message == null || message.isBlank()) {
            return Result.error("消息内容不能为空");
        }

        log.info("Agent 收到消息: {}", message);
        long start = System.currentTimeMillis();

        try {
            String response = campusAssistant.chat(message);
            long cost = System.currentTimeMillis() - start;
            log.info("Agent 响应完成, 耗时: {}ms, 响应长度: {}字符", cost, response.length());
            return Result.success(response);
        } catch (Exception e) {
            log.error("Agent 处理失败", e);
            return Result.error("智能助手暂时无法响应，请稍后重试。");
        }
    }

    /**
     * 智能体对话（流式 SSE，支持会话管理和长对话记忆）
     * <p>
     * 请求体格式：
     * {
     *   "message": "用户消息",
     *   "sessionId": "会话ID（前端生成，可选）",
     *   "history": [{"role": "user/ai", "content": "..."}]  （可选）
     * }
     * <p>
     * 特性：
     * - 消息持久化到数据库（ai_chat_message）
     * - 自动从数据库加载最近 30 条消息作为上下文
     * - 响应完成后自动保存 AI 回复到数据库
     */
    @PostMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentChatStream(@RequestBody Map<String, Object> requestBody) {
        String message = (String) requestBody.get("message");
        if (message == null || message.isBlank()) {
            return Flux.just("消息内容不能为空").concatWithValues("[DONE]");
        }

        String sessionId = (String) requestBody.get("sessionId");
        Integer userId = extractUserId();

        log.info("Agent 流式请求: message={}, sessionId={}, userId={}",
                message, sessionId, userId);

        long start = System.currentTimeMillis();

        try {
            // 1. 确保会话存在
            AiChatSession session = aiSessionService.getOrCreateSession(sessionId, userId);
            final String finalSessionId = session.getId();

            // 2. 保存用户消息到数据库
            aiSessionService.saveMessage(finalSessionId, "user", message);

            // 3. 从数据库加载最近 30 条消息作为上下文
            List<AiChatMessage> recentMessages = aiSessionService.getRecentMessages(finalSessionId, 30);

            // 4. 构建含历史上下文的提示词
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("【以下是本次对话的历史记录，请基于此上下文回答当前问题】\n\n");

            for (int i = 0; i < recentMessages.size() - 1; i++) {
                AiChatMessage msg = recentMessages.get(i);
                String prefix = "user".equals(msg.getRole()) ? "用户" : "AI";
                contextBuilder.append(prefix).append("：").append(msg.getContent()).append("\n\n");
            }

            // 5. 调用 Agent（含历史上下文）
            String contextualMessage;
            if (recentMessages.size() > 1) {
                contextualMessage = contextBuilder.toString()
                        + "【当前问题】\n" + message;
            } else {
                contextualMessage = message;
            }

            String fullResponse = campusAssistant.chat(contextualMessage);
            long cost = System.currentTimeMillis() - start;
            log.info("Agent 响应完成, 耗时: {}ms, 响应长度: {}字符", cost,
                    fullResponse != null ? fullResponse.length() : 0);

            // 6. 保存 AI 回复到数据库
            if (fullResponse != null && !fullResponse.isBlank()) {
                aiSessionService.saveMessage(finalSessionId, "ai", fullResponse);
            }

            // 7. 更新会话标题
            if ("新对话".equals(session.getTitle()) || session.getTitle() == null) {
                String title = message.length() > 20
                        ? message.substring(0, 20) + "..."
                        : message;
                aiSessionService.updateTitle(finalSessionId, title);
            }

            // 8. 模拟流式输出回前端
            if (fullResponse == null || fullResponse.isBlank()) {
                return Flux.just("抱歉，我没有理解您的问题，请换个方式描述。", "[DONE]");
            }

            String[] chars = fullResponse.split("");
            return Flux.fromArray(chars)
                    .delayElements(Duration.ofMillis(10))
                    .concatWithValues("[DONE]");

        } catch (Exception e) {
            log.error("Agent 流式处理失败", e);
            return Flux.just("智能助手暂时无法响应，请稍后重试。", "[DONE]");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从请求头中提取用户ID
     * 优先从 Authorization token 解析，失败则返回默认值
     */
    private Integer extractUserId() {
        try {
            // 1. 尝试从 ThreadLocal 获取（当拦截器生效时）
            Object claims = org.example.product.util.ThreadLocalUtil.get();
            if (claims instanceof Map) {
                Object idObj = ((Map<?, ?>) claims).get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).intValue();
                }
            }

            // 2. 尝试从 Authorization header 解析 JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                String token = authHeader;
                // 去掉 Bearer 前缀（如果有）
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                Map<String, Object> jwtClaims = JwtUtil.parseToken(token);
                if (jwtClaims != null && jwtClaims.containsKey("id")) {
                    Object idObj = jwtClaims.get("id");
                    if (idObj instanceof Integer) return (Integer) idObj;
                    if (idObj instanceof String) return Integer.parseInt((String) idObj);
                    if (idObj instanceof Number) return ((Number) idObj).intValue();
                }
            }
        } catch (Exception ignored) {
            log.debug("从请求头提取用户ID失败，使用默认值");
        }

        // 默认用户ID
        return 1;
    }

    private AiSessionVO buildSessionVO(AiChatSession session) {
        AiSessionVO vo = new AiSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setMessageCount(session.getMessageCount());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }
}
