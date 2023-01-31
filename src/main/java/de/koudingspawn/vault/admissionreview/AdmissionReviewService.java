package de.koudingspawn.vault.admissionreview;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.vault.VaultService;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdmissionReviewService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionReviewService.class);

    private final VaultService vaultService;

    public AdmissionReviewService(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public AdmissionResponse validate(AdmissionRequest admissionRequest) {
        try {
            String s = Serialization.asYaml(admissionRequest.getObject());
            Vault vault = Serialization.unmarshal(s, Vault.class);
            vaultService.generateSecret(vault);
        } catch (ClassCastException ex) {
            log.error("Received Admission Request of invalid type!");
            return invalidRequest(admissionRequest.getUid(), "Received Admission Request of invalid type!");
        } catch (SecretNotAccessibleException e) {
            log.error("Admission Request failed with Secret not Accessible Exception", e);
            return invalidRequest(admissionRequest.getUid(), e.getMessage());
        }

        return validRequest(admissionRequest.getUid());
    }

    private AdmissionResponse validRequest(String uuid) {
        return new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(uuid)
                .build();
    }

    private AdmissionResponse invalidRequest(String uid, String message) {
        Status status = new StatusBuilder()
                .withCode(400)
                .withMessage(message)
                .build();
        return new AdmissionResponseBuilder()
                .withAllowed(false)
                .withUid(uid)
                .withStatus(status)
                .build();
    }

}
