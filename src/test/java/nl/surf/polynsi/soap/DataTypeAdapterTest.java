package nl.surf.polynsi.soap;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DataTypeAdapterTest {

    public static final String DT_WITHOUT_NANO_SEC = "2013-07-24T16:50:00+02:00";
    public static final String DT_WITH_NANO_SEC = "2013-07-24T16:50:00.333+02:00";


    @Test
    public void testParseDateTime() {
        OffsetDateTime odtWithout = DataTypeAdapter.parseDateTime(DT_WITHOUT_NANO_SEC);
        assertEquals(OffsetDateTime.parse(DT_WITHOUT_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME), odtWithout);

        OffsetDateTime odtWith = DataTypeAdapter.parseDateTime(DT_WITH_NANO_SEC);
        assertEquals(OffsetDateTime.parse(DT_WITH_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME), odtWith);
    }

    @Test
    public void testPrintDateTime() {
        OffsetDateTime dtWithoutNanoSec = OffsetDateTime
                .parse(DT_WITHOUT_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(DT_WITHOUT_NANO_SEC, DataTypeAdapter.printDateTime(dtWithoutNanoSec));

        OffsetDateTime dtWithNanoSec = OffsetDateTime.parse(DT_WITH_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(DT_WITH_NANO_SEC, DataTypeAdapter.printDateTime(dtWithNanoSec));

    }
}
