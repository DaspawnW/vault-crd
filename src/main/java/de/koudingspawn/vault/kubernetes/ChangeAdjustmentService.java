package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultChangeAdjustmentCallback;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChangeAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(ChangeAdjustmentService.class);

    private final KubernetesClient client;

    public ChangeAdjustmentService(KubernetesClient client) {
        this.client = client;
    }

    public void handle(Vault resource) {
        VaultChangeAdjustmentCallback changeAdjustmentCallback = resource.getSpec().getChangeAdjustmentCallback();
        if (changeAdjustmentCallback != null && changeAdjustmentCallback.getType() != null && changeAdjustmentCallback.getName() != null) {
            switch (changeAdjustmentCallback.getType().toLowerCase()) {
                case "deployment" ->
                        rotateDeployment(resource.getMetadata().getNamespace(), changeAdjustmentCallback.getName());
                case "statefulset" ->
                        rotateStatefulSet(resource.getMetadata().getNamespace(), changeAdjustmentCallback.getName());
                default ->
                        log.info("Currently a change adjustment is only supported for type deployment. Resource {} in namespace {} has type {}",
                                resource.getMetadata().getName(), resource.getMetadata().getNamespace(), changeAdjustmentCallback.getType());
            }
        } else {
            log.warn("Change adjustment callback for resource {} in namespace {} is invalid!", resource.getMetadata().getName(), resource.getMetadata().getNamespace());
        }
    }

    private void rotateDeployment(String namespace, String name) {
        try {
            log.info("Start rotation of deployment {} in namespace {}", name, namespace);
            client.apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withName(name)
                    .edit(d -> new DeploymentBuilder(d)
                            .editSpec()
                            .editTemplate()
                            .editMetadata()
                            .addToAnnotations("certificate-change-on", "vault-crd_" + System.currentTimeMillis())
                            .endMetadata()
                            .endTemplate()
                            .endSpec()
                            .build());
        } catch (Exception ex) {
            log.error("Failed to rotate deployment {} in namespace {} with exception:", name, namespace, ex);
        }
    }

    private void rotateStatefulSet(String namespace, String name) {
        try {
            log.info("Start rotation of statefulSet {} in namespace {}", name, namespace);
            client.apps()
                    .statefulSets()
                    .inNamespace(namespace)
                    .withName(name)
                    .edit(statefulSet -> new StatefulSetBuilder(statefulSet)
                            .editSpec()
                            .editTemplate()
                            .editMetadata()
                            .addToAnnotations("certificate-change-on", "vault-crd_" + System.currentTimeMillis())
                            .endMetadata()
                            .endTemplate()
                            .endSpec()
                            .build());
        } catch (Exception ex) {
            log.error("Failed to rotate statefulSet {} in namespace {} with exception:", name, namespace, ex);
        }
    }
}
