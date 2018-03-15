#ifndef __SERIALSERVER_H__
#define __SERIALSERVER_H__

#include <Arduino.h>

#include "FunctionGenerator.h"
#include "EEPROMProgrammer.h"

enum ParseState {
    ReadQuery,
    ReadValue,
    ReadArgs
};

#define BUFFER_SIZE 128

class SerialServer {
    private:
        String _mode;
        FunctionGenerator* _functionGenerator;
        EEPROMProgrammer* _eepromProgrammer;

        int _inputPointer;
        char _inputBuffer[BUFFER_SIZE];

        ParseState _state;
        String _argumentName;
        String _argumentValueStr;

        void flushInputBuffer();
        void buildInputBuffer();

        void setArgumentName();
        void returnArgumentValue();
        void setArgumentValue();
        void callFunction();
        String bufferToString();

    public:
        SerialServer(FunctionGenerator* functionGenerator, EEPROMProgrammer* eepromProgrammer);
        void serve();
};

#endif // __SERIALSERVER_H__
