FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src/

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]