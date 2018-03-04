/* Arduino Function Generator Firmware
 * Generates a configurable square-wave
 * for electronics testing.
*/

#include <Arduino.h>
#include "SerialServer.h"
#include "FunctionGenerator.h"

FunctionGenerator G_Generator;
SerialServer G_Server(&G_Generator);

void setup() {
    Serial.begin(9600);
}

void loop() {
    G_Server.serve();
}
