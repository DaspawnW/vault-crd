package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;

import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;
import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
public class KubernetesServiceTest {

    private static String COMPARE = "COMPARE";
    private static String CRDNAME = "CRDNAME";

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private KubernetesService kubernetesService;

    @Before
    public void setUp() {
        kubernetesService = new KubernetesService(server.getClient(), CRDNAME);
    }

    @Test
    public void shouldCheckIfResourceExists() {
        Vault vault = generateVault();

        Secret testsecret = generateSecret();
        server.getClient().secrets().inNamespace("default").create(testsecret);

        boolean exists = kubernetesService.exists(vault);

        assertTrue(exists);
    }

    @Test
    public void shouldFindNoResource() {
        Vault vault = generateVault();

        boolean exists = kubernetesService.exists(vault);

        assertFalse(exists);
    }

    @Test
    public void shouldCreateSecret() {
        Vault vault = generateVault();
        VaultSecret vaultSecret = generateVaultSecret();

        kubernetesService.createSecret(vault, vaultSecret);

        SecretList secrets = server.getClient().secrets().inAnyNamespace().list();
        assertEquals(1, secrets.getItems().size());
        Secret secret = secrets.getItems().get(0);
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("testsecret", secret.getMetadata().getName());
        assertEquals("value", secret.getData().get("key"));
        assertEquals("Opaque", secret.getType());
        assertEquals(COMPARE, secret.getMetadata().getAnnotations().get(CRDNAME + COMPARE_ANNOTATION));
        assertNotNull(secret.getMetadata().getAnnotations().get(CRDNAME + COMPARE_ANNOTATION));
    }

    @Test
    public void shouldDeleteSecret() {
        Secret secret = generateSecret();

        server.getClient().secrets().inNamespace("default").create(secret);
        List<Secret> preList = server.getClient().secrets().inNamespace("default").list().getItems();
        assertEquals(1, preList.size());

        kubernetesService.deleteSecret(generateVault().getMetadata());

        List<Secret> secrets = server.getClient().secrets().inNamespace("default").list().getItems();
        assertEquals(0, secrets.size());
    }

    @Test
    public void shouldModifySecret() {
        Secret secret = generateSecret();
        server.getClient().secrets().inNamespace("default").create(secret);

        Vault vault = generateVault();
        HashMap<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        VaultSecret modifiedVaultSecret = new VaultSecret(data, COMPARE + "NEW", "SPECIALTYPE");

        kubernetesService.modifySecret(vault, modifiedVaultSecret);

        Secret foundSecret = server.getClient().secrets().inNamespace("default").withName("testsecret").get();

        assertEquals(COMPARE + "NEW", foundSecret.getMetadata().getAnnotations().get(CRDNAME + COMPARE_ANNOTATION));
        assertEquals("SPECIALTYPE", foundSecret.getType());
        assertEquals("value1", foundSecret.getData().get("key1"));
        assertNull(foundSecret.getData().get("key"));
    }

    private Secret generateSecret() {
        HashMap<String, String> data = new HashMap<>();
        data.put("key", "value");

        return new SecretBuilder()
                .withNewMetadata()
                    .withName("testsecret")
                .endMetadata()
                .addToData(data)
                .build();
    }

    private VaultSecret generateVaultSecret() {
        HashMap<String, String> data = new HashMap<>();
        data.put("key", "value");
        return new VaultSecret(data, COMPARE);
    }

    private Vault generateVault() {
        Vault vault = new Vault();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace("default");
        meta.setName("testsecret");
        vault.setMetadata(meta);

        return vault;
    }

}