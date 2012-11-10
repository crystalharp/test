#include "cairo-region-private.h"
#include "cairoint.h"

/* XXX need to update pixman headers to be const as appropriate */
#define CONST_CAST (pixman_region32_t *)

/**
 * cairo_region_num_rectangles:
 * @region: a #cairo_region_t
 *
 * Returns the number of rectangles contained in @region.
 *
 * Return value: The number of rectangles contained in @region.
 *
 * Since: 1.10
 **/
int
cairo_region_num_rectangles (const cairo_region_t *region)
{
    if (region->status)
	return 0;

    return pixman_region32_n_rects (CONST_CAST &region->rgn);
}
//slim_hidden_def (cairo_region_num_rectangles);
/**
 * cairo_region_get_rectangle:
 * @region: a #cairo_region_t
 * @nth: a number indicating which rectangle should be returned
 * @rectangle: return location for a #cairo_rectangle_int_t
 *
 * Stores the @nth rectangle from the region in @rectangle.
 *
 * Since: 1.10
 **/
void
cairo_region_get_rectangle (const cairo_region_t *region,
			    int nth,
			    cairo_rectangle_int_t *rectangle)
{
    pixman_box32_t *pbox;

    if (region->status) {
	rectangle->x = rectangle->y = 0;
	rectangle->width = rectangle->height = 0;
	return;
    }

    pbox = pixman_region32_rectangles (CONST_CAST &region->rgn, NULL) + nth;

    rectangle->x = pbox->x1;
    rectangle->y = pbox->y1;
    rectangle->width = pbox->x2 - pbox->x1;
    rectangle->height = pbox->y2 - pbox->y1;
}
//slim_hidden_def (cairo_region_get_rectangle);

