package de.koudingspawn.vault;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.crd.VaultType;
import de.koudingspawn.vault.kubernetes.EventHandler;
import de.koudingspawn.vault.kubernetes.scheduler.impl.CertRefresh;
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
                "kubernetes.vault.url=http://localhost:8201/v1/",
                "kubernetes.initial-delay=5000000"
        }

)
public class CertTest {

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
            new WireMockClassRule(wireMockConfig().port(8201));

    @Rule
    public WireMockClassRule instanceRule = wireMockClassRule;

    @Autowired
    public EventHandler handler;

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

    @Autowired
    CertRefresh certRefresh;

    @Before
    public void before() {
        WireMock.resetAllScenarios();
        client.secrets().inAnyNamespace().delete();

        TestHelper.generateLookupSelfStub();
    }

    @Test
    public void shouldGenerateCertFromVaultResource() {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("certificate").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.CERT);
        spec.setPath("secret/certificate");
        vault.setSpec(spec);

        stubFor(get(urlEqualTo("/v1/secret/certificate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"request_id\": \"6cc090a8-3821-8244-73e4-5ab62b605587\",\n" +
                                "  \"lease_id\": \"\",\n" +
                                "  \"renewable\": false,\n" +
                                "  \"lease_duration\": 2764800,\n" +
                                "  \"data\": {\n" +
                                "    \"data\": {\n" +
                                "      \"certificate\": \"CERTIFICATE\",\n" +
                                "      \"issuing_ca\": \"ISSUINGCA\",\n" +
                                "      \"private_key\": \"PRIVATEKEY\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"wrap_info\": null,\n" +
                                "  \"warnings\": null,\n" +
                                "  \"auth\": null\n" +
                                "}")));

        handler.addHandler(vault);

        Secret secret = client.secrets().inNamespace("default").withName("certificate").get();

        assertEquals("certificate", secret.getMetadata().getName());
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("Opaque", secret.getType());
        assertNotNull(secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + LAST_UPDATE_ANNOTATION));
        assertEquals("NNreOhDpdqcmcxEvF/KGNSQBZpAjszzrhjQVT4X8EXE=", secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + COMPARE_ANNOTATION));

        String crtB64 = secret.getData().get("tls.crt");
        String crt = new String(java.util.Base64.getDecoder().decode(crtB64));
        String keyB64 = secret.getData().get("tls.key");
        String key = new String(java.util.Base64.getDecoder().decode(keyB64));

        assertEquals("CERTIFICATE", crt);
        assertEquals("PRIVATEKEY", key);
    }

    @Test
    public void shouldCheckIfCertificateHasChangedAndReturnFalse() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("certificate").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.CERT);
        spec.setPath("secret/certificate");
        vault.setSpec(spec);

        stubFor(get(urlEqualTo("/v1/secret/certificate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"request_id\": \"6cc090a8-3821-8244-73e4-5ab62b605587\",\n" +
                                "  \"lease_id\": \"\",\n" +
                                "  \"renewable\": false,\n" +
                                "  \"lease_duration\": 2764800,\n" +
                                "  \"data\": {\n" +
                                "    \"data\": {\n" +
                                "      \"certificate\": \"CERTIFICATE\",\n" +
                                "      \"issuing_ca\": \"ISSUINGCA\",\n" +
                                "      \"private_key\": \"PRIVATEKEY\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"wrap_info\": null,\n" +
                                "  \"warnings\": null,\n" +
                                "  \"auth\": null\n" +
                                "}")));

        handler.addHandler(vault);

        assertFalse(certRefresh.refreshIsNeeded(vault));
    }

    @Test
    public void shouldCheckIfCertificateHasChangedAndReturnTrue() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("certificate").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.CERT);
        spec.setPath("secret/certificate");
        vault.setSpec(spec);

        stubFor(get(urlEqualTo("/v1/secret/certificate"))
                .inScenario("Cert secret change")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("Cert first request done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"request_id\": \"6cc090a8-3821-8244-73e4-5ab62b605587\",\n" +
                                "  \"lease_id\": \"\",\n" +
                                "  \"renewable\": false,\n" +
                                "  \"lease_duration\": 2764800,\n" +
                                "  \"data\": {\n" +
                                "    \"data\": {\n" +
                                "      \"certificate\": \"CERTIFICATE\",\n" +
                                "      \"issuing_ca\": \"ISSUINGCA\",\n" +
                                "      \"private_key\": \"PRIVATEKEY\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"wrap_info\": null,\n" +
                                "  \"warnings\": null,\n" +
                                "  \"auth\": null\n" +
                                "}")));

        stubFor(get(urlEqualTo("/v1/secret/certificate"))
                .inScenario("Cert secret change")
                .whenScenarioStateIs("Cert first request done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"request_id\": \"6cc090a8-3821-8244-73e4-5ab62b605587\",\n" +
                                "  \"lease_id\": \"\",\n" +
                                "  \"renewable\": false,\n" +
                                "  \"lease_duration\": 2764800,\n" +
                                "  \"data\": {\n" +
                                "    \"data\": {\n" +
                                "      \"certificate\": \"CERTIFICATECHANGE\",\n" +
                                "      \"issuing_ca\": \"ISSUINGCA\",\n" +
                                "      \"private_key\": \"PRIVATEKEY\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"wrap_info\": null,\n" +
                                "  \"warnings\": null,\n" +
                                "  \"auth\": null\n" +
                                "}")));

        handler.addHandler(vault);

        assertTrue(certRefresh.refreshIsNeeded(vault));
    }

    @After
    @Before
    public void cleanup() {
        Secret secret = client.secrets().inNamespace("default").withName("certificate").get();
        if (secret != null) {
            client.secrets().inNamespace("default").withName("certificate").cascading(true).delete();
        }
    }

}
