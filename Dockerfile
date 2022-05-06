FROM gcr.io/distroless/java17:nonroot AS SECURITY
FROM openjdk:17 AS BUILD

COPY . /opt
WORKDIR /opt
RUN ./mvnw clean install -DskipTests

ENV JAVA_RANDOM="file:/dev/./urandom"

COPY --from=SECURITY /etc/java-17-openjdk/security/java.security /java.security
RUN echo "networkaddress.cache.ttl=60" >> /java.security
RUN sed -i -e "s@^securerandom.source=.*@securerandom.source=${JAVA_RANDOM}@" /java.security

FROM gcr.io/distroless/java17:nonroot

COPY --from=BUILD /opt/target/vault-crd.jar /opt/vault-crd.jar
COPY --from=BUILD /java.security /etc/java-17-openjdk/security/java.security

ENTRYPOINT ["/usr/bin/java", "-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.ssl.trustStoreType=jks"]
CMD ["-jar", "/opt/vault-crd.jar"]
