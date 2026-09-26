# AGENTS.md — code-reviewer

## Project Overview

A GitHub App/service that performs automated code review on pull requests. It receives PR
webhooks (or REST triggers), fetches the PR diff from the GitHub API, runs review rules over
the added lines, persists findings in PostgreSQL, and posts them back to the PR as a review.
Dry-run mode (`github.enabled=false`) analyzes and persists without posting.

## Build and Test

- Maven, Java 21 (pinned via `<java.version>` in `pom.xml`), Spring Boot 3.5.x.
- Commands:
  - Build: `mvn clean package`
  - Tests: `mvn test`
  - Single test class: `mvn test -Dtest=DiffParserTest`
  - Run locally: `mvn spring-boot:run` (needs PostgreSQL: `docker compose up -d`)
- Tests are plain JUnit 5 + Mockito + AssertJ + standalone MockMvc — no Spring context and
  no database. Keep unit tests that way; do not add `@SpringBootTest` without Testcontainers.

## Technology Stack

Spring Boot (web, JPA, validation), JPA/Hibernate over PostgreSQL, Lombok, JUnit 5 +
Mockito + AssertJ (from `spring-boot-starter-test`).

## Architecture — Clean Architecture

Package root `com.codereviewer`, mirrored `src/test/java` tree.

- `application/` — use cases and boundary adapters.
  - `web/` — REST controllers (`ReviewController`, `WebhookController`) and
    `GlobalExceptionHandler`; the only layer that talks HTTP.
  - `dto/` — request/response records crossing the web boundary. Never expose JPA entities;
    map to DTOs.
  - `service/` — application services (`ReviewService`, `WebhookService`) orchestrating
    fetch → parse → analyze → persist → post.
  - `config/` — `DomainConfig` registers rules and the `ReviewEngine` as beans so the domain
    stays framework-free.
- `domain/` — pure business logic; no Spring imports (Jakarta persistence annotations on
  entities are the only exception).
  - `models/` — `Finding`, `Severity`, `DiffSide`, `DiffFile`/`DiffHunk`/`DiffLine`, JPA
    entities `Review`/`ReviewFinding`, `ReviewStatus`.
  - `service/` — `Rule` interface, `AbstractLineRule`, `DiffParser`, `ReviewEngine`, and
    rule implementations in `rules/`.
- `infrastructure/` — technical adapters.
  - `repository/` — Spring Data JPA interfaces (`ReviewRepository`, `ReviewFindingRepository`).
  - `github/` — `GitHubApiClient` (RestClient), `GitHubProperties`, GitHub DTO records,
    `GitHubApiException`. Nothing outside `infrastructure` may import GitHub API types.

Dependency direction: `application` → `domain` and `application` → `infrastructure`.
`domain` imports nothing from the other layers.

## Conventions

- One review rule = one stateless class extending `AbstractLineRule` (or implementing `Rule`
  for whole-file checks), registered as a `@Bean` in `DomainConfig`, with a unit test.
- Findings messages are user-facing: clear, actionable, English, no emoji.
- Records for immutable data (findings, DTOs, diff model); Lombok for entities and services.
- No `System.out` — SLF4J via Lombok `@Slf4j`.
- GitHub API failures degrade gracefully: findings are always persisted first; a failed
  submission marks the review `PARTIAL` instead of throwing.

## Configuration

`application.yml` — datasource (PostgreSQL), `github.token` (from `GITHUB_TOKEN` env, never
logged), `github.enabled` (dry-run switch), `github.api-base-url` (GitHub Enterprise).

## Pitfalls

- GitHub review comments address blob lines via `line` + `side`, not diff positions.
  `DiffParser` tracks new-file line numbers for additions — the mapping is centralized and
  tested there.
- Binary files and pure renames have no `patch` in the PR files response; `DiffParser`
  returns `Optional.empty()` — never NPE on `patch == null`.
- Webhook payloads are large and may contain secrets: model only needed fields
  (`@JsonIgnoreProperties(ignoreUnknown = true)`), never log raw payloads.
- Never log or persist the GitHub token.
- Lombok on Java 21 needs `annotationProcessorPaths` on `maven-compiler-plugin` (configured
  in `pom.xml`); "cannot find symbol" on getters usually means it is missing.
- `Review.findings` is EAGER by design: DTO mapping happens outside transactions, and a
  review is always rendered together with its findings.
