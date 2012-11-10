#ifndef CAIRO_FREED_POOL_H
#define CAIRO_FREED_POOL_H

#include "cairoint.h"

typedef int freed_pool_t;

#define _freed_pool_get(pool) NULL
#define _freed_pool_put(pool, ptr) free(ptr)
#define _freed_pool_reset(ptr)

#endif /* CAIRO_FREED_POOL_PRIVATE_H */
