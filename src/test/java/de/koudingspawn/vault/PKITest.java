package de.koudingspawn.vault;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultPkiConfiguration;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.crd.VaultType;
import de.koudingspawn.vault.kubernetes.EventHandler;
import de.koudingspawn.vault.kubernetes.scheduler.impl.CertRefresh;
import de.koudingspawn.vault.vault.impl.pki.VaultResponseData;
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
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.koudingspawn.vault.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "kubernetes.vault.url=http://localhost:8204/v1/",
                "kubernetes.initial-delay=5000000"
        }
)
public class PKITest {

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
            new WireMockClassRule(wireMockConfig().port(8204));

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
    public void shouldGeneratePkiFromVaultResource() throws Exception {
        Date startDate = new Date();
        VaultResponseData keyPair = generateKeyPair(startDate, 60L);
        Vault vaultResource = generateVaultResource();


        stubFor(post(urlEqualTo("/v1/testpki/issue/testrole"))
                .withRequestBody(matchingJsonPath("$.common_name", containing("test.url.de")))
                .withRequestBody(matchingJsonPath("$.ttl", containing("10m")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                String.format("{\n" +
                                        "  \"request_id\": \"6cc090a8-3821-8244-73e4-5ab62b605587\",\n" +
                                        "  \"lease_id\": \"\",\n" +
                                        "  \"renewable\": false,\n" +
                                        "  \"lease_duration\": 2764800,\n" +
                                        "  \"data\": {\n" +
                                        "      \"certificate\": \"%s\",\n" +
                                        "      \"private_key\": \"%s\"\n" +
                                        "  },\n" +
                                        "  \"wrap_info\": null,\n" +
                                        "  \"warnings\": null,\n" +
                                        "  \"auth\": null\n" +
                                        "}", keyPair.getCertificate(), keyPair.getPrivate_key())
                        )));

        handler.addHandler(vaultResource);

        Secret secret = client.secrets().inNamespace("default").withName("pki").get();

        // metadata
        assertEquals("pki", secret.getMetadata().getName());
        assertEquals("default", secret.getMetadata().getNamespace());
        assertEquals("Opaque", secret.getType());
        assertNotNull(secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + LAST_UPDATE_ANNOTATION));

        // compare date
        String formatedExpirationDate = secret.getMetadata().getAnnotations().get("vault.koudingspawn.de" + COMPARE_ANNOTATION);
        LocalDateTime parsedCompareDate = parseDate(formatedExpirationDate);
        LocalDateTime expirationDate = convertDate(startDate).plusMinutes(1);
        assertEquals(expirationDate, parsedCompareDate);

        // body
        String crtB64 = secret.getData().get("tls.crt");
        String crt = new String(java.util.Base64.getDecoder().decode(crtB64));
        String keyB64 = secret.getData().get("tls.key");
        String key = new String(java.util.Base64.getDecoder().decode(keyB64));

        // not so nice, but wiremock expects double escaping
        assertEquals(keyPair.getCertificate().replaceAll("\\\\n", ""), crt.replaceAll("\\n", ""));
        assertEquals(keyPair.getPrivate_key().replaceAll("\\\\n", ""), key.replaceAll("\\n", ""));
    }

    @After
    @Before
    public void cleanup() {
        Secret secret = client.secrets().inNamespace("default").withName("pki").get();
        if (secret != null) {
            client.secrets().inNamespace("default").withName("pki").cascading(true).delete();
        }
    }

    private VaultResponseData generateKeyPair(Date startDate, long valid) throws Exception {
        CertAndKeyGen certGen = new CertAndKeyGen("RSA", "SHA256WithRSA");
        certGen.generate(2048);

        X500Name x500Name = new X500Name("CN=Test");
        X509Certificate cert = certGen.getSelfCertificate(x500Name, startDate, valid);


        byte[] encodedPrivateKey = certGen.getPrivateKey().getEncoded();
        byte[] encodedPublicKey = cert.getEncoded();

        String privateKeySb = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder().encodeToString(encodedPrivateKey) +
                "\n-----END PRIVATE KEY-----";
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getMimeEncoder().encodeToString(encodedPublicKey) +
                "\n-----END PUBLIC KEY-----";

        privateKeySb = privateKeySb.replaceAll("\\n", "\\\\n");
        privateKeySb = privateKeySb.replaceAll("\\r", "");

        publicKey = publicKey.replaceAll("\\n", "\\\\n");
        publicKey = publicKey.replaceAll("\\r", "");

        VaultResponseData vaultResponseData = new VaultResponseData();
        vaultResponseData.setPrivate_key(privateKeySb);
        vaultResponseData.setCertificate(publicKey);
        return vaultResponseData;
    }

    private Vault generateVaultResource() {
        Vault vault = new Vault();
        vault.setMetadata(
                new ObjectMetaBuilder().withName("pki").withNamespace("default").withUid(UUID.randomUUID().toString()).build()
        );

        VaultSpec spec = new VaultSpec();
        spec.setType(VaultType.PKI);
        spec.setPath("testpki/issue/testrole");

        VaultPkiConfiguration vaultPkiConfiguration = new VaultPkiConfiguration();
        vaultPkiConfiguration.setCommonName("test.url.de");
        vaultPkiConfiguration.setTtl("10m");
        spec.setPkiConfiguration(vaultPkiConfiguration);

        vault.setSpec(spec);

        return vault;
    }

    private LocalDateTime convertDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private LocalDateTime parseDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        format.setTimeZone(tz);
        return convertDate(format.parse(date));
    }

}
