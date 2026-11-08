
-- ============================================
-- EVENT MANAGEMENT SYSTEM
-- PostgreSQL Database Schema
-- ============================================

-- 1. USERS
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash TEXT NOT NULL,

    role VARCHAR(20) NOT NULL
        CHECK (role IN ('ADMIN','ORGANIZER', 'STAFF')),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_users_username_lower
    ON users (LOWER(username));

CREATE UNIQUE INDEX uq_users_email_lower
    ON users (LOWER(email));


-- 2. EVENTS
CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_name VARCHAR(150) NOT NULL,
    description TEXT,

    venue VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING'
        CHECK (
            status IN (
                'UPCOMING',
                'ONGOING',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_events_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_event_dates
        CHECK (ends_at > starts_at)
);

CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_starts_at ON events(starts_at);


-- 3. VENDORS
CREATE TABLE vendors (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL,

    vendor_name VARCHAR(150) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    contact_person VARCHAR(120),
    phone VARCHAR(30),
    email VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendors_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_vendors_event_id ON vendors(event_id);


-- 4. ATTENDEES
CREATE TABLE attendees (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL,

    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30),

    is_vip BOOLEAN NOT NULL DEFAULT FALSE,
    vip_level VARCHAR(50),
    special_requirements TEXT,

    registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_attendees_vip_details
        CHECK (
            (
                is_vip = TRUE
                AND vip_level IS NOT NULL
                AND LENGTH(BTRIM(vip_level)) > 0
            )
            OR
            (
                is_vip = FALSE
                AND vip_level IS NULL
                AND special_requirements IS NULL
            )
        ),

    CONSTRAINT fk_attendees_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE RESTRICT
);

-- An attendee cannot register for the same event twice,
-- even when the email uses different letter casing.
CREATE UNIQUE INDEX uq_attendees_event_email
    ON attendees (event_id, LOWER(email));

CREATE INDEX idx_attendees_event_id ON attendees(event_id);


-- 5. TICKETS
CREATE TABLE tickets (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    attendee_id BIGINT NOT NULL UNIQUE,
    ticket_number VARCHAR(40) NOT NULL UNIQUE,

    status VARCHAR(20) NOT NULL DEFAULT 'VALID'
        CHECK (
            status IN ('VALID', 'CHECKED_IN', 'CANCELLED')
        ),

    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_in_at TIMESTAMPTZ,
    checked_in_by BIGINT,

    CONSTRAINT fk_tickets_attendee
        FOREIGN KEY (attendee_id)
        REFERENCES attendees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tickets_checked_in_by
        FOREIGN KEY (checked_in_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ticket_checkin
        CHECK (
            (
                status = 'CHECKED_IN'
                AND checked_in_at IS NOT NULL
                AND checked_in_by IS NOT NULL
            )
            OR
            (
                status IN ('VALID', 'CANCELLED')
                AND checked_in_at IS NULL
                AND checked_in_by IS NULL
            )
        )
);

CREATE INDEX idx_tickets_status ON tickets(status);


-- 6. ROOMS
CREATE TABLE rooms (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    room_name VARCHAR(100) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity > 0),

    event_id BIGINT UNIQUE,

    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (
            status IN ('AVAILABLE', 'ASSIGNED', 'MAINTENANCE')
        ),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rooms_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_room_assignment
        CHECK (
            (status = 'ASSIGNED' AND event_id IS NOT NULL)
            OR
            (
                status IN ('AVAILABLE', 'MAINTENANCE')
                AND event_id IS NULL
            )
        )
);

CREATE INDEX idx_rooms_event_id ON rooms(event_id);


-- ============================================
-- ROOM OCCUPANCY VIEW
-- Occupancy is calculated from actual check-ins.
-- It is not stored as a separate database value.
-- ============================================

CREATE OR REPLACE VIEW room_occupancy AS
SELECT
    r.id AS room_id,
    r.room_name,
    r.capacity,
    r.status AS room_status,

    e.id AS event_id,
    e.event_name,

    COUNT(t.id) FILTER (
        WHERE t.status = 'CHECKED_IN'
    )::INTEGER AS occupied,

    GREATEST(
        r.capacity - COUNT(t.id) FILTER (
            WHERE t.status = 'CHECKED_IN'
        ),
        0
    )::INTEGER AS remaining_capacity,

    CASE
        WHEN COUNT(t.id) FILTER (
            WHERE t.status = 'CHECKED_IN'
        ) >= r.capacity
            THEN 'FULL'

        WHEN COUNT(t.id) FILTER (
            WHERE t.status = 'CHECKED_IN'
        ) >= CEIL(r.capacity * 0.8)
            THEN 'NEAR_CAPACITY'

        ELSE 'AVAILABLE'
    END AS occupancy_status

FROM rooms r

LEFT JOIN events e
    ON r.event_id = e.id

LEFT JOIN attendees a
    ON a.event_id = e.id

LEFT JOIN tickets t
    ON t.attendee_id = a.id

GROUP BY
    r.id,
    r.room_name,
    r.capacity,
    r.status,
    e.id,
    e.event_name;


-- STAFF_ASSIGNMENT: event and/or room duties assigned to staff.
CREATE TABLE staff_assignment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    event_id BIGINT,
    room_id BIGINT,
    duty VARCHAR(500) NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_staff_assignment_staff
        FOREIGN KEY (staff_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_staff_assignment_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_staff_assignment_room
        FOREIGN KEY (room_id)
        REFERENCES rooms(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_staff_assignment_organizer
        FOREIGN KEY (assigned_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_staff_assignment_target
        CHECK (event_id IS NOT NULL OR room_id IS NOT NULL)
);

CREATE INDEX idx_staff_assignment_staff_id ON staff_assignment(staff_id);
CREATE INDEX idx_staff_assignment_event_id ON staff_assignment(event_id);
CREATE INDEX idx_staff_assignment_room_id ON staff_assignment(room_id);


