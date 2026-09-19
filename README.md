# Chaos Butler Plugin

This plugin is the Jenkins equivalent of Netflix's Chaos Monkey from the Simian Army. It periodically disconnects a random Jenkins node to help validate the resilience of your infrastructure and operational processes.  See this [plugin's wiki page][wiki] for more details.

## Environment

The following build environment is required to build this plugin

* `java-25` and `maven-3.9.16` (or newer)

## Build

To build the plugin locally:

```
mvn clean verify
```

## Release

To release the plugin:

```
mvn release:prepare release:perform -B
```

## Test local instance

To test in a local Jenkins instance

```
mvn hpi:run
```

[wiki]: http://wiki.jenkins-ci.org/display/JENKINS/Chaos+Butler+Plugin
