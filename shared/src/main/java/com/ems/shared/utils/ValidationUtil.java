package com.ems.shared.utils;


import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email)
                && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isPositiveNumber(int number) {
        return number > 0;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null
                && phone.matches("\\+?[0-9][0-9\\s()\\-]{6,19}");
    }
}
