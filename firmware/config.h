#ifndef __CONFIG_H__
#define __CONFIG_H__

#ifdef CONFIG_UNO_BASIC
// Function Generator stuff
#define GENERATOR_PIN      13
#define GENERATOR_BIT_MASK B00100000
#define GENERATOR_PORT     PORTB
#define DEFAULT_INTERVAL   150000
#define MIN_INTERVAL       6

// EEPROM Programmer stuff
#define EPORESET_PIN      6
#define EPOLATCH_MAIN_PIN 7
#define EPOLATCH_DATA_PIN 8
#define EPILATCH_PIN      9
#define EPOCLOCK_PIN      10
#define EPDATA_OUT_PIN    11
#define EPDATA_IN_PIN     12
#endif

#endif // __CONFIG_H__
