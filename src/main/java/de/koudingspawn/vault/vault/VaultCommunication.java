package de.koudingspawn.vault.vault;

import de.koudingspawn.vault.crd.VaultPkiConfiguration;
import de.koudingspawn.vault.vault.impl.cert.CertResponse;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.dockercfg.DockerCfgResponse;
import de.koudingspawn.vault.vault.impl.keyvalue.KeyValueResponse;
import de.koudingspawn.vault.vault.impl.pki.PKIRequest;
import de.koudingspawn.vault.vault.impl.pki.PKIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class VaultCommunication {

    private static final String VAULT_HEADER_NAME = "X-Vault-Token";

    private final RestTemplate restTemplate;
    private final String vaultToken;
    private final String vaultUrl;

    public VaultCommunication(RestTemplate restTemplate, @Value("${kubernetes.vault.token}") String vaultToken, @Value("${kubernetes.vault.url}") String vaultUrl) {
        this.restTemplate = restTemplate;
        this.vaultToken = vaultToken;
        this.vaultUrl = vaultUrl;
    }

    public PKIResponse createPki(String path, VaultPkiConfiguration configuration) throws SecretNotAccessibleException {
        PKIRequest pkiRequest = generateRequest(configuration);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(VAULT_HEADER_NAME, vaultToken);
        HttpEntity<PKIRequest> requestEntity = new HttpEntity<>(pkiRequest, httpHeaders);
        try {
            return restTemplate.postForObject(vaultUrl + path, requestEntity, PKIResponse.class);
        } catch (HttpStatusCodeException exception) {
            int statusCode = exception.getStatusCode().value();

            throw new SecretNotAccessibleException(
                    String.format("Couldn't generate pki secret from vault path %s status code %d", path, statusCode));
        }
    }

    public CertResponse getCert(String path) throws SecretNotAccessibleException {
        return getRequest(path, CertResponse.class);
    }

    public DockerCfgResponse getDockerCfg(String path) throws SecretNotAccessibleException {
        return getRequest(path, DockerCfgResponse.class);
    }

    public KeyValueResponse getKeyValue(String path) throws SecretNotAccessibleException {
        return getRequest(path, KeyValueResponse.class);
    }

    private <T> T getRequest(String path, Class<T> clazz) throws SecretNotAccessibleException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(VAULT_HEADER_NAME, vaultToken);
        HttpEntity entity = new HttpEntity(httpHeaders);

        try {
            return restTemplate.exchange(vaultUrl + path, HttpMethod.GET, entity, clazz).getBody();
        } catch (HttpStatusCodeException exception) {
            int statusCode = exception.getStatusCode().value();

            throw new SecretNotAccessibleException(
                    String.format("Couldn't load secret from vault path %s status code %d", path, statusCode));
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
}
