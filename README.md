# Chaos Butler Plugin

This plugin aims to be an equivalent to the Chaos Monkey in Netflix's Simian Army. See this [plugin's wiki page][wiki] for more details.

# Environment

The following build environment is required to build this plugin

* `java-1.7` and `maven-3.3.9`

# Build

To build the plugin locally:

    mvn clean verify

# Release

To release the plugin:

    mvn release:prepare release:perform -B

# Test local instance

To test in a local Jenkins instance

    mvn hpi:run

  [wiki]: http://wiki.jenkins-ci.org/display/JENKINS/Chaos+Butler+Plugin
