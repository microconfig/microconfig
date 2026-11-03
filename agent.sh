.graalvm/bin/java -agentlib:native-image-agent=config-merge-dir=microconfig-cli/src/main/resources/META-INF/native-image \
  -jar microconfig-cli/build/libs/microconfig-cli-$(sed -n 's/^version=//p' gradle.properties).jar \
  -r ~/microconfig/microconfig-quickstart -d ~/microconfig/microconfig-quickstart/build -e dev $1