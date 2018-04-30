package de.koudingspawn.vault.vault.impl.pki;

public class PKIRequest {
    private String common_name;
    private String alt_names;
    private String ip_sans;
    private String format = "pem";
    private String ttl;

    public String getCommon_name() {
        return common_name;
    }

    public void setCommon_name(String common_name) {
        this.common_name = common_name;
    }

    public String getAlt_names() {
        return alt_names;
    }

    public void setAlt_names(String alt_names) {
        this.alt_names = alt_names;
    }

    public String getIp_sans() {
        return ip_sans;
    }

    public void setIp_sans(String ip_sans) {
        this.ip_sans = ip_sans;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }
}
