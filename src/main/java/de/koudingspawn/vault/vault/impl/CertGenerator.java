package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.pki.PKIResponse;
import de.koudingspawn.vault.vault.impl.pki.VaultResponseData;
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
        PKIResponse pkiResponse = vaultCommunication.getCert(resource.getSpec().getPath());

        return sharedVaultResponseMapper.mapCert(pkiResponse.getData());
    }

    @Override
    public String getHash(VaultSpec spec) throws SecretNotAccessibleException {
        PKIResponse cert = vaultCommunication.getCert(spec.getPath());
        if (cert != null && cert.getData() != null) {
            VaultResponseData pkiResponse = cert.getData();
            return sharedVaultResponseMapper.mapCert(pkiResponse).getCompare();
        }

        throw new SecretNotAccessibleException("Secret has no data field");
    }




}
