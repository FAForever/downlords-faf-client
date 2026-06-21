#!/bin/bash
#
# FAF Client launcher for macOS (Apple Silicon).
# See scripts/mac/README.md for setup.
#

set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="${DIST_DIR:-$(cd "$SCRIPT_DIR/../.." && pwd)/build/install/faf-client}"

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "JAVA_HOME must point to a JDK 25 aarch64 installation." >&2
    exit 1
fi

if [ ! -d "$DIST_DIR" ]; then
    echo "Distribution not found at $DIST_DIR." >&2
    echo "Build with: ./gradlew installDist -PjavafxPlatform=mac-aarch64" >&2
    exit 1
fi

# discord-rpc and steamworks4j ship JNA natives with no arm64 macOS binary;
# faf-ice-adapter.jar is launched separately by the client.
CLASSPATH=""
for jar in "$DIST_DIR"/*.jar; do
    case "$(basename "$jar")" in
        faf-ice-adapter.jar|discord-rpc-*|steamworks4j-*) continue ;;
    esac
    CLASSPATH="${CLASSPATH:+$CLASSPATH:}$jar"
done

# -Djava.awt.headless=false       overrides Spring Boot's default (true),
#                                 which otherwise prevents JavaFX startup.
# --spring.profiles.active=prod,mac   the mac profile excludes Steam and
#                                     Discord beans that need native libs.
exec "$JAVA_HOME/bin/java" \
    -Djava.awt.headless=false \
    -DnativeDir="$DIST_DIR" \
    -cp "$CLASSPATH" \
    com.faforever.client.Main \
    --spring.profiles.active=prod,mac
