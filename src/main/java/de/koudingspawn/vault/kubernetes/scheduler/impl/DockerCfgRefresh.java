package de.koudingspawn.vault.kubernetes.scheduler.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.KubernetesService;
import de.koudingspawn.vault.kubernetes.scheduler.RequiresRefresh;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.TypedSecretGeneratorFactory;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.api.model.Secret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("DOCKERCFG")
public class DockerCfgRefresh extends CompareHash implements RequiresRefresh {

    private final String crdName;
    private final KubernetesService kubernetesService;
    private final TypedSecretGeneratorFactory typedSecretGeneratorFactory;


    public DockerCfgRefresh(@Value("${kubernetes.crd.name}") String crdName,
                            KubernetesService kubernetesService,
                            TypedSecretGeneratorFactory typedSecretGeneratorFactory) {
        this.crdName = crdName;
        this.kubernetesService = kubernetesService;
        this.typedSecretGeneratorFactory = typedSecretGeneratorFactory;
    }

    public boolean refreshIsNeeded(Vault resource) throws SecretNotAccessibleException {
        return dockerCfgHashHasChanged(resource);
    }

    private boolean dockerCfgHashHasChanged(Vault resource) throws SecretNotAccessibleException {
        Secret secretByVault = kubernetesService.getSecretByVault(resource);
        TypedSecretGenerator dockercfg = typedSecretGeneratorFactory.get("DOCKERCFGGENERATOR");
        String vaultSha256 = dockercfg.getHash(resource.getSpec());

        return super.hashHasChanged(secretByVault, vaultSha256, crdName);
    }

}
