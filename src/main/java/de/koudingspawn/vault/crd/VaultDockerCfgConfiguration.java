package de.koudingspawn.vault.crd;

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
}
