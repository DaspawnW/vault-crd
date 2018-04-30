package de.koudingspawn.vault.kubernetes.scheduler;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;

public interface RequiresRefresh {

    boolean refreshIsNeeded(Vault resource) throws SecretNotAccessibleException;

}
