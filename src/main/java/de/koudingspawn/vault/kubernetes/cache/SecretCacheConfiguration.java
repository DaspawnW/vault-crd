package de.koudingspawn.vault.kubernetes.cache;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SecretCacheConfiguration {

    @Bean
    @Profile("!test")
    public SecretCache secretCache(KubernetesClient client) {
        return new SecretCache(client, true);
    }

    @Bean
    @Profile("test")
    public SecretCache testSecretCache(KubernetesClient client) {
        return new SecretCache(client, false);
    }

}
