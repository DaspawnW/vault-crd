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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.koudingspawn.vault.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "kubernetes.vault.url=http://localhost:8205/v1/",
                "kubernetes.initial-delay=5000000"
        }
)
public class PKIChainTest {

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
            new WireMockClassRule(wireMockConfig().port(8205));

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
    public void shouldGeneratePkiFromVaultChainResource() throws Exception {
        VaultResponseData keyPair = generateKeyPair();
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
                                        "      \"ca_chain\": [\"%s\"],\n" +
                                        "      \"issuing_ca\": \"%s\",\n" +
                                        "      \"private_key\": \"%s\"\n" +
                                        "  },\n" +
                                        "  \"wrap_info\": null,\n" +
                                        "  \"warnings\": null,\n" +
                                        "  \"auth\": null\n" +
                                        "}", keyPair.getCertificate(), keyPair.getCa_chain().get(0), keyPair.getIssuing_ca(), keyPair.getPrivate_key())
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
        LocalDateTime expirationDate = parseDate("2018-06-25T16:02Z");
        assertEquals(expirationDate, parsedCompareDate);

        // body
        String crtB64 = secret.getData().get("tls.crt");
        String crt = new String(Base64.getDecoder().decode(crtB64));
        String keyB64 = secret.getData().get("tls.key");
        String key = new String(Base64.getDecoder().decode(keyB64));

