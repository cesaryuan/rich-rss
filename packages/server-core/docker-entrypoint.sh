#!/usr/bin/env sh

if ! sh ./pre-boot.sh;
then
  echo 'pre boot validation failed'
  exit 1
fi

echo 'Starting app...'
# see https://www.atamanroman.dev/articles/usecontainersupport-to-the-rescue/
#  -XX:+UseCGroupMemoryLimitForHeap \
#NODE_ID=`hexdump -n 16 -v -e '/1 "%02X"' -e '/16 "\n"' /dev/urandom`
#export NODE_ID=${NODE_ID}
java -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=85.0 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:HeapDumpPath=/usr/feedless/java_error_in_feedless_.hprof \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  -Duser.timezone=${APP_TIMEZONE} \
  -Dspring.profiles.active=prod,${APP_AUTHENTICATION},${APP_ACTIVE_PROFILES} \
  -XX:+HeapDumpOnOutOfMemoryError \
  -Dfile.encoding=UTF-8 \
  -jar app.jar
