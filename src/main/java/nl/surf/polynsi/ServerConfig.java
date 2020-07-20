package nl.surf.polynsi;

import nl.surf.polynsi.soap.connection.provider.ConnectionProviderPort;
import nl.surf.polynsi.soap.connection.provider.ConnectionServiceProviderPortImpl;
import nl.surf.polynsi.soap.connection.requester.ConnectionRequesterPort;
import nl.surf.polynsi.soap.connection.requester.ConnectionServiceRequesterPortImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;

@Configuration
public class ServerConfig {
    @Value("${soap.server.connection_provider.path}")
    private String connectionProviderPath;

    @Value("${soap.server.connection_requester.path}")
    private String connectionRequesterPath;

    @Autowired
    private Bus bus;

    @Bean
    public ConnectionProviderPort connectionProviderPort() {
        return new ConnectionServiceProviderPortImpl();
    }

    @Bean
    public ConnectionRequesterPort connectionRequesterPort() {
        return new ConnectionServiceRequesterPortImpl();
    }

    @Bean
    public Endpoint endpointConnectionProvider() {
        EndpointImpl endpoint = new EndpointImpl(bus, connectionProviderPort());
        endpoint.publish(this.connectionProviderPath);
        return endpoint;
    }

    @Bean
    public Endpoint endpointConnectionRequester() {
        EndpointImpl endpoint = new EndpointImpl(bus, connectionRequesterPort());
        endpoint.publish(this.connectionRequesterPath);
        return endpoint;
    }
}
