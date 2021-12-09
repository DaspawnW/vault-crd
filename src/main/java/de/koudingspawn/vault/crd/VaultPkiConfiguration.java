package de.koudingspawn.vault.crd;

import java.util.Objects;

public class VaultPkiConfiguration {

    private String commonName;
    private String altNames;
    private String ipSans;
    private String ttl;

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getAltNames() {
        return altNames;
    }

    public void setAltNames(String altNames) {
        this.altNames = altNames;
    }

    public String getIpSans() {
        return ipSans;
    }

    public void setIpSans(String ipSans) {
        this.ipSans = ipSans;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultPkiConfiguration that = (VaultPkiConfiguration) o;
        return Objects.equals(commonName, that.commonName) && Objects.equals(altNames, that.altNames) && Objects.equals(ipSans, that.ipSans) && Objects.equals(ttl, that.ttl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonName, altNames, ipSans, ttl);
    }
}
