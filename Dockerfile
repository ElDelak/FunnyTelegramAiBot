# Use a JDK base image
FROM eclipse-temurin:17-jdk

# Set working directory inside container
WORKDIR /app

# Copy the whole project into the container
COPY . .

# Build the project using Maven wrapper
RUN ./mvnw clean package -DskipTests

# Run the built Spring Boot app
CMD ["java", "-jar", "target/funnyTelegramAiBoot-1.0.0.jar"]