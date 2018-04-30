package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.cert.CertResponse;
import de.koudingspawn.vault.vault.impl.pki.PKIResponse;
import org.springframework.stereotype.Component;

@Component("CERTGENERATOR")
public class CertGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;
    private final SharedVaultResponseMapper sharedVaultResponseMapper;

    public CertGenerator(VaultCommunication vaultCommunication, SharedVaultResponseMapper sharedVaultResponseMapper) {
        this.vaultCommunication = vaultCommunication;
        this.sharedVaultResponseMapper = sharedVaultResponseMapper;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        PKIResponse pkiResponse = vaultCommunication.getCert(resource.getSpec().getPath()).getData();

        return sharedVaultResponseMapper.mapCert(pkiResponse.getData());
    }

    @Override
    public String getHash(VaultSpec spec) throws SecretNotAccessibleException {
        CertResponse cert = vaultCommunication.getCert(spec.getPath());
        if (cert != null && cert.getData() != null) {
            PKIResponse pkiResponse = cert.getData();
            return sharedVaultResponseMapper.mapCert(pkiResponse.getData()).getCompare();
        }

        throw new SecretNotAccessibleException("Secret has no data field");
    }




}
