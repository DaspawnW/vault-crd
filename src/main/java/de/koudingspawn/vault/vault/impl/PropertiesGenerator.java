package de.koudingspawn.vault.vault.impl;

import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import de.koudingspawn.vault.crd.Vault;
import de.koudingspawn.vault.crd.VaultPropertiesConfiguration;
import de.koudingspawn.vault.crd.VaultSpec;
import de.koudingspawn.vault.vault.TypedSecretGenerator;
import de.koudingspawn.vault.vault.VaultCommunication;
import de.koudingspawn.vault.vault.VaultSecret;
import de.koudingspawn.vault.vault.communication.SecretNotAccessibleException;
import de.koudingspawn.vault.vault.impl.properties.VaultJinjaLookup;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component("PROPERTIESGENERATOR")
public class PropertiesGenerator implements TypedSecretGenerator {

    private final VaultCommunication vaultCommunication;

    public PropertiesGenerator(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    @Override
    public VaultSecret generateSecret(Vault resource) throws SecretNotAccessibleException {
        VaultPropertiesConfiguration propertiesConfiguration = resource.getSpec().getPropertiesConfiguration();

        if (propertiesConfiguration != null && propertiesConfiguration.getFiles() != null) {
            Map<String, Object> context = Maps.newHashMap();
            context.put("vault", new VaultJinjaLookup(vaultCommunication));
            if (propertiesConfiguration.getContext() != null) {
                context.putAll(propertiesConfiguration.getContext());
            }

            try {
                Map<String, String> renderedFiles = renderFiles(context, propertiesConfiguration.getFiles());
                // TODO: support change in properties
                return new VaultSecret(renderedFiles, "COMPARE");
            } catch (FatalTemplateErrorsException ex) {
                throw new SecretNotAccessibleException(ex.getMessage(), ex);
            }
        }

        throw new SecretNotAccessibleException("Does not contain the required Files to render");
    }

    @Override
    public String getHash(VaultSpec spec) throws SecretNotAccessibleException {
        return "COMPARE";
    }

    private Map<String, String> renderFiles(Map<String, Object> context, Map<String, String> files) throws FatalTemplateErrorsException {
        Jinjava jinjava = new Jinjava();
        Map<String, String> targetFiles = new HashMap<>();

        files.forEach((key, value) -> {
            String renderedContent = jinjava.render(value, context);
            targetFiles.put(key, Base64.getEncoder().encodeToString(renderedContent.getBytes()));
        });

        return targetFiles;
    }

}
