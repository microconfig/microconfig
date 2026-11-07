#!/usr/bin/env bash
set -euo pipefail

curl --fail --silent --show-error --location https://github.com/graalvm/graalvm-ce-builds/releases/download/graal-25.5.5.0.3/graalvm-community-jdk-25i5-25.0.5.0.3_windows-x64_bin.zip -o graalvm.zip
unzip -q graalvm.zip -d graalvm
rm graalvm.zip
mv graalvm/graalvm-community-*/* graalvm/
rm -rf graalvm/graalvm-community-*
graalvm/bin/native-image.cmd --version
