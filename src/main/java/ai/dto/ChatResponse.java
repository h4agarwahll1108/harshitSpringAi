package ai.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String conversationId;
    private String response;
    private List<RagResponse.Source> sources;
    private LocalDateTime timestamp;
    private int tokenUsage;

    public static Builder builder() {
        return new Builder();
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public List<RagResponse.Source> getSources() { return sources; }
    public void setSources(List<RagResponse.Source> sources) { this.sources = sources; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(int tokenUsage) { this.tokenUsage = tokenUsage; }

    public static class Builder {
        private String conversationId;
        private String response;
        private List<RagResponse.Source> sources;
        private LocalDateTime timestamp;
        private int tokenUsage;

        public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public Builder response(String response) { this.response = response; return this; }
        public Builder sources(List<RagResponse.Source> sources) { this.sources = sources; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder tokenUsage(int tokenUsage) { this.tokenUsage = tokenUsage; return this; }

        public ChatResponse build() {
            return new ChatResponse(conversationId, response, sources, timestamp, tokenUsage);
        }
    }
}