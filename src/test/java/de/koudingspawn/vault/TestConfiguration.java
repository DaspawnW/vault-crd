package de.koudingspawn.vault;

import de.koudingspawn.vault.crd.DoneableVault;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import de.koudingspawn.vault.kubernetes.KubernetesService;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
@Import({KubernetesService.class, VaultApplication.class})
public class TestConfiguration {


    @Bean
    @Primary
    public KubernetesClient client(KubernetesServer server) {
        createCustomDefinition(server.getClient());
        return server.getClient();
    }

    @Bean
    public KubernetesServer server() {
        KubernetesServer kubernetesServer = new KubernetesServer(true, true);
        kubernetesServer.before();
        return kubernetesServer;
    }

    @Bean
    @Primary
    public MixedOperation<Vault, VaultList, DoneableVault, Resource<Vault, DoneableVault>> customResource(
            KubernetesClient client) {
        List<CustomResourceDefinition> crdResource
                = client.customResourceDefinitions().list().getItems();
        return client.customResources(crdResource.get(0), Vault.class, VaultList.class, DoneableVault.class);
    }

    @Bean
    @Primary
    public CommandLineRunner watchForResource() {
        return args -> {
        };
    }

    private static void createCustomDefinition(KubernetesClient client) {
        CustomResourceDefinition crDefinition = new CustomResourceDefinitionBuilder()
                .withApiVersion("apiextensions.k8s.io/v1beta1")
                .withNewMetadata()
                .withName("vault.koudingspawn.de")
                .endMetadata()
                .withNewSpec()
                .withGroup("koudingspawn.de")
                .withVersion("v1")
                .withScope("Namespaced")
                .withNewNames()
                .withKind("Vault")
                .withShortNames("vt")
                .withPlural("vault")
                .withSingular("vault")
                .endNames()
                .endSpec()
                .build();
        client.customResourceDefinitions().create(crDefinition);
    }

}
