package com.itheima.config;

// 该代码 OpenAI SDK 版本为 2.6.0
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class openAi {
    public static void main(String[] args) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage("你是谁")
                .model("qwen-plus")
                .build();

        try {
            ChatCompletion chatCompletion = client.chat().completions().create(params);
            System.out.println(chatCompletion);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
