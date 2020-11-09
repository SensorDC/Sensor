&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;![figure](https://github.com/SensorDC/Sensor/blob/master/Sensor%20logo2.png)

# How to use Sensor
Sensor can take a Maven based project (it should contain the complete Maven built project directory and file pom.xml) as input for analysis. The expected running environment is 64-bit Window operating system with JDK 1.8. **As Maven built projects need to download dependencies from Maven Central Repository, Sensor cannot work offline.**

You can run Sensor on our experimental subjects based on the following steps:

**Step 1**: Unzip the plugin-decca.zip to local directory. Recommended directory structure is:

>> /plugin-sensor

>>     ├─sensor-1.0.jar

>>     ├─sensor-1.0.pom

>>     ├─soot-3.2.0.jar
>>
>>     ├─soot-3.2.0.pom

>>     ├─evosuite-client-sensor-1.0.6.jar
>>
>>     ├─evosuite-client-sensor-1.0.6.pom
>>
>>     ├─jd-core-1.0.9.jar
>>
>>     ├─jd-core-1.0.9.pom

*Note: To facilitate testing, please keep the unzip directory to be consistent with the above example. It should be noted that the location of data (e.g, /plugin-sensor) is not hardcoded, it can be replaced with user's actual unzip directory in the install commands.*

**Step 2**: Install Sensor

(a) Execute the following Linux shell command to install soot:

>> mvn install:install-file  -Dfile=/plugin-sensor/soot-3.2.0.jar  -Dpom=/plugin-sensor/soot-3.2.0.jar -DgroupId=org.sensor  -DartifactId=soot -Dversion=3.2.0 -Dpackaging=jar

(a) Execute the following Linux shell command to install jd-core:

>> mvn install:install-file  -Dfile=/plugin-sensor/jd-core-1.0.9.jar -Dpom=/plugin-sensor/jd-core-1.0.9.pom -DgroupId=org.sensor  -DartifactId=jd-core -Dversion=1.0.9 -Dpackaging=jar

(a) Execute the following Linux shell command to install evosuite-client:

>> mvn install:install-file  -Dfile=/plugin-sensor/evosuite-client-1.0.6.jar  -Dpom=/plugin-sensor/evosuite-client-1.0.6.jar -DgroupId=org.sensor  -DartifactId=evosuite-client -Dversion=1.0.6 -Dpackaging=jar

(b) Execute the following Linux shell command to install Sensor:

>> mvn install:install-file  -Dfile=/plugin-sensor/sensor-1.0.jar  -Dpom=/plugin-sensor/sensor-1.0.jar -DgroupId=org.sensor  -DartifactId=sensor -Dversion=1.0 -Dpackaging=maven-plugin

**Step 3**: Detect and assess the dependency conflict issues.

Execute the following Linux shell command to analyze the project:

>>cd **projectDir**
>>
>>mvn -f=pom.xml -DresultPath=/Report/ -Dmaven.test.skip=true org.sensor:sensor:1.0:semanticsConflict –e

Then you can get the dependency issue report in your specified directory (e.g., **/Report/**).

>>> **Command explanation:**
>>>
>>> >(1) -f=pom file : Specify the project under analysis;
>>> >
>>> >(2) -DresultPath=output issue report directory : Output the issue report to the specified folder(default:"./");
>>> >
>>> >(3) -DprintDiff=output semantics method pairs diff(default:"false");
>>> >
>>> >(4) -DrunTime=How many times does Evosuite run(default:1);
>>>
>>> **Goals:**
>>>
>>> >(1) semanticsConflict: semantics methods pairs from dependency conflict;
>>> >(2) SemanticsConflictSupImpl: semantics methods pairs from dependency conflict with super impl;
>>> >(3) classDupRisk: semantics methods pairs from class conflict;
