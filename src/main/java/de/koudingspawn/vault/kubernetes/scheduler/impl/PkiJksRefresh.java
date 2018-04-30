package de.koudingspawn.vault.kubernetes.scheduler.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.scheduler.RequiresRefresh;
import org.springframework.stereotype.Component;

@Component("PKIJKS")
public class PkiJksRefresh implements RequiresRefresh {

    private final PkiRefresh pkiRefresh;

    public PkiJksRefresh(PkiRefresh pkiRefresh) {
        this.pkiRefresh = pkiRefresh;
    }

    @Override
    public boolean refreshIsNeeded(Vault resource) {
        return pkiRefresh.refreshIsNeeded(resource);
    }
}
