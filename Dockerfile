# ---------------------------------------------------------------
# Stage 1 — Build
# ---------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
# Cache dependency layer
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ---------------------------------------------------------------
# Stage 2 — Runtime
# ---------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S notifyhub && adduser -S notifyhub -G notifyhub
WORKDIR /app

COPY --from=builder /build/target/notify-hub-*.jar app.jar
RUN chown notifyhub:notifyhub app.jar

USER notifyhub

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
