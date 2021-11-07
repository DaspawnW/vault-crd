package de.koudingspawn.vault.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

@Version(Vault.VERSION)
@Group(Vault.GROUP)
@Kind(Vault.KIND)
@Singular(Vault.SINGULAR)
@Plural(Vault.PLURAL)
public class Vault extends CustomResource<VaultSpec, Void> implements Namespaced {

    public static final String GROUP = "koudingspawn.de";
    public static final String VERSION = "v1";
    public static final String KIND = "Vault";
    public static final String SINGULAR = "vault";
    public static final String PLURAL = "vault";

}
