package ai.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {
    private String answer;
    private List<Source> sources;
    private int relevantChunks;

    public static Builder builder() {
        return new Builder();
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }

    public int getRelevantChunks() { return relevantChunks; }
    public void setRelevantChunks(int relevantChunks) { this.relevantChunks = relevantChunks; }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private Long documentId;
        private String fileName;
        private int pageNumber;
        private int chunkIndex;

        public static Builder builder() {
            return new Builder();
        }

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

        public static class Builder {
            private Long documentId;
            private String fileName;
            private int pageNumber;
            private int chunkIndex;

            public Builder documentId(Long documentId) { this.documentId = documentId; return this; }
            public Builder fileName(String fileName) { this.fileName = fileName; return this; }
            public Builder pageNumber(int pageNumber) { this.pageNumber = pageNumber; return this; }
            public Builder chunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; return this; }

            public Source build() {
                return new Source(documentId, fileName, pageNumber, chunkIndex);
            }
        }
    }

    public static class Builder {
        private String answer;
        private List<Source> sources;
        private int relevantChunks;

        public Builder answer(String answer) { this.answer = answer; return this; }
        public Builder sources(List<Source> sources) { this.sources = sources; return this; }
        public Builder relevantChunks(int relevantChunks) { this.relevantChunks = relevantChunks; return this; }

        public RagResponse build() {
            return new RagResponse(answer, sources, relevantChunks);
        }
    }
}
