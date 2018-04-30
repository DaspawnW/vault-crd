package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.pki.PKIResponse;
import org.springframework.stereotype.Component;

@Component("PKIGENERATOR")
public class PkiSecretGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;
    private final SharedVaultResponseMapper sharedVaultResponseMapper;

    public PkiSecretGenerator(VaultCommunication vaultCommunication, SharedVaultResponseMapper sharedVaultResponseMapper) {
        this.vaultCommunication = vaultCommunication;
        this.sharedVaultResponseMapper = sharedVaultResponseMapper;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        PKIResponse pki = vaultCommunication.createPki(resource.getSpec().getPath(), resource.getSpec().getPkiConfiguration());

        return sharedVaultResponseMapper.mapPki(pki.getData());
    }

    @Override
    public String getHash(VaultSpec spec) {
        return null;
    }
}
