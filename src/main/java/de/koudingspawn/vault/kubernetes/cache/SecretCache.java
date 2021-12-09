package de.koudingspawn.vault.kubernetes.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SecretCache {

    private static final Logger log = LoggerFactory.getLogger(SecretCache.class);

    private Cache<String, Secret> secretResourceCache = Caffeine.newBuilder().build();
    private final KubernetesClient client;

    public SecretCache(KubernetesClient client, boolean watch) {
        this.client = client;

        if (watch) {
            this.watcher();
        }
    }

    public void watcher() {
        client.secrets().inAnyNamespace().withLabel("vault.koudingspawn.de=vault").inform(
                new ResourceEventHandler<>() {
                    @Override
                    public void onAdd(Secret obj) {
                        String key = String.format("%s/%s", obj.getMetadata().getNamespace(), obj.getMetadata().getName());
                        log.debug("Received create secret for {}", key);
                        secretResourceCache.put(key, obj);
                    }

                    @Override
                    public void onUpdate(Secret oldObj, Secret newObj) {
                        String key = String.format("%s/%s", newObj.getMetadata().getNamespace(), newObj.getMetadata().getName());
                        log.debug("Received update for secret {}", key);
                        secretResourceCache.put(key, newObj);
                    }

                    @Override
                    public void onDelete(Secret obj, boolean deletedFinalStateUnknown) {
                        String key = String.format("%s/%s", obj.getMetadata().getNamespace(), obj.getMetadata().getName());
                        log.debug("Invalidate secret cache for {} after delete", key);
                        secretResourceCache.invalidate(key);
                    }
                }, TimeUnit.MINUTES.toMillis(60));
    }

    public Secret get(String namespace, String name) {
        String key = String.format("%s/%s", namespace, name);

        Secret cacheSecret = secretResourceCache.getIfPresent(key);
        if (cacheSecret != null) {
            return cacheSecret;
        }

        Secret clusterSecret = client.secrets().inNamespace(namespace).withName(name).get();
        if (clusterSecret != null) {
            secretResourceCache.put(key, clusterSecret);
        }

        return clusterSecret;
    }

    public void invalidate(String namespace, String name) {
        String key = String.format("%s/%s", namespace, name);
        log.debug("Invalidate secret cache for {}", key);
        secretResourceCache.invalidate(key);
    }

}
