curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.1.0/graalvm-ce-java11-windows-amd64-20.1.0.zip -o graalvm.zip
unzip -q graalvm.zip -d graalvm
rm graalvm.zip
mv graalvm/graalvm-ce-java11-20.1.0/* graalvm/
rm -rf graalvm/graalvm-ce-java11-20.1.0
graalvm/bin/gu.cmd install -n native-image
graalvm/bin/native-image.cmd --version