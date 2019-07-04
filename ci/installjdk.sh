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
# shellcheck disable=SC2016
export PATH="$JAVA_HOME/bin:$PATH"
# shellcheck disable=2088
~/bin/install-jdk.sh --url "https://download.java.net/openjdk/jdk10/ri/jdk-10_linux-x64_bin_ri.tar.gz" --target "$JAVA_HOME" --workspace "$TRAVIS_HOME/.cache/install-jdk" --cacerts