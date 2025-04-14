# Eppo Server-Side SDK for Java

[![Test and lint SDK](https://github.com/Eppo-exp/java-server-sdk/actions/workflows/lint-test-sdk.yml/badge.svg)](https://github.com/Eppo-exp/java-server-sdk/actions/workflows/lint-test-sdk.yml)  
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cloud.eppo/eppo-server-sdk/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cloud.eppo/eppo-server-sdk)

## Usage

### build.gradle:

```groovy
dependencies {
  implementation 'cloud.eppo:eppo-server-sdk:5.2.0'
}
```

Refer to our [SDK documentation](https://docs.geteppo.com/sdks/server-sdks/java/) for how to install and use the SDK.

## Contributing

Java 8 is required to locally compile the SDK.

### Apple M-Series

Download a `arm64` compatible build: https://www.azul.com/downloads/?version=java-8-lts&architecture=arm-64-bit&package=jdk#zulu

## Releasing a new version

For publishing a release locally, follow the steps below.

### Prerequisites

1. [Generate a user token](https://central.sonatype.org/publish/generate-token/) on `s01.oss.sonatype.org`;
2. [Configure a GPG key](https://central.sonatype.org/publish/requirements/gpg/) for signing the artifact. Don't forget to upload it to the key server;
3. Make sure you have the following vars in your `~/.gradle/gradle.properties` file:
    1. `ossrhUsername` - User token username for Sonatype generated in step 1
    2. `ossrhPassword` - User token password for Sonatype generated in step 1
    3. `signing.keyId` - GPG key ID generated in step 2
    4. `signing.password` - GPG key password generated in step 2
    5. `signing.secretKeyRingFile` - Path to GPG key file generated in step 2

Once you have the prerequisites, follow the steps below to release a new version:

1. Bump the project version in `build.gradle`
2. Run `./gradlew publish`
3. Follow the steps in [this page](https://central.sonatype.org/publish/release/#credentials) to promote your release

## Using Snapshots

If you would like to live on the bleeding edge, you can try running against a snapshot build. Keep in mind that snapshots
represent the most recent changes on master and may contain bugs.
Snapshots are published automatically after each push to `main` branch.

### build.gradle:

```groovy
repositories {
  maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots" }
}

dependencies {
  implementation 'cloud.eppo:eppo-server-sdk:4.0.1-SNAPSHOT'
}
```
