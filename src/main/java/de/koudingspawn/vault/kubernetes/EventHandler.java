package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.VaultService;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandler.class);

    private final VaultService vaultService;
    private final KubernetesService kubernetesService;
    private final ChangeAdjustmentService changeAdjustmentService;

    public EventHandler(VaultService vaultService, KubernetesService kubernetesService, ChangeAdjustmentService changeAdjustmentService) {
        this.vaultService = vaultService;
        this.kubernetesService = kubernetesService;
        this.changeAdjustmentService = changeAdjustmentService;
    }

    public void addHandler(Vault resource) {
        if (!kubernetesService.exists(resource)) {
            try {
                VaultSecret secretContent = vaultService.generateSecret(resource);
                kubernetesService.createSecret(resource, secretContent);
            } catch (SecretNotAccessibleException e) {
                log.error("Failed to generate secret for vault resource {} in namespace {} failed with exception:",
                        resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
            }
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
        } catch (SecretNotAccessibleException e) {
            log.error("Failed to modify secret for vault resource {} in namespace {} failed with exception:",
                    resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
        }
    }

}
