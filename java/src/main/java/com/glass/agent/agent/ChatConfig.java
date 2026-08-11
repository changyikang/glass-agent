package com.glass.agent.agent;

import com.glass.agent.tool.GlassAdvisorTools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体装配：把 6 个配镜工具作为默认工具挂到 {@link ChatClient} 上，
 * 并设定系统提示词，形成一个「配镜顾问」智能体。
 */
@Configuration
public class ChatConfig {

    private static final String SYSTEM_PROMPT = """
            你是一名专业、严谨的配镜顾问助手。请遵循以下原则：
            1. 当用户的问题涉及视力检查、镜片选择、镜框选择、验光单解读、渐进/多焦点适配、新眼镜不适排查时，
               优先调用对应工具获取结构化建议，再用自然、通俗的语言解释给用户。
            2. 需要参数但用户没给全时，先礼貌地追问关键信息（如度数、年龄、用途、预算、脸型等），不要臆造数值。
            3. 你只提供配镜咨询，不做医疗诊断。若出现视力骤降、眼痛、闪光感、飞蚊骤增等情况，提醒用户及时就医。
            4. 回答使用中文，条理清晰。
            """;

    @Bean
    public ChatClient glassChatClient(ChatClient.Builder builder, GlassAdvisorTools tools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(tools)
                .build();
    }
}
