# Use OpenJDK 17 base image
FROM openjdk:17

# Set the working directory
WORKDIR /app

# Install necessary packages using microdnf
RUN microdnf install -y wget unzip \
    && wget https://services.gradle.org/distributions/gradle-8.6-bin.zip -O /tmp/gradle.zip \
    && unzip /tmp/gradle.zip -d /opt/ \
    && ln -s /opt/gradle-8.6/bin/gradle /usr/bin/gradle \
    && rm -rf /tmp/gradle.zip

# Copy the project files
COPY . .

# Make the Gradle wrapper executable (if using Gradle wrapper)
RUN chmod +x ./gradlew

# Default command to run the build
CMD ["./gradlew", "build"]