        // not so nice, but wiremock expects double escaping
        assertEquals(keyPair.getChainedCertificate().replaceAll("\\\\n", "").replaceAll("\\n", ""), crt.replaceAll("\\n", ""));
        assertEquals(keyPair.getPrivate_key().replaceAll("\\\\n", "").replaceAll("\\n", ""), key.replaceAll("\\n", ""));
    }

    @After
    @Before
    public void cleanup() {
        Secret secret = client.secrets().inNamespace("default").withName("pki").get();
        if (secret != null) {
            client.secrets().inNamespace("default").withName("pki").cascading(true).delete();
        }
    }

    private VaultResponseData generateKeyPair() {
        String certificate = "-----BEGIN CERTIFICATE-----\\n" +
                "MIIDZjCCAk6gAwIBAgIUc8PIl50sEQM28x6CV7iK6fae4t4wDQYJKoZIhvcNAQEL\\n" +
                "BQAwLTErMCkGA1UEAxMibXl2YXVsdC5jb20gSW50ZXJtZWRpYXRlIEF1dGhvcml0\\n" +
                "eTAeFw0xODA2MjIxNjAyMDVaFw0xODA2MjUxNjAyMzRaMBsxGTAXBgNVBAMTEGJs\\n" +
                "YWguZXhhbXBsZS5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCu\\n" +
                "lPq1WzzgImZFQ3NNlu9i7Xi6/U1a40csFz9Gvho3lIMgY2IgtxwaZTTO5vplKr+s\\n" +
                "VfM2f6Enxv89i5If6J1gE1R/X728XYqNeXAP/5jgRwaq9S7Eg1len5OgXdkjO3RV\\n" +
                "WkY8zMmG8N6e0viNgs9cYm9bJV9u9bKDeXYRaDeiSVIh77dL6Vaws06ViJeDzQxp\\n" +
                "kDiaeSY9jyjhwBor+nqw7Vrvqc8LjaKy5JzD9rUPcv7O0hy3HF0/D3s2ailNdLar\\n" +
                "4U9qEViI/5BzsykcJnvaLFW3RqZ1DmlUUoompOMURFMwbrEI3Gu4rKXmlu5zc9dx\\n" +
                "UiDjnTup7e0hK4mNhqZ/AgMBAAGjgY8wgYwwDgYDVR0PAQH/BAQDAgOoMB0GA1Ud\\n" +
                "JQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAdBgNVHQ4EFgQUEsdbwRqYE9nRbcJE\\n" +
                "XnAZHjxnnxEwHwYDVR0jBBgwFoAUTu0jLnLe15sBQhrYSEbXAILoLkMwGwYDVR0R\\n" +
                "BBQwEoIQYmxhaC5leGFtcGxlLmNvbTANBgkqhkiG9w0BAQsFAAOCAQEAjyqdfK81\\n" +
                "NjkwS4heUg3DQtSILurLyzt+x+yafPnJTByoX2UE+xAUBeKwyxUmcRO6/IEUdZmM\\n" +
                "qmGE44x2gyP3+YfjBSkfjKRQz8IslZWW4DPn11O+icpQieNAhFt6goD8rReNeH+i\\n" +
                "OWVr+vBwi71C7uR9W4NkBtdCqXBfXrSkwtb9aIFZxr+bfYTIFCfsFnv8OAYCbzhk\\n" +
                "6QtrWjQKduyuxisuvVAztJhk0JMg09xYgTsCJ8oQBNAwYR5UOl55TADgj19R1Xpq\\n" +
                "8qT7r56++C5I0BMCMk63Q1ofgeYyTGJYsxjjNa+rLLYlK9ysOofrrLYdyp03xniK\\n" +
                "IX4NZ1EHqWONxQ==\\n" +
                "-----END CERTIFICATE-----";
        String caChain = "-----BEGIN CERTIFICATE-----\\n" +
                "MIIDNDCCAhygAwIBAgIUOM/FWyCOxZuYgAanfIk+11NQHLswDQYJKoZIhvcNAQEL\\n" +
                "BQAwFjEUMBIGA1UEAxMLbXl2YXVsdC5jb20wHhcNMTgwNjIyMTU1OTI3WhcNMTgw\\n" +
                "NzI0MTU1OTU3WjAtMSswKQYDVQQDEyJteXZhdWx0LmNvbSBJbnRlcm1lZGlhdGUg\\n" +
                "QXV0aG9yaXR5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt6EKCNbb\\n" +
                "+02nY/jM8EIwJ8moLUo6hCqOsEb4jFWYLjbInKICqdO36KsUEQL9W9Kq6LGqdZV4\\n" +
                "cOYbpWYXr20Ni3p7hSgIttX+LJVPnm9g4Yc/71Wtzv9YsFXsudwQXE+iG+eBH2V2\\n" +
                "kHbqANh/8ZXDzhZUlNecgR44YOOmS8z0nh3fOYwBu4eTazBvRk9PaUqS6VPtgqNF\\n" +
                "sUAa7rszmOTRxVVsAN+O/HS08/+vwkIgvgTV849Pvb6diBlBWBc1LOOVuV+UWEsl\\n" +
                "jfmhCM/nHqtGxg/cnPEV35WjAZH+ND+nkC2wRKaxfxf3B03MUm6WwIrRZrhMsBt2\\n" +
                "OeEabv+NxKydDwIDAQABo2MwYTAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/BAUw\\n" +
                "AwEB/zAdBgNVHQ4EFgQUTu0jLnLe15sBQhrYSEbXAILoLkMwHwYDVR0jBBgwFoAU\\n" +
                "LzwRuDNnbxWfzW/iiM+Ek963I28wDQYJKoZIhvcNAQELBQADggEBADgwe8v8YPkJ\\n" +
                "8Rsx9VkACu6IZ8hDkhDEe82wtU9BdzyIahgPgSwbjoLSIxX9nN3b8ifX1ZNgeio7\\n" +
                "hkCQ8q0s3Eor479IqXv2i7yBMDcQ7o5DSh/g21/1IQ7cJ5rJVnpCpw6pb5Td2ww9\\n" +
                "6L90xrHSX13n90xIctglEKiMvAoB0UBQRlFG2qL1IgmhpVYBuiqLPIsaRbj2Bthd\\n" +
                "nmsvDBflruBcjuimmRyozOVT1Cgw+xxw7nMMYDDs9iDqSgYnuLJZRYiHDVTna/Vx\\n" +
                "UB6pS3TuoOKzJKuYL3lu2Yvjp0wTOXmaEg9wW9BqpIxu3U0Hd2ScEEOGQ+b3VEyp\\n" +
                "IwwSk9KcPFs=\\n" +
                "-----END CERTIFICATE-----";
        String issuingCa = "-----BEGIN CERTIFICATE-----\\n" +
                "MIIDNDCCAhygAwIBAgIUOM/FWyCOxZuYgAanfIk+11NQHLswDQYJKoZIhvcNAQEL\\n" +
                "BQAwFjEUMBIGA1UEAxMLbXl2YXVsdC5jb20wHhcNMTgwNjIyMTU1OTI3WhcNMTgw\\n" +
                "NzI0MTU1OTU3WjAtMSswKQYDVQQDEyJteXZhdWx0LmNvbSBJbnRlcm1lZGlhdGUg\\n" +
                "QXV0aG9yaXR5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt6EKCNbb\\n" +
                "+02nY/jM8EIwJ8moLUo6hCqOsEb4jFWYLjbInKICqdO36KsUEQL9W9Kq6LGqdZV4\\n" +
                "cOYbpWYXr20Ni3p7hSgIttX+LJVPnm9g4Yc/71Wtzv9YsFXsudwQXE+iG+eBH2V2\\n" +
                "kHbqANh/8ZXDzhZUlNecgR44YOOmS8z0nh3fOYwBu4eTazBvRk9PaUqS6VPtgqNF\\n" +
                "sUAa7rszmOTRxVVsAN+O/HS08/+vwkIgvgTV849Pvb6diBlBWBc1LOOVuV+UWEsl\\n" +
                "jfmhCM/nHqtGxg/cnPEV35WjAZH+ND+nkC2wRKaxfxf3B03MUm6WwIrRZrhMsBt2\\n" +
                "OeEabv+NxKydDwIDAQABo2MwYTAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/BAUw\\n" +
                "AwEB/zAdBgNVHQ4EFgQUTu0jLnLe15sBQhrYSEbXAILoLkMwHwYDVR0jBBgwFoAU\\n" +
                "LzwRuDNnbxWfzW/iiM+Ek963I28wDQYJKoZIhvcNAQELBQADggEBADgwe8v8YPkJ\\n" +
                "8Rsx9VkACu6IZ8hDkhDEe82wtU9BdzyIahgPgSwbjoLSIxX9nN3b8ifX1ZNgeio7\\n" +
                "hkCQ8q0s3Eor479IqXv2i7yBMDcQ7o5DSh/g21/1IQ7cJ5rJVnpCpw6pb5Td2ww9\\n" +
                "6L90xrHSX13n90xIctglEKiMvAoB0UBQRlFG2qL1IgmhpVYBuiqLPIsaRbj2Bthd\\n" +
                "nmsvDBflruBcjuimmRyozOVT1Cgw+xxw7nMMYDDs9iDqSgYnuLJZRYiHDVTna/Vx\\n" +
                "UB6pS3TuoOKzJKuYL3lu2Yvjp0wTOXmaEg9wW9BqpIxu3U0Hd2ScEEOGQ+b3VEyp\\n" +
                "IwwSk9KcPFs=\\n" +
                "-----END CERTIFICATE-----";

        String privateKey = "-----BEGIN RSA PRIVATE KEY-----\\n" +
                "MIIEowIBAAKCAQEArpT6tVs84CJmRUNzTZbvYu14uv1NWuNHLBc/Rr4aN5SDIGNi\\n" +
                "ILccGmU0zub6ZSq/rFXzNn+hJ8b/PYuSH+idYBNUf1+9vF2KjXlwD/+Y4EcGqvUu\\n" +
                "xINZXp+ToF3ZIzt0VVpGPMzJhvDentL4jYLPXGJvWyVfbvWyg3l2EWg3oklSIe+3\\n" +
                "S+lWsLNOlYiXg80MaZA4mnkmPY8o4cAaK/p6sO1a76nPC42isuScw/a1D3L+ztIc\\n" +
                "txxdPw97NmopTXS2q+FPahFYiP+Qc7MpHCZ72ixVt0amdQ5pVFKKJqTjFERTMG6x\\n" +
                "CNxruKyl5pbuc3PXcVIg4507qe3tISuJjYamfwIDAQABAoIBABTuFXSCoLS6SwqI\\n" +
                "wJ0PuFli4POCBLEdyF2X1+UyS1BYhLPwVkZXzY24jnEzrddNHbeaglMJUBfFurn1\\n" +
                "LqqWp69qAdpXbxbTHBZD9dRlLz3MJhd+14GFwcQfW4KBXdPkf9jvvrXxU0PTQs1F\\n" +
                "u7izcwq/XlxOCbfyytkKScZieTECaGmy7l6kJphaFP7m8eQ6vwI9LZXeFvA4URLJ\\n" +
                "IzxM36Y/DkY+ME5AWxc9L+bYZjGj4QjRtfe27Dpy6FyrZg99pFfhoU/mWop3dh9z\\n" +
                "rHBIvBppYUlp9BBBnOBxDTcmTh+dYGvApIud2gkpy3Om1BxCPXf7/TQgz3GQEnJW\\n" +
                "JVaE94ECgYEAwSl2a7NPOqP4pYeKjRcdgwaI+lmIRjT6oNgHH4VM1aGolb8vYvdP\\n" +
                "1VMwmMsDpxygX2p7gp9tbt2ZIcvwKBrIw6QTtS6kqxSqOSMlleeaHdd/WHYUmV9+\\n" +
                "xiU5uHEWwjYqYivVXo1br06eXTE7zD2bDg9hZHDgCRRGXc6IvniQpr8CgYEA52As\\n" +
                "I047YoJUX3OWE7Rxp6gIIO2St+HEKEZiV38m+7mWJOghUvGVssy/WYAPEvcGBAQp\\n" +
                "zty2daaZBevFeW499N7haAFfoVbxpvtCR9fxheL1EkbsFMRPpCjgRGzhI7Rx2QAi\\n" +
                "D5URL2WppkVDa+BW0wUIczL5jd5L57z3MT0XsEECgYEAviHO89JLEYCnVmAlbB2t\\n" +
                "qfQ7zplkfx7U+I/L6yXt7Ha0l7nZrgObrHK3ah6jGNIftev9aST+tdsgSVkRqpg6\\n" +
                "uACAeZ5Q7iloKNfEvlp7pBYjvnJ0ckfCZM3tk/SVH1Prwjg9TVW9QsETNs4oezDE\\n" +
                "uEFBb3l/vNAdN2b9yOaqE8cCgYAMZ/G16unwPEC95Xq0j8ZQUQgui86EIYzdA/kd\\n" +
                "6+lxMeBFFlVDF0UJk0TnTaCBSdF+waJkPx1hbY9i6+NowWp9CL5ZT0mLYxgN9gb1\\n" +
                "xzRiE2tEkZzy+Bu1F6P+xz/DJFe+ZO1unHWRbwgLrEcTL7I4Glr7ok4TN0omoNE4\\n" +
                "SKhOgQKBgDOZMbY2zzwozBQG5vxdxndIGmG874CRJ5CiS/hfTE0Gxdt54B9VadNU\\n" +
                "ouZSwidB4YQH2aYHH1aUGhPemExztAcDJz2UholDUoj3v+ft6jCuMjb1loofqJeW\\n" +
                "+hgtD7tH5sihc8tKYHXg6IfrZLdmUbWWj0qK6ow0hzSFNuJgZB+5\\n" +
                "-----END RSA PRIVATE KEY-----";

        VaultResponseData vaultResponseData = new VaultResponseData();
        vaultResponseData.setPrivate_key(privateKey);
        vaultResponseData.setCertificate(certificate);
        vaultResponseData.setCa_chain(Collections.singletonList(caChain));
        vaultResponseData.setIssuing_ca(issuingCa);
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
