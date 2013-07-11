#ifndef CAIRO_WIDEINT_TYPE_H
#define CAIRO_WIDEINT_TYPE_H

# include <stdint.h>

# ifndef HAVE_UINT64_T
#  define HAVE_UINT64_T 1
# endif
#ifndef INT16_MIN
# define INT16_MIN	(-32767-1)
#endif
#ifndef INT16_MAX
# define INT16_MAX	(32767)
#endif
#ifndef UINT16_MAX
# define UINT16_MAX	(65535)
#endif
#ifndef INT32_MIN
# define INT32_MIN	(-2147483647-1)
#endif
#ifndef INT32_MAX
# define INT32_MAX	(2147483647)
#endif

#if !HAVE_UINT64_T

typedef struct _cairo_uint64 {
    uint32_t	lo, hi;
} cairo_uint64_t, cairo_int64_t;

#else

typedef uint64_t    cairo_uint64_t;
typedef int64_t	    cairo_int64_t;

#endif

typedef struct _cairo_uquorem64 {
    cairo_uint64_t	quo;
    cairo_uint64_t	rem;
} cairo_uquorem64_t;

typedef struct _cairo_quorem64 {
    cairo_int64_t	quo;
    cairo_int64_t	rem;
} cairo_quorem64_t;

/* gcc has a non-standard name. */
#if HAVE___UINT128_T && !HAVE_UINT128_T
typedef __uint128_t uint128_t;
typedef __int128_t int128_t;
#define HAVE_UINT128_T 1
#endif

#if !HAVE_UINT128_T

typedef struct cairo_uint128 {
    cairo_uint64_t	lo, hi;
} cairo_uint128_t, cairo_int128_t;

#else

typedef uint128_t	cairo_uint128_t;
typedef int128_t	cairo_int128_t;

#endif

typedef struct _cairo_uquorem128 {
    cairo_uint128_t	quo;
    cairo_uint128_t	rem;
} cairo_uquorem128_t;

typedef struct _cairo_quorem128 {
    cairo_int128_t	quo;
    cairo_int128_t	rem;
} cairo_quorem128_t;


#endif /* CAIRO_WIDEINT_H */
