FROM gcr.io/distroless/java:8 AS SECURITY
FROM openjdk:8 AS BUILD

COPY . /opt
WORKDIR /opt
RUN ./mvnw clean install -DskipTests

ENV JAVA_RANDOM="file:/dev/./urandom"

COPY --from=SECURITY /etc/java-8-openjdk/security/java.security /usr/local/openjdk-8/jre/lib/security/java.security
RUN echo "networkaddress.cache.ttl=60" >> /usr/local/openjdk-8/jre/lib/security/java.security
RUN sed -i -e "s@^securerandom.source=.*@securerandom.source=${JAVA_RANDOM}@" /usr/local/openjdk-8/jre/lib/security/java.security

FROM gcr.io/distroless/java:8

COPY --from=BUILD /opt/target/vault-crd.jar /opt/vault-crd.jar
COPY --from=BUILD /usr/local/openjdk-8/jre/lib/security/java.security /etc/java-8-openjdk/security/java.security

ENTRYPOINT ["/usr/bin/java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.ssl.trustStoreType=jks"]
CMD ["-jar", "/opt/vault-crd.jar"]
