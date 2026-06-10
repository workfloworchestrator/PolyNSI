package nl.surf.polynsi;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
    private final ClientPrincipals clientPrincipals;
    private final QName faultCode = new QName("http://schemas.xmlsoap.org/soap/envelope/", "AuthInterceptor");

    public AuthInterceptor(ClientCertificateProperties clientCertificateProperties) {
        super(Phase.RECEIVE);
        this.clientCertificateProperties = clientCertificateProperties;
        this.clientPrincipals = new ClientPrincipals(clientCertificateProperties.getDistinguishedNames());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        X500Principal tlsClientSubjectPrincipal = null;

        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null) throw new SoapFault("HttpServletRequest not found on incoming request", faultCode);

        AuthorizeDnType at = clientCertificateProperties.getAuthorizeDnType();
        switch (at) {
            case AuthorizeDnType.JAKARTA_SERVLET_TLS_CLIENT_CERT:
                X509Certificate[] certificates = (X509Certificate[])
                        request.getAttribute(ClientCertificateProperties.JAKARTA_SERVLET_TLS_CLIENT_CERT_ATTRIBUTE);
                if (certificates == null || certificates.length == 0) {
                    throw new SoapFault("Client certificate not found on incoming request", faultCode);
                }
                tlsClientSubjectPrincipal = certificates[0].getSubjectX500Principal();
                break;
            case AuthorizeDnType.NGINX_TLS_CLIENT_SUBJECT_DN:
            case AuthorizeDnType.TRAEFIK_TLS_CLIENT_SUBJECT_DN:
            case AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT:
                String expectHeaderName = clientCertificateProperties.getTlsClientAuthNHeader();
                String headerValue = request.getHeader(expectHeaderName);
                if (headerValue != null) {
                    LOG.fine("Found HTTP header " + expectHeaderName + ": " + headerValue);
                    if (at == AuthorizeDnType.NGINX_TLS_CLIENT_SUBJECT_DN
                            || at == AuthorizeDnType.TRAEFIK_TLS_CLIENT_SUBJECT_DN) {
                        // ASSUME this DN has been sanitized by layer above, and is in RFC2253 format.
                        // Parse with the same OID map as the allow-list so equivalent DNs parse identically.
                        try {
                            tlsClientSubjectPrincipal = ClientPrincipals.parsePrincipal(headerValue);
                        } catch (Exception e) {
                            throw new SoapFault(
                                    clientCertificateProperties.getTlsClientAuthNHeader()
                                            + " header does not contain valid RFC2253 name",
                                    faultCode);
                        }
                    } else if (at == AuthorizeDnType.TRAEFIK_TLS_CLIENT_CERT) {
                        // Decode Traefik PEM, see ClientCertificateProperties for format.
                        try {
                            String base64String = null;
                            if (headerValue.contains(",")) {
                                // A chain is sent as comma-separated PEM blocks with the client (leaf)
                                // certificate first, intermediates following. (Undocumented by Traefik.)
                                // https://github.com/keycloak/keycloak/issues/46395#issuecomment-3915177071
                                String[] base64Strings = headerValue.split("[,]");
                                base64String = base64Strings[0];
                            } else {
                                base64String = headerValue;
                            }
                            // CertificateFactory accepts a Base64 certificate bounded by the BEGIN/END
                            // markers; line wrapping is not required.
                            // https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/security/cert/CertificateFactory.html#generateCertificate(java.io.InputStream)
                            String pemString = "-----BEGIN CERTIFICATE-----\n";
                            pemString = pemString + base64String;
                            pemString = pemString + "-----END CERTIFICATE-----\n";
                            ByteArrayInputStream pemStream = new ByteArrayInputStream(pemString.getBytes());
                            CertificateFactory factory = CertificateFactory.getInstance("X.509");
                            X509Certificate cert =
                                    (java.security.cert.X509Certificate) factory.generateCertificate(pemStream);
                            tlsClientSubjectPrincipal = cert.getSubjectX500Principal();
                        } catch (Exception e) {
                            LOG.fine("Error processing PEM cert: " + e.getMessage());
                            throw new SoapFault(
                                    clientCertificateProperties.getTlsClientAuthNHeader()
                                            + " header does not contain valid PEM certificate",
                                    faultCode);
                        }
                    }
                } else {
                    LOG.fine("Expected HTTP header " + expectHeaderName + " missing.");
                }
                if (tlsClientSubjectPrincipal == null)
                    throw new SoapFault(
                            clientCertificateProperties.getTlsClientAuthNHeader() + " header not found on HTTP request",
                            faultCode);
                break;
            default:
                throw new SoapFault("unsupported AuthorizeDnType: " + at, faultCode);
        }

        String rfc2253Dn = tlsClientSubjectPrincipal.getName(X500Principal.RFC2253);
        if (!isAllowed(tlsClientSubjectPrincipal))
            throw new SoapFault(rfc2253Dn + " not in list of allowed DNs", faultCode);
        else LOG.fine(rfc2253Dn + " in list of allowed DNs");
    }

    private boolean isAllowed(X500Principal tlsClientSubjectPrincipal) {

        if (this.clientPrincipals.numberOfAllowedPrincipals() == 0) {
            throw new SoapFault("list of allowed Principals is empty", faultCode);
        }

        // MAIN AUTH CHECK
        return this.clientPrincipals.isAllowedPrincipal(tlsClientSubjectPrincipal);
    }
}
