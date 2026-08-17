# JWT + Spring AI — Personal Learning & Reference Notes

> **Purpose:** My hands-on reference for the concepts, implementation patterns, errors, and lessons I learned while building a Spring Boot application with JWT authentication and Spring AI.
>
> **Last updated:** 17-Aug-2026

---

## 1. Project Context

### Main stack

- Java / Spring Boot
- Spring Security
- JWT authentication
- JPA / Hibernate
- PostgreSQL
- Spring AI
- OpenAI chat model
- PostgreSQL + pgvector for vector search
- Reactor `Flux` for streaming
- REST APIs
- Maven
- Postman

### Learning progression

1. JWT authentication
2. Access token / refresh token flow
3. Getting the authenticated user from `Authentication`
4. Spring AI architecture
5. `ChatClient` and `ChatModel`
6. Common `ChatClient` configuration
7. System messages / prompt templates
8. `.st` / StringTemplate-based prompt files
9. Synchronous AI calls
10. Streaming with `Flux`
11. Advisors
12. Chat memory
13. Vector store
14. Embeddings
15. RAG
16. PostgreSQL + pgvector
17. Combining JWT user identity + AI memory/RAG

---

# PART 1 — JWT + SPRING SECURITY

## 2. What is JWT?

JWT = JSON Web Token.

It is a compact token used to carry claims between parties.

Typical JWT structure:

```text
xxxxx.yyyyy.zzzzz
```

Three parts:

```text
Header.Payload.Signature
```

Example payload:

```json
{
  "sub": "123",
  "username": "harshit",
  "roles": ["USER"],
  "iat": 1720000000,
  "exp": 1720003600
}
```

### Important claims

| Claim | Meaning |
|---|---|
| `sub` | Subject / user identity |
| `iat` | Issued-at time |
| `exp` | Expiration time |
| `iss` | Issuer |
| `aud` | Audience |
| custom claims | Application-specific information |

JWT is normally **signed**, not encrypted.

Therefore, do not put passwords or sensitive secrets inside the payload.

---

## 3. JWT Authentication Flow

### Login

```text
Client
  |
  | username + password
  v
POST /login
  |
  v
AuthenticationManager
  |
  v
UserDetailsService
  |
  v
Database
  |
  v
Credentials verified
  |
  v
Generate JWT
  |
  +---- access token
  |
  +---- refresh token
  |
  v
Client
```

### Subsequent request

```text
Client
  |
  | Authorization: Bearer <access-token>
  v
Spring Security Filter Chain
  |
  v
JWT validation
  |
  v
SecurityContext
  |
  v
Controller
  |
  v
Service
```

---

## 4. Access Token vs Refresh Token

### Access token

Used to access protected APIs.

Usually:

- short-lived
- sent with API requests
- contains user/authorization claims
- should have limited lifetime

Example:

```text
Access token lifetime = 15 minutes
```

### Refresh token

Used to obtain a new access token.

Usually:

- longer-lived
- not sent to every API
- stored more securely
- rotated/revoked depending on the design

Example:

```text
Refresh token lifetime = 7 days
```

### Refresh flow

```text
Access token expires
        |
        v
Frontend detects 401 / expiration
        |
        v
POST /auth/refresh
        |
        | refresh token
        v
Backend validates refresh token
        |
        v
Generate new access token
        |
        v
Frontend retries original request
```

### Important point

The frontend does **not** need to call the refresh API for every request.

It normally refreshes when the access token is expired or about to expire.

---

## 5. JWT Authentication Filter

Typical custom filter responsibility:

```text
1. Read Authorization header
2. Check "Bearer "
3. Extract token
4. Validate token
5. Extract username/user ID
6. Create Authentication
7. Put Authentication into SecurityContext
8. Continue filter chain
```

Typical structure:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            // validate token
            // extract user identity
            // create Authentication

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Key principle

Do not manually authenticate the user inside every controller.

The filter establishes the authenticated identity once, and Spring Security makes it available downstream.

---

