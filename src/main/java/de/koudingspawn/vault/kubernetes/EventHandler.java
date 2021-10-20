package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.event.EventNotification;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static de.koudingspawn.vault.kubernetes.event.EventType.*;

@Component
public class EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandler.class);

    private final VaultService vaultService;
    private final KubernetesService kubernetesService;
    private final ChangeAdjustmentService changeAdjustmentService;
    private final EventNotification eventNotification;
    private final boolean fixOwnerReferenceEnabled;

    public EventHandler(VaultService vaultService,
                        KubernetesService kubernetesService,
                        ChangeAdjustmentService changeAdjustmentService,
                        EventNotification eventNotification,
                        @Value("${kubernetes.ownerreference-fix.enabled:true}") boolean fixOwnerReferenceEnabled) {
        this.vaultService = vaultService;
        this.kubernetesService = kubernetesService;
        this.changeAdjustmentService = changeAdjustmentService;
        this.eventNotification = eventNotification;
        this.fixOwnerReferenceEnabled = fixOwnerReferenceEnabled;
    }

    public void addHandler(Vault resource) {
        if (!kubernetesService.exists(resource)) {
            try {
                VaultSecret secretContent = vaultService.generateSecret(resource);
                kubernetesService.createSecret(resource, secretContent);

                eventNotification.storeNewEvent(CREATION_SUCCESSFUL, "Successfully created secret", resource);
            } catch (Exception e) {
                log.error("Failed to generate secret for vault resource {} in namespace {} failed with exception:",
                        resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);

                eventNotification.storeNewEvent(CREATION_FAILED, "Failed to generate secret with exception " + e.getMessage(), resource);
            }
        } else if (fixOwnerReferenceEnabled && kubernetesService.hasBrokenOwnerReference(resource)) {
            log.info("Fix owner reference for secret {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
            modifyHandler(resource);

            eventNotification.storeNewEvent(FIXED_REFERENCE, "Fixed owner reference", resource);
        }
    }

    void deleteHandler(Vault resource) {
        kubernetesService.deleteSecret(resource.getMetadata());

        eventNotification.storeNewEvent(DELETION, "Deleted secret for resource", resource);
    }

    public void modifyHandler(Vault resource) {

        try {
            VaultSecret secretContent = vaultService.generateSecret(resource);
            kubernetesService.modifySecret(resource, secretContent);

            eventNotification.storeNewEvent(MODIFICATION_SUCCESSFUL, "Successfully modified secret", resource);

            if (resource.getSpec().getChangeAdjustmentCallback() != null) {
                changeAdjustmentService.handle(resource);
                eventNotification.storeNewEvent(ROTATION,
                        "Successfully started rotation of associated resource " + resource.getSpec().getChangeAdjustmentCallback().toString(), resource);
            }
        } catch (Exception e) {
            log.error("Failed to modify secret for vault resource {} in namespace {} failed with exception:",
                    resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);

            eventNotification.storeNewEvent(MODIFICATION_FAILED, "Modification of secret failed with exception " + e.getMessage(), resource);
        }
    }

}
