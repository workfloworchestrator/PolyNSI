package nl.surf.polynsi;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

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
        String sslClientSubjectDn = null;

        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null)
            throw new SoapFault("HttpServletRequest not found on incoming request", faultCode);

        switch (clientCertificateProperties.getAuthorizeDn()) {
            case AuthorizeDnType.CERTIFICATE:
                X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
                if (certificates == null || certificates.length == 0) {
                    throw new SoapFault("Client certificate not found on incoming request", faultCode);
                }
                sslClientSubjectDn = certificates[0].getSubjectX500Principal().getName();
                break;
            case AuthorizeDnType.HEADER:
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (headerName.equals(clientCertificateProperties.getSslClientSubjectDnHeader()))
                        sslClientSubjectDn = headerValue;
                    LOG.fine("HTTP header " + headerName + ": " + headerValue);
                }
                if (sslClientSubjectDn == null)
                    throw new SoapFault(clientCertificateProperties.getSslClientSubjectDnHeader() + " header not found on HTTP request", faultCode);
                break;
        }

        if (!isAllowed(sslClientSubjectDn))
            throw new SoapFault(sslClientSubjectDn + " not in list of allowed DNs", faultCode);
        else
            LOG.fine(sslClientSubjectDn + " in list of allowed DNs");
    }

    private boolean isAllowed(String sslClientSubjectDn) {
        List<String> distinguishedNames = clientCertificateProperties.getDistinguishedNames();

        if (distinguishedNames == null) {
            throw new SoapFault("list of allowed DNs is empty", faultCode);
        }
        return clientCertificateProperties.getDistinguishedNames().contains(sslClientSubjectDn);
    }
}
