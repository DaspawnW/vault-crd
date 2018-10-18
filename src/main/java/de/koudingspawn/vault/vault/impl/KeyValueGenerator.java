package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component("KEYVALUEGENERATOR")
public class KeyValueGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;

    public KeyValueGenerator(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        HashMap keyValueResponse = vaultCommunication.getKeyValue(resource.getSpec().getPath());
        return mapKeyValueResponse(keyValueResponse);
    }

    @Override
    public String getHash(VaultSpec resource) throws SecretNotAccessibleException {
        HashMap keyValue = vaultCommunication.getKeyValue(resource.getPath());
        if (keyValue != null) {
            return mapKeyValueResponse(keyValue).getCompare();
        }

        throw new SecretNotAccessibleException("Secret has no data field");
    }

    private VaultSecret mapKeyValueResponse(HashMap<String, String> keyValue) {
        TreeMap<String, String> sortedMap = new TreeMap<>(keyValue);

        Map<String, String> base64Encoded = sortedMap.entrySet()
                .stream()
                .collect(Collectors
                        .toMap(Map.Entry::getKey,
                                e -> Base64.getEncoder().encodeToString(e.getValue().getBytes())));

        String sha256 = Sha256.generateSha256(base64Encoded.values().toArray(new String[0]));

        return new VaultSecret(base64Encoded, sha256);
    }

}
