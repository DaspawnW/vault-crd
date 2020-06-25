package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.VaultService;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandler.class);

    private final VaultService vaultService;
    private final KubernetesService kubernetesService;

    public EventHandler(VaultService vaultService, KubernetesService kubernetesService) {
        this.vaultService = vaultService;
        this.kubernetesService = kubernetesService;
    }

    protected void checkSecretWhitelist (VaultSecret secretContent, Vault resource) throws SecretNotAccessibleException {
        for (Entry<String,String> entry : secretContent.getData().entrySet ()) {
            if (entry.getKey().equals ("x-vault-crd-namespaces")) {
                String decodedValue = new String(Base64.getDecoder().decode(entry.getValue()));
                List<String> allowedNamespaces = Arrays.asList(decodedValue.split(","));
                if (!allowedNamespaces.contains(resource.getMetadata().getNamespace())) {
                    kubernetesService.deleteSecret(resource.getMetadata());
                    String message = String.format(
                        "Namespace is not whitelisted for secret '%s'",
                        resource.getMetadata().getName()
                    );
                    throw new SecretNotAccessibleException(message);
                }
            }
        }
    }

    public void addHandler(Vault resource) {
        if (!kubernetesService.exists(resource)) {
            try {
                VaultSecret secretContent = vaultService.generateSecret(resource);
                checkSecretWhitelist(secretContent, resource);
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
            checkSecretWhitelist(secretContent, resource);
            kubernetesService.modifySecret(resource, secretContent);
        } catch (SecretNotAccessibleException e) {
            log.error("Failed to modify secret for vault resource {} in namespace {} failed with exception:",
                    resource.getMetadata().getName(), resource.getMetadata().getNamespace(), e);
        }
    }

}
