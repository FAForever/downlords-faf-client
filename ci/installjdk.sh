#!/usr/bin/env bash

if test -d "~/$JDK"; then
  echo "already installed"
  exit 0
fi

mkdir -p ~/bin
url="https://raw.githubusercontent.com/sormuras/bach/master/install-jdk.sh"
wget "$url" -P ~/bin/ || {
  echo "${ANSI_RED}Could not acquire install-jdk.sh. Stopping build.${ANSI_RESET}" >/dev/stderr
  exit 2
}
chmod +x ~/bin/install-jdk.sh
export JAVA_HOME="~/$JDK"
~/bin/install-jdk.sh --url "https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1%2B9/OpenJDK15U-jdk_x64_linux_hotspot_15.0.1_9.tar.gz" --target "$JAVA_HOME" --workspace "$TRAVIS_HOME/.cache/install-jdk" --cacerts
