curl -sL https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/20.1.0-dev-20200506_1135/graalvm-ce-java11-windows-amd64-20.1.0-dev.zip -o graalvm.zip
unzip -q graalvm.zip -d graalvm
rm graalvm.zip
mv graalvm/graalvm-ce-java11-20.1.0-dev/* graalvm/
rm -rf graalvm/graalvm-ce-java11-20.1.0-dev
graalvm/bin/gu.cmd install -n native-image
ls graalvm/bin
graalvm/bin/native-image.cmd --version
graalvm/bin/native-image.bat --version