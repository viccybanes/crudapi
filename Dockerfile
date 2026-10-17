FROM maven:3.9.11-eclipse-temurin-17
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
COPY docs/index.html ./docs/index.html
COPY openapi.json .
RUN mvn -q test-compile
EXPOSE 3000
CMD ["mvn", "-q", "exec:java"]
