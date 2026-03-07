# GitHub User Activity CLI

[![CI](https://github.com/simple85/Github-User-Activity-CLI/actions/workflows/ci.yml/badge.svg)](https://github.com/simple85/Github-User-Activity-CLI/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A small command-line tool that fetches a GitHub user's recent public activity from the [GitHub REST API](https://docs.github.com/en/rest/activity/events) and prints it as a readable feed.

Built as a solution to the [roadmap.sh GitHub User Activity](https://roadmap.sh/projects/github-user-activity) project.

## Features

- Fetches the latest public events for any GitHub username
- Human-readable summaries for pushes, issues, stars, forks and branch/tag creation
- Graceful fallback for every other event type
- Clear error messages for unknown users, rate limits and network failures
- No API token required

## Tech Stack

| Area | Tools |
|------|-------|
| Language | Java 21 (`java.net.http.HttpClient`) |
| Build | Maven, Maven Shade (runnable fat jar) |
| JSON | [org.json](https://github.com/stleary/JSON-java) |
| Testing | JUnit 5 (incl. parameterized tests), AssertJ, Mockito, JaCoCo |
| CI | GitHub Actions |

## Getting Started

**Prerequisites:** JDK 21+ and Maven 3.9+

```bash
git clone https://github.com/simple85/Github-User-Activity-CLI.git
cd Github-User-Activity-CLI
mvn package
```

## Usage

```bash
java -jar target/github-activity.jar <github-username>
```

### Example

```text
$ java -jar target/github-activity.jar simple85
- Pushed 3 commits to simple85/Task-Tracker-CLI
- Opened an issue in simple85/Weather-App
- Starred spring-projects/spring-boot
- Created branch in simple85/Snake-Game
```

> Unauthenticated requests to the GitHub API are limited to 60 per hour per IP address.

## How It Works

```text
GithubActivityCLI  ──►  GithubActivity.fetchUserActivity()  ──►  GET /users/{username}/events
                                    │
                                    ▼
                        GithubActivity.describeEvent()  ──►  "- Pushed 3 commits to …"
```

- `fetchUserActivity` is built on an injectable `HttpClient`, so the network layer is mocked in tests.
- `describeEvent` is a pure function that turns one event into one line of text.

## Running Tests

```bash
mvn verify
```

Tests never hit the real API: the HTTP client is mocked with Mockito, and event fixtures live in `src/test/resources`. The coverage report is written to `target/site/jacoco/index.html`.

## License

Released under the [MIT License](LICENSE).
