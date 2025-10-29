#!/bin/sh

# Configure JAVA_OPTS

# Useful for setting version specific options
JAVA_MAJOR_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)

# Source bin/config.sh if present
if [ -f "${CATALINA_HOME}/bin/config.sh" ]; then
    set -o allexport
    source "${CATALINA_HOME}/bin/config.sh"
    set +o allexport
fi

# JAVA_OPTS
NORMAL="-server -Djava.awt.headless=true -XX:+UseCompactObjectHeaders"
HEAP_DUMP="-XX:+HeapDumpOnOutOfMemoryError"

# Memory
if [ -n "$ERDDAP_MAX_RAM_PERCENTAGE" ]; then
  JVM_MEMORY_ARGS="-XX:MaxRAMPercentage=${ERDDAP_MAX_RAM_PERCENTAGE}"
else
  ERDDAP_MEMORY="${ERDDAP_MEMORY:-4G}"
  JVM_MEMORY_ARGS="-Xms${ERDDAP_MIN_MEMORY:-${ERDDAP_MEMORY}} -Xmx${ERDDAP_MAX_MEMORY:-${ERDDAP_MEMORY}}"
fi

CONTENT_ROOT="-DerddapContentDirectory=${CATALINA_HOME}/content/erddap/"

JAVA_OPTS="$JAVA_OPTS $NORMAL $JVM_MEMORY_ARGS $HEAP_DUMP"
echo "ERDDAP running with JAVA_OPTS: $JAVA_OPTS"

# Show ERDDAP configuration environment variables
ERDDAP_CONFIG=$(env | grep --regexp "^ERDDAP_.*$" | sort)
if [ -n "$ERDDAP_CONFIG" ]; then
    echo -e "ERDDAP configured with:\n$ERDDAP_CONFIG"
fi

