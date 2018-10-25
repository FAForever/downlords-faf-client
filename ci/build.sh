#!/usr/bin/env bash

# Download JavaFX only if it doesn't already exist (from Travis cache)
if [ ! -d "$HOME/openjfx/javafx-sdk-11" ]; then
  mkdir -p "$HOME/openjfx"
  curl http://content.faforever.com/openjfx/openjfx-11_linux-x64_bin-sdk.zip -o "$HOME/openjfx/openjfx.zip"
  unzip -d "$HOME/openjfx" "$HOME/openjfx/openjfx.zip"
  ls -lah $HOME/openjfx/javafx-sdk-11/lib
fi

./gradlew --stacktrace -Pversion=${APP_VERSION} -PjavafxPlatform=linux jar
