# Authentication and Dashboard

**Owner:** Member 1  
**Package:** `com.ems.auth`  
**Primary table:** `users`

## Purpose

Provide the application's login, user administration, role-based access control, dashboard, and main navigation. This module is the integration point for the other four modules and coordinates shared application components.

## Responsibilities

- Authenticate users by username or email and password; store and verify password hashes, reject inactive accounts, and show useful error messages.
- Maintain and clear the logged-in user's `UserSession` on login and logout.
- Create, list, search, edit, activate/deactivate, and assign roles to user accounts. Enforce unique usernames and email addresses.
- Enforce `ADMIN`, `ORGANIZER`, and `STAFF` permissions in services as well as in navigation and UI.
- Build the dashboard with event, attendee, ticket, check-in, and recent/upcoming-event statistics.
- Maintain the single application entry point and login/dashboard navigation. Load the other modules' reusable Swing panels and pass the current session where needed.
- Coordinate shared components: `DatabaseConnection`, `UserSession`, `ValidationUtil`, and `NotificationUtil`.

## Required classes

- Model: `User`
- DAO: `UserDAO`
- Service: `AuthService`
- UI: `LoginFrame`, `DashboardFrame`, `DashboardPanel`, `UserManagementPanel`
- Application integration: `Main`

Keep SQL in DAOs and business rules in services. Use JDBC `PreparedStatement`s and try-with-resources. Do not manage records owned by other modules; dashboard queries may read their tables for aggregate statistics.

## Permissions

- **ADMIN:** control all modules, manage user accounts (create, edit, activate/deactivate, and assign roles), and perform staff duties.
- **ORGANIZER:** manage staff duties, events, attendees, vendors, rooms, tickets, check-in, reports, and notifications. User administration is reserved for ADMIN.
- **STAFF:** register attendees, validate tickets, check in guests, view assigned duties, and view notifications.

## Done when

- Login/logout, inactive-account rejection, and invalid-credential handling work.
- The initial account uses `EMS_ADMIN_ROLE` (`ADMIN`, `ORGANIZER`, or `STAFF`); it defaults to ADMIN.
- User accounts and roles can be managed without duplicate usernames or email addresses.
- Unauthorized users cannot access restricted service operations.
- Dashboard statistics load from PostgreSQL.
- The single application can load all five module panels.

## Integration notes

Coordinate shared API changes with every member. Module owners should expose reusable `JPanel`s and service methods; do not create separate application entry points or duplicate shared infrastructure.

Dashboard module slots resolve the README-named panel classes once they extend `JPanel`. The loader passes the current `UserSession` and can construct required public no-argument module services for panel constructors. Until a module exposes a compatible panel, its tab shows an availability message.