# 6. Getting User ID from Authentication

A useful pattern in Spring applications is:

```java
Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();
```

Then:

```java
String username = authentication.getName();
```

If your JWT stores user ID as the subject:

```java
String userId = authentication.getName();
```

If you put a custom claim such as:

```json
{
  "sub": "harshit",
  "userId": 123
}
```

then the exact extraction depends on how the JWT is converted into the `Authentication` object.

### Better design

Instead of repeatedly parsing JWT manually in controllers, create a small authenticated-user helper/service.

Example:

```java
@Component
public class CurrentUserService {

    public String getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication.getName();
    }
}
```

Then:

```java
String userId = currentUserService.getCurrentUser();
```

This keeps security-specific code out of business logic.

---

# 7. SecurityContext

The `SecurityContext` holds the current authenticated identity.

Conceptually:

```text
Request
  |
  v
JWT Filter
  |
  v
Authentication
  |
  v
SecurityContextHolder
  |
  v
Controller / Service
```

For JWT-based stateless APIs, the server normally does not keep a server-side login session.

---

# 8. Stateless Authentication

JWT APIs are commonly configured as stateless:

```java
http
    .sessionManagement(session ->
        session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        )
    );
```

Meaning:

```text
Request 1 -> JWT -> authenticate
Request 2 -> JWT -> authenticate
Request 3 -> JWT -> authenticate
```

---

# PART 2 — SPRING AI

# 9. Spring AI Architecture

The important mental model is:

```text
Application
    |
    v
ChatClient
    |
    v
ChatModel
    |
    v
AI Provider
    |
    v
LLM
```

### Main components

| Component | Responsibility |
|---|---|
| `ChatClient` | High-level fluent API |
| `ChatModel` | Model abstraction |
| `ChatResponse` | Model response |
| `Prompt` | Input sent to model |
| `Message` | System/user/assistant message |
| `Advisor` | Intercepts/enriches AI calls |
| `ChatMemory` | Conversation memory |
| `VectorStore` | Stores/searches embeddings |
| `EmbeddingModel` | Converts text into vectors |
| RAG | Retrieves relevant data before generation |

---

# 10. ChatClient vs ChatModel

## ChatModel

Lower-level abstraction.

```text
ChatModel -> send prompt -> model -> response
```

## ChatClient

Higher-level fluent API.

Example:

```java
String response = chatClient
        .prompt()
        .user("Explain JWT")
        .call()
        .content();
```

Think:

```text
ChatModel = model interaction abstraction

ChatClient = developer-friendly application API
```

---

# 11. Common ChatClient Configuration

A clean project should avoid creating unrelated `ChatClient` instances everywhere.

Example:

```java
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .build();
    }
}
```

Then inject it:

```java
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
```

### Why centralize configuration?

Because later you may add:

- default system prompt
- default advisors
- memory
- RAG
- logging
- model options
- tools
- observability

without duplicating configuration.

---

# 12. System Message

A system message defines the model's behavior/instructions.

Example:

```java
chatClient.prompt()
    .system("""
        You are a Java Spring Boot expert.
        Explain concepts with practical examples.
        """)
    .user("Explain JWT")
    .call()
    .content();
```

### System vs User

**System message** defines:

- role
- rules
- style
- constraints
- behavior

**User message** contains the actual request.

---

# 13. External `.st` Prompt File

For maintainability, prompts should not necessarily be hard-coded in Java.

Example:

```text
src/main/resources/prompts/system.st
```

Example:

```text
You are a helpful Spring Boot assistant.

Rules:
- Explain concepts clearly.
- Prefer practical Java examples.
- Mention important production considerations.
- If the information is unavailable, say so.
```

Architecture:

```text
system.st
   |
   v
Template / Resource Loader
   |
   v
System message
   |
   v
ChatClient
   |
   v
LLM
```

Spring AI supports StringTemplate-style rendering through `StTemplateRenderer` for parameterized prompt templates.

---

# 14. Simple ChatClient Call

