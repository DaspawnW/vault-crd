package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandler.class);

    private final VaultService vaultService;
    private final KubernetesService kubernetesService;
    private final ChangeAdjustmentService changeAdjustmentService;
    private final boolean fixOwnerReferenceEnabled;

    public EventHandler(VaultService vaultService,
                        KubernetesService kubernetesService,
                        ChangeAdjustmentService changeAdjustmentService,
                        @Value("${kubernetes.ownerreference-fix.enabled:true}") boolean fixOwnerReferenceEnabled) {
        this.vaultService = vaultService;
        this.kubernetesService = kubernetesService;
        this.changeAdjustmentService = changeAdjustmentService;
        this.fixOwnerReferenceEnabled = fixOwnerReferenceEnabled;
    }

    public void addHandler(Vault resource) {
        if (!kubernetesService.exists(resource)) {
            try {
                VaultSecret secretContent = vaultService.generateSecret(resource);
                kubernetesService.createSecret(resource, secretContent);
            } catch (Exception e) {
                log.error("Failed to generate secret for vault resource {} in namespace {} failed with exception:",
                        resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
            }
        } else if (fixOwnerReferenceEnabled && kubernetesService.hasBrokenOwnerReference(resource)) {
            log.info("Fix owner reference for secret {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
            modifyHandler(resource);
        }
    }

    void deleteHandler(Vault resource) {
        kubernetesService.deleteSecret(resource.getMetadata());
    }

    public void modifyHandler(Vault resource) {

        try {
            VaultSecret secretContent = vaultService.generateSecret(resource);
            kubernetesService.modifySecret(resource, secretContent);

            if (resource.getSpec().getChangeAdjustmentCallback() != null) {
                changeAdjustmentService.handle(resource);
            }
        } catch (Exception e) {
            log.error("Failed to modify secret for vault resource {} in namespace {} failed with exception:",
                    resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
        }
    }

}
