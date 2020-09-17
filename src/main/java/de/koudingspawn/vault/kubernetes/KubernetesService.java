package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import io.fabric8.kubernetes.api.model.DoneableSecret;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;
import static de.koudingspawn.vault.Constants.LAST_UPDATE_ANNOTATION;

@Component
public class KubernetesService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesService.class);

    private final KubernetesClient client;
    private final String crdName;

    public KubernetesService(KubernetesClient client, @Value("${kubernetes.crd.name}") String crdName) {
        this.client = client;
        this.crdName = crdName;
    }

    boolean exists(Vault resource) {
        return getSecretByVault(resource) != null;
    }

    private Secret newSecretInstance(Vault resource, VaultSecret vaultSecret){
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
        client.secrets().inNamespace(resourceMetadata.getNamespace()).withName(resourceMetadata.getName()).cascading(true).delete();
        log.info("Deleted secret {} in namespace {}", resourceMetadata.getName(), resourceMetadata.getNamespace());
    }

    void modifySecret(Vault resource, VaultSecret vaultSecret) {
        Resource<Secret, DoneableSecret> secretDoneableSecretResource = client.secrets().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName());
        Secret secret;

        if (secretDoneableSecretResource.get() != null) {
            secret = secretDoneableSecretResource.get();
        } else {
            secret = newSecretInstance(resource, vaultSecret);
        }

        secret.setType(vaultSecret.getType());
        updateAnnotations(secret, vaultSecret.getCompare());
        secret.setData(vaultSecret.getData());

        secretDoneableSecretResource.createOrReplace(secret);
        log.info("Modified secret {} in namespace {}", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
    }

    public Secret getSecretByVault(Vault resource) {
        Resource<Secret, DoneableSecret> secretDoneableSecretResource =
                client.secrets().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName());

        return secretDoneableSecretResource.get();
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

        boolean blockOwnerDeletion = false;
        boolean controller = true;
        OwnerReference owner = new OwnerReference(
          crdName + "/v1",
          blockOwnerDeletion,
          controller,
          "Vault",
          resource.getName(),
          resource.getUid()
        );
        ArrayList<OwnerReference> owners = new ArrayList<>();
        owners.add(owner);
        meta.setOwnerReferences(owners);

        return meta;
    }


    private void updateAnnotations(Secret secret, String compare) {
        if (secret.getMetadata().getAnnotations() == null) {
            secret.getMetadata().setAnnotations(new HashMap<>());
        }

        Map<String, String> annotations = secret.getMetadata().getAnnotations();
        annotations.put(crdName + LAST_UPDATE_ANNOTATION, LocalDateTime.now().toString());
        annotations.put(crdName + COMPARE_ANNOTATION, compare);
    }
}
