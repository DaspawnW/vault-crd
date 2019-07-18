package de.koudingspawn.vault.crd;

import java.util.HashMap;

public class VaultPropertiesConfiguration {

    private HashMap<String, String> files;
    private HashMap<String, String> context;

    public HashMap<String, String> getFiles() {
        return files;
    }

    public void setFiles(HashMap<String, String> files) {
        this.files = files;
    }

    public HashMap<String, String> getContext() {
        return context;
    }

    public void setContext(HashMap<String, String> context) {
        this.context = context;
    }
}
