package de.koudingspawn.vault.crd;

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
}
