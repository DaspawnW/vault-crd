package de.koudingspawn.vault.kubernetes.scheduler;

import de.koudingspawn.vault.crd.DoneableVault;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import de.koudingspawn.vault.kubernetes.EventHandler;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ScheduledRefresh {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefresh.class);

    private final TypeRefreshFactory typeRefreshFactory;
    private final EventHandler eventHandler;
    private final MixedOperation<Vault, VaultList, DoneableVault, Resource<Vault, DoneableVault>> customResource;

    public ScheduledRefresh(
            EventHandler eventHandler,
            TypeRefreshFactory typeRefreshFactory,
            MixedOperation<Vault, VaultList, DoneableVault, Resource<Vault, DoneableVault>> customResource) {
        this.typeRefreshFactory = typeRefreshFactory;
        this.eventHandler = eventHandler;
        this.customResource = customResource;
    }

    @Scheduled(fixedRateString = "${kubernetes.interval}000", initialDelayString = "${kubernetes.initial-delay}000")
    public void refreshCertificates() {
        log.info("Start refresh of secret...");

        VaultList list = customResource.inAnyNamespace().list();
        for (Vault resource : list.getItems()) {
            RequiresRefresh requiresRefresh = typeRefreshFactory.get(resource.getSpec().getType().toString());
            try {
                if (requiresRefresh.refreshIsNeeded(resource)) {
                    eventHandler.modifyHandler(resource);
                }
            } catch (SecretNotAccessibleException e) {
                log.info("Refresh of secret {} in namespace {} failed with exception", resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
            }
        }

        log.info("Finished refresh of secret...");

    }



}
