package de.koudingspawn.vault.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class VaultSpec implements KubernetesResource {
    private String path;
    private VaultType type;
    private VaultPkiConfiguration pkiConfiguration;
    private VaultJKSConfiguration jksConfiguration;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public VaultType getType() {
        return type;
    }

    public void setType(VaultType type) {
        this.type = type;
    }

    public VaultPkiConfiguration getPkiConfiguration() {
        return pkiConfiguration;
    }

    public void setPkiConfiguration(VaultPkiConfiguration pkiConfiguration) {
        this.pkiConfiguration = pkiConfiguration;
    }

    public VaultJKSConfiguration getJksConfiguration() {
        return jksConfiguration;
    }

    public void setJksConfiguration(VaultJKSConfiguration jksConfiguration) {
        this.jksConfiguration = jksConfiguration;
    }
}
