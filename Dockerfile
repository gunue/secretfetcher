# Use the official Maven image to build the Java code
FROM maven:3.8.5-openjdk-17 AS build

# Set the current working directory inside the image
WORKDIR /app

# Copy the Maven pom.xml and source files into the image
COPY pom.xml .
COPY src ./src

# Package the Java code and its dependencies into a fat JAR
RUN mvn package

# List contents of /app/target (for debugging purposes)
RUN ls -l

# Using OpenJDK 17 base image
FROM openjdk:17

# Set the current working directory in the image to /app
WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /app/target/app-1.0-SNAPSHOT.jar .

# Expose port 8080 for the API endpoint
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app-1.0-SNAPSHOT.jar"]
