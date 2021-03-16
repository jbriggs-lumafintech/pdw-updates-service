FROM openjdk:11.0.3-jdk-slim
VOLUME /tmp
ARG JAR_FILE

ENV _JAVA_OPTIONS "-Xms256m -Xmx512m -Djava.awt.headless=true"
ADD ca.jks /app/ca.jks

COPY ${JAR_FILE} /opt/app.jar

WORKDIR /opt
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Djavax.net.ssl.trustStore=/app/ca.jks", "-jar", "/opt/app.jar"]
