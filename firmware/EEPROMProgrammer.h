#ifndef __EEPROMPROGRAMMER_H_
#define __EEPROMPROGRAMMER_H_

// This should eventually be able to handle more than one type of EEPROM.
// Pinouts for known types are as follows:

/* Parallel28
            ┌───┐┌───┐
RDY/BUSY 1 ─┤        ├─ 28 VCC
     A12 2 ─┤        ├─ 27 /WE
      A7 3 ─┤        ├─ 26 NC
      A6 4 ─┤        ├─ 25 A8
      A5 5 ─┤        ├─ 24 A9
      A4 6 ─┤        ├─ 23 A11
      A3 7 ─┤        ├─ 22 /OE
      A2 8 ─┤        ├─ 21 A10
      A1 9 ─┤        ├─ 20 /CE
     A0 10 ─┤        ├─ 19 D7
     D0 11 ─┤        ├─ 18 D6
     D1 12 ─┤        ├─ 17 D5
     D2 13 ─┤        ├─ 16 D4
    GND 14 ─┤        ├─ 15 D3
            └────────┘
*/

enum EEPROMType {
    Parallel28 = 0,
};

class EEPROMProgrammer {
private:
    bool _burning;
    bool _dumping;

    EEPROMType _eepromType;
    unsigned int _romLength;
    unsigned int _address;
    unsigned long _baseBits;

public:
    EEPROMProgrammer();
    void initializeBurn(unsigned int romLength, EEPROMType eepromType);
    bool writeByte(unsigned char byte);

    //void initializeDump(unsigned int romCapacity, EEPROMType eepromType);
    //unsigned char readByte();
};

#endif // __EEPROMPROGRAMMER_H_
