package nl.surf.polynsi;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/*
 * Generates throw-away, self-signed X.509 certificates with fictional subjects so tests do not embed
 * real certificates or personal data. The subject is encoded via ClientPrincipals.parsePrincipal, so a
 * certificate minted from a DN string is byte-identical to that same string configured as an allowed DN
 * (this matters for attribute types such as organizationIdentifier that X500Principal compares by raw DER).
 */
final class TestCertificates {

    private TestCertificates() {}

    static X509Certificate selfSigned(String distinguishedName) {
        try {
            X500Principal subject = ClientPrincipals.parsePrincipal(distinguishedName);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            KeyPair keyPair = generator.generateKeyPair();
            Instant now = Instant.now();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(System.nanoTime()),
                    Date.from(now.minus(1, ChronoUnit.DAYS)),
                    Date.from(now.plus(365, ChronoUnit.DAYS)),
                    subject,
                    keyPair.getPublic());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());
            return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new IllegalStateException("failed to generate test certificate for " + distinguishedName, e);
        }
    }

    // Mimic Traefik's X-Forwarded-Tls-Client-Cert header: base64 DER per cert, comma-separated,
    // PEM delimiters and newlines stripped.
    static String traefikHeader(X509Certificate... chain) {
        return Arrays.stream(chain).map(TestCertificates::base64Der).collect(Collectors.joining(","));
    }

    private static String base64Der(X509Certificate cert) {
        try {
            return Base64.getEncoder().encodeToString(cert.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
