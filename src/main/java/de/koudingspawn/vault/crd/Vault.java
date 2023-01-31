package de.koudingspawn.vault.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

import java.util.HashMap;
import java.util.Objects;

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

    public boolean modifyHandlerEquals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Vault vault = (Vault) o;

        // spec equals
        if (vault.getSpec() == null && spec != null) return false;
        if (vault.getSpec() != null && spec == null) return false;
        if (vault.getSpec() != null && spec != null) {
            if (!vault.getSpec().equals(spec)) return false;
        } // null && null => true for spec

        // metadata equals
        if (vault.getMetadata() == null && getMetadata() == null) return true;
        if (vault.getMetadata() == null) return false;
        if (getMetadata() == null) return false;

        // metadata.name, metadata.namespace, metadata.uid equals
        if (!vault.getMetadata().getName().equals(getMetadata().getName())) return false;
        if (!vault.getMetadata().getNamespace().equals(getMetadata().getNamespace())) return false;
        if (!vault.getMetadata().getUid().equals(getMetadata().getUid())) return false;

        // metadata.labels equals
        if (vault.getMetadata().getLabels() == null && getMetadata().getLabels() != null) return false;
        if (vault.getMetadata().getLabels() != null && getMetadata().getLabels() == null) return false;
        if (!Objects.equals(vault.getMetadata().getLabels(), getMetadata().getLabels())) return false;

        // metadata.annotations equals
        if (vault.getMetadata().getAnnotations() == null && getMetadata().getAnnotations() != null) return false;
        if (vault.getMetadata().getAnnotations() != null && getMetadata().getAnnotations() == null) return false;
        if (vault.getMetadata().getAnnotations() != null && getMetadata().getAnnotations() != null) {
            HashMap<String, String> vaultAnnotations = new HashMap<>(vault.getMetadata().getAnnotations());
            vaultAnnotations.remove("kubectl.kubernetes.io/last-applied-configuration");

            HashMap<String, String> annotations = new HashMap<>(getMetadata().getAnnotations());
            annotations.remove("kubectl.kubernetes.io/last-applied-configuration");
            return Objects.equals(vaultAnnotations, annotations);
        }


        return true;
    }

}
