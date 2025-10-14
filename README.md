# Kutu-Tipp

A prediction game (Tippspiel) for Swiss Cup gymnastics competitions. Users predict scores for individual gymnasts on
specific apparatus, earning points based on prediction accuracy.

## Technology Stack

- **Framework**: Spring Boot 3.5.6 with Vaadin 24.9.2 (Flow)
- **Language**: Java 25
- **Database**: PostgreSQL 16.1
- **Database Access**: jOOQ 3.20.7
- **Database Migrations**: Flyway
- **Testing**: Testcontainers for integration tests
- **Build Tool**: Maven

## Prerequisites

- Java 25
- Docker (required for Testcontainers)
- Maven 3.9+ (or use the included Maven wrapper `./mvnw`)

## Running the Application for Development

### Recommended: Run TestKutuTippApplication (with Testcontainers)

The easiest way to run the application during development is to use the `TestKutuTippApplication` class, which
automatically starts a PostgreSQL database using Testcontainers.

**From your IDE:**

Run the main method in:

```
src/test/java/ch/martinelli/fun/kututipp/TestKutuTippApplication.java
```

**From the command line:**

```bash
./mvnw spring-boot:test-run
```

This approach:

- Automatically starts a PostgreSQL 16.1 container
- Applies all Flyway migrations
- Launches the application with the browser automatically opening
- No manual database setup required

## Building the Application

### Standard Build

```bash
./mvnw package
```

### Production Build

```bash
./mvnw package -Pproduction
```

The production profile optimizes the Vaadin frontend for deployment.

## Testing

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=KutuTippApplicationTests

# Run a specific test method
./mvnw test -Dtest=KutuTippApplicationTests#contextLoads
```

## Database

The application uses PostgreSQL with jOOQ for type-safe database access and Flyway for migrations.

### Database Configuration (Development)

- **Database**: PostgreSQL 16.1 (via Testcontainers)
- **Username**: `kututipp`
- **Password**: `secret`
- **Database Name**: `kututipp`

### Migrations

Database migrations are located in `src/main/resources/db/migration/` and follow the Flyway naming convention:

```
V{version}__{description}.sql
```

### jOOQ Code Generation

jOOQ classes are automatically generated during the build process:

```bash
./mvnw generate-sources
```

This will:

1. Start a PostgreSQL container
2. Run Flyway migrations
3. Generate type-safe jOOQ classes in `target/generated-sources/jooq`

## Project Structure

```
src/
├── main/
│   ├── java/ch/martinelli/fun/kututipp/
│   │   ├── views/          # Vaadin Flow UI views
│   │   ├── services/       # Business logic
│   │   └── repositories/   # jOOQ repositories
│   └── resources/
│       └── db/migration/   # Flyway SQL migrations
└── test/
    └── java/ch/martinelli/fun/kututipp/
        └── TestKutuTippApplication.java  # Development entry point
```

## Domain Overview

Kutu-Tipp is a prediction game for Swiss Cup gymnastics competitions where:

- Users predict individual gymnast scores on specific apparatus
- Points are awarded based on prediction accuracy (0-3 points per prediction)
- Predictions close 30 minutes before competition starts
- Live scoring updates during competitions
- Leaderboards show rankings

### Scoring System

- **Exact match**: 3 points
- **Within 5% deviation**: 2 points
- **Within 10% deviation**: 1 point
- **More than 10% deviation**: 0 points

## Additional Documentation

For detailed development guidelines, architecture decisions, and domain knowledge, see:

- `CLAUDE.md` - Development guidance and conventions
- `docs/1_Konzept.md` - Domain concept and requirements (in German)

