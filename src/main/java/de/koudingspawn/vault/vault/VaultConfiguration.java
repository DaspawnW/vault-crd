package de.koudingspawn.vault.vault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Configuration
public class VaultConfiguration {

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder()
                .build();
    }

    @Bean
    public ServiceLocatorFactoryBean slfbForTypeRefresh() {
        ServiceLocatorFactoryBean slfb = new ServiceLocatorFactoryBean();
        slfb.setServiceLocatorInterface(TypedSecretGeneratorFactory.class);
        return slfb;
    }

    @Configuration
    class VaultConnection extends AbstractVaultConfiguration {

        private final String vaultToken;
        private final String vaultUrl;

        VaultConnection(@Value("${kubernetes.vault.token}") String vaultToken,
                        @Value("${kubernetes.vault.url}") String vaultUrl) {
            this.vaultToken = vaultToken;
            this.vaultUrl = vaultUrl;
        }

        @Override
        public VaultEndpoint vaultEndpoint() {
            return VaultEndpoint.from(getVaultUrlWithoutPath());
        }

        @Override
        public ClientAuthentication clientAuthentication() {
            return new TokenAuthentication(vaultToken);
        }

        private URI getVaultUrlWithoutPath() {
            return URI.create(vaultUrl.replace("/v1/", ""));
        }
    }

}
