package de.koudingspawn.vault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultDockerCfgConfiguration;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.crd.VaultType;
import de.koudingspawn.vault.kubernetes.EventHandler;
import de.koudingspawn.vault.kubernetes.scheduler.impl.DockerCfgRefresh;
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

import java.io.IOException;
import java.util.Base64;
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
                "kubernetes.vault.url=http://localhost:8202/v1/",
                "kubernetes.initial-delay=5000000"
        }

)
public class DockerCfgTest {

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
            new WireMockClassRule(wireMockConfig().port(8202));

    @Rule
    public WireMockClassRule instanceRule = wireMockClassRule;

    @Autowired
    public EventHandler handler;

    @Autowired
    public DockerCfgRefresh dockerCfgRefresh;

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
    public void shouldGenerateDockerCfgFromVaultResource() throws IOException {
        Vault vault = new Vault();
        vault.setMetadata(
            new ObjectMetaBuilder().withName("dockercfg").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.DOCKERCFG);
        spec.setPath("secret/docker");
        vault.setSpec(spec);

        stubFor(get(urlEqualTo("/v1/secret/docker"))
                .inScenario("Simple Vault request")
                .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                        "  \"request_id\": \"6cc090a8-3821-8244-73e4-5ab62b605587\",\n" +
                        "  \"lease_id\": \"\",\n" +
                        "  \"renewable\": false,\n" +
                        "  \"lease_duration\": 2764800,\n" +
                        "  \"data\": {\n" +
                        "    \"username\": \"username\",\n" +
                        "    \"password\": \"password\",\n" +
                        "    \"url\": \"hub.docker.com\",\n" +
                        "    \"email\": \"test-user@test.com\"\n" +
                        "  },\n" +
                        "  \"wrap_info\": null,\n" +
                        "  \"warnings\": null,\n" +
                        "  \"auth\": null\n" +
                        "}")));

        handler.addHandler(vault);

        Secret secret = client.secrets().inNamespace("default").withName("dockercfg").get();

        assertEquals("dockercfg", secret.getMetadata().getName());
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("kubernetes.io/dockercfg", secret.getType());
        assertNotNull(secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + LAST_UPDATE_ANNOTATION));
        assertEquals("+gE+L0DNsGWDlNz5T3jLp1/U08KbD4OF+ez2lXQlTPM=", secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + COMPARE_ANNOTATION));

        String dockerCfgBase64 = secret.getData().get(".dockercfg");
        String dockerCfg = new String(Base64.getDecoder().decode(dockerCfgBase64));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dockerCfgNode = mapper.readTree(dockerCfg);
        assertTrue(dockerCfgNode.has("hub.docker.com"));

        JsonNode credentials = dockerCfgNode.get("hub.docker.com");
        assertEquals("username", credentials.get("username").asText());
        assertEquals("password", credentials.get("password").asText());
        assertEquals("test-user@test.com", credentials.get("email").asText());
        assertEquals("username:password", new String(Base64.getDecoder().decode(credentials.get("auth").asText())));
    }

    @Test
    public void shouldCheckIfDockerCfgHasChangedAndReturnTrue() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("dockercfg").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.DOCKERCFG);
        spec.setPath("secret/docker");
        vault.setSpec(spec);

        stubFor(get(urlEqualTo("/v1/secret/docker"))
                .inScenario("Docker secret change")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("Docker first request done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"username\":\"username\", \"password\": \"password\", \"url\": \"hub.docker.com\", \"email\": \"test-user@test.com\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        stubFor(get(urlEqualTo("/v1/secret/docker"))
                .inScenario("Docker secret change")
                .whenScenarioStateIs("Docker first request done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"username\":\"usernamehaschanged\", \"password\": \"password\", \"url\": \"hub.docker.com\", \"email\": \"test-user@test.com\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        handler.addHandler(vault);
        assertTrue(dockerCfgRefresh.refreshIsNeeded(vault));
    }

    @Test
    public void shouldCheckIfDockerCfgHasChangedAndReturnFalse() throws SecretNotAccessibleException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("dockercfg").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.DOCKERCFG);
        spec.setPath("secret/docker");
        vault.setSpec(spec);

        stubFor(get(urlEqualTo("/v1/secret/docker"))
                .inScenario("Simple Vault request")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":{\"username\":\"username\", \"password\": \"password\", \"url\": \"hub.docker.com\", \"email\": \"test-user@test.com\"},\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

        handler.addHandler(vault);
        assertFalse(dockerCfgRefresh.refreshIsNeeded(vault));
    }

    @Test
    public void shouldGenerateDockerCfgV2() throws JsonProcessingException {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("dockercfg").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );
        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.DOCKERCFG);
        spec.setPath("secret/docker");

        VaultDockerCfgConfiguration dockerConfig = new VaultDockerCfgConfiguration();
        dockerConfig.setType(VaultType.KEYVALUEV2);
        spec.setDockerCfgConfiguration(dockerConfig);
        vault.setSpec(spec);

        stubFor(get(urlPathMatching("/v1/secret/data/docker"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"request_id\": \"1cfee2a6-318a-ea12-f5b5-6fd52d74d2c6\",\n" +
                                "  \"lease_id\": \"\",\n" +
                                "  \"renewable\": false,\n" +
                                "  \"lease_duration\": 0,\n" +
                                "  \"data\": {\n" +
                                "    \"data\": {\n" +
                                "      \"username\": \"username\",\n" +
                                "      \"password\": \"password\",\n" +
                                "      \"url\": \"hub.docker.com\",\n" +
                                "      \"email\": \"test-user@test.com\"\n" +
                                "    },\n" +
                                "    \"metadata\": {\n" +
                                "      \"created_time\": \"2018-12-10T18:59:53.337997525Z\",\n" +
                                "      \"deletion_time\": \"\",\n" +
                                "      \"destroyed\": false,\n" +
                                "      \"version\": 1\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"wrap_info\": null,\n" +
                                "  \"warnings\": null,\n" +
                                "  \"auth\": null\n" +
                                "}")));

        handler.addHandler(vault);

        Secret secret = client.secrets().inNamespace("default").withName("dockercfg").get();
        assertEquals("dockercfg", secret.getMetadata().getName());
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("kubernetes.io/dockercfg", secret.getType());
        assertNotNull(secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + LAST_UPDATE_ANNOTATION));
        assertEquals("+gE+L0DNsGWDlNz5T3jLp1/U08KbD4OF+ez2lXQlTPM=", secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + COMPARE_ANNOTATION));

        String dockerCfgBase64 = secret.getData().get(".dockercfg");
        String dockerCfg = new String(Base64.getDecoder().decode(dockerCfgBase64));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dockerCfgNode = mapper.readTree(dockerCfg);
        assertTrue(dockerCfgNode.has("hub.docker.com"));

        JsonNode credentials = dockerCfgNode.get("hub.docker.com");
        assertEquals("username", credentials.get("username").asText());
        assertEquals("password", credentials.get("password").asText());
        assertEquals("test-user@test.com", credentials.get("email").asText());
        assertEquals("username:password", new String(Base64.getDecoder().decode(credentials.get("auth").asText())));
    }

    @After
    @Before
    public void cleanup() {
        Secret secret = client.secrets().inNamespace("default").withName("dockercfg").get();
        if (secret != null) {
            client.secrets().inNamespace("default").withName("dockercfg").cascading(true).delete();
        }
    }

}
