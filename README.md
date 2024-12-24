# Ember Trade Connector Samples

This project illustrates how to build, test, debug, and deploy trading connector for [Deltix Execution Server (Ember)](https://ember.deltixlab.com/docs/index/). \
Execution Server is a container of trading algorithms that interface with market using market data and trading connectors.

Each Trading Connector adapts API of specific execution venue to normalized Deltix data model. \
For more information see [Trade Connector Developer's Guide](https://ember.deltixlab.com/docs/tc/tc_dev_guide/) and [Trading Data Model](https://ember.deltixlab.com/docs/data_model/trading/).


## Prerequisite

You need local installation of [Deltix QuantServer](https://kb.timebase.info/docs/deployment/installer-ee). 

- Download and install [Eclipse Temurin JDK 17](https://adoptium.net/temurin/releases?version=17)
- Download and install [QuantServer 5.6 (TimeBase and Aggregator)](https://deltix-installers.s3.eu-west-3.amazonaws.com/5.6/deltix-windows-installer-online-5.6.23.jar)


## Development Environment

### How to create dev environment:

- Create (if not exist) `%USERPROFILE%\.gradle\gradle.properties` and define properties like below:
  ```properties
  # Deltix Nexus repository credentials
  NEXUS_USER=username
  NEXUS_PASS=password
  
  # TimeBase serial number
  serialNumber=XXXX-XXXXXXXXXX-XXXX
  
  # path to dev environment 
  devenvDir=D:/Projects/Deltix
  
  # path to TimeBase installation
  devenvDeltixHome=C:/Deltix/QuantServer
  
  # path to Java 17 JDK
  devenvJavaHome=C:/Program Files/Eclipse Temurin/jdk-17.0.12.2-hotspot
  ```
- Execute Gradle task to create dev env:
  ```shell
  gradlew clean build gatherJars buildDevEnv
  ```

### Configuration

- Ember configuration: see `<devenvDir>/ember-home/ember.conf` \
  See more about [Ember Configuration](https://ember.deltixlab.com/docs/config/config_reference/)
- Logging configuration: see `<devenvDir>/ember-home/gflog.xml` \
  See more about [Garbage Free Log](https://github.com/epam/gflog#garbage-free-log)

### Testing

- Navigate to `<devenvDir>`
- Open _QuantServer Architect_ via `qsadmin.bat`
- Launch _TimeBase_ (via context menu on TimeBase bar)
- Launch _Ember_ via `start-ember.bat`
- Launch _Ember Monitor_ via `start-monitor.bat`
- Open browser and follow link: `http://localhost:8988`


## Build

To build all samples and update connectors in dev env - execute the command below:
```shell
gradlew clean build updateConnectors
```


## Debug

One simple way to debug your connector is running entire [Execution Server under debugger](https://ember.deltixlab.com/docs/tc/tc_dev_guide/#appendix-a-debugging-trade-connector).

Create Run configuration inside IntelliJ/IDEA. It uses `deltix.ember.app.EmberApp` as a main class and `ember.home`
system property that point to ember configuration home. \
You can setup breakpoints in your connector and launch EmperApp under debugger.


## Deltix FIX 4.4 Trade Connector

Deltix FIX connector sample demonstrates how build trade connector for FIX protocol-based API. \
In particular this connector allows to connect to [Ember FIX Gateway](https://ember.deltixlab.com/docs/config/config_reference/?_highlight=fix&_highlight=gatew#fix-api-gateway).
See [Deltix FIX API](https://ember.deltixlab.com/docs/api/fix/fix_api_roe/).

### Sources

- Common code for FIX trade connector:  
`/fix/core/src/main/java/deltix/fix/*`
- Deltix FIX 4.4 trade connector:  
`/fix/generic/src/main/java/deltix/fix/deltix/*`

### Deploy

To deploy your connector to _actual_ server copy JAR files below under `lib/custom/` directory of your Ember installation.
- `/common/build/libs/common-2.0.0.jar`
- `/fix/core/build/libs/deltix-fix-core-2.0.0.jar`
- `/fix/deltix/build/libs/deltix-fix-deltix-2.0.0.jar`

The last step is to define your connector in server's `ember.conf` (see Dev Env).


## REST+WS API Gemini Trade Connector

Gemini connector sample demonstrates how build trade connector for REST/Websocket-based API. \
In particular this connector allows to connect to [Gemini Exchange](https://www.gemini.com/).

### Sources

- Gemini REST+WS trade connector:  
`/gemini/src/main/java/deltix/crypto/gemini/*`

### Deploy

To deploy your connector to actual server copy JAR files below under `lib/custom/` directory of your ES installation.
- `/common/build/libs/common-2.0.0.jar`
- `/gemini/build/libs/deltix-crypto-gemini-2.0.0.jar`

The last step is to define your connector in server's `ember.conf` (see Dev Env).  

## gRPC Syneroex Trade Connector

Syneroex connector sample demonstrates how build trade connector against gRPC. \

### Sources

- Common code for gRPC trade connector:  
  `/grpc/syneroex/src/main/java/deltix/fix/*`


The last step is to define your connector in server's `ember.conf` (see Dev Env).

----

Please let us know if you have any questions: support@deltixlab.com



