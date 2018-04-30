package de.koudingspawn.vault.kubernetes.scheduler;

import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshConfiguration {

    @Bean("typeRefreshFactory")
    public ServiceLocatorFactoryBean slfbForTypeRefresh() {
        ServiceLocatorFactoryBean slfb = new ServiceLocatorFactoryBean();
        slfb.setServiceLocatorInterface(TypeRefreshFactory.class);
        return slfb;
    }

}
