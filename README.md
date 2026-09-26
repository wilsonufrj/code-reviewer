# code-reviewer

A GitHub App/service that performs automated code review on pull requests. It receives PR
webhooks (or REST triggers), fetches the PR diff from the GitHub API, runs review rules over
the added lines, persists findings in PostgreSQL, and posts them back to the PR as a review.
Dry-run mode (`github.enabled=false`) analyzes and persists without posting.

## Requirements

- Java 21
- Maven 3.8+
- Docker (for PostgreSQL)

## Quick start

```bash
docker compose up -d          # PostgreSQL on localhost:5432
```

## Start the application

Run the Spring Boot application from the repository root:

```bash
mvn spring-boot:run

# Debug mode; attach the debugger to localhost:5005
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

The application starts on `http://localhost:8080`.

## API documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- Endpoint guide: [`doc/API.md`](doc/API.md)

## Configuration

| Property | Env var | Default | Description |
|----------|---------|---------|-------------|
| `github.api-base-url` | `GITHUB_API_BASE_URL` | `https://api.github.com` | GitHub Enterprise support |
| `github.token` | `GITHUB_TOKEN` | — | GitHub token; never logged |
| `github.enabled` | `GITHUB_REVIEWS_ENABLED` | `false` | Dry-run switch |

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/reviews` | Trigger a review: `{"owner": "...", "repo": "...", "pullRequestNumber": 42}` |
| `GET` | `/api/reviews/repositories/{owner}/{repo}` | List reviews of a repository |
| `GET` | `/api/reviews/repositories/{owner}/{repo}/pulls/{number}` | Latest review of a PR |
| `POST` | `/webhook` | GitHub webhook receiver (`pull_request` events) |
| `GET` | `/api/repository-info/repositories/{owner}/{repo}` | Repository metadata and contributors |
| `GET` | `/api/repository-info/authors/{username}` | Author profile and owned repositories |

## Review rules

| Rule id | Severity | Description |
|---------|----------|-------------|
| `tab-indentation` | MINOR | Tabs used for indentation |
| `debug-print` | MAJOR | `System.out`/`System.err`/`printStackTrace` left in code |
| `todo-marker` | INFO | TODO/FIXME markers |
| `trailing-whitespace` | MINOR | Trailing spaces/tabs |
| `line-length` | MINOR | Lines over 120 characters |

Add a rule by extending `AbstractLineRule` in `domain/service/rules/`, registering it as a
`@Bean` in `DomainConfig`, and adding a unit test.

## Development

```bash
mvn clean package   # build + tests
mvn test            # tests only
```

Architecture follows Clean Architecture — see `AGENTS.md` for the layer rules and conventions.
