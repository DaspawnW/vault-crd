package de.koudingspawn.vault.vault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class VaultHealthCheckTest {

    @Mock
    VaultCommunication vaultCommunication;

    @InjectMocks
    VaultHealthCheck vaultHealthCheck;

    @Test
    public void shouldReturnUnhealthyIfVaultCommunicationFails() {
        when(vaultCommunication.isHealthy()).thenReturn(false);

        assertEquals(Status.DOWN, vaultHealthCheck.health().getStatus());
    }

    @Test
    public void shouldReturnHealthyResultIfVaultCommunicationWorks() {
        when(vaultCommunication.isHealthy()).thenReturn(true);

        assertEquals(Status.UP, vaultHealthCheck.health().getStatus());
    }



}