
#ifndef CAIRO_MUTEX_IMPL_PRIVATE_H
#define CAIRO_MUTEX_IMPL_PRIVATE_H

/* A fully qualified no-operation statement */
#define CAIRO_MUTEX_IMPL_NOOP	do {/*no-op*/} while (0)
/* And one that evaluates its argument once */
#define CAIRO_MUTEX_IMPL_NOOP1(expr)        do { (void)(expr); } while (0)
# define CAIRO_MUTEX_IMPL_LOCK(mutex) CAIRO_MUTEX_IMPL_NOOP1(mutex)
# define CAIRO_MUTEX_IMPL_UNLOCK(mutex) CAIRO_MUTEX_IMPL_NOOP1(mutex)

#endif
