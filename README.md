# Eppo Server-Side SDK for Java

[![Test and lint SDK](https://github.com/Eppo-exp/java-server-sdk/actions/workflows/lint-test-sdk.yml/badge.svg)](https://github.com/Eppo-exp/java-server-sdk/actions/workflows/lint-test-sdk.yml)  
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cloud.eppo/eppo-server-sdk/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cloud.eppo/eppo-server-sdk)

## Usage

### build.gradle:

```groovy
dependencies {
  implementation 'cloud.eppo:eppo-server-sdk:6.0.0'
}
```

Refer to our [SDK documentation](https://docs.geteppo.com/sdks/server-sdks/java/) for how to install and use the SDK.

## Upgrading from 5.x to 6.0.0

6.0.0 moves the `cloud.eppo:sdk-common-jvm` API dependency from 3.13.2 to 4.0.3.
Common 4.0.3 adds `cloud.eppo:eppo-sdk-framework` 0.1.3 as an API dependency.
Recompile code that you built against 5.x.

If your code uses common SDK types directly:

- DTOs moved from `cloud.eppo.ufc.dto` to `cloud.eppo.api.dto`. Except for the
  enums, they are now interfaces. Replace constructors such as
  `new FlagConfig(...)` with `new FlagConfig.Default(...)`. `BanditAttributeCoefficients` has no `Default`
  implementation; supply an implementation if you construct it directly.
- `cloud.eppo.ufc.dto.adapters.EppoModule` and `EppoValueDeserializer` did not
  move. The other adapters are no longer public.
- `cloud.eppo.model.ShardRange` is now `cloud.eppo.api.dto.ShardRange`.
- `Configuration.builder(byte[])` and `builder(byte[], boolean)` were removed. Use
  `Configuration.builder(new JacksonConfigurationParser().parseFlagConfig(bytes))`,
  or `new Configuration.Builder(flagConfigResponse, isConfigObfuscated)` to set the
  obfuscation flag yourself.
- `Configuration.serializeFlagConfigToBytes()`,
  `Configuration.serializeBanditParamsToBytes()` and
  `Attributes.serializeNonNullAttributesToJSONString()` were removed.
  `cloud.eppo.exception.InvalidApiKeyException` was also removed.
- `cloud.eppo.EppoHttpClient` was removed. To supply your own HTTP client,
  implement `cloud.eppo.http.EppoConfigurationClient` and pass it to
  `EppoClient.builder(sdkKey).configurationClient(...)`. The default is
  `OkHttpEppoClient`; pass `new OkHttpEppoClient(okHttpClient)` to reuse your own
  OkHttp settings. To point tests at a stub server, use `apiBaseUrl(...)`.

Behaviour changes:

- With `isGracefulMode(false)`, `getJSONAssignment()`, `getJSONStringAssignment()`
  and their `*Details()` variants throw when the assigned variation is not valid
  JSON. 5.x returned the default (for `getJSONAssignment()`, only a non-null
  default). Graceful mode still returns the default.
- Once the client saves a configuration that came with an ETag, polls send
  `If-None-Match`. When the server answers 304 Not Modified, the client keeps its
  configuration and `onConfigurationChange` does not fire. In 5.x it fired after
  every successful poll.

New: `get*AssignmentDetails()` and `getBanditActionDetails()` return evaluation
details with the value.

The SDK no longer brings Ehcache and its supporting libraries transitively.
If your application uses Ehcache directly, declare its dependency explicitly.

Common 4.0.2 changes how the client handles bandit failures:

- A failed bandit fetch no longer blocks flag updates. The client applies the new
  flags, keeps its loaded bandit models, and retries the bandits on the next poll.
- When bandit evaluation fails, for example because the models are not loaded yet,
  graceful mode (the default) makes `getBanditAction` return the default value with
  a null action, and `getBanditActionDetails` reports evaluation code `BANDIT_ERROR`.
  Common 3.13.2 returned the bandit variation. The assignment logger has already
  recorded the flag's bandit variation for that call, so the logged variation can
  differ from the value your code receives. With graceful mode disabled, both
  methods throw, as in 5.x.

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
Snapshots are published automatically after each push to the `main` branch while `build.gradle` has a
`-SNAPSHOT` version.

### build.gradle:

```groovy
repositories {
  maven { url "https://central.sonatype.com/repository/maven-snapshots/" }
}

dependencies {
  implementation 'cloud.eppo:eppo-server-sdk:X.Y.Z-SNAPSHOT'
}
```
