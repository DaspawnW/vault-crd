package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component("KEYVALUEV2GENERATOR")
public class KeyValueV2Generator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;

    public KeyValueV2Generator(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        Optional<Integer> version = getVersion(resource.getSpec());

        List<VaultSecret> versionedKVResponse = new ArrayList<>();

        for (String path : resource.getSpec().getPath() ){
            versionedKVResponse.add(
                    mapKeyValueResponse(vaultCommunication.getVersionedSecret(path, version)));
        }

        //HashMap versionedKVResponse =
        //        vaultCommunication.getVersionedSecret(resource.getSpec().getPath(), version);

        //Se devuelve solo el primero para poder compilar, es necesario revisar si este es el flujo correcto.
        return versionedKVResponse.get(0);
    }

    @Override
    public String getHash(VaultSpec resource) throws SecretNotAccessibleException {
        Optional<Integer> version = getVersion(resource);
        //Se usa solo el primero para poder compilar, es necesario revisar si este es el flujo correcto.
        HashMap keyValue = vaultCommunication.getVersionedSecret(resource.getPath().get(0), version);
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

    private Optional<Integer> getVersion(VaultSpec resource) {
        if (resource.getVersionConfiguration() != null ) {
            return Optional.ofNullable(resource.getVersionConfiguration().getVersion());
        }
        return Optional.empty();
    }

}
