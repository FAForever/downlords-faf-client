#!/usr/bin/env bash

# Download install4j and the required JRE, only if it doesn't already exist (from Travis cache)
INSTALL4J_DIR="$HOME/install4j/install4j7.0.10"
if [ ! -d "${INSTALL4J_DIR}" ]; then
  mkdir -p "$HOME/install4j"
  curl https://download-keycdn.ej-technologies.com/install4j/install4j_unix_7_0_10.tar.gz -o "$HOME/install4j/install4j.tar.gz"
  mkdir -p "${INSTALL4J_DIR}/jres/"
  curl http://content.faforever.com/jre/windows-amd64-10.0.2.tar.gz -o "${INSTALL4J_DIR}/jres/windows-amd64-10.0.2.tar.gz"
  tar xzf "$HOME/install4j/install4j.tar.gz" -C "$HOME/install4j"
fi

./gradlew -Pversion=${APP_VERSION} \
  -PjavafxPlatform=linux \
  -Pinstall4jHomeDir="${INSTALL4J_DIR}" \
  -Pinstall4jLicense=${install4jLicense} \
  buildInstall4jMediaFiles

./gradlew -Pversion=${APP_VERSION} \
  -PjavafxPlatform=win \
  -Pinstall4jHomeDir="${INSTALL4J_DIR}" \
  -Pinstall4jLicense=${install4jLicense} \
  --info \
  --stacktrace \
  buildInstall4jMediaFiles

./gradlew -Pversion=${APP_VERSION} \
  -PjavafxPlatform=mac \
  -Pinstall4jHomeDir="${INSTALL4J_DIR}" \
  -Pinstall4jLicense=${install4jLicense} \
  buildInstall4jMediaFiles

RELEASE_BODY=$(python3 release-body.py ${GITHUB_RELEASE_VERSION})
echo "Release body:
${RELEASE_BODY}";
