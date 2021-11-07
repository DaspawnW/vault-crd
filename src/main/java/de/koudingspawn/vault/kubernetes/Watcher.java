package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class Watcher {

    private static final Logger log = LoggerFactory.getLogger(Watcher.class);

    private final EventHandler eventHandler;
    private final KubernetesClient client;

    public Watcher(EventHandler eventHandler, KubernetesClient client) {
        this.eventHandler = eventHandler;
        this.client = client;
    }

    @Bean
    CommandLineRunner watchForResource() {
        return (args) -> run();
    }

    private void run() {
        SharedInformerFactory sharedInformerFactory = client.informers();
        SharedIndexInformer<Vault> vaultInformer = sharedInformerFactory.sharedIndexInformerFor(Vault.class, 0L);
        vaultInformer.addEventHandler(
                new ResourceEventHandler<Vault>() {
                    @Override
                    public void onAdd(Vault resource) {
                        log.info("Received add for {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                        eventHandler.addHandler(resource);
                    }

                    @Override
                    public void onUpdate(Vault oldObj, Vault resource) {
                        log.info("Received update for {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                        eventHandler.modifyHandler(resource);
                    }

                    @Override
                    public void onDelete(Vault resource, boolean deletedFinalStateUnknown) {
                        log.info("Received delete for {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                        eventHandler.deleteHandler(resource);
                    }
                }
        );

        sharedInformerFactory.addSharedInformerEventListener(ex ->
                log.error("Exception occurred in shared informer, but caught: {}", ex.getMessage()));

        log.info("Starting informer");
        sharedInformerFactory.startAllRegisteredInformers();
    }

}
