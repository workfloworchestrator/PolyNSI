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

    @Nested
    class TraefikCertificateAuth {

        /* From: https://raw.githubusercontent.com/pyca/cryptography/refs/heads/main/docs/x509/reference.rst
         * Subject: CN=Good CA,O=Test Certificates 2011,C=US
         */
        public static final String _TRUST_ANCHOR_CERT_PEM =
                "-----BEGIN CERTIFICATE-----" + "MIIDfDCCAmSgAwIBAgIBAjANBgkqhkiG9w0BAQsFADBFMQswCQYDVQQGEwJVUzEf"
                        + "MB0GA1UEChMWVGVzdCBDZXJ0aWZpY2F0ZXMgMjAxMTEVMBMGA1UEAxMMVHJ1c3Qg"
                        + "QW5jaG9yMB4XDTEwMDEwMTA4MzAwMFoXDTMwMTIzMTA4MzAwMFowQDELMAkGA1UE"
                        + "BhMCVVMxHzAdBgNVBAoTFlRlc3QgQ2VydGlmaWNhdGVzIDIwMTExEDAOBgNVBAMT"
                        + "B0dvb2QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCQWJpHYo37"
                        + "Xfb7oJSPe+WvfTlzIG21WQ7MyMbGtK/m8mejCzR6c+f/pJhEH/OcDSMsXq8h5kXa"
                        + "BGqWK+vSwD/Pzp5OYGptXmGPcthDtAwlrafkGOS4GqIJ8+k9XGKs+vQUXJKsOk47"
                        + "RuzD6PZupq4s16xaLVqYbUC26UcY08GpnoLNHJZS/EmXw1ZZ3d4YZjNlpIpWFNHn"
                        + "UGmdiGKXUPX/9H0fVjIAaQwjnGAbpgyCumWgzIwPpX+ElFOUr3z7BoVnFKhIXze+"
                        + "VmQGSWxZxvWDUN90Ul0tLEpLgk3OVxUB4VUGuf15OJOpgo1xibINPmWt14Vda2N9"
                        + "yrNKloJGZNqLAgMBAAGjfDB6MB8GA1UdIwQYMBaAFOR9X9FclYYILAWuvnW2ZafZ"
                        + "XahmMB0GA1UdDgQWBBRYAYQkG7wrUpRKPaUQchRR9a86yTAOBgNVHQ8BAf8EBAMC"
                        + "AQYwFwYDVR0gBBAwDjAMBgpghkgBZQMCATABMA8GA1UdEwEB/wQFMAMBAf8wDQYJ"
                        + "KoZIhvcNAQELBQADggEBADWHlxbmdTXNwBL/llwhQqwnazK7CC2WsXBBqgNPWj7m"
                        + "tvQ+aLG8/50Qc2Sun7o2VnwF9D18UUe8Gj3uPUYH+oSI1vDdyKcjmMbKRU4rk0eo"
                        + "3UHNDXwqIVc9CQS9smyV+x1HCwL4TTrq+LXLKx/qVij0Yqk+UJfAtrg2jnYKXsCu"
                        + "FMBQQnWCGrwa1g1TphRp/RmYHnMynYFmZrXtzFz+U9XEA7C+gPq4kqDI/iVfIT1s"
                        + "6lBtdB50lrDVwl2oYfAvW/6sC2se2QleZidUmrziVNP4oEeXINokU6T6p//HM1FG"
                        + "QYw2jOvpKcKtWCSAnegEbgsGYzATKjmPJPJ0npHFqzM="
                        + "-----END CERTIFICATE-----";

        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert");
            String chainSubjectDN =
                    "CN=University Corporation For Advanced Internet Development,emailAddress=knewell@internet2.edu,organizationIdentifier=NTRUS\\+MI-801069584,O=University Corporation For Advanced Internet Development,ST=Michigan,C=US";
            properties.setDistinguishedNames(List.of("CN=Good CA,O=Test Certificates 2011,C=US", chainSubjectDN));
        }

        String getPemCertString() {
            String pemString = _TRUST_ANCHOR_CERT_PEM;
            pemString = pemString.replaceFirst("-----BEGIN CERTIFICATE-----", "");
            pemString = pemString.replaceFirst("-----END CERTIFICATE-----", "");
            return pemString.strip();
        }

        @Test
        void allowsValidPEMHeader() {
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(getPemCertString());

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }
    }
}
