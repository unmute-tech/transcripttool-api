FROM gradle:7.0.2-jdk11 AS BUILD_JAR
WORKDIR /usr/app/
COPY . .
RUN gradle clean :build -x test --no-daemon --stacktrace

FROM openjdk:11-jre
RUN apt-get -y update && apt-get clean && rm -rf /tmp/* /var/tmp/*

RUN mkdir /app
COPY --from=BUILD_JAR /usr/app/build/libs/transcribeapi.jar /app/transcribeapi.jar

WORKDIR /app
CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "transcribeapi.jar"]
