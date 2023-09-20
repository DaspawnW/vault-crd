package de.koudingspawn.vault.admissionreview;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultList;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.VaultService;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "kubernetes.vault.url=http://localhost:8206/v1/",
                "kubernetes.vault.token=c73ab0cb-41e6-b89c-7af6-96b36f1ac87b"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdmissionReviewTest {

    @MockBean
    VaultService vaultService;

    @MockBean
    public MixedOperation<Vault, VaultList, Resource<Vault>> customResource;

    @Autowired
    private MockMvc mvc;

    @Before
    public void setup() {
        KubernetesDeserializer kubernetesDeserializer = new KubernetesDeserializer();
        String version = StringUtils.substringAfter("vault.koudingspawn.de", ".") + "/v1";
        String kind = "Vault";
        kubernetesDeserializer.registerCustomKind(version, kind, Vault.class);
    }

    @Test
    public void shouldFailWithInvalidRequest() throws Exception {
        SecretNotAccessibleException secretException = new SecretNotAccessibleException("Secret is not accessible");
        Mockito.when(vaultService.generateSecret(any())).thenThrow(secretException);

        mvc.perform(post("/validation/vault-crd").content("""
                        {
                          "apiVersion": "admission.k8s.io/v1",
                          "kind": "AdmissionReview",
                          "request": {
                            "uid": "705ab4f5-6393-11e8-b7cc-42010a800002",
                            "object": {
                              "apiVersion": "koudingspawn.de/v1",
                              "kind": "Vault",
                              "metadata": {
                                "name": "test-vault",
                                "namespace": "default"
                              },
                              "spec": {
                                "type": "KEYVALUE",
                                "path": "secret/qweasd"
                              }
                            }
                          }
                        }
                        """).contentType("application/json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.uid").value("705ab4f5-6393-11e8-b7cc-42010a800002"))
                .andExpect(jsonPath("$.response.allowed").value("false"))
                .andExpect(jsonPath("$.response.status.code").value("400"))
                .andExpect(jsonPath("$.response.status.message").value("Secret is not accessible"));
    }

    @Test
    public void shouldReturnValidValue() throws Exception {
        VaultSecret vaultSecret = new VaultSecret(new HashMap<>(), "qweasd");
        Mockito.when(vaultService.generateSecret(any())).thenReturn(vaultSecret);

        mvc.perform(post("/validation/vault-crd").content("""
                        {
                          "apiVersion": "admission.k8s.io/v1",
                          "kind": "AdmissionReview",
                          "request": {
                            "uid": "705ab4f5-6393-11e8-b7cc-42010a800002",
                            "object": {
                              "apiVersion": "koudingspawn.de/v1",
                              "kind": "Vault",
                              "metadata": {
                                "name": "test-vault",
                                "namespace": "default"
                              },
                              "spec": {
                                "type": "KEYVALUE",
                                "path": "secret/qweasd"
                              }
                            }
                          }
                        }""").contentType("application/json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.uid").value("705ab4f5-6393-11e8-b7cc-42010a800002"))
                .andExpect(jsonPath("$.response.allowed").value("true"));
    }

}
