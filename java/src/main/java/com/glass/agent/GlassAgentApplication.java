package com.glass.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 配眼镜指南智能体启动类。
 *
 * <p>提供两种入口：
 * <ul>
 *     <li>REST 工具接口 {@code /api/tools/**}：不接大模型也能直接调用 6 个配镜工具；</li>
 *     <li>对话智能体接口 {@code /api/agent/chat}：由大模型通过 Function Calling 自动编排工具。</li>
 * </ul>
 */
@SpringBootApplication
public class GlassAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlassAgentApplication.class, args);
    }
}
