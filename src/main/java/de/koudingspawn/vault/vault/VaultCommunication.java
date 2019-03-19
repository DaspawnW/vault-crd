package de.koudingspawn.vault.vault;

import de.koudingspawn.vault.crd.VaultPkiConfiguration;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.dockercfg.PullSecret;
import de.koudingspawn.vault.vault.impl.pki.PKIRequest;
import de.koudingspawn.vault.vault.impl.pki.PKIResponse;
import jdk.nashorn.internal.parser.TokenLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.VaultResponseSupport;
import org.springframework.vault.support.Versioned;
import org.springframework.vault.support.Versioned.Version;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class VaultCommunication {

    private static final Logger log = LoggerFactory.getLogger(VaultCommunication.class);
    private static final Pattern keyValuePattern = Pattern.compile("^.*?\\/.*?$");

    private final VaultTemplate vaultTemplate;

    public VaultCommunication(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public PKIResponse createPki(String path, VaultPkiConfiguration configuration) throws SecretNotAccessibleException {
        PKIRequest pkiRequest = generateRequest(configuration);

        HttpEntity<PKIRequest> requestEntity = new HttpEntity<>(pkiRequest);
        try {
            return vaultTemplate.doWithSession(restOperations -> restOperations.postForObject(path, requestEntity, PKIResponse.class));
        } catch (HttpStatusCodeException exception) {
            int statusCode = exception.getStatusCode().value();

            throw new SecretNotAccessibleException(
                    String.format("Couldn't generate pki secret from vault path %s status code %d", path, statusCode));
        } catch (RestClientException ex) {
            throw new SecretNotAccessibleException("Couldn't communicate with vault", ex);
        }
    }

    public PKIResponse getCert(String path) throws SecretNotAccessibleException {
        return getRequest(path, PKIResponse.class);
    }

    public PullSecret getDockerCfg(String path) throws SecretNotAccessibleException {
        return getRequest(path, PullSecret.class);
    }

    public HashMap getKeyValue(String path) throws SecretNotAccessibleException {
        return getRequest(path, HashMap.class);
    }

    private <T> T getRequest(String path, Class<T> clazz) throws SecretNotAccessibleException {
        try {
            VaultResponseSupport<T> response = vaultTemplate.read(path, clazz);
            if (response != null) {
                return response.getData();
            } else {
                throw new SecretNotAccessibleException(String.format("The secret %s is not available or in the wrong format.", path));
            }
        } catch (VaultException exception) {
            throw new SecretNotAccessibleException(
                    String.format("Couldn't load secret from vault path %s", path), exception);
        }
    }

    private PKIRequest generateRequest(VaultPkiConfiguration configuration) {
        PKIRequest pkiRequest = new PKIRequest();

        if (configuration != null) {
            if (!StringUtils.isEmpty(configuration.getCommonName())) {
                pkiRequest.setCommon_name(configuration.getCommonName());
            }
            if (!StringUtils.isEmpty(configuration.getAltNames())) {
                pkiRequest.setAlt_names(configuration.getAltNames());
            }
            if (!StringUtils.isEmpty(configuration.getIpSans())) {
                pkiRequest.setIp_sans(configuration.getIpSans());
            }
            if (!StringUtils.isEmpty(configuration.getTtl())) {
                pkiRequest.setTtl(configuration.getTtl());
            }
        }

        return pkiRequest;
    }

    public HashMap getVersionedSecret(String path, Optional<Integer> version) throws SecretNotAccessibleException {
        String mountPoint = extractMountPoint(path);
        String extractedKey = extractKey(path);

        VaultVersionedKeyValueOperations versionedKV = vaultTemplate.opsForVersionedKeyValue(mountPoint);
        Versioned<HashMap> versionedResponse;

        try {
            if (version.isPresent()) {
                versionedResponse = versionedKV.get(extractedKey, Version.from(version.get()), HashMap.class);
            } else {
                versionedResponse = versionedKV.get(extractedKey, HashMap.class);
            }

            if (versionedResponse != null) {
                return versionedResponse.getData();
            }

            throw new SecretNotAccessibleException(String.format("The secret %s is not available or in the wrong format.", path));

        } catch (VaultException ex) {
            throw new SecretNotAccessibleException(
                    String.format("Couldn't load secret from vault path %s", path), ex);
        }
    }

    public boolean isHealthy() {
        return vaultTemplate.doWithSession(this::doWithRestOperations);
    }

    private boolean doWithRestOperations(RestOperations restOperations) {
        try {
            ResponseEntity<TokenLookup> healthEntity = restOperations.getForEntity("/auth/token/lookup-self", TokenLookup.class);
            return healthEntity.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            log.error("Vault health check failed!", ex);
            return false;
        }
    }

    private String extractMountPoint(String path) throws SecretNotAccessibleException {
        if (keyValuePattern.matcher(path).matches()) {
            return path.split("/")[0];
        }

        throw new SecretNotAccessibleException(String.format("Could not extract mountpoint from path: %s. A valid path looks like 'mountpoint/key'", path));
    }

    private String extractKey(String path) throws SecretNotAccessibleException {
        if (keyValuePattern.matcher(path).matches()) {
            return path.split("/")[1];
        }

        throw new SecretNotAccessibleException(String.format("Could not extract key from path: %s. A valid path looks like 'mountpoint/key'", path));
    }
}
