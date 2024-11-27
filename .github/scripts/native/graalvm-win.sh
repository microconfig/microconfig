curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_windows-x64_bin.zip -o graalvm.zip
unzip -q graalvm.zip -d graalvm
rm graalvm.zip
mv graalvm/graalvm-community-openjdk-*/* graalvm/
rm -rf graalvm/graalvm-community-openjdk-*
graalvm/bin/gu.cmd install -n native-image
graalvm/bin/native-image.cmd --version