```java
public String chat(String message) {

    return chatClient
            .prompt()
            .user(message)
            .call()
            .content();
}
```

Flow:

```text
Controller
    |
    v
Service
    |
    v
ChatClient
    |
    v
ChatModel
    |
    v
OpenAI
    |
    v
Response
```

---

# 15. Streaming with Flux

Normal call:

```java
String response = chatClient
        .prompt()
        .user(message)
        .call()
        .content();
```

Streaming:

```java
Flux<String> response = chatClient
        .prompt()
        .user(message)
        .stream()
        .content();
```

Conceptually:

```text
LLM
 |
 +--> "Hello"
 |
 +--> " Harshit"
 |
 +--> " how"
 |
 +--> " can"
 |
 +--> " I"
 |
 +--> " help?"
```

### Controller

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String message) {
    return chatService.stream(message);
}
```

### Service

```java
public Flux<String> stream(String message) {
    return chatClient
            .prompt()
            .user(message)
            .stream()
            .content();
}
```

### Why streaming?

Useful for:

- chat applications
- AI assistants
- long responses
- better perceived latency
- real-time UI rendering

---

# PART 3 — ADVISORS

# 16. What is an Advisor?

An Advisor is middleware around an AI request.

Think:

```text
Application
    |
    v
Advisor 1
    |
    v
Advisor 2
    |
    v
Advisor 3
    |
    v
ChatModel
    |
    v
Response
```

An advisor can:

- modify prompts
- add memory
- retrieve documents
- log requests
- validate output
- perform tool calling
- implement custom AI behavior

---

# 17. Advisor Chain

Example:

```java
chatClient.prompt()
    .advisors(advisor1, advisor2, advisor3)
    .user(message)
    .call()
    .content();
```

Order matters.

Example:

```text
Memory Advisor
      |
      v
RAG Advisor
      |
      v
Logging Advisor
      |
      v
LLM
```

The output/context from one advisor can affect the next advisor.

---

# 18. Important Advisors to Learn

- `MessageChatMemoryAdvisor`
- `VectorStoreChatMemoryAdvisor`
- `QuestionAnswerAdvisor`
- `RetrievalAugmentationAdvisor`
- `SimpleLoggerAdvisor`
- `ToolCallingAdvisor`
- `StructuredOutputValidationAdvisor`
- custom advisors

---

# PART 4 — CHAT MEMORY

# 19. Why Chat Memory?

LLMs are fundamentally stateless per request.

If you send:

```text
Request 1:
My name is Harshit.
```

and then:

```text
Request 2:
What is my name?
```

the model does not automatically know Request 1 unless previous conversation context is supplied.

---

# 20. Memory Flow

```text
User
 |
 v
ChatClient
 |
 v
Memory Advisor
 |
 +--> load previous messages
 |
 v
LLM
 |
 v
Response
 |
 v
Memory Store
```

Example:

```java
chatClient.prompt()
    .advisors(advisor -> advisor
        .param(
            ChatMemory.CONVERSATION_ID,
            conversationId
        ))
    .user(message)
    .call()
    .content();
```

The conversation ID identifies which conversation's history should be retrieved.

---

# 21. Memory vs Database

Do not confuse:

```text
Chat Memory
```

with:

```text
Business Database
```

Chat memory answers:

> What did the user and assistant say previously in this conversation?

Business DB answers:

> What is the user's actual application data?

Example:

```text
Chat memory:
User: My favorite language is Java.

Database:
user_id = 123
email = ...
subscription = ...
```

---

# PART 5 — VECTOR DATABASE

# 22. What is an Embedding?

An embedding converts text into a numerical vector.

```text
"Spring Boot is a Java framework"
            |
            v
       Embedding Model
            |
            v
[0.12, -0.42, 0.91, ...]
```

Semantically similar text tends to have vectors that are close according to a similarity measure.

---

# 23. Vector Store

A vector store stores:

```text
Document
   +
Embedding
   +
Metadata
```

Example:

```text
Document:
"JWT is a token-based authentication mechanism."

