package nl.surf.polynsi.soap;


import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class DataTypeAdapter {
    private DataTypeAdapter() {
    }

    public static OffsetDateTime parseDateTime(String s) {
        return s == null ? null : OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static String printDateTime(OffsetDateTime odt) {
        if (odt == null) {
            return null;
        } else {
            return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }
}

