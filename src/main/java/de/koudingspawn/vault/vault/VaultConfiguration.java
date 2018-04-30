package de.koudingspawn.vault.vault;

import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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

}
