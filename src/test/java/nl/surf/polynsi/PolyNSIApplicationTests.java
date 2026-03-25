package nl.surf.polynsi;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.jaxws.EndpointImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class PolyNSIApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ClientCertificateProperties clientCertificateProperties;

    @Autowired
    @Qualifier("endpointConnectionProvider")
    private Endpoint providerEndpoint;

    @Autowired
    @Qualifier("endpointConnectionRequester")
    private Endpoint requesterEndpoint;

    @Value("${spring.grpc.server.port}")
    private int grpcServerPort;

    @Test
    void contextLoads() {}

    @Test
    void connectionProviderPortBeanExists() {
        assertNotNull(context.getBean("connectionProviderPort"));
    }

    @Test
    void connectionRequesterPortBeanExists() {
        assertNotNull(context.getBean("connectionRequesterPort"));
    }

    @Test
    void authInterceptorBeanExists() {
        assertNotNull(context.getBean(AuthInterceptor.class));
    }

    @Test
    void defaultAuthorizeDnIsNo() {
        assertEquals(AuthorizeDnType.NO, clientCertificateProperties.getAuthorizeDn());
    }

    @Test
    void defaultSslHeaderIsConfigured() {
        assertEquals("ssl-client-subject-dn", clientCertificateProperties.getSslClientSubjectDnHeader());
    }

    @Test
    void distinguishedNamesAreLoadedFromProperties() {
        assertNotNull(clientCertificateProperties.getDistinguishedNames());
        assertEquals(3, clientCertificateProperties.getDistinguishedNames().size());
    }

    @Test
    void soapProviderEndpointIsPublished() {
        assertTrue(providerEndpoint.isPublished());
    }

    @Test
    void soapRequesterEndpointIsPublished() {
        assertTrue(requesterEndpoint.isPublished());
    }

    @Test
    void authInterceptorNotAddedWhenAuthorizeDnIsNo() {
        EndpointImpl impl = (EndpointImpl) providerEndpoint;
        boolean hasAuthInterceptor = impl.getInInterceptors().stream().anyMatch(i -> i instanceof AuthInterceptor);
        assertFalse(hasAuthInterceptor);
    }

    @Test
    void grpcServerPortIsConfigured() {
        assertEquals(9090, grpcServerPort);
    }
}