Embedding:
[0.12, 0.54, -0.31, ...]

Metadata:
{
  "source": "security.md",
  "topic": "jwt"
}
```

---

# 24. PostgreSQL + pgvector

Your project uses PostgreSQL with vector support.

Conceptually:

```text
Application
    |
    v
Spring AI VectorStore
    |
    v
PostgreSQL
    |
    v
pgvector
    |
    v
vector column
```

PostgreSQL handles normal relational data while pgvector adds vector similarity search capability.

---

# 25. RAG

RAG = Retrieval-Augmented Generation.

Instead of:

```text
User Question
      |
      v
     LLM
      |
      v
   Answer
```

RAG does:

```text
User Question
      |
      v
Create query embedding
      |
      v
Vector Store
      |
      v
Similarity Search
      |
      v
Relevant Documents
      |
      v
Add context to prompt
      |
      v
LLM
      |
      v
Grounded Answer
```

---

# 26. Why RAG?

An LLM may be:

- outdated
- incomplete
- unaware of private company data
- unable to access internal documents

RAG gives the model relevant external context at request time.

---

# 27. RAG Ingestion Pipeline

Most important ingestion pipeline:

```text
Document
   |
   v
Document Reader
   |
   v
Text Splitter
   |
   v
Chunks
   |
   v
Embedding Model
   |
   v
Vectors
   |
   v
VectorStore
```

Example:

```text
100-page PDF
     |
     v
Document
     |
     v
Split into chunks
     |
     v
Embeddings
     |
     v
pgvector
```

---

# 28. RAG Query Pipeline

```text
Question
   |
   v
Embedding
   |
   v
Vector similarity search
   |
   v
Top-K documents
   |
   v
Prompt + retrieved context
   |
   v
LLM
   |
   v
Answer
```

---

# 29. QuestionAnswerAdvisor

A straightforward RAG approach is:

```java
QuestionAnswerAdvisor.builder(vectorStore).build()
```

Then:

```java
String response = chatClient
        .prompt()
        .advisors(
            QuestionAnswerAdvisor
                .builder(vectorStore)
                .build()
        )
        .user(question)
        .call()
        .content();
```

The advisor performs vector retrieval and adds retrieved context to the model request.

---

# 30. Important Spring AI Version Note

Spring AI has evolved.

For Spring AI 2.0, the vector-store advisor dependency was renamed:

```text
spring-ai-advisors-vector-store
```

to:

```text
spring-ai-vector-store-advisor
```

Also, `QuestionAnswerAdvisor` has **not simply disappeared** from the current Spring AI API. Current API documentation still contains it.

Spring AI also provides:

```text
RetrievalAugmentationAdvisor
```

for more modular RAG architectures.

When older code does not compile, check:

1. Spring AI version
2. dependency name
3. package name
4. migration/upgrade notes
5. current API documentation

---

# PART 6 — MEMORY + RAG TOGETHER

# 31. Combining Memory and RAG

You can have:

```text
User
 |
 v
ChatClient
 |
 +--> Memory Advisor
 |
 +--> RAG Advisor
 |
 +--> Logger Advisor
 |
 v
LLM
```

Example:

```java
chatClient.prompt()
    .advisors(advisor -> advisor
        .advisors(
            memoryAdvisor,
            ragAdvisor,
            loggerAdvisor
        )
        .param(
            ChatMemory.CONVERSATION_ID,
            conversationId
        ))
    .user(message)
    .call()
    .content();
```

### Difference

Memory:

```text
"What did we discuss?"
```

RAG:

```text
"What relevant information exists in my knowledge base?"
```

Together:

```text
Conversation context
        +
Knowledge-base context
        +
Current question
        |
        v
       LLM
```

---

# 32. Memory + RAG + JWT

JWT gives:

```text
Authenticated User
       |
       v
userId
```

Use that identity to associate conversations and/or user-specific data.

Example:

```text
JWT
 |
 v
Authentication
 |
 v
