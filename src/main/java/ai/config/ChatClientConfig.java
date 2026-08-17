package ai.config;


import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor memoryAdvisor(
            ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .build();
    }

    @Bean
    public QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .build();
    }


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 SystemPromptTemplate systemPromptTemplate,
                                 MessageChatMemoryAdvisor memoryAdvisor,
                                 QuestionAnswerAdvisor ragAdvisor) {

        return builder
                .defaultSystem(Objects.requireNonNull(systemPromptTemplate.createMessage().getText()))
                // Common options
//                .defaultOptions(ChatOptions.builder()
//                        .temperature(0.7)
//                        .maxTokens(1000)
//                )

                // Common advisors can be added here
                .defaultAdvisors(memoryAdvisor, ragAdvisor)

                // Common tools can be added here
                // .defaultTools(...)

                .build();
    }

}