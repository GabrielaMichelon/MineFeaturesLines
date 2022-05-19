#include "macros.h"

#define A 1

#if MIN(2, 3) > 1
 int a;
#endif

#if ENABLED(MACROS_H)
    int thisisatest = 10;
#endif

#if ENABLED(A)
    #define C
    #if AA
        int aa = 20;
    #endif
#else
    int elseA = 0;
#endif

#if DISABLED(B)
    int b = 30;
#endif

#if ENABLED(C)
    int c = 40;
#endif

int base = 10;