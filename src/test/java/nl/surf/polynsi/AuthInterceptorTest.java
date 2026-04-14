package nl.surf.polynsi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Collections;
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
    class CertificateAuth {
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
    class HeaderAuth {
        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.NGINX_TLS_CLIENT_SUBJECT_DN);
            properties.setTlsClientAuthNHeader("ssl-client-subject-dn");
            properties.setDistinguishedNames(List.of("CN=test,O=SURF,C=NL"));
        }

        @Test
        void allowsValidHeader() {
            when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(List.of("ssl-client-subject-dn")));
            when(httpRequest.getHeader("ssl-client-subject-dn")).thenReturn("CN=test,O=SURF,C=NL");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsMissingHeader() {
            when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(List.of("other-header")));
            when(httpRequest.getHeader("other-header")).thenReturn("some-value");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("header not found on HTTP request"));
        }

        @Test
        void rejectsUnauthorizedDnFromHeader() {
            when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(List.of("ssl-client-subject-dn")));
            when(httpRequest.getHeader("ssl-client-subject-dn")).thenReturn("CN=unauthorized,O=Evil,C=XX");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
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
        assertTrue(fault.getMessage().contains("list of allowed DNs is empty"));
    }
}
