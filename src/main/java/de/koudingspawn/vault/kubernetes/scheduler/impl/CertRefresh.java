package de.koudingspawn.vault.kubernetes.scheduler.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.KubernetesService;
import de.koudingspawn.vault.kubernetes.scheduler.RequiresRefresh;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.TypedSecretGeneratorFactory;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.api.model.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("CERT")
public class CertRefresh extends CompareHash implements RequiresRefresh {

    private static final Logger log = LoggerFactory.getLogger(CertRefresh.class);

    private final String crdName;
    private final KubernetesService kubernetesService;
    private final TypedSecretGeneratorFactory typedSecretGeneratorFactory;

    public CertRefresh(@Value("${kubernetes.crd.name}") String crdName,
                       KubernetesService kubernetesService,
                       TypedSecretGeneratorFactory typedSecretGeneratorFactory) {
        this.crdName = crdName;
        this.typedSecretGeneratorFactory = typedSecretGeneratorFactory;
        this.kubernetesService = kubernetesService;
    }

    @Override
    public boolean refreshIsNeeded(Vault resource) throws SecretNotAccessibleException {
        return certHashHasChanged(resource);
    }

    private boolean certHashHasChanged(Vault resource) throws SecretNotAccessibleException {
        Secret secretByVault = kubernetesService.getSecretByVault(resource);
        TypedSecretGenerator certGenerator = typedSecretGeneratorFactory.get("CERTGENERATOR");
        String vaultSha256 = certGenerator.getHash(resource.getSpec());

        return super.hashHasChanged(secretByVault, vaultSha256, crdName);
    }


}
