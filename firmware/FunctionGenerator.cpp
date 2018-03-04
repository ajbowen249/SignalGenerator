#include "FunctionGenerator.h"
#include "TimerOne.h"

#define CONFIG_UNO_BASIC 1
#include "config.h"

FunctionGenerator::FunctionGenerator() {
    pinMode(PIN, OUTPUT);
    _interval = DEFAULT_INTERVAL;
    Timer1.initialize(_interval);
    Timer1.attachInterrupt(toggle);
}

long FunctionGenerator::getInterval() {
    return _interval;
}

void FunctionGenerator::setInterval(long newInterval) {
    if(newInterval >= MIN_INTERVAL) {
        _interval = newInterval;
        Timer1.initialize(_interval);
    }
}

void toggle() {
    PORT ^= BIT_MASK;
}
