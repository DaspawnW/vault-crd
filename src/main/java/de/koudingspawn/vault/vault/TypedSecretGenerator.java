package de.koudingspawn.vault.vault;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;

public interface TypedSecretGenerator {

    VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException;

    String getHash(VaultSpec spec) throws SecretNotAccessibleException;

}
