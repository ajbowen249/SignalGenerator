#include "FunctionGenerator.h"
#include "TimerOne.h"

#include "config.h"

FunctionGenerator::FunctionGenerator() :
    _interval(DEFAULT_INTERVAL) { }

void FunctionGenerator::start() {
    pinMode(GENERATOR_PIN, OUTPUT);
    Timer1.initialize(_interval);
    Timer1.attachInterrupt(toggle);
    Timer1.start();
}

void FunctionGenerator::stop() {
    Timer1.stop();
    Timer1.detachInterrupt();
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
    GENERATOR_PORT ^= GENERATOR_BIT_MASK;
}
