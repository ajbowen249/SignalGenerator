#include "EEPROMProgrammer.h"

EEPROMProgrammer::EEPROMProgrammer() {
    _burning = false;
    _dumping = false;
}

void EEPROMProgrammer::initializeBurn(unsigned int romLength, EEPROMType eepromType) {
    _romLength = romLength;
    _address = 0;
    _eepromType = eepromType;

    // _baseBits represents the base state of IO for each pin of the device.
    // See pinout comments in header for explanation.
    switch(_eepromType) {
    case Parallel28:
        _baseBits = 0x0000010C;
        break;
    default:
        break;
    }

    _burning = true;
}

bool EEPROMProgrammer::writeByte(unsigned char byte) {

    return !_burning;
}
