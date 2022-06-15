#!/usr/bin/env sh

# Build Docker Image
./gradlew jibDockerBuild

# transfer docker image to server:
# docker save transcribeapi:0.0.1 | bzip2 | pv | ssh thomas@home docker load

# only compress on "slow" connects < 3MB/s upload
docker save transcriptapi:0.0.1 | bzip2 | pv | ssh thomas@transcriptapi.reitmaier.xyz docker load
ssh thomas@transcriptapi.reitmaier.xyz cd \~/docker \&\& docker-compose up -d transcriptapi
