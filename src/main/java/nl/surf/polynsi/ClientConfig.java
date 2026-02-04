package nl.surf.polynsi;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.LinkedHashSet;
import java.util.logging.Logger;
import org.apache.cxf.Bus;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Message;
import org.ogf.nsi.grpc.connection.provider.ConnectionProviderGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

/*
This client config pertains to the configuration of the SOAP clients. The gRPC clients are 'configured'
where their implementations are actually used and annotated with `@GrpcClient`. I'd rather have had this all done in
the same location, but this all-over-the-place configuration/hooking up of things seem to be a side effect of Spring
Boot's dependency injection. It makes searching for things a little difficult.
*/
@Configuration
@ImportGrpcClients(
        basePackages = "org.ogf.nsi.grpc",
        target = "connection-provider",
        types = ConnectionProviderGrpc.class)
public class ClientConfig {

    @Autowired
    private Bus bus;

    /*
       Fault listener to catch exceptions in CXF chain and log warning message.
    */
    static class PolyNsiFaultListener implements FaultListener {

        public boolean faultOccurred(final Exception exception, final String description, final Message message) {
            // Try to name logger after reflected method, otherwise use own class name.
            Method method = (Method) message.getContextualProperty("java.lang.reflect.Method");
            String logName = "";
            if (method != null)
                logName = String.join(".", method.getDeclaringClass().getName(), method.getName());
            else logName = PolyNsiFaultListener.class.getName();
            Logger LOG = Logger.getLogger(logName);

            // try to get url that triggered this exception (not used at the moment).
            String url = null;
            if (message.getContextualPropertyKeys().contains("http.connection"))
                url = ((HttpURLConnection) message.getContextualProperty("http.connection"))
                        .getURL()
                        .toString();
            else url = (String) message.getContextualProperty("org.apache.cxf.request.url");

            // Concatenate all messages from linked exceptions and remove duplicates.
            LinkedHashSet<String> messages = new LinkedHashSet<>();
            Throwable throwable = exception;
            while (throwable != null) {
                messages.add(throwable.getMessage());
                throwable = throwable.getCause();
            }
            LOG.warning(String.join(": ", messages));

            // Return false to disable the default CXF fault handling (which normally just logs the exception).
            return false;
        }
    }

    /*
       This listener catches both SOAP client and server fault,
       that is why we only set the fault listener here and not again in ServerConfig.
    */
    @Bean
    public Object config() {
        bus.getProperties().put("org.apache.cxf.logging.FaultListener", new PolyNsiFaultListener());
        return null;
    }
}
