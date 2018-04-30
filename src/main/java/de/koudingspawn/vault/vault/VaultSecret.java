package de.koudingspawn.vault.vault;

import java.util.Map;

public class VaultSecret {

    private Map<String, String> data;

    private String compare;

    private String type;

    public VaultSecret(Map<String, String> data, String compare) {
        this.data = data;
        this.compare = compare;
        this.type = "Opaque";
    }

    public VaultSecret(Map<String, String> data, String compare, String type) {
        this.data = data;
        this.compare = compare;
        this.type = type;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public String getCompare() {
        return compare;
    }

    public void setCompare(String compare) {
        this.compare = compare;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
