curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.0/graalvm-ce-java11-windows-amd64-22.3.0.zip -o graalvm.zip
unzip -q graalvm.zip -d graalvm
rm graalvm.zip
mv graalvm/graalvm-ce-java11-22.3.0/* graalvm/
rm -rf graalvm/graalvm-ce-java11-22.3.0
graalvm/bin/gu.cmd install -n native-image
graalvm/bin/native-image.cmd --version