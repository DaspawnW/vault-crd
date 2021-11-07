package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;
import static de.koudingspawn.vault.Constants.LAST_UPDATE_ANNOTATION;

@Component
public class KubernetesService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesService.class);

    private final KubernetesClient client;
    private final String crdName;
    private final String crdGroup;

    public KubernetesService(KubernetesClient client,
                             @Value("${kubernetes.crd.name}") String crdName,
                             @Value("${kubernetes.crd.group}") String crdGroup) {
        this.client = client;
        this.crdName = crdName;
        this.crdGroup = crdGroup;
    }

    boolean exists(Vault resource) {
        return getSecretByVault(resource) != null;
    }

    private Secret newSecretInstance(Vault resource, VaultSecret vaultSecret) {
        Secret secret = new Secret();
        secret.setType(vaultSecret.getType());
        secret.setMetadata(metaData(resource.getMetadata(), vaultSecret.getCompare()));
        secret.setData(vaultSecret.getData());

        return secret;
    }

    void createSecret(Vault resource, VaultSecret vaultSecret) {
        client.secrets().inNamespace(resource.getMetadata().getNamespace()).create(newSecretInstance(resource, vaultSecret));

        log.info("Created secret for vault resource {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
    }

    void deleteSecret(ObjectMeta resourceMetadata) {
        client.secrets().inNamespace(resourceMetadata.getNamespace()).withName(resourceMetadata.getName()).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
        log.info("Deleted secret {} in namespace {}", resourceMetadata.getName(), resourceMetadata.getNamespace());
    }

    void modifySecret(Vault resource, VaultSecret vaultSecret) {
        Resource<Secret> secretResource = client.secrets().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName());
        Secret secret;

        if (secretResource.get() != null) {
            secret = secretResource.get();
        } else {
            secret = newSecretInstance(resource, vaultSecret);
        }

        secret.setType(vaultSecret.getType());
        secret.setMetadata(metaData(resource.getMetadata(), vaultSecret.getCompare()));
        secret.setData(vaultSecret.getData());

        secretResource.createOrReplace(secret);
        log.info("Modified secret {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
    }

    public Secret getSecretByVault(Vault resource) {
        Resource<Secret> secretResource =
                client.secrets().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName());

        return secretResource.get();
    }

    private ObjectMeta metaData(ObjectMeta resource, String compare) {
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(resource.getNamespace());
        meta.setName(resource.getName());
        if (resource.getLabels() != null) {
            meta.setLabels(resource.getLabels());
        }

        HashMap<String, String> annotations = new HashMap<>();
        if (resource.getAnnotations() != null) {
            annotations.putAll(resource.getAnnotations());
        }
        annotations.put(crdName + LAST_UPDATE_ANNOTATION, LocalDateTime.now().toString());
        annotations.put(crdName + COMPARE_ANNOTATION, compare);
        meta.setAnnotations(annotations);
        meta.setOwnerReferences(getOwnerReference(resource));

        return meta;
    }

    private List<OwnerReference> getOwnerReference(ObjectMeta resource) {
        boolean blockOwnerDeletion = false;
        boolean controller = true;
        OwnerReference owner = new OwnerReference(
                crdGroup + "/v1",
                blockOwnerDeletion,
                controller,
                "Vault",
                resource.getName(),
                resource.getUid()
        );
        ArrayList<OwnerReference> owners = new ArrayList<>();
        owners.add(owner);

        return owners;
    }

    public boolean hasBrokenOwnerReference(Vault resource) {
        Resource<Secret> secretResource = client.secrets().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName());

        if (secretResource.get() != null) {
            Secret secret = secretResource.get();

            if (secret.getMetadata() != null && secret.getMetadata().getOwnerReferences() != null && secret.getMetadata().getOwnerReferences().size() == 1) {
                OwnerReference ownerReference = secret.getMetadata().getOwnerReferences().get(0);
                if (ownerReference.getApiVersion().equals(crdName + "/v1")) {
                    return true;
                }
            }
        }

        return false;
    }
}