userId
 |
 +--------------------+
 |                    |
 v                    v
Conversation ID      User-specific data
 |                    |
 v                    v
Chat Memory          Vector metadata
 |
 +----------+
            |
            v
         ChatClient
            |
            v
       Memory + RAG
            |
            v
           LLM
```

Example business flow:

```text
POST /ai/chat
Authorization: Bearer <JWT>

        |
        v

Extract authenticated user ID

        |
        v

Create/resolve conversation

        |
        v

Call ChatClient

        |
        +--> memory for conversation
        |
        +--> vector search for knowledge
        |
        v

Generate answer
```

This creates a clean bridge between an existing Spring Security application and Spring AI.

---

# PART 7 — COMMON PROBLEMS

# 33. pgvector Index / SQL Error

When using PostgreSQL + pgvector, Spring AI may execute SQL for vector-store schema/index creation.

If you see an error similar to:

```text
StatementCallback:
bad SQL grammar

CREATE INDEX IF NOT EXISTS spring_ai_vector...
```

check:

```text
1. PostgreSQL version
2. pgvector extension
3. vector column type
4. vector dimension
5. index configuration
6. Spring AI version
7. generated SQL
8. database permissions
```

Useful PostgreSQL check:

```sql
SELECT * FROM pg_extension
WHERE extname = 'vector';
```

---

# 34. `QuestionAnswerAdvisor` Not Found

Do not immediately assume the class was removed.

Check the Spring AI version.

Current Spring AI documentation still exposes:

```java
QuestionAnswerAdvisor
```

and current API documentation lists it under:

```text
org.springframework.ai.chat.client.advisor.vectorstore
```

The vector-store advisor dependency/module naming changed in Spring AI 2.0.

---

# 35. `ChatClient` Options / Temperature Error

If code such as:

```java
.options(
    ChatOptions.builder()
        .temperature(0.7)
        .build()
)
```

does not compile, do not blindly copy code from an older Spring AI tutorial.

Model-specific options and API signatures can change between versions.

Use the API that matches your exact Spring AI version.

Rule:

```text
Tutorial version
       !=
Your project version
```

Always verify imports and builder methods against your dependency version.

---

# 36. Spring AI Debugging Checklist

```text
[ ] Spring Boot version
[ ] Spring AI version
[ ] Java version
[ ] Dependency names
[ ] Package imports
[ ] Model configuration
[ ] API key configuration
[ ] ChatClient bean
[ ] VectorStore bean
[ ] Embedding model
[ ] PostgreSQL extension
[ ] pgvector version
[ ] vector dimensions
[ ] advisor dependency
[ ] advisor package
[ ] conversation ID
```

---

# PART 8 — IMPORTANT MENTAL MODELS

# 37. ChatClient Mental Model

```text
ChatClient
   |
   +-- prompt
   |     |
   |     +-- system
   |     +-- user
   |     +-- messages
   |
   +-- advisors
   |
   +-- tools
   |
   +-- call()
   |
   +-- stream()
```

---

# 38. Advisor Mental Model

Think:

```text
Advisor = interceptor/middleware for AI
```

Example:

```text
Request
  |
  v
Memory Advisor
  |
  v
RAG Advisor
  |
  v
Tool Advisor
  |
  v
LLM
  |
  v
Response
```

---

# 39. Memory Mental Model

Think:

```text
Memory = previous conversation
```

Not:

```text
Memory = model training
```

The model is not being retrained.

Previous messages are retrieved and supplied as context.

---

# 40. RAG Mental Model

Think:

```text
RAG = Retrieve first, Generate second
```

```text
Question
   |
   v
Retrieve relevant information
   |
   v
Put information into prompt
   |
   v
Generate answer
```

---

# 41. Vector DB Mental Model

Think:

```text
Traditional DB:
"Find rows matching conditions"

