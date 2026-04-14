package nl.surf.polynsi;

import java.util.List;
import java.util.logging.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nl.surf.polynsi.client.certificate")
public class ClientCertificateProperties {

    /*
    # Client TLS certificate Subject DistinguishedName as HTTPS Header:
    # -----------------------------------------------------------------
    # Kubernetes ingress NGINX's annotation: https://github.com/kubernetes/ingress-nginx/blob/main/docs/user-guide/nginx-configuration/annotations.md
    # defined as 'The subject information of the client certificate. Example: "CN=My Client"'
    # If we ASSUME this is the $ssl_client_s_dn variable from ngx_http_ssl_module then this is
    # defined as (https://nginx.org/en/docs/http/ngx_http_ssl_module.html):
    # '$ssl_client_s_dn' returns the “subject DN” string of the client certificate for an
    #  established SSL connection according to RFC 2253 (1.11.6);'
    # So RFC2253 format. Note that itself is obsoleted by RFC4514, so NGINX has work to do.
    #
    */
    public static final String K8S_NGINX_TLS_CLIENT_SUBJECT_DN_HEADER = "ssl-client-subject-dn";

    /*
    # Full Client TLS certificate as HTTPS Header:
    # --------------------------------------------
    # For Traefik:
    # * https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/passtlsclientcert/
    # * ``that contains the pem.''
    # * https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/passtlsclientcert/#pem
    # * ``The delimiters and \n will be removed.
    # * If there are more than one certificate, they are separated by a ",".''
    # * More elaborate:
    # * https://doc.traefik.io/traefik/v2.1/middlewares/passtlsclientcert/
    # * ``In the example, it is the part between -----BEGIN CERTIFICATE----- and -----END CERTIFICATE----- delimiters :''
    #
    # * k8s ingress Traefik uses this header, see https://doc.traefik.io/traefik/v1.7/configuration/backends/kubernetes/#general-annotations
    # Traefik will send multiple certs if a chain is presented, separated by comma's.
    #
    # Undocumented: Client cert apparently is first in list
    # This issue says client cert comes first: https://github.com/keycloak/keycloak/issues/46395#issuecomment-3915177071
    # Code suggest extra certs follow client cert: https://github.com/tdiesler/keycloak/commit/8d318c552a2c778b65265f4c46a3b30c7dc99a27#diff-4a1b33f7b0a6b8526caf3186df5ccd193f7efcb683dcff9e515de2765ec9fd19R236
    # "Traefik sends the client certificate and any intermediate CA certificates as PEM blocks in a single `X-Forwarded-Tls-Client-Cert` header, separated by commas."
    #  --- https://github.com/tdiesler/keycloak/commit/8d318c552a2c778b65265f4c46a3b30c7dc99a27#diff-4a1b33f7b0a6b8526caf3186df5ccd193f7efcb683dcff9e515de2765ec9fd19R288
    # If Traefik this is in PEM with some changes, see above
    */
    public static final String K8S_TRAEFIK_TLS_CLIENT_CERT_HEADER = "X-Forwarded-Tls-Client-Cert";

    /*
    # Traefik can also send the subject info from the cert:
    # * https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/passtlsclientcert/
    # * ``X-Forwarded-Tls-Client-Cert-Info header value is a string that has been escaped in order to be a valid URL query.''
    # NOTE: Traefik adminstrator must select which fields are put in this Info field! Unclear
    # what format (RFC2253/RFC4514_ is used...
    #
    */
    public static final String K8S_TRAEFIK_TLS_CLIENT_SUBJECT_DN_HEADER = "X-Forwarded-Tls-Client-Cert-Info";

    /*
    # Legacy: PolyNSI can also take the full client certificate from the
    # ""jakarta.servlet.request.X509Certificate" header.
    */
    public static final String JAKARTA_SERVLET_TLS_CLIENT_CERT_HEADER = "jakarta.servlet.request.X509Certificate";

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
