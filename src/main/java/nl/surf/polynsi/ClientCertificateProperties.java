package nl.surf.polynsi;

import java.util.List;
import java.util.logging.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nl.surf.polynsi.client.certificate")
public class ClientCertificateProperties {

    private static final Logger LOG = Logger.getLogger(ClientCertificateProperties.class.getName());

    private AuthorizeDnType authorizeDn = AuthorizeDnType.NO;
    private String sslClientSubjectDnHeader = "ssl-client-subject-dn";
    private List<String> distinguishedNames;
    private List<String> subjectPublicKeyInfo;

    public AuthorizeDnType getAuthorizeDn() {
        return authorizeDn;
    }

    public void setAuthorizeDn(AuthorizeDnType authorizeDn) {
        this.authorizeDn = authorizeDn;
        LOG.fine("client certificate authorize distinguished name: " + authorizeDn);
    }

    public String getSslClientSubjectDnHeader() {
        return sslClientSubjectDnHeader;
    }

    public void setSslClientSubjectDnHeader(String sslClientSubjectDnHeader) {
        this.sslClientSubjectDnHeader = sslClientSubjectDnHeader;
        LOG.fine("client certificate subject distinguished name header: " + sslClientSubjectDnHeader);
    }

    public List<String> getDistinguishedNames() {
        return distinguishedNames;
    }

    public void setDistinguishedNames(List<String> distinguishedNames) {
        this.distinguishedNames = distinguishedNames;
        for (String distinguishedName : distinguishedNames)
            LOG.fine("client certificate distinguished name: " + distinguishedName);
    }

    // client certificate public key info not used yet
    public List<String> getSubjectPublicKeyInfo() {
        return subjectPublicKeyInfo;
    }

    public void setSubjectPublicKeyInfo(List<String> subjectPublicKeyInfo) {
        this.subjectPublicKeyInfo = subjectPublicKeyInfo;
        for (String info : subjectPublicKeyInfo) LOG.fine("client certificate subject public key info: " + info);
    }
}
