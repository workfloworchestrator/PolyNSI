package nl.surf.polynsi;

// See ClientCertificateProperties for explanation of the enum values.
// To use these values in Spring Boot properties, convert to lower case and replace _ with -.
// This is apparently the conversion method used: https://github.com/spring-projects/spring-tools/issues/1605

public enum AuthorizeDnType {
    NO,
    NGINX_TLS_CLIENT_SUBJECT_DN,
    JAKARTA_SERVLET_TLS_CLIENT_CERT, // "jakarta.servlet.request.X509Certificate"
    TRAEFIK_TLS_CLIENT_CERT,
    TRAEFIK_TLS_CLIENT_SUBJECT_DN,
}