Vector DB:
"Find content semantically similar to this query"
```

---

# 42. JWT + AI Mental Model

JWT identifies:

```text
WHO is the user?
```

Memory identifies:

```text
WHAT did this user discuss?
```

RAG identifies:

```text
WHAT relevant knowledge do we have?
```

LLM generates:

```text
WHAT should we answer?
```

Combined:

```text
JWT
 |
 | identity
 v
User
 |
 +----> Conversation Memory
 |
 +----> User/tenant metadata
 |
 +----> RAG filtering
 |
 v
ChatClient
 |
 +----> Advisors
 |
 v
LLM
```

---

# PART 9 — INTERVIEW EXPLANATIONS

# 43. Explain JWT in an Interview

> JWT is a signed token used for stateless authentication. After successful login, the server issues an access token containing claims such as the subject and expiration time. The client sends the token with subsequent requests using the Authorization Bearer header. A security filter validates the token and creates an Authentication object that is stored in the SecurityContext. The application can then access the authenticated user's identity without maintaining an HTTP session.

---

# 44. Explain Spring AI ChatClient

> ChatClient is Spring AI's high-level fluent API for interacting with chat models. It allows us to build prompts using system and user messages, configure advisors and tools, and perform either synchronous calls or streaming calls using Flux. Internally, ChatClient communicates with a ChatModel implementation that integrates with the actual AI provider.

---

# 45. Explain Advisor

> An Advisor acts like middleware around an AI request. It can inspect or modify the request before it reaches the model and can process the response afterward. Spring AI provides advisors for memory, RAG, logging, tool calling, structured output and other reusable AI patterns. Advisors are executed in a defined order, so the chain order matters.

---

# 46. Explain RAG

> RAG stands for Retrieval-Augmented Generation. Instead of relying only on the model's trained knowledge, we first retrieve relevant information from an external knowledge base, typically using embeddings and vector similarity search. The retrieved content is added to the prompt as context, and then the LLM generates an answer based on that context.

---

# 47. Explain Memory vs RAG

A strong interview answer:

> Memory and RAG solve different problems. Memory maintains conversational context, such as what the user and assistant discussed earlier. RAG retrieves external knowledge relevant to the current question. Memory answers "what did we discuss?", while RAG answers "what relevant information exists in the knowledge base?"

---

# PART 10 — COMPLETE TARGET ARCHITECTURE

# 48. Target Spring Boot + JWT + Spring AI Architecture

```text
                         CLIENT
                           |
                           | JWT
                           v
                 +-------------------+
                 | Spring Security   |
                 | JWT Filter        |
                 +-------------------+
                           |
                           v
                    Authentication
                           |
                           v
                    +-------------+
                    | Controller  |
                    +-------------+
                           |
                           v
                    +-------------+
                    | AI Service  |
                    +-------------+
                           |
                           v
                    +-------------+
                    | ChatClient  |
                    +-------------+
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
        Memory         RAG Advisor    Tools
        Advisor             |             |
             |              v             |
             |          VectorStore      |
             |              |             |
             v              v             |
       Chat Memory      PostgreSQL        |
             |           pgvector         |
             +-------------+-------------+
                           |
                           v
                       ChatModel
                           |
                           v
                         OpenAI
```

---

# 49. Learning Roadmap From Here

The practical implementation order:

```text
1. JWT
   |
2. Current authenticated user
   |
3. ChatClient
   |
4. External system prompt
   |
5. Streaming Flux
   |
6. Advisors
   |
7. Chat Memory
   |
8. PostgreSQL-backed memory
   |
9. Embeddings
   |
10. PgVector VectorStore
   |
11. Document ingestion
   |
12. Chunking
   |
13. Similarity search
   |
14. QuestionAnswerAdvisor
   |
15. RetrievalAugmentationAdvisor
   |
16. Memory + RAG together
   |
17. JWT user isolation
   |
18. Metadata filtering
   |
19. Tool calling
   |
20. Structured output
   |
21. Observability
   |
