curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.0.0/graalvm-ce-java8-darwin-amd64-20.0.0.tar.gz -o graalvm.tar.gz
mkdir .graalvm
tar -xf graalvm.tar.gz -C .graalvm
rm graalvm.tar.gz
mv .graalvm/graalvm-ce-java8-20.0.0/Contents/Home/* .graalvm/
rm -rf .graalvm/graalvm-ce-java8-20.0.0
chmod +x .graalvm/bin/gu
.graalvm/bin/gu install -n native-image
.graalvm/bin/native-image --version