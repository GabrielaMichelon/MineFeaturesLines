#include "macros.h"

#if 2 > 1
 int a;
#endif

#if 1
    int thisisatest = 10;
#endif

#if SWITCH_ENABLED_A
    #define C
    #if AA
        int aa = 20;
    #endif
#else
    int elseA = 0;
#endif

#if !SWITCH_ENABLED_B
    int b = 30;
#endif

#if 1
    int c = 40;
#endif

int base = 10;