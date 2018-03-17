#include "EEPROMProgrammer.h"
#include "config.h"

EEPROMProgrammer::EEPROMProgrammer() {
    _burning = false;
    _dumping = false;
    setupPins();
}

void EEPROMProgrammer::setupPins() {
    pinMode(EPO_DATA_ENABLE_PIN, OUTPUT);
    pinMode(EPOLATCH_PIN, OUTPUT);
    pinMode(EPILATCH_PIN, OUTPUT);
    pinMode(EPOCLOCK_PIN, OUTPUT);
    pinMode(EPICLOCK_PIN, OUTPUT);
    pinMode(EPDATA_OUT_PIN, OUTPUT);
    pinMode(EPDATA_IN_PIN, INPUT);

    digitalWrite(EPO_DATA_ENABLE_PIN, HIGH);
    digitalWrite(EPOLATCH_PIN, HIGH);
    digitalWrite(EPILATCH_PIN, HIGH);
    digitalWrite(EPOCLOCK_PIN, LOW);
    digitalWrite(EPICLOCK_PIN, LOW);
    digitalWrite(EPDATA_OUT_PIN, LOW);
}

void EEPROMProgrammer::imbueAddress(unsigned long& outputBits) {
    switch(_eepromType) {
    case Parallel28:
        outputBits |= ((_address << 20) & 0x0FF00000); // A0-7
        outputBits |= ((_address >> 3) & 0x00000060); // A9 and A8
        outputBits |= ((_address >> 1) & 0x00000200); // A10
        // IMPROVE: Everything above this point is the same for all parallel types.
        outputBits |= ((_address >> 4) & 0x00000080); // A11
        outputBits |= ((_address << 16) & 0x10000000); // A12
        outputBits |= ((_address >> 9) & 0x00000010); // A13
        outputBits |= ((_address << 15) & 0x20000000); // A14
        break;
    default:
        break;
    }
}

void EEPROMProgrammer::imbueData(unsigned long& outputBits, unsigned char data) {
    // swap data
    data = ((data >> 1) & 0x55) | ((data << 1) & 0xAA);
    data = ((data >> 2) & 0x33)| ((data << 2) & 0xCC);
    data = (data >> 4) | (data << 4);
    unsigned long lData = (unsigned long)data;

    if(_isParallel) {
        outputBits |= ((lData << 12) & 0x000E0000); // D0-2
        outputBits |= ((lData << 11) & 0x0000F800); // D3-7
    }
}

void EEPROMProgrammer::setOutputs(unsigned long state) {
    // The incoming state assumes bits and pins map 1:1.
    // The actual shift registers are rearranged to allow the
    // data outputs to free up the bus, so we need to shift a
    // few bits around before we send it off.

    unsigned long data012 = state & 0x000E0000;
    unsigned long data3to7 = state & 0x0000F800;
    unsigned long o14 = state & 0x00010000;
    unsigned long o15to7 = state & 0x00000700;

    state = state & 0xFFF000FF;
    state |= (o14 << 3);
    state |= (o15to7 << 8);

    state |= (data012 >> 4);
    state |= (data3to7 >> 3);

    digitalWrite(EPOLATCH_PIN, LOW);
    shiftOut(EPDATA_OUT_PIN, EPOCLOCK_PIN, LSBFIRST, state);
    shiftOut(EPDATA_OUT_PIN, EPOCLOCK_PIN, LSBFIRST, state >> 8);
    shiftOut(EPDATA_OUT_PIN, EPOCLOCK_PIN, LSBFIRST, state >> 16);
    shiftOut(EPDATA_OUT_PIN, EPOCLOCK_PIN, LSBFIRST, state >> 24);
    digitalWrite(EPOLATCH_PIN, HIGH);
}

void EEPROMProgrammer::toggleWrite(unsigned long state) {
    switch(_eepromType) {
    case Parallel28: {
        unsigned long stateWithWriteEnable = state & 0xFFFFFFF7;
        setOutputs(stateWithWriteEnable);
        setOutputs(state);
        break;
    }
    default:
        break;
    }
}

void EEPROMProgrammer::initializeBurn(unsigned long romLength, EEPROMType eepromType) {
    _romLength = romLength;
    _address = 0;
    _eepromType = eepromType;

    // _baseBits represents the base state of IO for each pin of the device.
    // See pinout comments in header for explanation.
    switch(_eepromType) {
    case Parallel28:
        _baseBits = 0x0000010C;
        _isParallel = true;
        break;
    default:
        break;
    }

    setOutputs(_baseBits);
    digitalWrite(EPO_DATA_ENABLE_PIN, LOW);

    _burning = true;
}

bool EEPROMProgrammer::writeByte(unsigned char data) {
    if(_burning) {
        if(_isParallel) {
            unsigned long state = _baseBits;
            imbueAddress(state);
            imbueData(state, data);
            setOutputs(state);
            toggleWrite(state);

            if(++_address >= _romLength) {
                digitalWrite(EPO_DATA_ENABLE_PIN, HIGH);
                _burning = false;
            }
        }
    }

    return _burning;
}
