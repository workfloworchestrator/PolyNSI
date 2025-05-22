package nl.surf.polynsi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

@Component
@ConfigurationProperties(prefix = "nl.surf.polynsi.client.certificate")
public class ClientCertificateProperties {

    private static final Logger LOG = Logger.getLogger(ClientCertificateProperties.class.getName());

    private List<String> distinguishedNames;
    private List<String> subjectPublicKeyInfo;

    public List<String> getDistinguishedNames() {
        return distinguishedNames;
    }

    public void setDistinguishedNames(List<String> distinguishedNames) {
        this.distinguishedNames = distinguishedNames;
        for(String distinguishedName : distinguishedNames)
            LOG.fine("client certificate distinguished name: " + distinguishedName);
    }

    // client certificate public key info not used yet
    public List<String> getSubjectPublicKeyInfo() {
        return subjectPublicKeyInfo;
    }

    public void setSubjectPublicKeyInfo(List<String> subjectPublicKeyInfo) {
        this.subjectPublicKeyInfo = subjectPublicKeyInfo;
        for(String info : subjectPublicKeyInfo)
            LOG.fine("client certificate subject public key info: " + info);      }
}
