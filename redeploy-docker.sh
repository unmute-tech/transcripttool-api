#!/bin/env bash
mkdir -p data
./gradlew jibDockerBuild && docker-compose down && docker-compose up -d
