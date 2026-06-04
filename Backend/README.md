# Personal Knowledge Base Backend

Spring Boot backend for the personal knowledge base assistant.

## Setup

1. Create a MySQL database named `knowledge_base`.
2. Run `src/main/resources/schema.sql`.
3. Update `src/main/resources/application.yml` with your MySQL, Redis, and DeepSeek settings.
4. Start the app:

```bash
mvn spring-boot:run
```

## Main APIs

- `POST /api/documents/upload`
- `GET /api/documents`
- `POST /api/search`
- `POST /api/chat`
- `POST /api/review/summary`
- `POST /api/papers/recommend`
