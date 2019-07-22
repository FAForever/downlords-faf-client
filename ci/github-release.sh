#!/usr/bin/env bash

# Download install4j and the required JRE, only if it doesn't already exist (from Travis cache)
INSTALL4J_DIR="$HOME/install4j/install4j8.0"
JRE_PLACE="$HOME/.install4j8"
if [[ ! -d "${INSTALL4J_DIR}" || ! -d "${JRE_PLACE}" ]]; then
  rm -rf "$HOME/install4j"
  rm -rf "$JRE_PLACE"
  mkdir "$HOME/install4j"
  curl https://download-keycdn.ej-technologies.com/install4j/install4j_unix_8_0.tar.gz -o "$HOME/install4j/install4j.tar.gz"
  mkdir -p "${JRE_PLACE}/jres/"
  curl http://content.faforever.com/jre/windows-amd64-10.0.2.tar.gz -o "${JRE_PLACE}/jres/windows-amd64-10.0.2.tar.gz"
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
