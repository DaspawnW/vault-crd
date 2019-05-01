package de.koudingspawn.vault.vault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.*;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.support.VaultToken;

import java.net.URI;

import static org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId.provided;
import static org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId.wrapped;

@Configuration
public class VaultConfiguration {

    @Bean
    public ServiceLocatorFactoryBean slfbForTypeRefresh() {
        ServiceLocatorFactoryBean slfb = new ServiceLocatorFactoryBean();
        slfb.setServiceLocatorInterface(TypedSecretGeneratorFactory.class);
        return slfb;
    }

    @Configuration
    @ConditionalOnProperty(name = "kubernetes.vault.auth", havingValue = "token")
    class VaultTokenConnection extends AbstractVaultConfiguration {

        private final String vaultToken;
        private final String vaultUrl;

        VaultTokenConnection(@Value("${kubernetes.vault.token}") String vaultToken,
                             @Value("${kubernetes.vault.url}") String vaultUrl) {
            this.vaultToken = vaultToken;
            this.vaultUrl = vaultUrl;
        }

        @Override
        public VaultEndpoint vaultEndpoint() {
            return VaultEndpoint.from(getVaultUrlWithoutPath(vaultUrl));
        }

        @Override
        public ClientAuthentication clientAuthentication() {
            return new TokenAuthentication(vaultToken);
        }

    }

    @Configuration
    @ConditionalOnProperty(name = "kubernetes.vault.auth", havingValue = "serviceAccount")
    class VaultServiceAccountConnection extends AbstractVaultConfiguration {

        private final String vaultUrl;
        private final String role;

        VaultServiceAccountConnection(@Value("${kubernetes.vault.url}") String vaultUrl,
                                      @Value("${kubernetes.vault.role}") String role) {
            this.vaultUrl = vaultUrl;
            this.role = role;
        }

        @Override
        public VaultEndpoint vaultEndpoint() {
            return VaultEndpoint.from(getVaultUrlWithoutPath(vaultUrl));
        }

        @Override
        public ClientAuthentication clientAuthentication() {
            KubernetesAuthenticationOptions options =
                    KubernetesAuthenticationOptions.builder().role(role).build();

            return new KubernetesAuthentication(options, restOperations());
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "kubernetes.vault.auth", havingValue = "appRole")
    class VaultAppRoleAuthentication extends AbstractVaultConfiguration {
        private final String vaultUrl;
        private final String roleId;
        private final String vaultToken;

        VaultAppRoleAuthentication(@Value("${kubernetes.vault.url}") String vaultUrl,
                                   @Value("${kubernetes.vault.token}") String vaultToken,
                                   @Value("${kubernetes.vault.roleId}") String roleId) {
            this.vaultUrl = vaultUrl;
            this.roleId = roleId;
            this.vaultToken = vaultToken;
        }

        @Override
        public VaultEndpoint vaultEndpoint() {
            return VaultEndpoint.from(getVaultUrlWithoutPath(vaultUrl));
        }

        @Override
        public ClientAuthentication clientAuthentication() {
            AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
                    .roleId(provided(roleId))
                    .secretId(wrapped(VaultToken.of(vaultToken)))
                    .build();
            return new AppRoleAuthentication(options, restOperations());
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "kubernetes.vault.auth", havingValue = "appRolePull")
    class VaultAppRolePullAuthentication extends AbstractVaultConfiguration {

        private final String vaultUrl;
        private final String vaultToken;
        private final String role;

        VaultAppRolePullAuthentication(@Value("${kubernetes.vault.url}") String vaultUrl,
                                       @Value("${kubernetes.vault.token}") String vaultToken,
                                       @Value("${kubernetes.vault.role}") String role) {
            this.vaultUrl = vaultUrl;
            this.vaultToken = vaultToken;
            this.role = role;
        }

        @Override
        public VaultEndpoint vaultEndpoint() {
            return VaultEndpoint.from(getVaultUrlWithoutPath(vaultUrl));
        }

        @Override
        public ClientAuthentication clientAuthentication() {
            VaultToken initialToken = VaultToken.of(vaultToken);

            AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
                    .appRole(role)
                    .roleId(AppRoleAuthenticationOptions.RoleId.pull(initialToken))
                    .secretId(AppRoleAuthenticationOptions.SecretId.pull(initialToken))
                    .build();

            return new AppRoleAuthentication(options, restOperations());
        }
    }

    private URI getVaultUrlWithoutPath(String vaultUrl) {
        return URI.create(vaultUrl.replace("/v1/", ""));
    }

}
