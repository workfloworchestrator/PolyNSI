package nl.surf.polynsi.soap;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DataTypeAdapter extends XmlAdapter<String, OffsetDateTime> {
    @Override
    public String marshal(OffsetDateTime odt) {
        return odt == null ? null : odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public OffsetDateTime unmarshal(String s) {
        return s == null ? null : OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}