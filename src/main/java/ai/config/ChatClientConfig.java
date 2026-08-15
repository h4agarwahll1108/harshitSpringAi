package ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.prompt.ChatOptions;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {

        return ChatClient.builder(chatModel)

                // Common system prompt
                .defaultSystem("""
                        You are a helpful AI assistant.
                        Answer clearly and accurately.
                        If you don't know the answer, say that you don't know.
                        """)

                // Common options
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.7)
                        .maxTokens(1000)
                )

                // Common advisors can be added here
                // .defaultAdvisors(...)

                // Common tools can be added here
                // .defaultTools(...)

                .build();
    }
}