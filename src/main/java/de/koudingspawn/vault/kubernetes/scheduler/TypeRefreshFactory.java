package de.koudingspawn.vault.kubernetes.scheduler;

public interface TypeRefreshFactory {

    RequiresRefresh get(String name);

}
