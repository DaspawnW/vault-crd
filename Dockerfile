FROM openjdk:8-alpine AS BUILD

COPY . /opt
WORKDIR /opt
RUN ./mvnw clean install -DskipTests


FROM openjdk:8-alpine

COPY --from=BUILD /opt/target/vault-crd.jar /opt/vault-crd.jar
WORKDIR /opt

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar vault-crd.jar
