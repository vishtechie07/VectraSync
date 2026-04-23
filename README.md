# VectraSync

CRM-agnostic web app for CSV → CRM sync. An LLM ([LangChain4j](https://github.com/langchain4j/langchain4j)) proposes column mappings from live CRM schema discovery; you review, then run the sync. UI is [Vaadin 24](https://vaadin.com/) on [Spring Boot 3.3](https://spring.io/projects/spring-boot) with Java 21 virtual threads.

## Requirements

- JDK 21
- Maven 3.8+
- An API key for **OpenAI** (`sk-…`) or **Google Gemini** (`AIza…`) for mapping and trace streaming

## Run locally

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Vaadin regenerates `src/main/frontend/generated/` on build; it is gitignored—run Maven at least once after clone.

### Production build

Frontend bundles are built when the `production` Maven profile is active (see `pom.xml`). Use your usual Spring Boot packaging flow with that profile for deployable JARs.

## Configuration (UI)

All secrets are **session-only** (in-memory for your browser); they are not written to application properties or the server filesystem by this app.

| Setting | Purpose |
|--------|---------|
| **LLM API key** | Required for AI mapping and live trace. Provider is inferred from the key prefix (`sk-` → OpenAI `gpt-4o`, `AIza` → Gemini `gemini-1.5-flash`). |
| **Target CRM** | **Mock** — in-memory demo, no external CRM. **Attio** — live [Attio](https://attio.com/) API; paste an Attio API key from your workspace. |

Use **Settings** in the app to save keys before running sync on the home view.

## Workflow

1. **Upload** a CSV (up to 50 MB per Spring multipart defaults).
2. **Propose mapping** — the assistant calls tools to read CRM field schema and CSV headers/sample rows, then returns a JSON mapping list.
3. **Execute sync** — rows are mapped and upserted through the CRM client (rate-limited wrapper on outbound calls).

**History** lists past sync summaries stored in memory for the session.

## CRM notes

- **Attio**: client targets `https://api.attio.com/v2` with your API key. Integration tests use WireMock, not your real workspace.
- **Twenty**: a `TwentyClient` exists in the codebase but is not wired in the Settings UI; Attio and Mock are the selectable targets today.

## Tests and logs

```bash
mvn test
```

File logging defaults to `logs/vectrasync.log` (see `application.properties`). Console logging includes `com.vectrasync` at DEBUG.

## License

See [LICENSE](LICENSE) in this repository.
