package nl.surf.polynsi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.springframework.stereotype.Component;

/*
 * Aux class to compare Distinguished Names from various sources using the Java X500Principal class:
 *
 *    https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/security/auth/x500/X500Principal.html
 *    https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/javax/security/auth/x500/X500Principal.java
 *
 * Various sources can be:
    1. k8s ingress:
    # Kubernetes ingress NGINX's annotation: https://github.com/kubernetes/ingress-nginx/blob/main/docs/user-guide/nginx-configuration/annotations.md
    # defined as 'The subject information of the client certificate. Example: "CN=My Client"'
    # If we ASSUME this is the $ssl_client_s_dn variable from ngx_http_ssl_module then this is
    # defined as (https://nginx.org/en/docs/http/ngx_http_ssl_module.html):
    # '$ssl_client_s_dn' returns the “subject DN” string of the client certificate for an
    #  established SSL connection according to RFC 2253 (1.11.6);'
    # So RFC2253 format. Note that itself is obsoleted by RFC4514, so NGINX has work to do.
    #

   2. ClientCertificateProperties in the application.properties file. These are strings and we
   want to be somewhat flexible in the format there, so if X500Principal can parse it, we accept the input.
   @author: Arno Bakker
*/

@Component
public class ClientPrincipals {

    private static final Logger LOG = Logger.getLogger(ClientPrincipals.class.getName());
    private List<X500Principal> allowedPrincipals;

    public ClientPrincipals(List<String> propDistinguishedNames) {
        // Convert from application.properties input, which is assumed to be somewhat free format to normal form
        // in this case RFC2253, because RFC4514 is apparently not yet implemented by Java.
        this.allowedPrincipals = new ArrayList<X500Principal>();
        for (String propDistinguishedName : propDistinguishedNames) {
            try {
                X500Principal p = new X500Principal(propDistinguishedName);
                // rfc2253DistinguishedName = p.getName(X500Principal.RFC2253);
                this.allowedPrincipals.add(p);
            } catch (Exception e) {
                LOG.fine(propDistinguishedName + " not a proper DN:" + e);
                // continue parsing others
            }
        }
        for (X500Principal p : this.allowedPrincipals) {
            String rfc2253DistinguishedName = p.getName(X500Principal.RFC2253);
            LOG.fine("added client certificate distinguished name: " + rfc2253DistinguishedName);
        }
    }

    public boolean isAllowedPrincipal(X500Principal sslClientSubjectPrincipal) {
        for (X500Principal allowedPrincipal : allowedPrincipals) {
            // Main authentication line
            // javax.security.auth.x500.X500Principal object equals() method does comparison
            if (sslClientSubjectPrincipal.equals(allowedPrincipal)) return true;
        }
        return false;
    }
}
