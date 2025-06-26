# ===== Stage 1: Build with Maven =====
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy all files to the container
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# ===== Stage 2: Run the Spring Boot app =====
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the built jar from the previous stage
COPY --from=builder /app/target/online-banking-0.0.1-SNAPSHOT.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
