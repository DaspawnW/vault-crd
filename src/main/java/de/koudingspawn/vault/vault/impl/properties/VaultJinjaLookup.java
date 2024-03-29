package de.koudingspawn.vault.vault.impl.properties;

import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;

import java.util.HashMap;
import java.util.Optional;

public class VaultJinjaLookup {

    private final VaultCommunication vaultCommunication;

    public VaultJinjaLookup(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    public String lookup(String path, String key) throws SecretNotAccessibleException {
        return vaultCommunication.getKeyValue(path).get(key).toString();
    }

    public HashMap lookup(String path) throws SecretNotAccessibleException {
        return vaultCommunication.getKeyValue(path);
    }

    public HashMap lookupV2(String path) throws SecretNotAccessibleException {
        return vaultCommunication.getVersionedSecret(path, Optional.empty());
    }

    public String lookupV2(String path, String key) throws SecretNotAccessibleException {
        HashMap versionedSecret = vaultCommunication.getVersionedSecret(path, Optional.empty());
        if (versionedSecret.containsKey(key)) {
            return versionedSecret.get(key).toString();
        }

        throw new SecretNotAccessibleException(String.format("Secret at path %s with key %s not available", path, key));
    }

    public String lookupV2(String path, int version, String key) throws SecretNotAccessibleException {
        HashMap versionedSecret = vaultCommunication.getVersionedSecret(path, Optional.of(version));
        if (versionedSecret.containsKey(key)) {
            return versionedSecret.get(key).toString();
        }

        throw new SecretNotAccessibleException(String.format("Secret at path %s in version %d with key %s not available", path, version, key));
    }

}
