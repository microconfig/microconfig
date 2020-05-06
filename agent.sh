~/graalvm/bin/java -agentlib:native-image-agent=config-output-dir=native \
  -jar microconfig-cli/build/libs/microconfig-cli-*-all.jar \
  -r /Users/kapodes/IdeaProjects/microconfig-quickstart -d /Users/kapodes/IdeaProjects/microconfig-quickstart/build -e dev