package nl.surf.polynsi;

import jakarta.xml.ws.Endpoint;
import nl.surf.polynsi.soap.connection.provider.ConnectionProviderPort;
import nl.surf.polynsi.soap.connection.provider.ConnectionServiceProviderPortImpl;
import nl.surf.polynsi.soap.connection.requester.ConnectionRequesterPort;
import nl.surf.polynsi.soap.connection.requester.ConnectionServiceRequesterPortImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
This server config pertains to the configuration of the SOAP servers. The gRPC servers are 'configured'
where their classes are implemented and annotated with `@GrpcService`. I'd rather have had this all done in the
same location, but this all-over-the-place configuration/hooking up of things seem to be side-effect of Spring Boot's
dependency injection. It makes searching for things a little difficult
*/
@Configuration
public class ServerConfig {
    @Value("${soap.server.connection_provider.path}")
    private String connectionProviderPath;

    @Value("${soap.server.connection_requester.path}")
    private String connectionRequesterPath;

    private final ClientCertificateProperties clientCertificateProperties;

    public ServerConfig(ClientCertificateProperties clientCertificateProperties) {
        this.clientCertificateProperties = clientCertificateProperties;
    }

    @Bean
    public ConnectionProviderPort connectionProviderPort() {
        return new ConnectionServiceProviderPortImpl();
    }

    @Bean
    public ConnectionRequesterPort connectionRequesterPort() {
        return new ConnectionServiceRequesterPortImpl();
    }

    @Bean
    public Endpoint endpointConnectionProvider(
            Bus bus, ConnectionProviderPort connectionProviderPort, AuthInterceptor authInterceptor) {
        EndpointImpl endpoint = new EndpointImpl(bus, connectionProviderPort);
        endpoint.setWsdlLocation("wsdl/connection/ogf_nsi_connection_provider_v2_0.wsdl");
        endpoint.publish(this.connectionProviderPath);
        if (clientCertificateProperties.getAuthorizeDn() != AuthorizeDnType.NO)
            endpoint.getInInterceptors().add(authInterceptor);
        return endpoint;
    }

    @Bean
    public Endpoint endpointConnectionRequester(Bus bus) {
        EndpointImpl endpoint = new EndpointImpl(bus, connectionRequesterPort());
        endpoint.setWsdlLocation("wsdl/connection/ogf_nsi_connection_requester_v2_0.wsdl");
        endpoint.publish(this.connectionRequesterPath);
        return endpoint;
    }
}
