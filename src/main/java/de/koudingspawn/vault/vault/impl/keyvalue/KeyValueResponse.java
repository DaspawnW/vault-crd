package de.koudingspawn.vault.vault.impl.keyvalue;

import java.util.HashMap;

public class KeyValueResponse {

    private HashMap<String, String> data;

    public HashMap<String, String> getData() {
        return data;
    }

    public void setData(HashMap<String, String> data) {
        this.data = data;
    }
}
