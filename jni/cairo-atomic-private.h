#ifndef CAIRO_ATOMIC_PRIVATE_H
#define CAIRO_ATOMIC_PRIVATE_H

# include "cairo-compiler-private.h"

#define HAS_ATOMIC_OPS 1
typedef int cairo_atomic_int_t;
# define _cairo_atomic_int_inc(x) (x += 1)
# define _cairo_atomic_int_dec_and_test(x) (__sync_fetch_and_add(x, -1) == 1)
# define _cairo_atomic_int_cmpxchg(x, oldv, newv) __sync_bool_compare_and_swap (x, oldv, newv)
# define _cairo_atomic_int_get(x) (*x)
# define _cairo_atomic_ptr_get(x) (*x)
#define _cairo_atomic_uint_get(x) _cairo_atomic_int_get(x)
#define _cairo_atomic_uint_cmpxchg(x, oldv, newv) \
    _cairo_atomic_int_cmpxchg((cairo_atomic_int_t *)x, oldv, newv)

#endif

