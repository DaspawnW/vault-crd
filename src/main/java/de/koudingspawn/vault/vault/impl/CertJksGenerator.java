package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.pki.PKIResponse;
import org.springframework.stereotype.Component;

@Component("CERTJKSGENERATOR")
public class CertJksGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;
    private final SharedVaultResponseMapper sharedVaultResponseMapper;

    public CertJksGenerator(VaultCommunication vaultCommunication, SharedVaultResponseMapper sharedVaultResponseMapper) {
        this.vaultCommunication = vaultCommunication;
        this.sharedVaultResponseMapper = sharedVaultResponseMapper;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        PKIResponse jksCert = vaultCommunication.getCert(resource.getSpec().getPath()).getData();
        return sharedVaultResponseMapper.mapJks(jksCert.getData(), resource.getSpec().getJksConfiguration(), resource.getSpec().getType());
    }

    @Override
    public String getHash(VaultSpec spec) throws SecretNotAccessibleException {
        return null;
    }
}
