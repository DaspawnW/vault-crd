package de.koudingspawn.vault.kubernetes.scheduler.impl;

import io.fabric8.kubernetes.api.model.Secret;
import org.springframework.util.StringUtils;

import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;

abstract public class CompareHash {

    boolean hashHasChanged(Secret secretByVault, String vaultSha256, String crdName) {
        if (secretByVault == null) {
            // secret is not available...
            return true;
        }

        if (secretByVault.getMetadata().getAnnotations() != null) {
            String kubernetesSha256 = secretByVault.getMetadata().getAnnotations().get(crdName + COMPARE_ANNOTATION);

            if (StringUtils.isEmpty(kubernetesSha256)) {
                // has no sha256 then calculate it
                return true;
            }

            // check if vault and kubernetes are identical
            return !vaultSha256.equals(kubernetesSha256);
        }

        return true;
    }
}
