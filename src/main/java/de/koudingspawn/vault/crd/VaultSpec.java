package de.koudingspawn.vault.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.Objects;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultSpec implements KubernetesResource {
    private String path;
    private VaultType type;
    private VaultPkiConfiguration pkiConfiguration;
    private VaultJKSConfiguration jksConfiguration;
    private VaultVersionedConfiguration versionConfiguration;
    private VaultPropertiesConfiguration propertiesConfiguration;
    private VaultDockerCfgConfiguration dockerCfgConfiguration;
    private VaultChangeAdjustmentCallback changeAdjustmentCallback;

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

    public VaultVersionedConfiguration getVersionConfiguration() {
        return versionConfiguration;
    }

    public void setVersionConfiguration(VaultVersionedConfiguration versionConfiguration) {
        this.versionConfiguration = versionConfiguration;
    }

    public VaultPropertiesConfiguration getPropertiesConfiguration() {
        return propertiesConfiguration;
    }

    public void setPropertiesConfiguration(VaultPropertiesConfiguration propertiesConfiguration) {
        this.propertiesConfiguration = propertiesConfiguration;
    }

    public VaultDockerCfgConfiguration getDockerCfgConfiguration() {
        return dockerCfgConfiguration;
    }

    public void setDockerCfgConfiguration(VaultDockerCfgConfiguration dockerCfgConfiguration) {
        this.dockerCfgConfiguration = dockerCfgConfiguration;
    }

    public VaultChangeAdjustmentCallback getChangeAdjustmentCallback() {
        return changeAdjustmentCallback;
    }

    public void setChangeAdjustmentCallback(VaultChangeAdjustmentCallback changeAdjustmentCallback) {
        this.changeAdjustmentCallback = changeAdjustmentCallback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultSpec vaultSpec = (VaultSpec) o;
        return Objects.equals(path, vaultSpec.path) && type == vaultSpec.type && Objects.equals(pkiConfiguration, vaultSpec.pkiConfiguration) && Objects.equals(jksConfiguration, vaultSpec.jksConfiguration) && Objects.equals(versionConfiguration, vaultSpec.versionConfiguration) && Objects.equals(propertiesConfiguration, vaultSpec.propertiesConfiguration) && Objects.equals(dockerCfgConfiguration, vaultSpec.dockerCfgConfiguration) && Objects.equals(changeAdjustmentCallback, vaultSpec.changeAdjustmentCallback);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, type, pkiConfiguration, jksConfiguration, versionConfiguration, propertiesConfiguration, dockerCfgConfiguration, changeAdjustmentCallback);
    }
}
