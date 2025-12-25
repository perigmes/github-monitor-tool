# GitHub Workflow Monitor

A command-line tool written in Java that monitors GitHub Actions workflow runs in real-time. It reports new workflows, jobs, and steps with their status and timestamps.

## Features

- **Real-time Monitoring**: Polls the GitHub API to detect changes.
- **Granular Details**: Tracks Workflows, Jobs, and individual Steps.
- **State Persistence**: Remembers the last check time to handle restarts gracefully (reports missed events upon restart).
- **Graceful Shutdown**: Saves state automatically on `Ctrl+C`.

## Prerequisites

- Java 17 or higher
- A GitHub Personal Access Token (PAT) with `repo` scope.

## How to Build

Use the included Gradle wrapper to build the project:

```bash
# Linux/macOS
./gradlew build

# Windows
./gradlew.bat build

# command for run:
 ./gradlew run --args="<owner/repo> <token>"
