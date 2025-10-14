# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is "Kutu-Tipp", a prediction game (Tippspiel) for Swiss Cup gymnastics competitions. Users predict scores for
individual gymnasts on specific apparatus, earning points based on prediction accuracy.

### Technology Stack

- **Framework**: Spring Boot 3.5.6 with Vaadin 24.9.2 (Flow)
- **Language**: Java 25
- **Database**: PostgreSQL 16.1
- **Database Access**: jOOQ 3.20.7
- **Database Migrations**: Flyway
- **Testing**: Testcontainers for integration tests
- **Build Tool**: Maven

## Build and Development Commands

### Running the Application

```bash
# Run in development mode (uses Testcontainers for PostgreSQL)
./mvnw spring-boot:run

# Or using the test application which includes Testcontainers configuration
./mvnw spring-boot:test-run
```

The application will automatically launch a browser at startup (configured via `vaadin.launch-browser=true`).

### Building

```bash
# Standard build
./mvnw clean install

# Production build with optimized Vaadin frontend
./mvnw clean install -Pproduction
```

### Testing

```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=KutuTippApplicationTests

# Run a single test method
./mvnw test -Dtest=KutuTippApplicationTests#contextLoads
```

### Database Code Generation

jOOQ code is generated from the database schema using Testcontainers and Flyway migrations:

```bash
# Generate jOOQ classes (runs automatically during build)
./mvnw generate-sources

# This will:
# 1. Start a PostgreSQL container (postgres:16.1)
# 2. Run Flyway migrations from src/main/resources/db/migration
# 3. Generate jOOQ classes in target/generated-sources/jooq
```

**Generated package**: `ch.martinelli.oss.registration.db` (note: this package name appears to be from a template and
should potentially be updated to `ch.martinelli.fun.kututipp.db`)

**Custom Generator**: Uses `ch.martinelli.oss.jooq.EqualsAndHashCodeJavaGenerator` from jooq-utilities to add
equals/hashCode methods to generated records.

## Architecture and Code Organization

### Package Structure

- `ch.martinelli.fun.kututipp` - Main application package
    - Expected subpackages (to be created):
        - `views` - Vaadin Flow views for UI
        - `services` - Business logic
        - `repositories` - jOOQ repository classes
        - `domain` - Domain models if needed

### Database Schema Design

Based on the domain concept (docs/1_Konzept.md), the system should have these main entities:

**Core Tables**:

- `Competition` - Competitions (Halbfinal, Final) with status (upcoming/live/finished)
- `Gymnast` - Gymnasts with name, team, gender
- `Apparatus` - Equipment (Reck, Boden, etc.) by gender
- `CompetitionEntry` - Links gymnasts to apparatus for specific competitions, stores actual scores
- `User` - Application users (tippers)
- `Prediction` - User predictions with points earned

**Flyway Migrations**: Create migration files in `src/main/resources/db/migration` with naming pattern
`V{version}__{description}.sql`

### Vaadin Flow Architecture

This project uses **Vaadin Flow** (Java-based views), not Hilla (React-based).

**Key Views to Implement**:

- Registration/Login view
- Competition overview (list of upcoming/live/finished competitions)
- Prediction entry view (grid for gymnast/apparatus combinations)
- Live results view
- Leaderboard view
- Admin view for data management

**Vaadin Components**: Use standard Vaadin Flow components (Grid, TextField, Button, VerticalLayout, etc.)

### Score Calculation Logic

The prediction scoring system (docs/1_Konzept.md):

- **Exact match**: 3 points
- **Within 5% deviation**: 2 points
- **Within 10% deviation**: 1 point
- **More than 10%**: 0 points

```java
public int calculatePoints(double predicted, double actual) {
    double difference = Math.abs(predicted - actual);
    double percentage = (difference / actual) * 100;

    if (difference < 0.001) return 3;  // Exact
    if (percentage <= 5) return 2;
    if (percentage <= 10) return 1;
    return 0;
}
```

## Development Environment

### Database Configuration

- **Development**: Uses Testcontainers automatically (configured in `TestcontainersConfiguration.java`)
- **Database Image**: `postgres:16.1`
- **Default Credentials**: username=`kututipp`, password=`secret`
- **Database Name**: `kututipp`

### Logging

jOOQ logging is set to DEBUG level to see generated SQL queries during development.

## Domain Context

### Swiss Cup Gymnastics

The Swiss Cup is a team-based gymnastics competition with:

- **Men's apparatus**: Floor, Pommel Horse, Rings, Vault, Parallel Bars, High Bar (6 total)
- **Women's apparatus**: Vault, Uneven Bars, Balance Beam, Floor (4 total)
- **Team formats**: Typically 5-4-3 or 6-5-4 (e.g., 5 gymnasts, 4 compete per apparatus, best 3 scores count)
- **Scoring**: D-score (difficulty) + E-score (execution) = total score

### Prediction Game Rules

- Users predict individual gymnast scores on specific apparatus
- Predictions close 30 minutes before competition starts
- Points are calculated based on prediction accuracy
- Live scoring updates during competitions
- Leaderboard shows overall and per-competition rankings

