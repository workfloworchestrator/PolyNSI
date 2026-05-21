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
    class NGINXHeaderAuth {
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
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert-Info")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn("CN=test,O=SURF,C=NL");

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsUnauthorizedDnFromHeader() {
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert-Info")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn("CN=unauthorized,O=Evil,C=XX");

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

            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }
    }

    @Nested
    class TraefikSingleCertificateAuthProblematicOIDs {

        public static final String _AUTHORIZED_CERT_PEM =
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
                    "CN=Arno Bakker,GN=Arno,SN=Bakker,emailAddress=arno.bakker@surf.nl,organizationIdentifier=NTRNL-50277375,O=SURF B.V.,ST=Utrecht,C=NL";
            properties.setDistinguishedNames(List.of("CN=Good CA,O=Test Certificates 2011,C=US", chainSubjectDN));
        }

        String getPemCertString(String pemString) {
            pemString = pemString.replaceFirst("-----BEGIN CERTIFICATE-----", "");
            pemString = pemString.replaceFirst("-----END CERTIFICATE-----", "");
            return pemString.strip();
        }

        @Test
        void allowsAuthorizedPemCertWithProblematicOIDs() {
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert"))
                    .thenReturn(getPemCertString(_AUTHORIZED_CERT_PEM));

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }
    }

    @Nested
    class TraefikMultipleCertificateAuth {

        //  Real Internet2 cert chain (client cert + 3 intermediate CAs), comma-separated as Traefik sends it
        public static final String _AUTHORIZED_CERT_PEM_CHAIN =
                "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,"
                        + "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,"
                        + "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,"
                        + "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";

        @BeforeEach
        void setUp() {
            properties.setAuthorizeDnType(AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT);
            properties.setTlsClientAuthNHeader("X-Forwarded-Tls-Client-Cert");
            String chainSubjectDN =
                    "CN=University Corporation For Advanced Internet Development,emailAddress=knewell@internet2.edu,organizationIdentifier=NTRUS\\+MI-801069584,O=University Corporation For Advanced Internet Development,ST=Michigan,C=US";
            properties.setDistinguishedNames(List.of("CN=Good CA,O=Test Certificates 2011,C=US", chainSubjectDN));
        }

        @Test
        void allowsAuthorizedPemCertChain() {
            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(_AUTHORIZED_CERT_PEM_CHAIN);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            assertDoesNotThrow(() -> interceptor.handleMessage(message));
        }

        @Test
        void rejectsBadlySeparatedPemCertChain() {
            String badPemChainString = _AUTHORIZED_CERT_PEM_CHAIN;
            badPemChainString = badPemChainString.replaceFirst(",", "#");

            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(badPemChainString);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));
            assertTrue(fault.getMessage().contains("not contain valid PEM certificate"));
        }

        @Test
        void rejectsWrongOrderPemCertChain() {
            String badPemChainString = _AUTHORIZED_CERT_PEM_CHAIN;
            String[] base64Strings = badPemChainString.split("[,]");
            badPemChainString = base64Strings[2] + "," + base64Strings[1] + "," + base64Strings[0];

            when(httpRequest.getHeaderNames())
                    .thenReturn(Collections.enumeration(List.of("X-Forwarded-Tls-Client-Cert")));
            when(httpRequest.getHeader("X-Forwarded-Tls-Client-Cert")).thenReturn(badPemChainString);

            AuthInterceptor interceptor = new AuthInterceptor(properties);

            SoapFault fault = assertThrows(SoapFault.class, () -> interceptor.handleMessage(message));

            System.out.println("UNAUTH MESSAGE " + fault.getMessage());

            assertTrue(fault.getMessage().contains("not in list of allowed DNs"));
        }
    }
}
