package nl.surf.polynsi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AuthInterceptorTest {

    private ClientCertificateProperties properties;
    private Message message;
    private HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        properties = new ClientCertificateProperties();
        message = mock(Message.class);
        httpRequest = mock(HttpServletRequest.class);
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(httpRequest);
    }

    @Nested
    class JakartaCertificateAuth {
        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT);
            properties.setDistinguishedNames(List.of("CN=test,O=SURF,C=NL"));
        }

        @Test
        void allowsValidCertificate() {
            X509Certificate cert = mock(X509Certificate.class);
            when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test,O=SURF,C=NL"));
            when(httpRequest.getAttribute("jakarta.servlet.request.X509Certificate"))
                    .thenReturn(new X509Certificate[] {cert});

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsMissingCertificate() {
            when(httpRequest.getAttribute("jakarta.servlet.request.X509Certificate"))
                    .thenReturn(null);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("Client certificate not found"));
        }

        @Test
        void rejectsEmptyCertificateArray() {
            when(httpRequest.getAttribute("jakarta.servlet.request.X509Certificate"))
                    .thenReturn(new X509Certificate[] {});

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("Client certificate not found"));
        }

        @Test
        void rejectsUnauthorizedDn() {
            X509Certificate cert = mock(X509Certificate.class);
            when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=unauthorized,O=Evil,C=XX"));
            when(httpRequest.getAttribute("jakarta.servlet.request.X509Certificate"))
                    .thenReturn(new X509Certificate[] {cert});

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }
    }

    @Nested
    class NGINXHeaderAuth {
        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.NGINX_TLS_CLIENT_SUBJECT_DN);
            properties.setTlsClientAuthNHeader("ssl-client-subject-dn");
            properties.setDistinguishedNames(List.of("CN=test,O=SURF,C=NL"));
        }

        @Test
        void allowsValidHeader() {
            when(httpRequest.getHeader("ssl-client-subject-dn")).thenReturn("CN=test,O=SURF,C=NL");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsMissingHeader() {
            // ssl-client-subject-dn is not stubbed, so getHeader returns null and the header is missing
            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("header not found on HTTP request"));
        }

        @Test
        void rejectsUnauthorizedDnFromHeader() {
            when(httpRequest.getHeader("ssl-client-subject-dn")).thenReturn("CN=unauthorized,O=Evil,C=XX");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }

        @Test
        void rejectsInvalidRfc2253Dn() {
            when(httpRequest.getHeader("ssl-client-subject-dn")).thenReturn("this-is-not-a-valid-dn");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("does not contain valid RFC2253 name"));
        }

        @Test
        void allowsHeaderDnWithGivenNameAndSurname() {
            // GN/SN are not in X500Principal's default keyword set; the incoming DN must be parsed with
            // the same OID map as the allow-list, otherwise this authorized client would be rejected.
            String dn = "CN=Example Client,GN=Example,SN=Client,O=Example Org,C=NL";
            properties.setDistinguishedNames(List.of(dn));
            when(httpRequest.getHeader("ssl-client-subject-dn")).thenReturn(dn);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }
    }

    @Nested
    class TraefikInfoHeaderAuth {
        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_SUBJECT_DN);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert-Info");
            properties.setDistinguishedNames(List.of("CN=test,O=SURF,C=NL"));
        }

        @Test
        void allowsValidHeader() {
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn("CN=test,O=SURF,C=NL");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsUnauthorizedDnFromHeader() {
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn("CN=unauthorized,O=Evil,C=XX");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }

        @Test
        void rejectsInvalidRfc2253Dn() {
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn("this-is-not-a-valid-dn");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("does not contain valid RFC2253 name"));
        }
    }

    @Test
    void rejectsMissingHttpServletRequest() {
        properties.setAuthorizeDnType(AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT);
        properties.setDistinguishedNames(List.of("CN=test"));
        when(message.get(AbstractHTTPDestination.HTTP_REQUEST)).thenReturn(null);

        AuthInterceptor interceptor = new AuthInterceptor(properties);

        SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
        assertTrue(fault.getMessage().contains("HttpServletRequest not found"));
    }

    @Test
    void rejectsNullDistinguishedNamesList() {
        properties.setAuthorizeDnType(AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT);
        // Don't set distinguishedNames, leaving it null

        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test"));
        when(httpRequest.getAttribute("jakarta.servlet.request.X509Certificate"))
                .thenReturn(new X509Certificate[] {cert});

        ClientCertificateProperties nullDnProps = new ClientCertificateProperties();
        nullDnProps.setAuthorizeDnType(AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT);
        AuthInterceptor interceptor = new AuthInterceptor(nullDnProps);

        SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
        assertTrue(fault.getMessage().contains("list of allowed Principals is empty"));
    }

    @Test
    void rejectsMalformedDistinguishedNameAtConstruction() {
        properties.setAuthorizeDnType(AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT);
        properties.setDistinguishedNames(List.of("CN=test,O=SURF,C=NL", "this-is-not-a-valid-dn"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new AuthInterceptor(properties));
        assertTrue(ex.getMessage().contains("this-is-not-a-valid-dn"));
    }

    @Test
    void rejectsUnsupportedAuthorizeDnType() {
        // NO (and any future unhandled enum value) must fail closed rather than authorize.
        properties.setAuthorizeDnType(AuthorizeDnType.NO);
        properties.setDistinguishedNames(List.of("CN=test,O=SURF,C=NL"));

        AuthInterceptor interceptor = new AuthInterceptor(properties);

        SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
        assertTrue(fault.getMessage().contains("unsupported AuthorizeDnType"));
    }

    @Nested
    class TraefikSingleCertificateAuth {
        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert");
        }

        @Test
        void allowsValidPEMHeader() {
            String dn = "CN=Example Client,O=Example Org,C=NL";
            X509Certificate client = TestCertificates.selfSigned(dn);
            properties.setDistinguishedNames(List.of(dn));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn(TestCertificates.traefikHeader(client));

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsInvalidPEMHeader() {
            properties.setDistinguishedNames(List.of("CN=Example Client,O=Example Org,C=NL"));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn("this-is-not-a-valid-base64-certificate");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not contain valid PEM certificate"));
        }

        @Test
        void rejectsUnauthorizedPEMHeader() {
            X509Certificate other = TestCertificates.selfSigned("CN=Some Other Client,O=Example Org,C=NL");
            properties.setDistinguishedNames(List.of("CN=Allowed Client,O=Example Org,C=NL"));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn(TestCertificates.traefikHeader(other));

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }
    }

    @Nested
    class TraefikSingleCertificateAuthProblematicOIDs {
        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert");
        }

        @Test
        void allowsAuthorizedPemCertWithProblematicOIDs() {
            // Subject contains GN/SN/organizationIdentifier/emailAddress, which are absent from
            // X500Principal's default keyword set and must be resolved via the shared names2oid map.
            String dn = "CN=Example Client,GN=Example,SN=Client,emailAddress=client@example.org,"
                    + "organizationIdentifier=EXAMPLE-12345,O=Example Org,C=NL";
            X509Certificate client = TestCertificates.selfSigned(dn);
            properties.setDistinguishedNames(List.of(dn));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn(TestCertificates.traefikHeader(client));

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }
    }

    @Nested
    class TraefikMultipleCertificateAuth {

        private X509Certificate[] chain;

        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert");
            String leafDn = "CN=Example Client,O=Example Org,C=NL";
            X509Certificate leaf = TestCertificates.selfSigned(leafDn);
            X509Certificate intermediate = TestCertificates.selfSigned("CN=Example Intermediate CA,O=Example Org,C=NL");
            X509Certificate root = TestCertificates.selfSigned("CN=Example Root CA,O=Example Org,C=NL");
            chain = new X509Certificate[] {leaf, intermediate, root};
            // Only the leaf is authorized; production parses the first cert and ignores the rest.
            properties.setDistinguishedNames(List.of(leafDn));
        }

        @Test
        void allowsAuthorizedPemCertChain() {
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn(TestCertificates.traefikHeader(chain));

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsBadlySeparatedPemCertChain() {
            String badHeader = TestCertificates.traefikHeader(chain).replaceFirst(",", "#");
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(badHeader);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not contain valid PEM certificate"));
        }

        @Test
        void rejectsWrongOrderPemCertChain() {
            // A CA certificate first means the parsed leaf subject is not the authorized one.
            String reordered = TestCertificates.traefikHeader(chain[2], chain[1], chain[0]);
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(reordered);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }
    }
}
