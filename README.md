# Eppo Java SDK

[![Test and lint SDK](https://github.com/Eppo-exp/java-server-sdk/actions/workflows/lint-test-sdk.yml/badge.svg)](https://github.com/Eppo-exp/java-server-sdk/actions/workflows/lint-test-sdk.yml)

[Eppo](https://www.geteppo.com/) is a modular flagging and experimentation analysis tool. Eppo's Java SDK is built to make assignments in multi-user server side contexts. Before proceeding you'll need an Eppo account.

## Features

- Feature gates
- Kill switches
- Progressive rollouts
- A/B/n experiments
- Mutually exclusive experiments (Layers)
- Dynamic configuration

## Installation

In your pom.xml, add the SDK package as a dependency:

```
<dependency>
  <groupId>cloud.eppo</groupId>
  <artifactId>eppo-server-sdk</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Quick start

Begin by initializing a singleton instance of Eppo's client. Once initialized, the client can be used to make assignments anywhere in your app.

#### Initialize once

```java
EppoClientConfig config = EppoClientConfig.builder()
    .apiKey("SDK-KEY-FROM-DASHBOARD")
    .build();

EppoClient eppoClient = EppoClient.init(config);
```


#### Assign anywhere

```java
Optional<String> assignedVariation = eppoClient.getStringAssignment(
   'new-user-onboarding', 
   user.id, 
   user.attributes, 
   'control'
);
```

## Assignment functions

Every Eppo flag has a return type that is set once on creation in the dashboard. Once a flag is created, assignments in code should be made using the corresponding typed function: 

```java
getBooleanAssignment(...)
getNumericAssignment(...)
getIntegerAssignment(...)
getStringAssignment(...)
getJSONAssignment(...)
```

Each function has the same signature, but returns the type in the function name. For booleans use `getBooleanAssignment`, which has the following signature:

```java
public boolean getBooleanAssignment(
  String flagKey, 
  String subjectKey, 
  Map<String, Object> subjectAttributes, 
  String defaultValue
)
  ```

## Assignment logger 

To use the Eppo SDK for experiments that require analysis, pass in a callback logging function to the `init` function on SDK initialization. The SDK invokes the callback to capture assignment data whenever a variation is assigned. The assignment data is needed in the warehouse to perform analysis.

Here we define an implementation of the Eppo `IAssignmentLogger` interface containing a single function named `logAssignment`:

```java
import com.eppo.sdk.dto.IAssignmentLogger;
import com.eppo.sdk.dto.AssignmentLogData;

public class AssignmentLoggerImpl implements IAssignmentLogger {
  public void logAssignment(AssignmentLogData event) {
    ...
  }
}
```

## Philosophy

Eppo's SDKs are built for simplicity, speed and reliability. Flag configurations are compressed and distributed over a global CDN (Fastly), typically reaching your servers in under 15ms. Server SDKs continue polling Eppo’s API at 30-second intervals. Configurations are then cached locally, ensuring that each assignment is made instantly. Evaluation logic within each SDK consists of a few lines of simple numeric and string comparisons. The typed functions listed above are all developers need to understand, abstracting away the complexity of the Eppo's underlying (and expanding) feature set.

### Apple M-Series

Download a `arm64` compatible build: https://www.azul.com/downloads/?version=java-8-lts&architecture=arm-64-bit&package=jdk#zulu
