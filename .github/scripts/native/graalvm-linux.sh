curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.0/graalvm-ce-java11-linux-amd64-22.3.0.tar.gz -o graalvm.tar.gz
mkdir .graalvm
tar -xf graalvm.tar.gz -C .graalvm
rm graalvm.tar.gz
mv .graalvm/graalvm-ce-java11-22.3.0/* .graalvm/
rm -rf .graalvm/graalvm-ce-java11-22.3.0
chmod +x .graalvm/bin/gu
.graalvm/bin/gu install -n native-image
.graalvm/bin/native-image --version