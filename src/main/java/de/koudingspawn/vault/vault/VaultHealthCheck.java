package de.koudingspawn.vault.vault;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class VaultHealthCheck implements HealthIndicator {

    private final VaultCommunication vaultCommunication;

    public VaultHealthCheck(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.down();

        if (this.vaultCommunication.isHealthy()) {
            healthBuilder.up();
        }

        return healthBuilder.build();
    }
}
