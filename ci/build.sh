#!/usr/bin/env bash

./gradlew --stacktrace -Pversion=${APP_VERSION} -PjavafxPlatform=linux jar
