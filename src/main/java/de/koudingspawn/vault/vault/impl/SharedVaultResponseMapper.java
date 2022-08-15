package de.koudingspawn.vault.vault.impl;

import de.koudingspawn.vault.Constants;
import de.koudingspawn.vault.crd.VaultJKSConfiguration;
import de.koudingspawn.vault.crd.VaultType;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.pki.VaultResponseData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Component
public class SharedVaultResponseMapper {

    @Value("${kubernetes.jks.default-alias}")
    private String defaultAlias;
    @Value("${kubernetes.jks.default-password}")
    private String defaultPassword;
    @Value("${kubernetes.jks.default-secret-key-name}")
    private String defaultKeyName;

    VaultSecret mapPki(VaultResponseData responseData) throws SecretNotAccessibleException {
        try {
            Certificate[] publicKeyList = getPublicKey(responseData.getCertificate());
            X509Certificate compareCert = getCertificateWithShortestLivetime(publicKeyList);
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
            TimeZone tz = TimeZone.getTimeZone("UTC");
            dateFormat.setTimeZone(tz);
            String compare = dateFormat.format(compareCert.getNotAfter());
            Map<String, String> mappedPki = mappedCertValues(responseData);

            return new VaultSecret(mappedPki, compare);
        } catch (CertificateException e) {
            throw new SecretNotAccessibleException("Couldn't get Expiration date of pki", e);
        }
    }

    VaultSecret mapCert(VaultResponseData vaultResponseData) {
        Map<String, String> mappedPki = mappedCertValues(vaultResponseData);
        String compareSha = Sha256.generateSha256(
                mappedPki.get("tls.crt"),
                mappedPki.get("tls.key")
        );

        return new VaultSecret(mappedPki, compareSha);
    }

    private Map<String, String> mappedCertValues(VaultResponseData vaultResponseData) {
        String crt = getCrt(vaultResponseData);
        String key = getKey(vaultResponseData);

        Map<String, String> mappedPki = new HashMap<>();
        mappedPki.put("tls.crt", crt);
        mappedPki.put("tls.key", key);

        return mappedPki;
    }

    private String getCrt(VaultResponseData responseData) {
        return Base64.getEncoder().encodeToString(responseData.getChainedCertificate().getBytes());
    }

    private String getKey(VaultResponseData responseData) {
        return Base64.getEncoder().encodeToString(responseData.getPrivate_key().getBytes());
    }

    VaultSecret mapJks(VaultResponseData data, VaultJKSConfiguration jksConfiguration, VaultType type) throws SecretNotAccessibleException {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            Certificate[] publicKeyList = getPublicKey(data.getCertificate());

            keyStore.setKeyEntry(
                    getAlias(jksConfiguration),
                    EncryptionUtils.loadPrivateKey(data.getPrivate_key()),
                    getPassword(jksConfiguration).toCharArray(),
                    publicKeyList);

            if (jksConfiguration != null && StringUtils.hasText(jksConfiguration.getCaAlias())) {
                keyStore.setCertificateEntry(
                        jksConfiguration.getCaAlias(),
                        getPublicKey(data.getIssuing_ca())[0]
                );
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keyStore.store(outputStream, getPassword(jksConfiguration).toCharArray());
            String b64KeyStore = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            HashMap<String, String> secretData = new HashMap<>() {{
                put(getKey(jksConfiguration), b64KeyStore);
            }};

            String compare;
            if (type.equals(VaultType.CERTJKS)) {
                String base64Cert = Base64.getEncoder().encodeToString(data.getCertificate().getBytes());
                String base64Key = Base64.getEncoder().encodeToString(data.getPrivate_key().getBytes());
                compare = Sha256.generateSha256(base64Cert, base64Key);
            } else {
                // VaultType.PKIJKS
                X509Certificate compareCert = getCertificateWithShortestLivetime(publicKeyList);
                SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
                TimeZone tz = TimeZone.getTimeZone("UTC");
                dateFormat.setTimeZone(tz);
                compare = dateFormat.format(compareCert.getNotAfter());
            }
            return new VaultSecret(secretData, compare);


        } catch (IOException | GeneralSecurityException e) {
            throw new SecretNotAccessibleException("Couldn't generate keystore", e);
        }

    }

    private String getAlias(VaultJKSConfiguration jksConfiguration) {
        if (jksConfiguration == null || !StringUtils.hasText(jksConfiguration.getAlias())) {
            return defaultAlias;
        }
        return jksConfiguration.getAlias();
    }

    private String getPassword(VaultJKSConfiguration jksConfiguration) {
        if (jksConfiguration == null || !StringUtils.hasText(jksConfiguration.getPassword())) {
            return defaultPassword;
        }
        return jksConfiguration.getPassword();
    }

    private String getKey(VaultJKSConfiguration jksConfiguration) {
        if (jksConfiguration == null || !StringUtils.hasText(jksConfiguration.getKeyName())) {
            return defaultKeyName;
        }
        return jksConfiguration.getKeyName();
    }

    private Certificate[] getPublicKey(String pem) throws CertificateException {
        return CertificateFactory.getInstance("X509")
                .generateCertificates(new ByteArrayInputStream(pem.getBytes())).toArray(new Certificate[0]);
    }

    private X509Certificate getCertificateWithShortestLivetime(Certificate[] certificates) {
        if (certificates.length == 1) {
            return (X509Certificate) certificates[0];
        } else {
            X509Certificate shortestLiveTime = (X509Certificate) certificates[0];

            for (Certificate certificate : certificates) {
                if (((X509Certificate) certificate).getNotAfter().before(shortestLiveTime.getNotAfter())) {
                    shortestLiveTime = (X509Certificate) certificate;
                }
            }

            return shortestLiveTime;
        }
    }

}
