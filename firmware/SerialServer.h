#ifndef __SERIALSERVER_H__
#define __SERIALSERVER_H__

#include <Arduino.h>

#include "FunctionGenerator.h"

enum ParseState {
  ReadQuery,
  ReadValue
};

class SerialServer {
    private:
        FunctionGenerator* _functionGenerator;

        int _inputPointer;
        char _inputBuffer[128];

        ParseState _state;
        String _argumentName;
        String _argumentValueStr;

        void flushInputBuffer();
        void buildInputBuffer();

        void setArgumentName();
        void returnArgumentValue();
        void setArgumentValue();
        String bufferToString();

    public:
        SerialServer(FunctionGenerator* functionGenerator);
        void serve();
};

#endif // __SERIALSERVER_H__
