# syntax=docker/dockerfile:1

# --- build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Baixa as dependencias primeiro: essa camada so e refeita quando o pom muda.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# --- runtime ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Nao roda como root.
RUN addgroup -S app && adduser -S app -G app
USER app

EXPOSE 8080

COPY --from=build /build/target/sistema-emprestimo-*.jar app.jar

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
