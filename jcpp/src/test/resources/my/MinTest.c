#include <stdio.h>

#define C 1

#ifdef DO_SWAP
#define MIN(a, b) ((a) < (b) ? (a) : (b))
#define B 2
#else 
#define MIN(a, b) ((a) == (b) ? (a) : (b))
#define B 11
#define C 0
#endif

#define MAX(a, b) ((a) < (b) ? (b) : (a))

#ifdef DO_SWAP
#define SWAP(a, b) { a ^= b; b ^= a; a ^= b; }
#endif

#define ADD(a, b) (a + b)

#if MAX(2, 3) < 10 && \
	ADD(3, 4) < 10

#define A 2

#endif

#if MAX(A, B) < 10

int a;

#elif MAX(2, MAX(4, 3)) < 3
int b;
#else
#if C
int c;
#endif
#endif

#if MIN(A, B) < 3
int min;
#endif

#if NUMBER > 5 && MAX(NUMBER, 5) > 5

#endif

#if MAX(NUMBER, 5) > 5 && !(MAX(NUMBER, 5) > 5)

#endif

#ifndef DO_SWAP
#undef SWAP
#define X
#else
#define Y	
#endif

#if defined(UNDEFINED)
#endif

#ifdef SWAP
#endif

#ifndef SWAP
#endif

#if defined(SWAP)
#endif

#if !defined(SWAP)
#endif

#if defined(NUMBER)
#endif

#ifdef DO_SWAP
#undef NUMBER
#endif

#if defined(NUMBER)
#endif

#if defined(DO_SWAP) && defined(NUMBER)
#endif

int main(){
	
	int a1 = 1;
	int a2 = 4;
	
	printf("MIN: %d - ", MIN(a1, a2));
	
	printf("MAX: %d - ", MAX(a1, a2));
	
	#ifdef DO_SWAP
 
	int x = 10;
	int y = 5;
	int z = 4;
  
	// What happens now?
	if(x < 0)
		#if defined(SWAP) && defined(DO_SWAP)
	    SWAP(x, y);
		#endif
		
		#if !defined(SWAP) && defined(DO_SWAP)
		#endif
	else
	    SWAP(x, z); 
	
	#endif
	
	#ifndef DO_SWAP
	//no swaping
	#endif
	
	#if 'a'
	
	#endif
}

