package nl.surf.polynsi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.springframework.stereotype.Component;

/*
 * Aux class to compare Distinguished Names from various sources using the Java X500Principal class:
 *
 *    https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/security/auth/x500/X500Principal.html
 *    https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/javax/security/auth/x500/X500Principal.java
 *
 * Various sources are documented in ClientCertificateProperties.
 */

@Component
public class ClientPrincipals {

    private static final Logger LOG = Logger.getLogger(ClientPrincipals.class.getName());
    private List<X500Principal> allowedPrincipals;

    public ClientPrincipals(List<String> propDistinguishedNames) {
        this.allowedPrincipals = new ArrayList<X500Principal>();

        // Please AuthInterceptorTest.rejectsNullDistinguishedNamesList()
        if (propDistinguishedNames == null) {
            LOG.fine("propDistinguishedNames is null");
            return;
        }

        // Convert from application.properties input, which is assumed to be somewhat free format to normal form
        // in this case RFC2253, because RFC4514 is apparently not yet implemented by Java.
        for (String propDistinguishedName : propDistinguishedNames) {
            try {
                // Arno: this parser is somewhat flexible regarding format. WONTFIX more flexible parsing as in
                // nsi-auth.
                // https://docs.oracle.com/en/java/javase/26/docs/api/java.base/javax/security/auth/x500/X500Principal.html#%3Cinit%3E(java.lang.String,java.util.Map)
                // "The distinguished name must be specified using the grammar defined in RFC 1779 or RFC 2253 (either
                // format is acceptable)."
                Map<String, String> names2oid = new HashMap<>();
                // Not all x509 implementations know the symbolic names for all OIDs.
                // This table fixes the most common ones.
                // Keep synchronized with nsi-auth/rfc4514_cmp.py
                // Oracle: "Keywords MUST be specified in all upper-case, otherwise they will be ignored."
                names2oid.put("SN", "2.5.4.4"); // surname
                names2oid.put("GN", "2.5.4.42"); // givenname
                names2oid.put("ORGANIZATIONIDENTIFIER", "2.5.4.97");
                names2oid.put("EMAILADDRESS", "1.2.840.113549.1.9.1");

                X500Principal p = new X500Principal(propDistinguishedName, names2oid);
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

    public int numberOfAllowedPrincipals() {
        return allowedPrincipals.size();
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
