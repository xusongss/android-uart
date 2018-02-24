//
// Timer functions.
//
#ifndef _LIBS_UTILS_TIMERS_H
#define _LIBS_UTILS_TIMERS_H

#include <stdint.h>
#include <sys/types.h>
#include <sys/time.h>

// ------------------------------------------------------------------
// C API

#ifdef __cplusplus
extern "C" {
#endif

typedef long long  usecs_t;       // Microseconds

// return the system-time according to the specified clock
#ifdef __cplusplus
usecs_t systemTime(int clock = 0);
#else
usecs_t systemTime(int clock);
#endif // def __cplusplus

#ifdef __cplusplus
}
#endif
#endif