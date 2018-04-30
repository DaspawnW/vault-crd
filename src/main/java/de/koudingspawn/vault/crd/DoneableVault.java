package de.koudingspawn.vault.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableVault extends CustomResourceDoneable<Vault> {

    public DoneableVault(Vault resource, Function<Vault, Vault> function) {
        super(resource, function);
    }
}
