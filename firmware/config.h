#ifndef __CONFIG_H__
#define __CONFIG_H__

#define CONFIG_UNO_BASIC 1

#ifdef CONFIG_UNO_BASIC
// Function Generator stuff
#define GENERATOR_PIN      13
#define GENERATOR_BIT_MASK B00100000
#define GENERATOR_PORT     PORTB
#define DEFAULT_INTERVAL   150000
#define MIN_INTERVAL       6

// EEPROM Programmer stuff
#define EPO_DATA_ENABLE_PIN 6
#define EPOLATCH_PIN        7
#define EPILATCH_PIN        8
#define EPOCLOCK_PIN        9
#define EPICLOCK_PIN        10
#define EPDATA_OUT_PIN      11
#define EPDATA_IN_PIN       12
#endif

#endif // __CONFIG_H__
