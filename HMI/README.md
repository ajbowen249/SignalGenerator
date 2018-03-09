# HMI

Java GUI for controlling the Signal Generator.

## Building
To stay independent of a build system, the whole app is a single file. Compile the Java file and `HMI` is the main class. It should also work fine with [VSCode's Java run feature](https://code.visualstudio.com/docs/java/java-debugging).

## Supported Platforms
Windows currently has full support. Linux supports devices on /dev/ttyUSB* (only tested on Ubuntu).
