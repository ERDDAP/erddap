#!/bin/bash
set -e

# preferable to fire up Tomcat via start-tomcat.sh which will start Tomcat with
# security manager, but inheriting containers can also start Tomcat via
# catalina.sh

if [ "$1" = 'start-tomcat.sh' ] || [ "$1" = 'catalina.sh' ]; then
    # generate random flagKeyKey if not set
    if [ -z "$ERDDAP_flagKeyKey" ] && grep "<flagKeyKey>CHANGE THIS TO YOUR FAVORITE QUOTE</flagKeyKey>" \
        "${CATALINA_HOME}/content/erddap/setup.xml" &> /dev/null; then
      echo "flagKeyKey isn't properly set. Generating a random value." >&2
      export ERDDAP_flagKeyKey=$(cat /proc/sys/kernel/random/uuid)
    fi

    echo "Starting preinitialization"
    PREINIT_START=$(date +%s%3N)

    USER_ID=${TOMCAT_USER_ID:-1000}
    GROUP_ID=${TOMCAT_GROUP_ID:-1000}

    ###
    # Tomcat user
    ###
    # create group for GROUP_ID if one doesn't already exist
    if ! getent group $GROUP_ID &> /dev/null; then
      groupadd -r tomcat -g $GROUP_ID
    fi
    # create user for USER_ID if one doesn't already exist
    if ! getent passwd $USER_ID &> /dev/null; then
      useradd -u $USER_ID -g $GROUP_ID tomcat
    fi
    # alter USER_ID with nologin shell and CATALINA_HOME home directory
    usermod -d "${CATALINA_HOME}" -s /sbin/nologin $(id -u -n $USER_ID)

    ###
    # Change CATALINA_HOME ownership to tomcat user and tomcat group
    # Restrict permissions on conf
    ###

    chown -R $USER_ID:$GROUP_ID \
         ${CATALINA_HOME}/bin \
         ${CATALINA_HOME}/conf \
         ${CATALINA_HOME}/content \
         ${CATALINA_HOME}/logs \
         ${CATALINA_HOME}/temp \
         ${CATALINA_HOME}/work
    find ${CATALINA_HOME}/webapps/erddap -type d -exec chown $USER_ID:$GROUP_ID {} +
    find ${CATALINA_HOME}/webapps/erddap -type f -name '*.sh' -exec chown $USER_ID:$GROUP_ID {} +
    find ${CATALINA_HOME}/conf -type d -exec chmod 755 {} +
    find ${CATALINA_HOME}/conf -type f -exec chmod 400 {} +

    if [ "${SKIP_ERDDAP_DATA_CHOWN:-0}" = "1" ]; then
      echo "Skipping /erddapData chown due to SKIP_ERDDAP_DATA_CHOWN setting"
    else
      chown -R $USER_ID:$GROUP_ID /erddapData
    fi
    sync

    ###
    # Run executables/shell scripts in /init.d on each container startup
    # Inspired by postgres' /docker-entrypoint-initdb.d
    # https://github.com/docker-library/docs/blob/master/postgres/README.md#initialization-scripts
    # https://github.com/docker-library/postgres/blob/master/docker-entrypoint.sh#L156
    ###
    if [ -d "/init.d" ]; then
      for f in /init.d/*; do
        if [ -x "$f" ]; then
          echo "Executing $f"
          "$f"
        elif [[ $f == *.sh ]]; then
          echo "Sourcing $f (not executable)"
          . "$f"
        fi
      done
    fi

    PREINIT_END=$(date +%s%3N)
    echo "Preinitialization finished in $(( $PREINIT_END - $PREINIT_START ))ms"

    exec setpriv --reuid $USER_ID --regid $GROUP_ID --init-groups "$@"
fi

exec "$@"
