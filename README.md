**Tested on Java LTS versions from <!--property:java-runtime.min-version-->11<!--/property--> to <!--property:java-runtime.max-version-->24<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->7.0<!--/property--> to <!--property:gradle-api.max-version-->9.1.0<!--/property-->.**

# `name.remal.build-time-constants` plugin

[![configuration cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

Usage:

<!--plugin-usage:name.remal.build-time-constants-->
```groovy
plugins {
    id 'name.remal.build-time-constants' version '2.0.2'
}
```
<!--/plugin-usage-->

&nbsp;

The plugin automatically adds
<code><!--property:apiNotationWithoutVersion-->name.remal.gradle-plugins.build-time-constants:build-time-constants-api<!--/property--></code>
dependency to `compileOnly` configuration of every [`SourceSet`](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/SourceSet.html).
The version of the added dependency will be the same as the plugin version.

You can use `name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants` class in any JVM-language statically compiled code.
The plugin processes `*.class` files and injects build-time constants instead of `BuildTimeConstants` methods invocation.

## Using build-time properties

Let's say a developer would like to use the project's current version in the project's code.

Common ways to do so are code generation and source preprocessing. And both of the ways are pretty complex.

This plugin provides a more straightforward solution.

First, set the property as a build-time property for replacement:

```groovy
buildTimeConstants {
  property('version', project.version)
  property('version', provider { project.version } ) // providers and Gradle properties are supported too
}
```

Second, use `BuildTimeConstants.getStringProperty("version")` in the code.

Third, the plugin will replace `getStringProperty()` invocation with the `project.version` value.

See the Javadoc documentation for other `BuildTimeConstants.get*Property()` and `BuildTimeConstants.get*Properties()` methods.

## Decoupling from class literals

When working with optional dependencies, class names are usually used instead of class literals (`"java.lang.Object"` instead of `Object.class`).
The issue with class names as strings is that they are not checked at build time and can be missed during refactoring.

This plugin can convert class literals to string values.
For example, `BuildTimeConstants.getClassName(Object.class)` will be converted to `java.lang.Object`.

See the Javadoc documentation for other `BuildTimeConstants.getClass*()` methods.

# Migration guide

## Version 1.* to 2.*

The minimum Java version is 11 (from 8).
The minimum Gradle version is 7.0 (from 6.7).