22. Production hardening
```

---

# 50. Production Considerations

### Security

- Never expose OpenAI API keys
- Keep secrets outside source code
- Validate JWT expiration
- Validate refresh tokens
- Consider refresh-token rotation
- Authorize access to conversations
- Prevent users from accessing another user's memory

### RAG

- Use appropriate chunk sizes
- Store useful metadata
- Apply metadata filtering
- Tune top-K retrieval
- Evaluate retrieval quality
- Prevent prompt injection from retrieved documents
- Keep source references where possible

### AI

- Set reasonable token limits
- Handle model/API failures
- Add timeouts
- Handle rate limits
- Consider retries carefully
- Monitor token usage/cost
- Log safely without leaking sensitive prompts

### Database

- PostgreSQL backups
- pgvector compatibility
- Index strategy
- Connection pool sizing
- Vector dimension consistency

---

# 51. One-Page Revision

```text
JWT
 |
 +-- Login
 +-- Access Token
 +-- Refresh Token
 +-- JWT Filter
 +-- Authentication
 +-- SecurityContext
 +-- Stateless API


SPRING AI
 |
 +-- ChatClient
 |     +-- system
 |     +-- user
 |     +-- call()
 |     +-- stream()
 |
 +-- ChatModel
 |
 +-- Advisors
 |     +-- Memory
 |     +-- RAG
 |     +-- Logging
 |     +-- Tools
 |
 +-- ChatMemory
 |
 +-- Embeddings
 |
 +-- VectorStore
 |
 +-- RAG


RAG
 |
 +-- Load document
 +-- Split document
 +-- Generate embeddings
 +-- Store vectors
 +-- Embed query
 +-- Similarity search
 +-- Retrieve top-K
 +-- Add context
 +-- Generate answer


JWT + AI
 |
 +-- JWT -> user identity
 +-- user identity -> conversation
 +-- conversation -> memory
 +-- question -> vector search
 +-- memory + RAG -> ChatClient
 +-- ChatClient -> LLM
```

---

# 52. Quick Reference: Key Classes

```text
Spring Security
----------------
SecurityContextHolder
SecurityContext
Authentication
UserDetails
OncePerRequestFilter
SecurityFilterChain
AuthenticationManager
JwtAuthenticationToken


Spring AI
---------
ChatClient
ChatModel
ChatResponse
Prompt
Message

Advisors
--------
Advisor
CallAdvisor
StreamAdvisor
MessageChatMemoryAdvisor
VectorStoreChatMemoryAdvisor
QuestionAnswerAdvisor
RetrievalAugmentationAdvisor
SimpleLoggerAdvisor
ToolCallingAdvisor

Vector / RAG
------------
EmbeddingModel
VectorStore
Document
SearchRequest
```

---

# 53. Final Mental Picture

```text
                 USER
                   |
                   v
                 JWT
                   |
                   v
          Spring Security
                   |
                   v
          Authenticated User
                   |
                   v
              AI Service
                   |
                   v
              ChatClient
                   |
        +----------+----------+
        |          |          |
        v          v          v
      Memory      RAG       Tools
        |          |          |
        v          v          v
    History     pgvector   Application APIs
        |          |
        +-----+----+
              |
              v
           ChatModel
              |
              v
             LLM
              |
              v
           Response
```

### Core concepts to remember

```text
JWT     = Who is the user?
Memory  = What did we discuss?
RAG     = What relevant knowledge do we have?
Advisor = What should happen around the AI call?
ChatClient = How does my application talk to the model?
VectorStore = Where do I search semantic knowledge?
Flux    = How do I stream the AI response?
```

---

## Official References

- Spring AI ChatClient — https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Spring AI Advisors — https://docs.spring.io/spring-ai/reference/api/advisors.html
- Spring AI RAG — https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/retrieval-augmented-generation.html
- Spring AI Upgrade Notes — https://docs.spring.io/spring-ai/reference/upgrade-notes.html
- Spring Security JWT API — https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/oauth2/server/resource/authentication/JwtAuthenticationToken.html

> **Note:** Spring AI APIs are evolving quickly. Before copying code from an older tutorial, verify the exact Spring AI version and its current package/dependency names.
