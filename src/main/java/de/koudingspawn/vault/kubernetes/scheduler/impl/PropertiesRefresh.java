package de.koudingspawn.vault.kubernetes.scheduler.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.scheduler.RequiresRefresh;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.springframework.stereotype.Component;

@Component("PROPERTIES")
public class PropertiesRefresh implements RequiresRefresh {

    @Override
    public boolean refreshIsNeeded(Vault resource) throws SecretNotAccessibleException {
        //TODO: allow properties refresh
        return false;
    }
}
