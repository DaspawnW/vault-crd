package de.koudingspawn.vault.kubernetes.scheduler.impl;

import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.kubernetes.KubernetesService;
import de.koudingspawn.vault.kubernetes.scheduler.RequiresRefresh;
import io.fabric8.kubernetes.api.model.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import static de.koudingspawn.vault.Constants.COMPARE_ANNOTATION;
import static de.koudingspawn.vault.Constants.DATE_FORMAT;

@Component("PKI")
public class PkiRefresh implements RequiresRefresh {

    private static final Logger log = LoggerFactory.getLogger(PkiRefresh.class);

    private final int interval;
    private final KubernetesService kubernetesService;
    private final String crdName;

    public PkiRefresh(@Value("${kubernetes.interval}") int interval, @Value("${kubernetes.crd.name}") String crdName, KubernetesService kubernetesService) {
        this.interval = interval;
        this.kubernetesService = kubernetesService;
        this.crdName = crdName;
    }

    public boolean refreshIsNeeded(Vault resource) {
        Secret secretByVault = kubernetesService.getSecretByVault(resource);
        return secretByVault == null || certificateIsNearExpirationDate(secretByVault);
    }

    private boolean certificateIsNearExpirationDate(Secret secretByVault) {

        if (secretByVault.getMetadata().getAnnotations() != null) {
            String expiration = secretByVault.getMetadata().getAnnotations().get(crdName + COMPARE_ANNOTATION);

            Optional<Date> expirationDate = parseDate(expiration);
            if (expirationDate.isPresent()) {

                Date nextIntervals = new Date();
                nextIntervals.setTime(nextIntervals.getTime() + (interval * 1000 * 5));

                return nextIntervals.after(expirationDate.get());
            } else {
                log.error("Failed to parse date of secret {} in namespace {}", secretByVault.getMetadata().getName(), secretByVault.getMetadata().getNamespace());
            }
        }

        return true;
    }

    private Optional<Date> parseDate(String date) {
        if (date == null) {
            return Optional.empty();
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
            TimeZone tz = TimeZone.getTimeZone("UTC");
            format.setTimeZone(tz);

            return Optional.of(format.parse(date));
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

}
