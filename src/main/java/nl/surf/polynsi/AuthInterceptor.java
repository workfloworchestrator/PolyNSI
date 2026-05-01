package nl.surf.polynsi;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.stereotype.Component;

@Component
public class AuthInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = Logger.getLogger(AuthInterceptor.class.getName());
    private final ClientCertificateProperties clientCertificateProperties;
    private final QName faultCode = new QName("http://schemas.xmlsoap.org/soap/envelope/", "AuthInterceptor");

    public AuthInterceptor(ClientCertificateProperties clientCertificateProperties) {
        super(Phase.RECEIVE);
        this.clientCertificateProperties = clientCertificateProperties;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        X500Principal tlsClientSubjectPrincipal = null;

        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null) throw new SoapFault("HttpServletRequest not found on incoming request", faultCode);

        AuthorizeDnType at = clientCertificateProperties.getAuthorizeDnType();
        switch (at) {
            // ARNOTODO: Client cert passed as PEM or Info summary from Traefik
            case AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT:
                X509Certificate[] certificates = (X509Certificate[])
                        request.getAttribute(ClientCertificateProperties.JAKARTA_SERVLET_TLS_CLIENT_CERT_HEADER);
                if (certificates == null || certificates.length == 0) {
                    throw new SoapFault("Client certificate not found on incoming request", faultCode);
                }
                // Get name as Principal object from certificate
                tlsClientSubjectPrincipal = certificates[0].getSubjectX500Principal();
                break;
            case AuthorizeDnType.NGINX_TLS_CLIENT_SUBJECT_DN:
            case AuthorizeDnType.TRAEFIK_TLS_CLIENT_SUBJECT_DN:
            case AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT:
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (headerName.equals(clientCertificateProperties.getTlsClientAuthNHeader())) {
                        if (at == AuthorizeDnType.NGINX_TLS_CLIENT_SUBJECT_DN
                                || at == AuthorizeDnType.TRAEFIK_TLS_CLIENT_SUBJECT_DN) {
                            // ASSUME this DN has been sanitized by layer above, and is in RFC2253 format
                            try {
                                // https://docs.oracle.com/en/java/javase/26/docs/api/java.base/javax/security/auth/x500/X500Principal.html#%3Cinit%3E(java.lang.String,java.util.Map)
                                X500Principal p = new X500Principal(headerValue);
                                tlsClientSubjectPrincipal = p;
                            } catch (Exception e) {
                                throw new SoapFault(
                                        clientCertificateProperties.getTlsClientAuthNHeader()
                                                + " header does not contain valid RFC2253 name",
                                        faultCode);
                            }
                        } else if (at == AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT) {
                            // Decode Traefik PEM, see ClientCertificateProperties for format.
                            try {
                                String pemString = null;
                                if (headerValue.contains(",")) {
                                    /*
                                    # Undocumented: assume client cert is first in list
                                    # This issue says client cert comes first: https://github.com/keycloak/keycloak/issues/46395#issuecomment-3915177071
                                    # Code suggest extra certs follow client cert: https://github.com/tdiesler/keycloak/commit/8d318c552a2c778b65265f4c46a3b30c7dc99a27#diff-4a1b33f7b0a6b8526caf3186df5ccd193f7efcb683dcff9e515de2765ec9fd19R236
                                    # "Traefik sends the client certificate and any intermediate CA certificates as PEM blocks in a single `X-Forwarded-Tls-Client-Cert` header, separated by commas."
                                    #  --- https://github.com/tdiesler/keycloak/commit/8d318c552a2c778b65265f4c46a3b30c7dc99a27#diff-4a1b33f7b0a6b8526caf3186df5ccd193f7efcb683dcff9e515de2765ec9fd19R288
                                    # If Traefik this is in PEM with some changes, see above
                                    */
                                    String[] pemStrings = headerValue.split("[,]");
                                    pemString = pemStrings[0];
                                } else {
                                    pemString = headerValue;
                                }
                                ByteArrayInputStream pemStream = new ByteArrayInputStream(pemString.getBytes());
                                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                                X509Certificate cert =
                                        (java.security.cert.X509Certificate) factory.generateCertificate(pemStream);
                                tlsClientSubjectPrincipal = cert.getSubjectX500Principal();
                            } catch (Exception e) {
                                throw new SoapFault(
                                        clientCertificateProperties.getTlsClientAuthNHeader()
                                                + " header does not contain valid PEM certificate",
                                        faultCode);
                            }
                        }
                    }
                    LOG.fine("HTTP header " + headerName + ": " + headerValue);
                }
                if (tlsClientSubjectPrincipal == null)
                    throw new SoapFault(
                            clientCertificateProperties.getTlsClientAuthNHeader() + " header not found on HTTP request",
                            faultCode);
                break;
        }

        String rfc2253Dn = tlsClientSubjectPrincipal.getName(X500Principal.RFC2253);
        if (!isAllowed(tlsClientSubjectPrincipal))
            throw new SoapFault(rfc2253Dn + " not in list of allowed DNs", faultCode);
        else LOG.fine(rfc2253Dn + " in list of allowed DNs");
    }

    private boolean isAllowed(X500Principal tlsClientSubjectPrincipal) {
        List<String> distinguishedNames = clientCertificateProperties.getDistinguishedNames();
        if (distinguishedNames == null) {
            throw new SoapFault("list of allowed DNs is empty", faultCode);
        }
        // Not ideal to convert strings to Objects on each call, but otherwise conflicts with Properties-based
        // config. And we must use the Principal equals() method for comparison.
        ClientPrincipals cps = new ClientPrincipals(distinguishedNames);
        if (cps == null) {
            throw new SoapFault("list of allowed Principals is empty", faultCode);
        }
        return cps.isAllowedPrincipal(tlsClientSubjectPrincipal);
    }
}
