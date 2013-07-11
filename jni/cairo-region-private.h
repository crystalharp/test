#include "cairo-types-private.h"
#include "pixman.h"

struct _cairo_region {
//    cairo_reference_count_t ref_count;
    cairo_status_t status;

    pixman_region32_t rgn;
};

