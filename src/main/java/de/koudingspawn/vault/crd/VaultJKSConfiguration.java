package de.koudingspawn.vault.crd;

public class VaultJKSConfiguration {

    private String password;
    private String alias;
    private String keyName;
    private String caAlias;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getCaAlias() {
        return caAlias;
    }

    public void setCaAlias(String caAlias) {
        this.caAlias = caAlias;
    }
}
