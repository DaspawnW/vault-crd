package de.koudingspawn.vault.kubernetes;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultSecret;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.UUID;

import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;
import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
public class KubernetesServiceTest {

    private static String COMPARE = "COMPARE";
    private static String CRDNAME = "CRDNAME";
    private static String CRDGROUP = "CRDGROUP";

    private static String NAMESPACE = "test";
    private static String SECRETNAME = "testsecret";

    @Autowired
    public KubernetesClient client;

    private KubernetesService kubernetesService;

    @org.springframework.boot.test.context.TestConfiguration
    static class KindConfig {

        @Bean
        @Primary
        public KubernetesClient client() {
            return new DefaultKubernetesClient();
        }

    }

    @Before
    public void setUp() {
        kubernetesService = new KubernetesService(client, CRDNAME, CRDGROUP);

        Namespace ns = new NamespaceBuilder().withMetadata(new ObjectMetaBuilder().withName(NAMESPACE).build()).build();
        client.namespaces().createOrReplace(ns);
    }

    @Test
    public void shouldCheckIfResourceExists() {
        Vault vault = generateVault();

        Secret testsecret = generateSecret();
        client.secrets().inNamespace(NAMESPACE).create(testsecret);

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

        Secret secret = client.secrets().inNamespace(NAMESPACE).withName(SECRETNAME).get();
        assertEquals("dmFsdWU=", secret.getData().get("key")); // value
        assertEquals("Opaque", secret.getType());
        assertEquals(COMPARE, secret.getMetadata().getAnnotations().get(CRDNAME + COMPARE_ANNOTATION));
        assertNotNull(secret.getMetadata().getAnnotations().get(CRDNAME + COMPARE_ANNOTATION));
    }

    @Test
    public void shouldDeleteSecret() {
        Secret secret = generateSecret();

        client.secrets().inNamespace(NAMESPACE).create(secret);

        assertNotNull(client.secrets().inNamespace(NAMESPACE).withName(SECRETNAME).get());

        kubernetesService.deleteSecret(generateVault().getMetadata());

        assertNull(client.secrets().inNamespace(NAMESPACE).withName(SECRETNAME).get());
    }

    @Test
    public void shouldModifySecret() {
        Secret secret = generateSecret();
        client.secrets().inNamespace(NAMESPACE).create(secret);

        Vault vault = generateVault();
        HashMap<String, String> data = new HashMap<>();
        data.put("key1", "dmFsdWUx"); // value1
        VaultSecret modifiedVaultSecret = new VaultSecret(data, COMPARE + "NEW");

        kubernetesService.modifySecret(vault, modifiedVaultSecret);

        Secret foundSecret = client.secrets().inNamespace(NAMESPACE).withName(SECRETNAME).get();

        assertEquals(COMPARE + "NEW", foundSecret.getMetadata().getAnnotations().get(CRDNAME + COMPARE_ANNOTATION));
        assertEquals("Opaque", foundSecret.getType());
        assertEquals("dmFsdWUx", foundSecret.getData().get("key1"));
        assertNull(foundSecret.getData().get("key"));
    }

    @After
    @Before
    public void cleanup() {
        Secret secret = client.secrets().inNamespace(NAMESPACE).withName(SECRETNAME).get();
        if (secret != null) {
            client.secrets().inNamespace(NAMESPACE).withName(SECRETNAME).cascading(true).delete();
        }
    }

    private Secret generateSecret() {
        HashMap<String, String> data = new HashMap<>();
        data.put("key", "dmFsdWU="); // value

        return new SecretBuilder()
                .withNewMetadata()
                    .withName(SECRETNAME)
                .endMetadata()
                .addToData(data)
                .build();
    }

    private VaultSecret generateVaultSecret() {
        HashMap<String, String> data = new HashMap<>();
        data.put("key", "dmFsdWU=");
        return new VaultSecret(data, COMPARE);
    }

    private Vault generateVault() {
        Vault vault = new Vault();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(NAMESPACE);
        meta.setName(SECRETNAME);
        meta.setUid(UUID.randomUUID().toString());
        vault.setMetadata(meta);

        return vault;
    }

}
