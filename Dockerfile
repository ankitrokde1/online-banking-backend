# Use Eclipse Temurin OpenJDK 17 (official image)
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the jar from build context into image
COPY target/online-banking-0.0.1-SNAPSHOT.jar app.jar

# Expose port (matches Spring Boot's default)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
