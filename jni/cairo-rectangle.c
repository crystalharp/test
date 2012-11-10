#include "cairoint.h"

void
_cairo_box_from_rectangle (cairo_box_t                 *box,
			   const cairo_rectangle_int_t *rect)
{
    box->p1.x = _cairo_fixed_from_int (rect->x);
    box->p1.y = _cairo_fixed_from_int (rect->y);
    box->p2.x = _cairo_fixed_from_int (rect->x + rect->width);
    box->p2.y = _cairo_fixed_from_int (rect->y + rect->height);
}

void
_cairo_boxes_get_extents (const cairo_box_t *boxes,
			  int num_boxes,
			  cairo_box_t *extents)
{
    int n;

    assert (num_boxes > 0);
    *extents = *boxes;

    for (n = 1; n < num_boxes; n++) {
	if (boxes[n].p1.x < extents->p1.x)
	    extents->p1.x = boxes[n].p1.x;
	if (boxes[n].p2.x > extents->p2.x)
	    extents->p2.x = boxes[n].p2.x;

	if (boxes[n].p1.y < extents->p1.y)
	    extents->p1.y = boxes[n].p1.y;
	if (boxes[n].p2.y > extents->p2.y)
	    extents->p2.y = boxes[n].p2.y;
    }
}

/* XXX We currently have a confusing mix of boxes and rectangles as
 * exemplified by this function.  A #cairo_box_t is a rectangular area
 * represented by the coordinates of the upper left and lower right
 * corners, expressed in fixed point numbers.  A #cairo_rectangle_int_t is
 * also a rectangular area, but represented by the upper left corner
 * and the width and the height, as integer numbers.
 *
 * This function converts a #cairo_box_t to a #cairo_rectangle_int_t by
 * increasing the area to the nearest integer coordinates.  We should
 * standardize on #cairo_rectangle_fixed_t and #cairo_rectangle_int_t, and
 * this function could be renamed to the more reasonable
 * _cairo_rectangle_fixed_round.
 */

void
_cairo_box_round_to_rectangle (const cairo_box_t     *box,
			       cairo_rectangle_int_t *rectangle)
{
    rectangle->x = _cairo_fixed_integer_floor (box->p1.x);
    rectangle->y = _cairo_fixed_integer_floor (box->p1.y);
    rectangle->width = _cairo_fixed_integer_ceil (box->p2.x) - rectangle->x;
    rectangle->height = _cairo_fixed_integer_ceil (box->p2.y) - rectangle->y;
}

cairo_bool_t
_cairo_rectangle_intersect (cairo_rectangle_int_t *dst,
			    const cairo_rectangle_int_t *src)
{
    int x1, y1, x2, y2;

    x1 = MAX (dst->x, src->x);
    y1 = MAX (dst->y, src->y);
    /* Beware the unsigned promotion, fortunately we have bits to spare
     * as (CAIRO_RECT_INT_MAX - CAIRO_RECT_INT_MIN) < UINT_MAX
     */
    x2 = MIN (dst->x + (int) dst->width,  src->x + (int) src->width);
    y2 = MIN (dst->y + (int) dst->height, src->y + (int) src->height);

    if (x1 >= x2 || y1 >= y2) {
	dst->x = 0;
	dst->y = 0;
	dst->width  = 0;
	dst->height = 0;

	return FALSE;
    } else {
	dst->x = x1;
	dst->y = y1;
	dst->width  = x2 - x1;
	dst->height = y2 - y1;

	return TRUE;
    }
}

