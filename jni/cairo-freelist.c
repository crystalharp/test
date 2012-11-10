#include "cairoint.h"

#include "cairo-freelist-private.h"

void
_cairo_freepool_init (cairo_freepool_t *freepool, unsigned nodesize)
{
    freepool->first_free_node = NULL;
    freepool->pools = &freepool->embedded_pool;
    freepool->freepools = NULL;
    freepool->nodesize = nodesize;

    freepool->embedded_pool.next = NULL;
    freepool->embedded_pool.size = sizeof (freepool->embedded_data);
    freepool->embedded_pool.rem = sizeof (freepool->embedded_data);
    freepool->embedded_pool.data = freepool->embedded_data;

//    VG (VALGRIND_MAKE_MEM_NOACCESS (freepool->embedded_data, sizeof (freepool->embedded_data)));
}

void
_cairo_freepool_fini (cairo_freepool_t *freepool)
{
    cairo_freelist_pool_t *pool;

    pool = freepool->pools;
    while (pool != &freepool->embedded_pool) {
	cairo_freelist_pool_t *next = pool->next;
	free (pool);
	pool = next;
    }

    pool = freepool->freepools;
    while (pool != NULL) {
	cairo_freelist_pool_t *next = pool->next;
	free (pool);
	pool = next;
    }

//    VG (VALGRIND_MAKE_MEM_NOACCESS (freepool, sizeof (freepool)));
}
void *
_cairo_freepool_alloc_from_new_pool (cairo_freepool_t *freepool)
{
    cairo_freelist_pool_t *pool;
    int poolsize;

    if (freepool->freepools != NULL) {
	pool = freepool->freepools;
	freepool->freepools = pool->next;

	poolsize = pool->size;
    } else {
	if (freepool->pools != &freepool->embedded_pool)
	    poolsize = 2 * freepool->pools->size;
	else
	    poolsize = (128 * freepool->nodesize + 8191) & -8192;

	pool = malloc (sizeof (cairo_freelist_pool_t) + poolsize);
	if (unlikely (pool == NULL))
	    return pool;

	pool->size = poolsize;
    }

    pool->next = freepool->pools;
    freepool->pools = pool;

    pool->rem = poolsize - freepool->nodesize;
    pool->data = (uint8_t *) (pool + 1) + freepool->nodesize;

//    VG (VALGRIND_MAKE_MEM_NOACCESS (pool->data, pool->rem));

    return pool + 1;
}

