package de.koudingspawn.vault.admissionreview;

import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validation/vault-crd")
public class AdmissionReviewRestService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionReviewRestService.class);

    private final AdmissionReviewService admissionReviewService;

    public AdmissionReviewRestService(AdmissionReviewService admissionReviewService) {
        this.admissionReviewService = admissionReviewService;
    }

    @PostMapping
    public AdmissionReview validate(@RequestBody AdmissionReview admissionRequest) {
        AdmissionResponse admissionResponse = admissionReviewService.validate(admissionRequest.getRequest());
        return new AdmissionReviewBuilder()
                .withResponse(admissionResponse)
                .build();
    }

}
