package nl.surf.polynsi;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

@Component
public class AuthInterceptor extends AbstractPhaseInterceptor<Message> {

    @Value("${nl.surf.polynsi.ssl-client-subject-dn-header:ssl-client-subject-dn}")
    private String sslClientSubjectDnHeader;

    private static final Logger LOG = Logger.getLogger(AuthInterceptor.class.getName());
    private final ClientCertificateProperties clientCertificateProperties;
    private final QName faultCode = new QName("http://schemas.xmlsoap.org/soap/envelope/", "AuthInterceptor");


    public AuthInterceptor(ClientCertificateProperties clientCertificateProperties) {
        super(Phase.READ);
        this.clientCertificateProperties = clientCertificateProperties;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String sslClientSubjectDn = null;
        HttpServletRequest request = (HttpServletRequest) message.get("HTTP.REQUEST");

        if (request == null)
            throw new SoapFault("HttpServletRequest not found in message context", faultCode);
        else {
            Enumeration<String> headerNames = request.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);

                if (headerName.equals(sslClientSubjectDnHeader))
                    sslClientSubjectDn = headerValue;
                LOG.fine("HTTP header " + headerName + ": " + headerValue);
            }
            if (sslClientSubjectDn == null)
                throw new SoapFault(sslClientSubjectDnHeader + " header not found on HTTP request", faultCode);
            else if (!isAllowed(sslClientSubjectDn))
                throw new SoapFault(sslClientSubjectDn + " not in list of allowed DNs", faultCode);
            else
                LOG.fine(sslClientSubjectDn + " in list of allowed DNs");
        }
    }

    private boolean isAllowed(String sslClientSubjectDn) {
        List<String> distinguishedNames = clientCertificateProperties.getDistinguishedNames();

        if (distinguishedNames == null) {
            throw new SoapFault("list of allowed DNs is empty", faultCode);
        }
        return clientCertificateProperties.getDistinguishedNames().contains(sslClientSubjectDn);
    }
}
