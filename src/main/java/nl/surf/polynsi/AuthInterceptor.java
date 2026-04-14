package nl.surf.polynsi;

import jakarta.servlet.http.HttpServletRequest;
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
        javax.security.auth.x500.X500Principal sslClientSubjectPrincipal = null;

        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null) throw new SoapFault("HttpServletRequest not found on incoming request", faultCode);

        switch (clientCertificateProperties.getAuthorizeDn()) {
            // ARNOTODO: Client cert passed as PEM or Info summary from Traefik
            case AuthorizeDnType.CERTIFICATE:
                X509Certificate[] certificates =
                        (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
                if (certificates == null || certificates.length == 0) {
                    throw new SoapFault("Client certificate not found on incoming request", faultCode);
                }
                // Get name as Principal object from certificate
                sslClientSubjectPrincipal = certificates[0].getSubjectX500Principal();
                break;
            case AuthorizeDnType.HEADER:
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (headerName.equals(clientCertificateProperties.getSslClientSubjectDnHeader())) {
                        // ASSUME this DN has been sanitized by layer above, and is in RFC2253 format
                        try {
                            // https://docs.oracle.com/en/java/javase/26/docs/api/java.base/javax/security/auth/x500/X500Principal.html#%3Cinit%3E(java.lang.String,java.util.Map)
                            X500Principal p = new X500Principal(headerValue);
                            sslClientSubjectPrincipal = p;
                        } catch (Exception e) {
                            throw new SoapFault(
                                    clientCertificateProperties.getSslClientSubjectDnHeader()
                                            + " header does not contain valid RFC2253 name",
                                    faultCode);
                        }
                    }
                    LOG.fine("HTTP header " + headerName + ": " + headerValue);
                }
                if (sslClientSubjectPrincipal == null)
                    throw new SoapFault(
                            clientCertificateProperties.getSslClientSubjectDnHeader()
                                    + " header not found on HTTP request",
                            faultCode);
                break;
        }

        String rfc2253Dn = sslClientSubjectPrincipal.getName(X500Principal.RFC2253);
        if (!isAllowed(sslClientSubjectPrincipal))
            throw new SoapFault(rfc2253Dn + " not in list of allowed DNs", faultCode);
        else LOG.fine(rfc2253Dn + " in list of allowed DNs");
    }

    private boolean isAllowed(X500Principal sslClientSubjectPrincipal) {
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
        return cps.isAllowedPrincipal(sslClientSubjectPrincipal);
    }
}
