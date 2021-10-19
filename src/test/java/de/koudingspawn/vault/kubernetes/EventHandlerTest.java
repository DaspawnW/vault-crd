package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.kubernetes.event.EventNotification;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.VaultService;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class EventHandlerTest {

    @Mock
    private VaultService vaultService;

    @Mock
    private KubernetesService kubernetesService;

    @Mock
    private ChangeAdjustmentService changeAdjustmentService;

    @Mock
    private EventNotification eventNotification;

    private EventHandler eventHandler;

    @Before
    public void setup() {
        eventHandler = new EventHandler(vaultService, kubernetesService, changeAdjustmentService, eventNotification, true);
    }


    @Test
    public void shouldGenerateKubernetesSecret() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        VaultSecret vaultSecret = new VaultSecret(new HashMap<>(), "COMPARE");

        when(vaultService.generateSecret(vault)).thenReturn(vaultSecret);
        eventHandler.addHandler(vault);

        verify(kubernetesService, times(1)).createSecret(vault, vaultSecret);
    }

    @Test
    public void shouldDoNotingIfSecretForVaultAlreadyExists() {
        Vault vault = new Vault();

        when(kubernetesService.exists(vault)).thenReturn(true);
        eventHandler.addHandler(vault);

        verify(kubernetesService, never()).createSecret(any(), any());
    }

    @Test
    public void shouldDoNothingIfGenerateSecretFails() throws SecretNotAccessibleException {
        Vault vault = new Vault();

        when(vaultService.generateSecret(vault)).thenThrow(SecretNotAccessibleException.class);
        eventHandler.addHandler(vault);

        verify(kubernetesService, never()).createSecret(any(), any());
    }

    @Test
    public void shouldModifySecret() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setSpec(new VaultSpec());
        VaultSecret vaultSecret = new VaultSecret(new HashMap<>(), "COMPARE");

        when(vaultService.generateSecret(vault)).thenReturn(vaultSecret);
        eventHandler.modifyHandler(vault);

        verify(kubernetesService, times(1)).modifySecret(vault, vaultSecret);
    }

    @Test
    public void shouldDoNothingIfCreateSecretForModificationFails() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setSpec(new VaultSpec());

        when(vaultService.generateSecret(vault)).thenThrow(SecretNotAccessibleException.class);
        eventHandler.modifyHandler(vault);

        verify(kubernetesService, never()).modifySecret(any(), any());
    }

}
