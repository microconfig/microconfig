curl -sL https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/20.1.0-dev-20200506_1135/graalvm-ce-java11-darwin-amd64-20.1.0-dev.tar.gz -o graalvm.tar.gz
mkdir .graalvm
tar -xf graalvm.tar.gz -C .graalvm
rm graalvm.tar.gz
mv .graalvm/graalvm-ce-java8-20.0.0/Contents/Home/* .graalvm/
rm -rf .graalvm/graalvm-ce-java8-20.0.0
chmod +x .graalvm/bin/gu
.graalvm/bin/gu install -n native-image
.graalvm/bin/native-image --version