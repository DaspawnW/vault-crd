package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.dockercfg.DockerCfgResponse;
import de.koudingspawn.vault.vault.impl.dockercfg.PullSecret;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component("DOCKERCFGGENERATOR")
public class DockerCfgGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;

    public DockerCfgGenerator(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        DockerCfgResponse dockerCfg = vaultCommunication.getDockerCfg(resource.getSpec().getPath());
        return mapDockerCfg(dockerCfg.getData());
    }

    @Override
    public String getHash(VaultSpec spec) throws SecretNotAccessibleException {
        DockerCfgResponse dockerCfg = vaultCommunication.getDockerCfg(spec.getPath());
        if (dockerCfg != null && dockerCfg.getData() != null) {
            return mapDockerCfg(dockerCfg.getData()).getCompare();
        }

        throw new SecretNotAccessibleException("Secret has no data field");
    }

    private VaultSecret mapDockerCfg(PullSecret pullSecret) {
        String dockerCfg = String.format("{\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"email\": \"%s\"}}",
                pullSecret.getUrl(), pullSecret.getUsername(), pullSecret.getPassword(), pullSecret.getEmail());

        Map<String, String> data = new HashMap<>();
        data.put(".dockercfg", Base64.getEncoder().encodeToString(dockerCfg.getBytes()));

        return new VaultSecret(data, Sha256.generateSha256(dockerCfg), "kubernetes.io/dockercfg");
    }
}
