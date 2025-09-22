# AI Integration Notes

## Current Setup
- Spring Boot 3.5 + Kotlin project with Spring AI Ollama starter (BOM 1.0.0-M6).
- `AiChatService` uses the Ollama `ChatModel` bean (`ollamaChatModel`) and validates requests.
- OpenAI starter dependency removed until GPT integration is ready.

## Outstanding Tasks
- Install JDK 17+ locally to allow Gradle toolchain resolution.
- Verify `/api/chat` endpoint after starting Ollama and running the app.
- Reintroduce OpenAI starter once API key and provider branching logic are finalised.

## Future Enhancements (Discussion)
- Introduce Retrieval-Augmented Generation for domain-specific answers.
  - Use Spring AI `VectorStore` abstraction; shortlisted option: Qdrant (open-source, metadata filters, hybrid search).
  - Alternative stores: pgvector, Milvus, Weaviate — pick based on operational stack.
- Implement ingestion pipeline:
  - Chunk domain documents (≈500–800 tokens) with metadata (source, category).
  - Generate embeddings via Ollama (e.g. `nomic-embed-text`) or OpenAI `text-embedding-3-large`.
  - Store vectors + payloads in Qdrant collection; include filters for ACL if needed.
- Build RAG flow:
  - Question → embed → retrieve top docs → craft prompt (system + retrieved context) → call LLM.
  - Expose metadata (source references) in final responses for traceability.
- Consider hybrid keyword + vector search as fallback when embeddings miss context.

## RAG 구현 메모 (Spring AI + Qdrant)
- **의존성**: `spring-ai-ollama-spring-boot-starter`, `spring-ai-qdrant-spring-boot-starter`, 선택적으로 임베딩 모델(`spring-ai-embeddings-ollama` 등)을 추가한다.
- **설정**: `application.yml`에 Qdrant 엔드포인트/토큰, 컬렉션 이름, 임베딩 모델을 명시한다. Spring AI가 자동으로 `VectorStore`, `EmbeddingModel` 빈을 올려준다.
- **인제스트 파이프라인**: 도메인 문서를 청크로 나누고(메타데이터 포함) 임베딩을 생성해 `VectorStore.add`로 Qdrant에 적재한다. 배치 작업이나 CLI로 주기적 실행을 구성한다.
- **검색 흐름**: 사용자의 질문 → 임베딩 생성 → `VectorStoreRetriever`로 상위 N개의 관련 청크 검색 → (필요 시) 1차 메타데이터 필터 → 검색된 payload를 프롬프트 템플릿에 삽입한다.
- **LLM 호출**: 조합된 프롬프트를 `ChatModel`에 전달하고, 응답에 근거(청크의 citation)를 함께 포함하도록 지시한다.
- **검증 & 모니터링**: 질의-답변 테스트 세트를 만들어 검색 정확도와 답변 품질을 정기적으로 측정하고, 근거 없는 답변은 로깅/차단한다.

## Operational Reminders
- When enabling OpenAI:
  1. Add dependency `org.springframework.ai:spring-ai-openai-spring-boot-starter`.
  2. Set `SPRING_AI_OPENAI_API_KEY` or `spring.ai.openai.api-key`.
  3. Update service layer to handle `Provider.OPENAI` branch.
- Keep IntelliJ HTTP script (`src/test/http/ai-chat.http`) updated as endpoints evolve.
