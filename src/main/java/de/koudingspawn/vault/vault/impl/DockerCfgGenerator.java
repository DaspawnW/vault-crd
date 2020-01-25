package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultDockerCfgConfiguration;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.dockercfg.PullSecret;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component("DOCKERCFGGENERATOR")
public class DockerCfgGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;

    public DockerCfgGenerator(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        PullSecret dockerCfg = vaultCommunication.getDockerCfg(resource.getSpec().getPath(), getConfiguration(resource.getSpec()));
        return mapDockerCfg(dockerCfg);
    }

    @Override
    public String getHash(VaultSpec spec) throws SecretNotAccessibleException {
        PullSecret dockerCfg = vaultCommunication.getDockerCfg(spec.getPath(), getConfiguration(spec));
        if (dockerCfg != null) {
            return mapDockerCfg(dockerCfg).getCompare();
        }

        throw new SecretNotAccessibleException("Secret has no data field");
    }

    private VaultDockerCfgConfiguration getConfiguration(VaultSpec spec) {
        return Optional.ofNullable(spec.getDockerCfgConfiguration()).orElse(new VaultDockerCfgConfiguration());
    }

    private VaultSecret mapDockerCfg(PullSecret pullSecret) {
        String dockerCfg = String.format("{\"%s\": {\"username\": \"%s\", \"password\": \"%s\", \"email\": \"%s\", \"auth\": \"%s\"}}",
                pullSecret.getUrl(), pullSecret.getUsername(), pullSecret.getPassword(), pullSecret.getEmail(), pullSecret.getAuth());

        Map<String, String> data = new HashMap<>();
        data.put(".dockercfg", Base64.getEncoder().encodeToString(dockerCfg.getBytes()));

        return new VaultSecret(data, Sha256.generateSha256(dockerCfg), "kubernetes.io/dockercfg");
    }
}
