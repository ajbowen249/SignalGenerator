/* Arduino Function Generator Firmware
 * Generates a configurable square-wave
 * for electronics testing.
*/

#include <Arduino.h>
#include "SerialServer.h"

FunctionGenerator G_Generator;
EEPROMProgrammer G_Programmer;
SerialServer G_Server(&G_Generator, &G_Programmer);

void setup() {
    Serial.begin(9600);
    G_Generator.start();
}

void loop() {
    G_Server.serve();
}
