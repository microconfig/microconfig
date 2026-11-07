#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  printf '%s\n' 'Usage: set_gradle_properties.sh key value' >&2
  exit 2
fi

properties_tmp=$(mktemp gradle.properties.XXXXXX)
value_tmp=
trap 'rm -f "$properties_tmp" "$value_tmp"' EXIT
value_tmp=$(mktemp gradle-property-value.XXXXXX)

# Properties.load(InputStream) reads Unicode escapes as UTF-16 code units.
printf '%s' "$2" | iconv -f UTF-8 -t UTF-16BE > "$value_tmp"
property_value=$(od -An -v -tx1 "$value_tmp" | tr -d ' \n' | sed 's/..../\\u&/g')

MICROCONFIG_PROPERTY_KEY="$1" MICROCONFIG_PROPERTY_VALUE="$property_value" awk '
BEGIN {
  propertyKey = ENVIRON["MICROCONFIG_PROPERTY_KEY"]
  propertyValue = ENVIRON["MICROCONFIG_PROPERTY_VALUE"]
}
index($0, propertyKey "=") == 1 {
  print propertyKey "=" propertyValue
  found = 1
  next
}
{ print }
END {
  if (!found) print propertyKey "=" propertyValue
}
' gradle.properties > "$properties_tmp"

mv "$properties_tmp" gradle.properties
