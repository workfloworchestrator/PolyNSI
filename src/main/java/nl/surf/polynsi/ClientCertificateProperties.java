package nl.surf.polynsi;

import java.util.List;
import java.util.logging.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nl.surf.polynsi.client.certificate")
public class ClientCertificateProperties {

    // k8s ingress-nginx annotation header carrying the client certificate Subject DN,
    // sourced from nginx $ssl_client_s_dn in RFC 2253 format.
    // https://github.com/kubernetes/ingress-nginx/blob/main/docs/user-guide/nginx-configuration/annotations.md
    // https://nginx.org/en/docs/http/ngx_http_ssl_module.html
    public static final String K8S_NGINX_TLS_CLIENT_SUBJECT_DN_HEADER = "ssl-client-subject-dn";

    // Traefik passTLSClientCert header carrying the full client certificate as PEM
    // (delimiters and newlines stripped). A presented chain is comma-separated; PolyNSI
    // assumes the client (leaf) certificate is first -- see AuthInterceptor for the rationale.
    // https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/passtlsclientcert/#pem
    public static final String K8S_TRAEFIK_TLS_CLIENT_CERT_HEADER = "X-Forwarded-Tls-Client-Cert";

    // Traefik passTLSClientCert header carrying selected Subject fields as a URL-query-escaped
    // string. The Traefik admin chooses which fields are included; the DN format (RFC 2253 vs
    // RFC 4514) is not specified.
    // https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/passtlsclientcert/
    public static final String K8S_TRAEFIK_TLS_CLIENT_SUBJECT_DN_HEADER = "X-Forwarded-Tls-Client-Cert-Info";

    // Legacy path: the full client certificate read from the Jakarta servlet request
    // attribute (set when mTLS is terminated by PolyNSI itself), not an HTTP header.
    public static final String JAKARTA_SERVLET_TLS_CLIENT_CERT_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private static final Logger LOG = Logger.getLogger(ClientCertificateProperties.class.getName());

    // Note: authorizeDnType and tlsClientAuthNHeader must be consistent.
    private AuthorizeDnType authorizeDnType = AuthorizeDnType.NO;
    private String tlsClientAuthNHeader = K8S_NGINX_TLS_CLIENT_SUBJECT_DN_HEADER;
    private List<String> distinguishedNames;
    private List<String> subjectPublicKeyInfo;

    public AuthorizeDnType getAuthorizeDnType() {
        return authorizeDnType;
    }

    public void setAuthorizeDnType(AuthorizeDnType authorizeDnType) {
        this.authorizeDnType = authorizeDnType;
        LOG.fine("client certificate authorize distinguished name: " + authorizeDnType);
    }

    public String getTlsClientAuthNHeader() {
        return tlsClientAuthNHeader;
    }

    public void setTlsClientAuthNHeader(String tlsClientAuthNHeader) {
        this.tlsClientAuthNHeader = tlsClientAuthNHeader;
        LOG.fine("client certificate subject distinguished name header: " + tlsClientAuthNHeader);
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
