#!/bin/env bash
./gradlew jibDockerBuild && docker-compose down && docker-compose up -d
