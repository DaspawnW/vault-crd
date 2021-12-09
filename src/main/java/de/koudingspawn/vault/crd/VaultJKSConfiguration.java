package de.koudingspawn.vault.crd;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultJKSConfiguration that = (VaultJKSConfiguration) o;
        return Objects.equals(password, that.password) && Objects.equals(alias, that.alias) && Objects.equals(keyName, that.keyName) && Objects.equals(caAlias, that.caAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(password, alias, keyName, caAlias);
    }
}
