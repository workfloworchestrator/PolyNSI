package nl.surf.polynsi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;

/*
 * Compares distinguished names from various sources (documented in ClientCertificateProperties)
 * using the Java X500Principal class.
 * https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/security/auth/x500/X500Principal.html
 */

public class ClientPrincipals {

    private static final Logger LOG = Logger.getLogger(ClientPrincipals.class.getName());
    private List<X500Principal> allowedPrincipals;

    public ClientPrincipals(List<String> propDistinguishedNames) {
        this.allowedPrincipals = new ArrayList<X500Principal>();

        // A null list is tolerated (see AuthInterceptorTest.rejectsNullDistinguishedNamesList).
        if (propDistinguishedNames == null) {
            LOG.fine("propDistinguishedNames is null");
            return;
        }

        for (String propDistinguishedName : propDistinguishedNames) {
            try {
                this.allowedPrincipals.add(parsePrincipal(propDistinguishedName));
            } catch (Exception e) {
                // Fail fast: a typo in the allow-list would otherwise only surface later as an
                // authorized client being rejected, which is hard to diagnose.
                throw new IllegalArgumentException(
                        "configured allowed distinguished name is not a valid DN: " + propDistinguishedName, e);
            }
        }
        for (X500Principal p : this.allowedPrincipals) {
            String rfc2253DistinguishedName = p.getName(X500Principal.RFC2253);
            LOG.fine("added client certificate distinguished name: " + rfc2253DistinguishedName);
        }
    }

    /*
     * Parse a distinguished name -- from application.properties or from a proxy-supplied header -- into
     * an X500Principal. Input may be in RFC 1779 or RFC 2253 grammar (X500Principal accepts either). The
     * names2oid map teaches X500Principal the symbolic attribute names that not all x509 implementations
     * know; it must be applied to both the allowed DNs and the incoming DN so the two parse identically.
     * https://docs.oracle.com/en/java/javase/26/docs/api/java.base/javax/security/auth/x500/X500Principal.html#%3Cinit%3E(java.lang.String,java.util.Map)
     */
    public static X500Principal parsePrincipal(String distinguishedName) {
        Map<String, String> names2oid = new HashMap<>();
        // Keep synchronized with nsi-auth/rfc4514_cmp.py
        // Oracle: "Keywords MUST be specified in all upper-case, otherwise they will be ignored."
        names2oid.put("SN", "2.5.4.4"); // surname
        names2oid.put("GN", "2.5.4.42"); // givenname
        names2oid.put("ORGANIZATIONIDENTIFIER", "2.5.4.97");
        names2oid.put("EMAILADDRESS", "1.2.840.113549.1.9.1");
        return new X500Principal(distinguishedName, names2oid);
    }

    public int numberOfAllowedPrincipals() {
        return allowedPrincipals.size();
    }

    public boolean isAllowedPrincipal(X500Principal sslClientSubjectPrincipal) {
        for (X500Principal allowedPrincipal : allowedPrincipals) {
            // X500Principal.equals() performs the actual authentication comparison.
            if (sslClientSubjectPrincipal.equals(allowedPrincipal)) return true;
        }
        return false;
    }
}
