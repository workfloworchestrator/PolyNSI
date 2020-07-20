package nl.surf.polynsi;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {
    public static final String DT_WITH_TZ = "2013-07-24T16:50:00.333+02:00";
    public static final String DT_EPOCH = "1970-01-01T00:00:00Z";

    @Test
    void toProtobufOffsetDatetime() {
        OffsetDateTime odtWithTz = OffsetDateTime.parse(DT_WITH_TZ, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Timestamp tsFromOdtWithTz = Converter.toProtobuf(odtWithTz);
        // Conversion to Protobuf version effectively turns it into a UTC timestamp.
        assertEquals(odtWithTz.atZoneSameInstant(ZoneOffset.UTC).toString(), Timestamps.toString(tsFromOdtWithTz));

        OffsetDateTime odtEpoch = OffsetDateTime.parse(DT_EPOCH, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Timestamp tsFromOdtEpoch = Converter.toProtobuf(odtEpoch);
        assertEquals(ZoneOffset.UTC, odtEpoch.getOffset());

        /*
         Using `.format` here as `.toString` uses the shortest format possible, stripping ':00' seconds that
         `Timestamps.toString` does include.
         */
        assertEquals(odtEpoch.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), Timestamps.toString(tsFromOdtEpoch));
    }

    @Test
    void toSoapTimestamp() {
        try {
            Timestamp tsFromDtWithTz = Timestamps.parse(DT_WITH_TZ);
            OffsetDateTime odtWithTz = OffsetDateTime.parse(DT_WITH_TZ, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            assertEquals(odtWithTz.atZoneSameInstant(ZoneOffset.UTC).toString(), Converter.toSoap(tsFromDtWithTz)
                    .toString());


            Timestamp tsFromDtEpoch = Timestamps.parse(DT_EPOCH);
            OffsetDateTime odtEpoch = OffsetDateTime.parse(DT_EPOCH, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            assertEquals(odtEpoch.toString(), Converter.toSoap(tsFromDtEpoch).toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}