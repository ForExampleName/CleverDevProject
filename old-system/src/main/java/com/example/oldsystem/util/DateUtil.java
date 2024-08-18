package com.example.oldsystem.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public final class DateUtil {
    private DateUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
