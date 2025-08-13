package nl.surf.polynsi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

@Configuration
public class SslSystemPropertiesConfig {

    private static final Logger LOG = Logger.getLogger(SslSystemPropertiesConfig.class.getName());

    @Value("${javax.net.ssl.keyStoreType}")
    private String keyStoreType;

    @Value("${javax.net.ssl.keyStore}")
    private String keyStore;

    @Value("${javax.net.ssl.keyStorePassword}")
    private String keyStorePassword;

    @Value("${javax.net.ssl.trustStoreType}")
    private String trustStoreType;

    @Value("${javax.net.ssl.trustStore}")
    private String trustStore;

    @Value("${javax.net.ssl.trustStorePassword}")
    private String trustStorePassword;

    @Bean
    public ApplicationRunner sslPropsInitializer() {
        LOG.info("set javax.net.ssl.* system properties from application.properties");
        return args -> {
            System.setProperty("javax.net.ssl.keyStoreType", resolveClasspath(keyStoreType));
            System.setProperty("javax.net.ssl.keyStore", resolveClasspath(keyStore));
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", resolveClasspath(trustStoreType));
            System.setProperty("javax.net.ssl.trustStore", resolveClasspath(trustStore));
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        };
    }

    private String resolveClasspath(String path) {
        if (path.startsWith("classpath:")) {
            return getClass().getResource(path.replace("classpath:", "/")).getPath();
        }
        return path;
    }
}
