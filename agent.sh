~/graalvm/bin/java -agentlib:native-image-agent=config-output-dir=native \
  -jar microconfig-cli/build/libs/microconfig-cli-4.1.1-all.jar \
  -r /Users/kapodes/IdeaProjects/microconfig/microconfig-core/src/test/resources/templates/ -d /Users/kapodes/microconfig/build -e dev