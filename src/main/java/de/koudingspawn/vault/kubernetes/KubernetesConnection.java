package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class KubernetesConnection {

    private static final Logger log = LoggerFactory.getLogger(KubernetesConnection.class);

    @Bean
    @Profile("development")
    public KubernetesClient testClient() {
        Config config = new ConfigBuilder().withMasterUrl("http://localhost:8001").withWatchReconnectLimit(5).build();
        return new KubernetesClientBuilder()
                .withConfig(config)
                .build();
    }

    @Bean
    @Profile("!development")
    public KubernetesClient client() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public MixedOperation<Vault, VaultList, Resource<Vault>> customResource(
            KubernetesClient client,
            @Value("${kubernetes.crd.name}") String crdName) {
        Resource<CustomResourceDefinition> crdResource = client.apiextensions().v1().customResourceDefinitions().withName(crdName);
        CustomResourceDefinition customResourceDefinition = crdResource.get();
        if (customResourceDefinition == null) {
            log.error("Please first apply custom resource definition and then restart vault-crd");
            System.exit(1);
        }

        return client.resources(Vault.class, VaultList.class);
    }
}
