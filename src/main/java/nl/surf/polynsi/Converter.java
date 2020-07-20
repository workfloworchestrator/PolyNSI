package nl.surf.polynsi;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class Converter {
    public static Timestamp toProtobuf(OffsetDateTime soapTimestamp) {
        Instant instant = soapTimestamp.toInstant();
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    public static OffsetDateTime toSoap(Timestamp pbTimestamp) {
        return OffsetDateTime
                .ofInstant(Instant.ofEpochSecond(pbTimestamp.getSeconds(), pbTimestamp.getNanos()), ZoneOffset.UTC);
    }
}
