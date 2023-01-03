VERSION=21.0.0

curl -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$VERSION/graalvm-ce-java11-linux-amd64-$VERSION.tar.gz -o graalvm.tar.gz
mkdir .graalvm
tar -xf graalvm.tar.gz -C .graalvm
rm graalvm.tar.gz
mv .graalvm/graalvm-ce-java11-*/* .graalvm/
rm -rf .graalvm/graalvm-ce-java11-*
chmod +x .graalvm/bin/gu
.graalvm/bin/gu install -n native-image
.graalvm/bin/native-image --version

#Install musl
mkdir musl
curl -sL https://musl.libc.org/releases/musl-1.2.1.tar.gz | tar xz
cd musl-1.2.1
./configure --disable-shared --prefix="../musl"
make && make install
cd ..
musl-gcc -v

# Install zlib
curl -sL https://zlib.net/zlib-1.2.11.tar.gz | tar xz
cd zlib-1.2.11
export CC=musl-gcc
./configure --static --prefix="../musl"
make && make install
cd ..