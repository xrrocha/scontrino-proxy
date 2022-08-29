#!/bin/bash
set -e
BASEDIR=$(dirname $0)/..

java -jar $BASEDIR/build/libs/scontrino-proxy-1.0-SNAPSHOT-all.jar \
  --host=localhost --remote-port=5432 --local-port=2345 \
  --dump-file=$BASEDIR/test-data/postgres-interactions.tsv \
  --buffer-size=32768
