package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.DoneableVault;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.HttpURLConnection;
import java.util.Optional;

@Configuration
@Profile("!test")
public class Watcher {

    private static final Logger log = LoggerFactory.getLogger(Watcher.class);

    private final EventHandler eventHandler;
    private final MixedOperation<Vault, VaultList, DoneableVault, Resource<Vault, DoneableVault>> customResource;

    public Watcher(EventHandler eventHandler, MixedOperation<Vault, VaultList, DoneableVault, Resource<Vault, DoneableVault>> customResource) {
        this.eventHandler = eventHandler;
        this.customResource = customResource;
    }

    @Bean
    CommandLineRunner watchForResource() {
        return (args) -> run();
    }

    private void run() {
        customResource.inAnyNamespace().watch(new io.fabric8.kubernetes.client.Watcher<Vault>() {
            @Override
            public void eventReceived(Action action, Vault resource) {
                log.info("Received action: {} for {} in namespace {}", action.name(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());

                switch (action) {
                    case ADDED:
                        eventHandler.addHandler(resource);
                        break;
                    case MODIFIED:
                        eventHandler.modifyHandler(resource);
                        break;
                    case DELETED:
                        eventHandler.deleteHandler(resource);
                        break;
                    default:
                        log.error("Handling of action failed, not implemented action");
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
                Optional<Integer> statusCode = Optional.ofNullable(cause)
                        .map(KubernetesClientException::getStatus)
                        .map(Status::getCode);

                int status = statusCode.orElse(-1);
                if (status == HttpURLConnection.HTTP_GONE) {
                    log.warn("Http Gone exception ignored!");
                    run();
                } else {
                    log.error("Watch for custom resource failed with status: " + status, cause);
                    System.exit(1);
                }
            }
        });
    }

}
