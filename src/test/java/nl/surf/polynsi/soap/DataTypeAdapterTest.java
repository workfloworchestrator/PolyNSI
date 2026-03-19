package nl.surf.polynsi.soap;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

public class DataTypeAdapterTest {

    static final String DT_WITHOUT_NANO_SEC = "2013-07-24T16:50:00+02:00";
    static final String DT_WITH_NANO_SEC = "2013-07-24T16:50:00.333+02:00";

    @Test
    public void unmarshall() {
        DataTypeAdapter dta = new DataTypeAdapter();
        OffsetDateTime odtWithout = dta.unmarshal(DT_WITHOUT_NANO_SEC);
        assertEquals(OffsetDateTime.parse(DT_WITHOUT_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME), odtWithout);

        OffsetDateTime odtWith = dta.unmarshal(DT_WITH_NANO_SEC);
        assertEquals(OffsetDateTime.parse(DT_WITH_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME), odtWith);
    }

    @Test
    public void marshall() {
        DataTypeAdapter dta = new DataTypeAdapter();
        OffsetDateTime dtWithoutNanoSec =
                OffsetDateTime.parse(DT_WITHOUT_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(DT_WITHOUT_NANO_SEC, dta.marshal(dtWithoutNanoSec));

        OffsetDateTime dtWithNanoSec = OffsetDateTime.parse(DT_WITH_NANO_SEC, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(DT_WITH_NANO_SEC, dta.marshal(dtWithNanoSec));
    }

    @Test
    public void unmarshallNull() {
        DataTypeAdapter dta = new DataTypeAdapter();
        assertNull(dta.unmarshal(null));
    }

    @Test
    public void marshallNull() {
        DataTypeAdapter dta = new DataTypeAdapter();
        assertNull(dta.marshal(null));
    }
}
