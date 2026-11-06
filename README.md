# Event Management System

Java Swing desktop application with PostgreSQL persistence. The repository is a Maven reactor: each team module has its own `pom.xml` and can be built with its required internal dependencies.

## Modules

- `shared` — database connection, session, validation, and notifications.
- `auth-dashboard` — authentication, user administration, roles, dashboard, and main integration.
- `event-management` — events and vendors.
- `attendee-management` — attendee registration and its ticket-issuance integration.
- `ticket-checkin` — tickets, check-in, and attendance reports.
- `room-management` — room allocation, registration capacity, and occupancy.

Read the module README before making changes. Keep each module's code under `src/main/java/com/ems/` in its assigned package. Shared modules use `com.ems.shared`.

## Requirements

- JDK 21
- Apache Maven 3.9 or newer
- PostgreSQL

## Build

From the repository root:

```powershell
mvn clean verify
```

Build one module and its required modules with, for example:

```powershell
mvn -pl event-management -am verify
```

The modules currently provide buildable class scaffolding. Their application features and tests are still to be implemented by module owners.

## Database configuration

Copy `.env.example` to `.env` and set local PostgreSQL credentials. `.env` is ignored by Git and must not be committed. The database URL defaults to `jdbc:postgresql://localhost:5432/event_management_system` if `EMS_DB_URL` is omitted. Apply `database/schema.sql` to the PostgreSQL database before using database features.

Use the shared `DatabaseConnection.getConnection()` for JDBC connections. Use `PreparedStatement` for SQL values and try-with-resources for JDBC resources.

Set `EMS_DB_SEED_FILE` to the SQL file to run when loading seed data. The initial account uses `EMS_ADMIN_FULL_NAME`, `EMS_ADMIN_USERNAME`, `EMS_ADMIN_EMAIL`, `EMS_ADMIN_PASSWORD`, and optionally `EMS_ADMIN_ROLE` in `.env`. `EMS_ADMIN_ROLE` accepts `ADMIN`, `ORGANIZER`, or `STAFF` and defaults to `ADMIN`. After applying the schema, run `com.ems.auth.service.DatabaseSeeder` explicitly; it applies the configured SQL file and creates or refreshes the configured account with a PBKDF2 password hash in one transaction. Re-running it updates an existing account with the same username, including its configured role.

From the project root, install the module dependencies and then run the seeder from its module (the `exec:java` goal must target `auth-dashboard`, not the root aggregator):

```bash
mvn "-Dmaven.repo.local=$PWD/.maven-repository" -pl auth-dashboard -am install -DskipTests
mvn "-Dmaven.repo.local=$PWD/.maven-repository" -f auth-dashboard/pom.xml exec:java -Dexec.mainClass=com.ems.auth.service.DatabaseSeeder
```

## Contribution workflow

Work on the assigned feature branch, build the affected module with its dependencies, and open a pull request to `main`. Coordinate public service APIs and shared-component changes with the other module owners before integration.
