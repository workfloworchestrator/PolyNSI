package nl.surf.polynsi.soap.connection.provider;

import org.apache.cxf.ext.logging.LoggingFeature;

public class PrettyLoggingFeature extends LoggingFeature {

    public PrettyLoggingFeature() {
        super.setPrettyLogging(true);
        super.setVerbose(true);
        super.setLogMultipart(true);
    }
}
