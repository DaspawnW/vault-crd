package de.koudingspawn.vault;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import de.koudingspawn.vault.crd.DoneableVault;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import de.koudingspawn.vault.kubernetes.EventHandler;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.koudingspawn.vault.PropertiesTest.generatePropertiesManifest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "kubernetes.vault.url=http://localhost:8210/v1/",
                "kubernetes.initial-delay=5000000",
                "kubernetes.vault.token=c73ab0cb-41e6-b89c-7af6-96b36f1ac87b"
        }

)
public class OwnerReferenceBugfix {

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
            new WireMockClassRule(wireMockConfig().port(8210));

    @Rule
    public WireMockClassRule instanceRule = wireMockClassRule;

    @Autowired
    public EventHandler handler;

    @Autowired
    public KubernetesClient client;

    @Autowired
    public MixedOperation<Vault, VaultList, DoneableVault, Resource<Vault, DoneableVault>> customResource;

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
    public void hasCorrectOwnerReference() throws IOException {
        TestHelper.generateKVStup("kv/key", ImmutableMap.of("value", "kv1content"));
        TestHelper.generateKV2Stup("kv2/key", ImmutableMap.of("value", "kv2content", "value2", "kv3content"));

        Vault vault = generatePropertiesManifest("properties-correct-owner-1");
        handler.addHandler(vault);

        Secret secret = client.secrets().inNamespace("default").withName("properties-correct-owner-1").get();
        assertEquals(1, secret.getMetadata().getOwnerReferences().size());
        assertEquals("something-not-garbage-collected.de/v1", secret.getMetadata().getOwnerReferences().get(0).getApiVersion());
    }

    @Test
    public void fixOwnerReference() throws IOException {
        TestHelper.generateKVStup("kv/key", ImmutableMap.of("value", "kv1content"));
        TestHelper.generateKV2Stup("kv2/key", ImmutableMap.of("value", "kv2content", "value2", "kv3content"));

        Vault vault = generatePropertiesManifest("properties-correct-owner-2");
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder().withName("properties-correct-owner-2").withNamespace("default")
                                .addToOwnerReferences(
                                        new OwnerReference(
                                                "vault.koudingspawn.de/v1",
                                                false,
                                                true,
                                                "Vault",
                                                "properties-correct-owner-2",
                                                vault.getMetadata().getUid()
                                        )
                                ).build()
                )
                .withData(Collections.singletonMap("key", "dmFsdWU="))
                .build();
        client.secrets().inNamespace("default").withName("properties-correct-owner-2").create(secret);

        handler.addHandler(vault);

        Secret foundSecret = client.secrets().inNamespace("default").withName("properties-correct-owner-2").get();
        assertNotNull(foundSecret);
        assertEquals(1, foundSecret.getMetadata().getOwnerReferences().size());
        assertEquals("something-not-garbage-collected.de/v1", foundSecret.getMetadata().getOwnerReferences().get(0).getApiVersion());
    }

}
