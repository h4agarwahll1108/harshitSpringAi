package ai.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Objects;

@Configuration
public class ChatClientConfig {

    @Value("classpath:/prompts/system-prompt.st")
    private Resource systemPromptResource;

    @Bean
    public SystemPromptTemplate systemPromptTemplate() {
        return new SystemPromptTemplate(systemPromptResource);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, SystemPromptTemplate systemPromptTemplate) {

        return builder
                .defaultSystem(Objects.requireNonNull(systemPromptTemplate.createMessage().getText()))
                // Common options
//                .defaultOptions(ChatOptions.builder()
//                        .temperature(0.7)
//                        .maxTokens(1000)
//                )

                // Common advisors can be added here
                // .defaultAdvisors(...)

                // Common tools can be added here
                // .defaultTools(...)

                .build();
    }


//    @Bean
//    ChatClient chatClient(
//            ChatClient.Builder builder,
//            MessageChatMemoryAdvisor memoryAdvisor,
//            QuestionAnswerAdvisor ragAdvisor,
//            SimpleLoggerAdvisor loggerAdvisor) {
//
//        return builder
//                .defaultAdvisors(
//                        loggerAdvisor,
//                        memoryAdvisor,
//                        ragAdvisor
//                )
//                .build();
//    }

}