#!/usr/bin/env bash

[ -z ${PUBLISH_SSH_PATH+x} ] && {
  echo "PUBLISH_SSH_PATH is unset"
  exit 1
}
[ -z ${APP_VERSION+x} ] && {
  echo "APP_VERSION is unset"
  exit 1
}

APP_VERSION_UNDERSCORES=$(echo ${APP_VERSION} | tr '.' '_')

ZIP_FILE_NAME="faf_windows-x64_${APP_VERSION_UNDERSCORES}.zip"
TAR_FILE_NAME="faf_unix_${APP_VERSION_UNDERSCORES}.tar.gz"

sshNoCheck() {
  ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" "$@"
}
scpNoCheck() {
  scp -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" "$@"
}

sshNoCheck -i /tmp/id_rsa.key travisci@${PUBLISH_SSH_HOST} "rm -rf ${PUBLISH_SSH_PATH}/win/${APP_VERSION}" &&
  scpNoCheck -i /tmp/id_rsa.key build/install4j/${ZIP_FILE_NAME} travisci@${PUBLISH_SSH_HOST}:${PUBLISH_SSH_PATH}/win/${ZIP_FILE_NAME} &&
  sshNoCheck -i /tmp/id_rsa.key travisci@${PUBLISH_SSH_HOST} "pushd ${PUBLISH_SSH_PATH}/win && unzip ${ZIP_FILE_NAME} \
    && rm ${ZIP_FILE_NAME} \
    && mv faf-client-${APP_VERSION} ${APP_VERSION}" &&
  scpNoCheck -i /tmp/id_rsa.key build/update4j/win/faf-client-${APP_VERSION}/update4j.xml travisci@${PUBLISH_SSH_HOST}:${PUBLISH_SSH_PATH}/win/${APP_VERSION}/update4j.xml

sshNoCheck -i /tmp/id_rsa.key travisci@${PUBLISH_SSH_HOST} "rm -rf ${PUBLISH_SSH_PATH}/unix/${APP_VERSION}" &&
  scpNoCheck -i /tmp/id_rsa.key build/install4j/${TAR_FILE_NAME} travisci@${PUBLISH_SSH_HOST}:${PUBLISH_SSH_PATH}/unix/${TAR_FILE_NAME} &&
  sshNoCheck -i /tmp/id_rsa.key travisci@${PUBLISH_SSH_HOST} "pushd ${PUBLISH_SSH_PATH}/unix && tar xzf ${TAR_FILE_NAME} \
    && rm ${TAR_FILE_NAME} \
    && mv faf-client-${APP_VERSION} ${APP_VERSION}" &&
  scpNoCheck -i /tmp/id_rsa.key build/update4j/unix/faf-client-${APP_VERSION}/update4j.xml travisci@${PUBLISH_SSH_HOST}:${PUBLISH_SSH_PATH}/unix/${APP_VERSION}/update4j.xml

sshNoCheck -i /tmp/id_rsa.key travisci@${PUBLISH_SSH_HOST} "rdfind -makehardlinks true -makeresultsfile false ${PUBLISH_SSH_PATH}"
