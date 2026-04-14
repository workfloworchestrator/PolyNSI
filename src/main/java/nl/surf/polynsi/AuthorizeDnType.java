package nl.surf.polynsi;

// See ClientCertificateProperties for explanation of the enum values.

public enum AuthorizeDnType {
    NO,
    NGINX_TLS_CLIENT_SUBJECT_DN,
    JAKARTA_SERVLET_TLS_CLIENT_CERT, // "jakarta.servlet.request.X509Certificate"
    TRAEFIK_TLS_CLIENT_CERT,
    TRAEFIK_TLS_CLIENT_SUBJECT_DN,
}
