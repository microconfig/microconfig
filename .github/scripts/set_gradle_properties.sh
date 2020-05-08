sed "s+$1=+$1=$2+g" gradle.properties > gradle.properties.tmp && mv gradle.properties.tmp gradle.properties
