package de.koudingspawn.vault.kubernetes.scheduler.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.scheduler.RequiresRefresh;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.springframework.stereotype.Component;

@Component("CERTJKS")
public class CertJksRefresh implements RequiresRefresh {

    private final CertRefresh certRefresh;

    public CertJksRefresh(CertRefresh certRefresh) {
        this.certRefresh = certRefresh;
    }

    @Override
    public boolean refreshIsNeeded(Vault resource) throws SecretNotAccessibleException {
        return certRefresh.refreshIsNeeded(resource);
    }
}
