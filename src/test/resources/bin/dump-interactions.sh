#!/bin/bash
awk -F\\t '{print $5}' "./build/tmp/test/proxy/$1-interactions.tsv" |\
while read payload
do
  echo -n "$payload" | base64 -d
done
