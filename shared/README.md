# Shared Components

**Package:** `com.ems.shared`

## Purpose

Provide infrastructure and reusable UI components used by the application modules.

## Components

- `DatabaseConnection` — configured PostgreSQL connections.
- `UserSession` — the authenticated user's identity and role.
- `ValidationUtil` — shared input validation.
- `NotificationUtil` — modal messages and a thread-safe in-memory notification feed.
- `NotificationPanel` — reusable Swing view of recent notifications published through `NotificationUtil`.

Notifications remain in memory for the current login session; the database schema has no notifications table. `showInfo`, `showWarning`, and `showError` both publish to the feed and show their existing dialog. Other modules can publish a feed-only notification with `NotificationUtil.publish(level, message)`. The authentication flow calls `NotificationUtil.clearHistory()` on logout so the next user does not inherit the prior session's feed.

The application dashboard owns notification access control and adds `NotificationPanel` only for roles permitted by `AuthorizationService.Feature.NOTIFICATIONS`.
