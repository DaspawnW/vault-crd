package de.koudingspawn.vault.admissionreview;

import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validation/vault-crd")
public class AdmissionReviewRestService {

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
