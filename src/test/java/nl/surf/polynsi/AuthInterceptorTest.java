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
    class TraefikSingleCertificateAuth {

        /* From: https://raw.githubusercontent.com/pyca/cryptography/refs/heads/main/docs/x509/reference.rst
         * Subject: CN=Good CA,O=Test Certificates 2011,C=US
         */
        public static final String _AUTHORIZED_CERT_PEM =
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

        public static final String _UNAUTH_CERT_PEM =
                "-----BEGIN CERTIFICATE-----" + "MIIHKjCCBRKgAwIBAgIQT3TgVL+kR4NPPrgo4CwCwTANBgkqhkiG9w0BAQwFADBG"
                        + "MQswCQYDVQQGEwJOTDEZMBcGA1UEChMQR0VBTlQgVmVyZW5pZ2luZzEcMBoGA1UE"
                        + "AxMTR0VBTlQgUGVyc29uYWwgQ0EgNDAeFw0yNDExMDcwMDAwMDBaFw0yNjExMDcy"
                        + "MzU5NTlaMIGmMQswCQYDVQQGEwJOTDEQMA4GA1UECBMHVXRyZWNodDESMBAGA1UE"
                        + "ChMJU1VSRiBCLlYuMRcwFQYDVQRhEw5OVFJOTC01MDI3NzM3NTEiMCAGCSqGSIb3"
                        + "DQEJARYTYXJuby5iYWtrZXJAc3VyZi5ubDEPMA0GA1UEBBMGQmFra2VyMQ0wCwYD"
                        + "VQQqEwRBcm5vMRQwEgYDVQQDEwtBcm5vIEJha2tlcjCCAiIwDQYJKoZIhvcNAQEB"
                        + "BQADggIPADCCAgoCggIBAI+7fvRM4fU5dgZ91/fsn1uRuR+i+4Cbg1F5XhL97VU7"
                        + "+zPelZKiDme+kseAbRNSuuWcODy/Itd3Rp+tTbJvUk/E5D9U6i5QnA60IRyJbqVf"
                        + "v1aOmk6MBXd4dLsjLYGUV/4oUSBiQNyr/kIRlUykKBh4wW/XneTfkzeq+ZaSfPts"
                        + "ztzfluHsEIba4zVcPFUxk/V0jR1hnp+QNewtrjwY7pZ5Gc54UVZHiwxOcbUQlpmV"
                        + "KElo9VTBbxJ3YdWWWi8Iz0EqZk/bg6Q7ug0ZNQbw0rQru0zzqxgz+5QVQg5uzqzi"
                        + "BJOQUILhF6tI3ZdHq7qYK3ZjjNBOX7b3VildE8xpF0eoKGdewd5qRQfE2nHcifTk"
                        + "oXMdA03XLFnSZ/YwUi3OXsX0P6w2GjomLbwDaOC9ub7H3Qr5J1Ji2j1o5ZIHEB4M"
                        + "Tp5iX9q/LlMiiT1iFNO8CEILqHpkwB8FbyL1yO8mwlFtztGJk0etnjFkKBMZjr2A"
                        + "PYCG/DRdL4vqA3q8ggw3tK49+fOxHs0JSx6xl7rnt1AyRlCqJCZwyKMwnHLn5K0p"
                        + "bvr4mjhOm/hPh0DbTd+0PMQAT2WTfaT0GRdHbigB5P35sYp4uHcdVdd4gC1V+D9h"
                        + "8F86uKWNKmxC2Nny0rTbBpj7BaX1nAOqJrJ8S98WXLdBRbhYrtR80e55zZOA+9R7"
                        + "AgMBAAGjggGxMIIBrTAfBgNVHSMEGDAWgBRpAKHHIVj44MUbILAK3adRvxPZ5DAd"
                        + "BgNVHQ4EFgQUrbE2uOX0ARMWjvBunRFqpY0rULEwDgYDVR0PAQH/BAQDAgWgMAwG"
                        + "A1UdEwEB/wQCMAAwHQYDVR0lBBYwFAYIKwYBBQUHAwQGCCsGAQUFBwMCMFAGA1Ud"
                        + "IARJMEcwOgYMKwYBBAGyMQECAQoEMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8vc2Vj"
                        + "dGlnby5jb20vU01JTUVDUFMwCQYHZ4EMAQUDAjBCBgNVHR8EOzA5MDegNaAzhjFo"
                        + "dHRwOi8vR0VBTlQuY3JsLnNlY3RpZ28uY29tL0dFQU5UUGVyc29uYWxDQTQuY3Js"
                        + "MHgGCCsGAQUFBwEBBGwwajA9BggrBgEFBQcwAoYxaHR0cDovL0dFQU5ULmNydC5z"
                        + "ZWN0aWdvLmNvbS9HRUFOVFBlcnNvbmFsQ0E0LmNydDApBggrBgEFBQcwAYYdaHR0"
                        + "cDovL0dFQU5ULm9jc3Auc2VjdGlnby5jb20wHgYDVR0RBBcwFYETYXJuby5iYWtr"
                        + "ZXJAc3VyZi5ubDANBgkqhkiG9w0BAQwFAAOCAgEAPg5PiIkN5ZVWKeqiFZP1ECcG"
                        + "v+c3XSwETci1DxuIAsxR9oQDRrSWG5DivHw/lcUH0S/NRF2tnLU3lD1Iu//5v9fr"
                        + "VO97rzByHxajSxog51XzCZ4/v64tJmmY/cFS2EgI2GS+KiLDvQt521jHY0FcJK0B"
                        + "xg8XXxm6f3cYjD6rYO9t6E6DB2b//i1tCkp0FmhEBnJ9i+VO+uIT93pwLAhxu37P"
                        + "1rLKPwlOJVLNRm63QUPZIPAa6tIeIcpsNycjudA/DnK+PteUZbh+dywZVdEmY2+x"
                        + "9bo4n50g9J2piRbNIBFTqhJe8uG4Dg256bolvjEkzBaCv0Ode5N/mNEaRdpoD0gZ"
                        + "KBdchgB2IVzeoxW6R8AD3QWtc2GokwtPWiYyY8iaQYclnMYZZOpb+NDt5a+nbMGI"
                        + "8casq10uu2eBVIzI3woGG44pDGIN341nsfbhwq2Jx2B8An2bWRiaxNP1hQC2uvkI"
                        + "J2e07hNXUOEscHVSVnwLlZ7tMQ6vG7fKNuCarlxBYUiIWHqgJlZvD6M9G6WF4NMZ"
                        + "BRRbBSwyceEg9hRHAstAKEl0E2xZbyilkqd0uKYJaHqquf5C9IUc0JmGcDfrkdKv"
                        + "w5vjD0zfFB82WbPMQCXO6Xz4GDljqKyr6Ubuf1hcc34LbvzUQoBZywG2z0AcuFXI"
                        + "HXHoFOXF1qRNDhervac="
                        + "-----END CERTIFICATE-----";

        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert");
            String chainSubjectDN =
                    "CN=University Corporation For Advanced Internet Development,emailAddress=knewell@internet2.edu,organizationIdentifier=NTRUS\\+MI-801069584,O=University Corporation For Advanced Internet Development,ST=Michigan,C=US";
            properties.setDistinguishedNames(List.of("CN=Good CA,O=Test Certificates 2011,C=US", chainSubjectDN));
        }

        String getPemCertString(String pemString) {
            pemString = pemString.replaceFirst("-----BEGIN CERTIFICATE-----", "");
            pemString = pemString.replaceFirst("-----END CERTIFICATE-----", "");
            return pemString.strip();
        }

        @Test
        void allowsValidPEMHeader() {
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn(getPemCertString(_AUTHORIZED_CERT_PEM));

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsInvalidPEMHeader() {
            String badPemString = getPemCertString(_AUTHORIZED_CERT_PEM);
            badPemString = badPemString.replaceFirst("[MQPXY]+", "S");
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(badPemString);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not contain valid PEM certificate"));
        }

        @Test
        void rejectsUnauthorizedPEMHeader() {
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(getPemCertString(_UNAUTH_CERT_PEM));
            ;

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            System.out.println("UNAUTH MESSAGE " + fault.getMessage());
            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }
    }
}
