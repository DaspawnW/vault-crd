package de.koudingspawn.vault;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.event.EventNotification;
import de.koudingspawn.vault.kubernetes.event.EventType;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "kubernetes.vault.url=http://localhost:8202/v1/",
        "kubernetes.initial-delay=5000000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventNotificationTest {

    @Autowired
    public KubernetesClient client;

    @Autowired
    public EventNotification evtNotification;

    @org.springframework.boot.test.context.TestConfiguration
    static class KindConfig {

        @Bean
        @Primary
        public KubernetesClient client() {
            return new DefaultKubernetesClient();
        }
    }

    @Test
    public void shouldBeAbleToCreateEvent() {
        String uuid = UUID.randomUUID().toString();

        Vault vault = new Vault();
        vault.setMetadata(new ObjectMetaBuilder()
                .withName("test")
                .withNamespace("default")
                .withUid(uuid)
                .build());

        evtNotification.storeNewEvent(EventType.CREATION_SUCCESSFUL, "Successfully created secret", vault);

        assertEquals(1, client.events().v1().events().inNamespace("default").list().getItems()
                .stream().filter(event -> event.getRegarding().getName().equals("test") && event.getRegarding().getUid().equals(uuid)).count());
    }

}
