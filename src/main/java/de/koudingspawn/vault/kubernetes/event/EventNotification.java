package de.koudingspawn.vault.kubernetes.event;

import de.koudingspawn.vault.crd.Vault;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Service
public class EventNotification {

    private static final Logger log = LoggerFactory.getLogger(EventNotification.class);

    private final String crdGroup;
    private final KubernetesClient client;

    public EventNotification(@Value("${kubernetes.crd.group}") String crdGroup, KubernetesClient client) {
        this.crdGroup = crdGroup;
        this.client = client;
    }

    public void storeNewEvent(EventType type, String message, Vault resource) {
        ObjectReference ref = new ObjectReferenceBuilder()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withApiVersion(crdGroup + "/v1")
                .withKind("Vault")
                .withUid(resource.getMetadata().getUid())
                .build();

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        Event evt = new EventBuilder()
                .withNewMetadata()
                .withGenerateName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .endMetadata()
                .withInvolvedObject(ref)
                .withLastTimestamp(nowAsISO)
                .withFirstTimestamp(nowAsISO)
                .withReportingComponent("vault-crd")
                .withType(type.getEventType())
                .withReason(type.getReason())
                .withMessage(message)
                .build();

        try {
            client.events().inNamespace(resource.getMetadata().getNamespace()).create(evt);
        } catch (Exception ex) {
            log.error("Failed to store event for {} in namespace {} next to resource with error",
                    resource.getMetadata().getName(), resource.getMetadata().getNamespace(), ex);
        }
    }

}
