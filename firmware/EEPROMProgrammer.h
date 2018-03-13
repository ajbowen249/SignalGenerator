#ifndef __EEPROMPROGRAMMER_H_
#define __EEPROMPROGRAMMER_H_

#include <Arduino.h>

/*
This should eventually be able to handle more than one type of EEPROM.
Pinouts for known types are as follows:

Parallel24
       ──────||──────
       ──────||──────
       ──────||──────
       ──────||──────
         ┌───┐┌───┐
   A7 1 ─┤        ├─ 24 VCC
   A6 2 ─┤        ├─ 23 A8
   A5 3 ─┤        ├─ 22 A9
   A4 4 ─┤        ├─ 21 /WE
   A3 5 ─┤        ├─ 20 /OE
   A2 6 ─┤        ├─ 19 A10
   A1 7 ─┤        ├─ 18 /CE
  A0  8 ─┤        ├─ 17 D7
  D0  9 ─┤        ├─ 16 D6
  D1 10 ─┤        ├─ 15 D5
  D2 11 ─┤        ├─ 14 D4
 GND 12 ─┤        ├─ 13 D3
         └────────┘

Parallel28
       ──────||──────
       ──────||──────
         ┌───┐┌───┐
  A14 1 ─┤        ├─ 28 VCC
  A12 2 ─┤        ├─ 27 /WE
   A7 3 ─┤        ├─ 26 A13
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

Parallel32
         ┌───┐┌───┐
   NC 1 ─┤        ├─ 32 VCC
  A16 2 ─┤        ├─ 31 /WE
  A15 3 ─┤        ├─ 30 A17
  A12 4 ─┤        ├─ 29 A14
   A7 5 ─┤        ├─ 28 A13
   A6 6 ─┤        ├─ 27 A8
   A5 7 ─┤        ├─ 26 A9
   A4 8 ─┤        ├─ 25 A11
   A3 9 ─┤        ├─ 24 /OE
  A2 10 ─┤        ├─ 23 A10
  A1 11 ─┤        ├─ 22 /CE
  A0 12 ─┤        ├─ 21 D7
  D0 13 ─┤        ├─ 20 D6
  D1 14 ─┤        ├─ 19 D5
  D2 15 ─┤        ├─ 18 D4
 GND 16 ─┤        ├─ 17 D3
         └────────┘

The shift registers are mapped based on 32 pins of output and 8 pins of input.
Their connections are as follows, where Ox[i] is an output bit i on shift
register x, and I[i] is an input on the input shift register. The outputs are
not contiguous to allow for Output register 2 to give up the bus for the input
register to take over reading.

ZIF Socket
                 ┌───┐┌───┐
        O0[0] 1 ─┤        ├─ 32 O3[7]
        O0[1] 2 ─┤        ├─ 31 O3[6]
        O0[2] 3 ─┤        ├─ 30 O3[5]
        O0[3] 4 ─┤        ├─ 29 O3[4]
        O0[4] 5 ─┤        ├─ 28 O3[3]
        O0[5] 6 ─┤        ├─ 27 O3[2]
        O0[6] 7 ─┤        ├─ 26 O3[1]
        O0[7] 8 ─┤        ├─ 25 O3[0]
        O1[0] 9 ─┤        ├─ 24 O1[7]
       O1[1] 10 ─┤        ├─ 23 O1[6]
       O1[2] 11 ─┤        ├─ 22 O1[5]
       O1[3] 12 ─┤        ├─ 21 O2[7], I[7]
 I[0], O2[0] 13 ─┤        ├─ 20 O2[6], I[6]
 I[1], O2[1] 14 ─┤        ├─ 19 O2[5], I[5]
 I[2], O2[2] 15 ─┤        ├─ 18 O2[4], I[4]
       O1[4] 16 ─┤        ├─ 17 O2[3], I[3]
                 └────────┘
*/

enum EEPROMType {
    Parallel24 = 0,
    Parallel28 = 1,
    Parallel32 = 2,
};

class EEPROMProgrammer {
private:
    bool _burning;
    bool _dumping;

    EEPROMType _eepromType;
    bool _isParallel;
    unsigned long _romLength;
    unsigned long _address;
    unsigned long _baseBits;

    void setupPins();
    void imbueAddress(unsigned long& outputBits);
    void imbueData(unsigned long& outputBits, unsigned char data);
    void setOutputs(unsigned long state);
    void toggleWrite(unsigned long state);

public:
    EEPROMProgrammer();
    void initializeBurn(unsigned long romLength, EEPROMType eepromType);
    bool writeByte(unsigned char data);

    //void initializeDump(unsigned int romCapacity, EEPROMType eepromType);
    //unsigned char readByte();
};

#endif // __EEPROMPROGRAMMER_H_
