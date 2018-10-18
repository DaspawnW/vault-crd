package de.koudingspawn.vault.vault;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.springframework.stereotype.Component;

@Component
public class VaultService {

    private final TypedSecretGeneratorFactory typedSecretGeneratorFactory;

    public VaultService(TypedSecretGeneratorFactory typedSecretGeneratorFactory) {
        this.typedSecretGeneratorFactory = typedSecretGeneratorFactory;
    }

    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        TypedSecretGenerator typedSecretGenerator = typedSecretGeneratorFactory.get(resource.getSpec().getType().toString() + "GENERATOR");
        return typedSecretGenerator.generateSecret(resource);
    }

}
