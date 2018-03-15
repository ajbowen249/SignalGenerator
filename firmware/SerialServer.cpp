#include "SerialServer.h"

SerialServer::SerialServer(FunctionGenerator* functionGenerator) {
    _functionGenerator = functionGenerator;
    flushInputBuffer();
    _state = ReadQuery;
}

void SerialServer::serve() {
    buildInputBuffer();
    if(_inputBuffer[_inputPointer - 1] == '#') {
        flushInputBuffer();
        return;
    }

    switch(_state) {
        case ReadQuery: {
            if(_inputBuffer[_inputPointer - 1] == '?') {
                setArgumentName();
                returnArgumentValue();
            } else if(_inputBuffer[_inputPointer - 1] == '=') {
                setArgumentName();
                _state = ReadValue;
            } else if(_inputBuffer[_inputPointer - 1] == '(') {
                setArgumentName();
                _state = ReadArgs;
            }

            break;
        }
        case ReadValue: {
            if(_inputBuffer[_inputPointer - 1] == ';') {
                setArgumentValue();
                _state = ReadQuery;
            }

            break;
        }
        case ReadArgs: {
            if(_inputBuffer[_inputPointer - 1] == ')') {
                setArgumentValue();
                _state = ReadQuery;
            }

            break;
        }
    }
}

void SerialServer::setArgumentName() {
    _argumentName = bufferToString();
}

void SerialServer::returnArgumentValue() {
    if(_argumentName.equalsIgnoreCase("interval")) {
        Serial.println(_functionGenerator->getInterval());
    } else {
        Serial.println();
    }
}

void SerialServer::setArgumentValue() {
    String val = bufferToString();
    long longValue = atol(val.c_str());
    if(_argumentName.equalsIgnoreCase("interval")) {
        _functionGenerator->setInterval(longValue);
    } else {
        Serial.println();
    }
}

void callFunction() {
    String rawArgs = bufferToString();
    char args[BUFFER_SIZE];
    rawArgs.toCharArray(args, BUFFER_SIZE);
    char* arg;

    if(_argumentName.equalsIgnoreCase("initializeBurn")) {
        _functionGenerator.stop();

        arg = strtok(args, " ,");
        if(!arg) {
            Serial.println();
            return;
        }

        unsigned long romLength = atoul(arg);

        arg = strtok(args, " ,");
        if(!arg) {
            Serial.println();
            return;
        }

        int romType = atoi(arg);
        _eepromProgrammer.initializeBurn(romLength, (EEPROMType)romType);
    } else if(_argumentName.equalsIgnoreCase("w")) {
        bool stillBurning = _eepromProgrammer.writeByte((unsigned char)args[0]));
        if(!stillBurning) {
            _functionGenerator.start();
        }

        return;
    }

    Serial.println();
}

void SerialServer::buildInputBuffer() {
    while(Serial.available()) {
        _inputBuffer[_inputPointer++] = Serial.read();
    }
}

void SerialServer::flushInputBuffer() {
    for(int i = 0; i < BUFFER_SIZE; i++) {
        _inputBuffer[i] = 0x00;
    }

    _inputPointer = 0;
}

String SerialServer::bufferToString() {
    _inputBuffer[_inputPointer - 1] = 0;
    char subStr[_inputPointer];
    memcpy(subStr, _inputBuffer, _inputPointer);
    flushInputBuffer();

    return String(subStr);
}
