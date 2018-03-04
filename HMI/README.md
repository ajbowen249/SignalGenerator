# HMI

Java GUI for controlling the Signal Generator.

## Building
To stay independent of a build system, the whole app is a single file. Compile the Java file and `HMI` is the main class. It is dependent on [jSSC](https://code.google.com/archive/p/java-simple-serial-connector/), and should work if the jar is included in your system CLASSPATH. There're basic `build`, `clean`, and `run` scripts in this folder.
