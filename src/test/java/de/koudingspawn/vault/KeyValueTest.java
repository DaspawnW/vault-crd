package de.koudingspawn.vault;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.crd.VaultType;
import de.koudingspawn.vault.kubernetes.EventHandler;
import de.koudingspawn.vault.kubernetes.scheduler.impl.KeyValueRefresh;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;
import static de.koudingspawn.vault.Constants.LAST_UPDATE_ANNOTATION;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "kubernetes.vault.url=http://localhost:8209/v1/"
        }
)
public class KeyValueTest {

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
            new WireMockClassRule(wireMockConfig().port(8209));

    @Rule
    public WireMockClassRule instanceRule = wireMockClassRule;

    @Autowired
    public EventHandler handler;

    @Autowired
    public KeyValueRefresh keyValueRefresh;

    @Autowired
    public KubernetesClient client;

    @org.springframework.boot.test.context.TestConfiguration
    static class KindConfig {

        @Bean
        @Primary
        public KubernetesClient client() {
            return new DefaultKubernetesClient();
        }

    }

    @Before
    public void before() {
        WireMock.resetAllScenarios();
        client.secrets().inAnyNamespace().delete();

        TestHelper.generateLookupSelfStub();
    }

    @Test
    public void shouldGenerateSimpleSecretFromVaultCustomResource() {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("simple").withNamespace("default").withUid(UUID.randomUUID().toString()).build());
        VaultSpec vaultSpec = new VaultSpec();
        vaultSpec.setType(VaultType.KEYVALUE);
        vaultSpec.setPath("secret/simple");
        vault.setSpec(vaultSpec);

        stubFor(get(urlPathMatching("/v1/secret/simple"))
            .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        handler.addHandler(vault);

        Secret secret = client.secrets().inNamespace("default").withName("simple").get();
        assertEquals("simple", secret.getMetadata().getName());
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("Opaque", secret.getType());
        assertEquals("dmFsdWU=", secret.getData().get("key"));
        assertNotNull(secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + LAST_UPDATE_ANNOTATION));
        assertEquals("dYxf3NXqZ1l2d1YL1htbVBs6EUot33VjoBUUrBJg1eY=", secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + COMPARE_ANNOTATION));
    }

    @Test
    public void shouldCheckIfSimpleSecretHasChangedAndReturnTrue() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("simple").withNamespace("default").withUid(UUID.randomUUID().toString()).build());
        VaultSpec vaultSpec = new VaultSpec();
        vaultSpec.setType(VaultType.KEYVALUE);
        vaultSpec.setPath("secret/simple");
        vault.setSpec(vaultSpec);

        stubFor(get(urlPathMatching("/v1/secret/simple"))
                .inScenario("Vault secret change")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("First request done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        stubFor(get(urlPathMatching("/v1/secret/simple"))
                .inScenario("Vault secret change")
                .whenScenarioStateIs("First request done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"key\":\"value1\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        handler.addHandler(vault);

        assertTrue(keyValueRefresh.refreshIsNeeded(vault));
    }

    @Test
    public void shouldCheckIfSimpleSecretHasChangedAndReturnFalse() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("simple").withNamespace("default").withUid(UUID.randomUUID().toString()).build());
        VaultSpec vaultSpec = new VaultSpec();
        vaultSpec.setType(VaultType.KEYVALUE);
        vaultSpec.setPath("secret/simple");
        vault.setSpec(vaultSpec);

        stubFor(get(urlPathMatching("/v1/secret/simple"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        handler.addHandler(vault);

        assertFalse(keyValueRefresh.refreshIsNeeded(vault));
    }

    @Test
    public void preventNullPointerExceptionWhenSecretDoesNotExist() {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("simple").withNamespace("default").withUid(UUID.randomUUID().toString()).build());
        VaultSpec vaultSpec = new VaultSpec();
        vaultSpec.setType(VaultType.KEYVALUE);
        vaultSpec.setPath("secret/simple");
        vault.setSpec(vaultSpec);

        stubFor(get(urlPathMatching("/v1/secret/simple"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"key\":\"value\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        handler.modifyHandler(vault);

        Secret secret = client.secrets().inNamespace("default").withName("simple").get();
        assertEquals("simple", secret.getMetadata().getName());
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("Opaque", secret.getType());
        assertEquals("dmFsdWU=", secret.getData().get("key"));
        assertNotNull(secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + LAST_UPDATE_ANNOTATION));
        assertEquals("dYxf3NXqZ1l2d1YL1htbVBs6EUot33VjoBUUrBJg1eY=", secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + COMPARE_ANNOTATION));
    }

    @After
    @Before
    public void cleanup() {
        Secret secret = client.secrets().inNamespace("default").withName("simple").get();
        if (secret != null) {
            client.secrets().inNamespace("default").withName("simple").cascading(true).delete();
        }
    }

}
