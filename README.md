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
| `github.pull-request-monitor.enabled` | `GITHUB_PR_MONITOR_ENABLED` | `false` | Enables PR polling |
| `github.pull-request-monitor.owner` | `GITHUB_PR_MONITOR_OWNER` | — | Repository owner to monitor |
| `github.pull-request-monitor.repository` | `GITHUB_PR_MONITOR_REPOSITORY` | — | Repository name to monitor |
| `github.pull-request-monitor.interval-ms` | `GITHUB_PR_MONITOR_INTERVAL_MS` | `60000` | Poll delay in milliseconds |

## Pull request monitor

Enable the scheduler for one repository before starting the application:

```bash
export GITHUB_PR_MONITOR_ENABLED=true
export GITHUB_PR_MONITOR_OWNER=octocat
export GITHUB_PR_MONITOR_REPOSITORY=hello-world
export GITHUB_PR_MONITOR_INTERVAL_MS=60000
mvn spring-boot:run
```

The first successful poll silently records the current open pull requests. Later polls print each
new pull request once:

```text
New open pull request: octocat/hello-world#42 | title="Add feature" | author=octocat | url=https://github.com/octocat/hello-world/pull/42
```

For every newly detected pull request, the application posts one or more Markdown comments listing
all changed files and their GitHub status (`added`, `modified`, `removed`, or `renamed`). Reports are
split into groups of 100 files. The GitHub token therefore needs pull-request read access and issue
write access.

The monitor checks the newest 100 open pull requests. Its in-memory baseline resets whenever the
application restarts. It reports and comments on new pull requests, but it does not trigger an
automated code review.

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
