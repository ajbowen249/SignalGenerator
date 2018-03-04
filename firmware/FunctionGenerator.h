#ifndef __FUNTIONGENERATOR_H__
#define __FUNTIONGENERATOR_H__

#include <Arduino.h>

void toggle();

class FunctionGenerator {
    private:
        long _interval;

    public:
        FunctionGenerator();
        long getInterval();
        void setInterval(long newInterval);
};

#endif // __FUNTIONGENERATOR_H__
