package de.koudingspawn.vault.crd;

import io.fabric8.kubernetes.client.CustomResource;

public class Vault extends CustomResource {

    private VaultSpec spec;

    public VaultSpec getSpec() {
        return spec;
    }

    public void setSpec(VaultSpec spec) {
        this.spec = spec;
    }
}
