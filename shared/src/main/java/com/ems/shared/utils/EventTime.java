package com.ems.shared.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Shared input and display rules for event times, which are entered in East Africa Time. */
public final class EventTime {

    public static final ZoneId ZONE = ZoneId.of("Africa/Nairobi");
    public static final String ZONE_LABEL = "East Africa Time (EAT, Africa/Nairobi)";

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm 'EAT (Africa/Nairobi)'");

    private EventTime() {
    }

    /** Parses an event form value as a Nairobi wall-clock time and returns its explicit offset. */
    public static OffsetDateTime parseInput(String value) {
        LocalDateTime localDateTime = LocalDateTime.parse(value, INPUT_FORMAT);
        return localDateTime.atZone(ZONE).toOffsetDateTime();
    }

    /** Formats an instant as an editable wall-clock value in Nairobi. */
    public static String formatInput(OffsetDateTime value) {
        return value == null ? "" : INPUT_FORMAT.format(value.atZoneSameInstant(ZONE));
    }

    /** Formats an instant for display in Nairobi with the timezone made explicit. */
    public static String formatDisplay(OffsetDateTime value) {
        return value == null ? "" : DISPLAY_FORMAT.format(value.atZoneSameInstant(ZONE));
    }
}
