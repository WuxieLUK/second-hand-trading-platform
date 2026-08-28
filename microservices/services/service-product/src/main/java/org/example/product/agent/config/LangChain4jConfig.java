package org.example.product.agent.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.example.product.agent.CampusAssistant;
import org.example.product.agent.tool.GoodsSearchTool;
import org.example.product.agent.tool.OrderQueryTool;
import org.example.product.agent.tool.PlatformRuleTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 智能体配置 —— 创建本地 Ollama 模型实例和 CampusAssistant Bean
 * <p>
 * 支持长对话：消息窗口从 10 条扩展到 50 条，配合数据库持久化实现跨会话上下文记忆。
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    /**
     * 创建同步模型实例（通过 Ollama 的 OpenAI 兼容 API）
     */
    @Bean
    public OpenAiChatModel openAiChatModel() {
        log.info("正在初始化 OpenAiChatModel（通过 Ollama /v1 兼容端点）: baseUrl=http://localhost:11434/v1, model=qwen3:4b");
        return OpenAiChatModel.builder()
                .baseUrl("http://localhost:11434/v1")
                .apiKey("ollama")
                .modelName("qwen3:4b")
                .temperature(0.4)
                .topP(0.9)
                .maxTokens(8192)        // 从 4096 增加到 8192，支持更长回复
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建校园助手 Agent
     * <p>
     * 长对话支持增强：
     * - chatMemory 从 MaxMessages(10) 增加到 MaxMessages(50)
     * - maxTokens 从 4096 增加到 8192
     * - 配合数据库持久化的历史消息，实现超长对话上下文记忆
     */
    @Bean
    public CampusAssistant campusAssistant(
            OpenAiChatModel openAiChatModel,
            GoodsSearchTool goodsSearchTool,
            OrderQueryTool orderQueryTool,
            PlatformRuleTool platformRuleTool) {
        log.info("正在初始化 CampusAssistant Agent（qwen3:4b via Ollama）...");

        // 使用扩展的消息窗口（50 条），支持长对话
        // 对于超过窗口的消息，由 AIController 从数据库加载并拼接到提示中
        return AiServices.builder(CampusAssistant.class)
                .chatLanguageModel(openAiChatModel)
                .tools(goodsSearchTool, orderQueryTool, platformRuleTool)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(50))
                .build();
    }
}
