package de.koudingspawn.vault.crd;

import java.util.HashMap;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultPropertiesConfiguration that = (VaultPropertiesConfiguration) o;
        return Objects.equals(files, that.files) && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, context);
    }
}
