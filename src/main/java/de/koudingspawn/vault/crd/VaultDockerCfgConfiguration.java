package de.koudingspawn.vault.crd;

import java.util.Objects;

public class VaultDockerCfgConfiguration {

    private VaultType type;
    private Integer version;

    public VaultDockerCfgConfiguration() {
        this.type = VaultType.KEYVALUE;
    }


    public VaultType getType() {
        return type;
    }

    public void setType(VaultType version) {
        this.type = version;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultDockerCfgConfiguration that = (VaultDockerCfgConfiguration) o;
        return type == that.type && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version);
    }
}
