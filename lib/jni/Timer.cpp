#include <stdlib.h>
#include "Timer.h"
usecs_t systemTime(int clock)
{
    // we don't support the clocks here.
    struct timeval t;
    t.tv_sec = t.tv_usec = 0;
    gettimeofday(&t, NULL);
    return t.tv_sec*1000000LL + t.tv_usec;
}