#include "cairoint.h"

/* private functions */

void
_cairo_traps_init (cairo_traps_t *traps)
{
//    VG (VALGRIND_MAKE_MEM_UNDEFINED (traps, sizeof (cairo_traps_t)));

    traps->status = CAIRO_STATUS_SUCCESS;

    traps->maybe_region = 1;
    traps->is_rectilinear = 0;
    traps->is_rectangular = 0;

    traps->num_traps = 0;

    traps->traps_size = ARRAY_LENGTH (traps->traps_embedded);
    traps->traps = traps->traps_embedded;

    traps->num_limits = 0;
    traps->has_intersections = FALSE;
}

void
_cairo_traps_limit (cairo_traps_t	*traps,
		    const cairo_box_t	*limits,
		    int			 num_limits)
{
    traps->limits = limits;
    traps->num_limits = num_limits;
}

void
_cairo_traps_fini (cairo_traps_t *traps)
{
    if (traps->traps != traps->traps_embedded)
	free (traps->traps);

//    VG (VALGRIND_MAKE_MEM_NOACCESS (traps, sizeof (cairo_traps_t)));
}

cairo_status_t
_cairo_traps_tessellate_rectangle (cairo_traps_t *traps,
				   const cairo_point_t *top_left,
				   const cairo_point_t *bottom_right)
{
    cairo_line_t left;
    cairo_line_t right;
    cairo_fixed_t top, bottom;

    if (top_left->y == bottom_right->y)
	return CAIRO_STATUS_SUCCESS;

    if (top_left->x == bottom_right->x)
	return CAIRO_STATUS_SUCCESS;

     left.p1.x =  left.p2.x = top_left->x;
     left.p1.y = right.p1.y = top_left->y;
    right.p1.x = right.p2.x = bottom_right->x;
     left.p2.y = right.p2.y = bottom_right->y;

     top = top_left->y;
     bottom = bottom_right->y;

    if (traps->num_limits) {
	cairo_bool_t reversed;
	int n;

	/* support counter-clockwise winding for rectangular tessellation */
	reversed = top_left->x > bottom_right->x;
	if (reversed) {
	    right.p1.x = right.p2.x = top_left->x;
	    left.p1.x = left.p2.x = bottom_right->x;
	}

	for (n = 0; n < traps->num_limits; n++) {
	    const cairo_box_t *limits = &traps->limits[n];
	    cairo_line_t _left, _right;
	    cairo_fixed_t _top, _bottom;

	    if (top >= limits->p2.y)
		continue;
	    if (bottom <= limits->p1.y)
		continue;

	    /* Trivially reject if trapezoid is entirely to the right or
	     * to the left of the limits. */
	    if (left.p1.x >= limits->p2.x)
		continue;
	    if (right.p1.x <= limits->p1.x)
		continue;

	    /* Otherwise, clip the trapezoid to the limits. */
	    _top = top;
	    if (_top < limits->p1.y)
		_top = limits->p1.y;

	    _bottom = bottom;
	    if (_bottom > limits->p2.y)
		_bottom = limits->p2.y;

	    if (_bottom <= _top)
		continue;

	    _left = left;
	    if (_left.p1.x < limits->p1.x) {
		_left.p1.x = limits->p1.x;
		_left.p1.y = limits->p1.y;
		_left.p2.x = limits->p1.x;
		_left.p2.y = limits->p2.y;
	    }

	    _right = right;
	    if (_right.p1.x > limits->p2.x) {
		_right.p1.x = limits->p2.x;
		_right.p1.y = limits->p1.y;
		_right.p2.x = limits->p2.x;
		_right.p2.y = limits->p2.y;
	    }

	    if (left.p1.x >= right.p1.x)
		continue;

	    if (reversed)
		_cairo_traps_add_trap (traps, _top, _bottom, &_right, &_left);
	    else
		_cairo_traps_add_trap (traps, _top, _bottom, &_left, &_right);
	}
    } else {
	_cairo_traps_add_trap (traps, top, bottom, &left, &right);
    }

    return traps->status;
}
/* make room for at least one more trap */
static cairo_bool_t
_cairo_traps_grow (cairo_traps_t *traps)
{
    cairo_trapezoid_t *new_traps;
    int new_size = 4 * traps->traps_size;

    if (CAIRO_INJECT_FAULT ()) {
	traps->status = CAIRO_STATUS_NO_MEMORY;//_cairo_error (CAIRO_STATUS_NO_MEMORY);
	return FALSE;
    }

    if (traps->traps == traps->traps_embedded) {
	new_traps = _cairo_malloc_ab (new_size, sizeof (cairo_trapezoid_t));
	if (new_traps != NULL)
	    memcpy (new_traps, traps->traps, sizeof (traps->traps_embedded));
    } else {
	new_traps = _cairo_realloc_ab (traps->traps,
	                               new_size, sizeof (cairo_trapezoid_t));
    }

    if (unlikely (new_traps == NULL)) {
	traps->status = CAIRO_STATUS_NO_MEMORY;//_cairo_error (CAIRO_STATUS_NO_MEMORY);
	return FALSE;
    }

    traps->traps = new_traps;
    traps->traps_size = new_size;
    return TRUE;
}

void
_cairo_traps_add_trap (cairo_traps_t *traps,
		       cairo_fixed_t top, cairo_fixed_t bottom,
		       cairo_line_t *left, cairo_line_t *right)
{
    cairo_trapezoid_t *trap;

    if (unlikely (traps->num_traps == traps->traps_size)) {
	if (unlikely (! _cairo_traps_grow (traps)))
	    return;
    }

    trap = &traps->traps[traps->num_traps++];
    trap->top = top;
    trap->bottom = bottom;
    trap->left = *left;
    trap->right = *right;
}

