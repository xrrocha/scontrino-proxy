#!/bin/bash
cd "$BASEDIR/test-data" || exit 1
basename="$(basename $1-interactions.tsv .tsv)"
rm -f "$basename.dump" $basename-*.b64 $basename-*.raw
sort -t'	' -k3,3n -k4,4 "$basename.tsv" |
awk -F\\t '{ print $4, $5 }' |\
while read type interaction
do
  echo $type >> "$basename.dump"
  echo -n "$interaction" | base64 -d | xxd >> "$basename.dump"
  echo >> "$basename.dump"
done
