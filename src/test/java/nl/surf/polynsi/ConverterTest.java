package nl.surf.polynsi;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import nl.surf.polynsi.soap.framework.headers.SessionSecurityAttrType;
import org.junit.jupiter.api.Test;
import org.oasis.saml.AttributeType;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void fromSessionSecurityAttr() {
        var xmlSSA = "<sessionSecurityAttr><ns4:Attribute Name=\"user\" xmlns:ns4=\"urn:oasis:names:tc:SAML:2" +
                ".0:assertion\"><ns4:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:type=\"xs:string\">urn:collab:person:surfnet" +
                ".nl:hanst</ns4:AttributeValue></ns4:Attribute><ns4:Attribute Name=\"token\" " +
                "xmlns:ns4=\"urn:oasis:names:tc:SAML:2.0:assertion\"><ns4:AttributeValue xmlns:xs=\"http://www.w3" +
                ".org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:type=\"xs:string\">2506fc3c-56fd-4efa-ba5b-01cb73bba316</ns4:AttributeValue></ns4:Attribute" +
                "></sessionSecurityAttr>";
        assertDoesNotThrow(() -> {
            ArrayList<SessionSecurityAttrType> ssas = Converter.toSessionSecurityAttr(xmlSSA);
            assertEquals(1, ssas.size());
            assertAll("AttributeOrEncryptedAttribute", () -> {
                /*
                 We know we get back elements of type `AttributeType` because that's how we constructed the snippet
                 of SAML above. So we blindly cast them to `AttributeType` in order to test its properties using
                 regular getters.
                 */
                List<AttributeType> attrTypes = ssas.get(0).getAttributeOrEncryptedAttribute().stream()
                        .map(e -> (AttributeType) e).collect(Collectors.toList());
                assertAll("AttributeTypes", () -> assertEquals("user", attrTypes.get(0)
                        .getName()), () -> assertEquals("token", attrTypes.get(1).getName()));
            });
        });
    }
}