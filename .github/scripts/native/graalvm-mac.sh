curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_macos-x64_bin.tar.gz -o graalvm.tar.gz
mkdir .graalvm
tar -xf graalvm.tar.gz -C .graalvm
rm graalvm.tar.gz
mv .graalvm/graalvm-community-openjdk-*/Contents/Home/* .graalvm/
rm -rf .graalvm/graalvm-community-openjdk-*
.graalvm/bin/native-image --version