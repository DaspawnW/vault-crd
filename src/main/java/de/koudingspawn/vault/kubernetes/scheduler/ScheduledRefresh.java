package de.koudingspawn.vault.kubernetes.scheduler;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import de.koudingspawn.vault.kubernetes.EventHandler;
import de.koudingspawn.vault.kubernetes.event.EventNotification;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static de.koudingspawn.vault.kubernetes.event.EventType.MODIFICATION_FAILED;

@Component
public class ScheduledRefresh {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefresh.class);

    private final TypeRefreshFactory typeRefreshFactory;
    private final EventHandler eventHandler;
    private final EventNotification eventNotification;

    public ScheduledRefresh(
            EventHandler eventHandler,
            TypeRefreshFactory typeRefreshFactory,
            EventNotification eventNotification) {
        this.typeRefreshFactory = typeRefreshFactory;
        this.eventHandler = eventHandler;
        this.eventNotification = eventNotification;
    }

    public void refreshVaultResource(Vault resource) {
        RequiresRefresh requiresRefresh = typeRefreshFactory.get(resource.getSpec().getType().toString());
        try {
            if (requiresRefresh.refreshIsNeeded(resource)) {
                log.info("Executing scheduled refresh for {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
                eventHandler.modifyHandler(resource);
            }
        } catch (SecretNotAccessibleException e) {
            log.info("Refresh of secret {} in namespace {} failed with exception", resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);

            eventNotification.storeNewEvent(MODIFICATION_FAILED, "Modification of secret failed with exception " + e.getMessage(), resource);
        }
    }

}
