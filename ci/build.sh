#!/usr/bin/env bash

./gradlew --stacktrace -Pversion=${APP_VERSION} -PjavafxPlatform=linux -Pupdate4jBaseUrl=https://${PUBLISH_SSH_HOST}/ jar
