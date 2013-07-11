#include "cairoint.h"
#include "cairo-freed-pool-private.h"
#include "cairo-reference-count-private.h"

#if HAS_FREED_POOL
static freed_pool_t freed_pattern_pool[4];
#endif
static const cairo_solid_pattern_t _cairo_pattern_nil = {
    { CAIRO_PATTERN_TYPE_SOLID,		/* type */
      CAIRO_REFERENCE_COUNT_INVALID,	/* ref_count */
      CAIRO_STATUS_NO_MEMORY,		/* status */
      { 0, 0, 0, NULL },		/* user_data */
      { 1., 0., 0., 1., 0., 0., },	/* matrix */
      CAIRO_FILTER_DEFAULT,		/* filter */
      CAIRO_EXTEND_GRADIENT_DEFAULT },	/* extend */
};

const cairo_solid_pattern_t _cairo_pattern_clear = {
    { CAIRO_PATTERN_TYPE_SOLID,		/* type */
      CAIRO_REFERENCE_COUNT_INVALID,	/* ref_count */
      CAIRO_STATUS_SUCCESS,		/* status */
      { 0, 0, 0, NULL },		/* user_data */
      { 1., 0., 0., 1., 0., 0., },	/* matrix */
      CAIRO_FILTER_DEFAULT,		/* filter */
      CAIRO_EXTEND_GRADIENT_DEFAULT},	/* extend */
    { 0., 0., 0., 0., 0, 0, 0, 0 },/* color (double rgba, short rgba) */
};

void
_cairo_pattern_transform (cairo_pattern_t	*pattern,
			  const cairo_matrix_t  *ctm_inverse)
{
    if (pattern->status)
	return;

    cairo_matrix_multiply (&pattern->matrix, ctm_inverse, &pattern->matrix);
}

const cairo_solid_pattern_t _cairo_pattern_black = {
    { CAIRO_PATTERN_TYPE_SOLID,		/* type */
      CAIRO_REFERENCE_COUNT_INVALID,	/* ref_count */
      CAIRO_STATUS_SUCCESS,		/* status */
      { 0, 0, 0, NULL },		/* user_data */
      { 1., 0., 0., 1., 0., 0., },	/* matrix */
      CAIRO_FILTER_DEFAULT,		/* filter */
      CAIRO_EXTEND_GRADIENT_DEFAULT},	/* extend */
    { 0., 0., 0., 1., 0, 0, 0, 0xffff },/* color (double rgba, short rgba) */
};
static void
_cairo_pattern_init (cairo_pattern_t *pattern, cairo_pattern_type_t type)
{
#if HAVE_VALGRIND
    switch (type) {
    case CAIRO_PATTERN_TYPE_SOLID:
	VALGRIND_MAKE_MEM_UNDEFINED (pattern, sizeof (cairo_solid_pattern_t));
	break;
    case CAIRO_PATTERN_TYPE_SURFACE:
	VALGRIND_MAKE_MEM_UNDEFINED (pattern, sizeof (cairo_surface_pattern_t));
	break;
    case CAIRO_PATTERN_TYPE_LINEAR:
	VALGRIND_MAKE_MEM_UNDEFINED (pattern, sizeof (cairo_linear_pattern_t));
	break;
    case CAIRO_PATTERN_TYPE_RADIAL:
	VALGRIND_MAKE_MEM_UNDEFINED (pattern, sizeof (cairo_radial_pattern_t));
	break;
    }
#endif

    pattern->type      = type;
    pattern->status    = CAIRO_STATUS_SUCCESS;

    /* Set the reference count to zero for on-stack patterns.
     * Callers needs to explicitly increment the count for heap allocations. */
    CAIRO_REFERENCE_COUNT_INIT (&pattern->ref_count, 0);

    _cairo_user_data_array_init (&pattern->user_data);

    if (type == CAIRO_PATTERN_TYPE_SURFACE)
	pattern->extend = CAIRO_EXTEND_SURFACE_DEFAULT;
    else
	pattern->extend = CAIRO_EXTEND_GRADIENT_DEFAULT;

    pattern->filter    = CAIRO_FILTER_DEFAULT;

    pattern->has_component_alpha = FALSE;

    cairo_matrix_init_identity (&pattern->matrix);
}

void
_cairo_pattern_init_solid (cairo_solid_pattern_t *pattern,
			   const cairo_color_t	 *color)
{
    _cairo_pattern_init (&pattern->base, CAIRO_PATTERN_TYPE_SOLID);
    pattern->color = *color;
}
static cairo_bool_t
_surface_is_clear (const cairo_surface_pattern_t *pattern)
{
//    cairo_rectangle_int_t extents;
//
//    if (_cairo_surface_get_extents (pattern->surface, &extents) &&
//	(extents.width == 0 || extents.height == 0))
//	return TRUE;
//
//    return pattern->surface->is_clear &&
//	pattern->surface->content & CAIRO_CONTENT_ALPHA;
    return TRUE;
}

static cairo_bool_t
_gradient_is_clear (const cairo_gradient_pattern_t *gradient,
		    const cairo_rectangle_int_t *extents)
{
//    unsigned int i;
//
//    assert (gradient->base.type == CAIRO_PATTERN_TYPE_LINEAR ||
//	    gradient->base.type == CAIRO_PATTERN_TYPE_RADIAL);
//
//    if (gradient->n_stops == 0 ||
//	(gradient->base.extend == CAIRO_EXTEND_NONE &&
//	 gradient->stops[0].offset == gradient->stops[gradient->n_stops - 1].offset))
//	return TRUE;
//
//    /* Check if the extents intersect the drawn part of the pattern. */
//    if (gradient->base.type == CAIRO_PATTERN_TYPE_LINEAR) {
//	if (gradient->base.extend == CAIRO_EXTEND_NONE) {
//	    cairo_linear_pattern_t *linear = (cairo_linear_pattern_t *) gradient;
//	    /* EXTEND_NONE degenerate linear gradients are clear */
//	    if (_linear_pattern_is_degenerate (linear))
//		return TRUE;
//
//	    if (extents != NULL) {
//		double t[2];
//		_extents_to_linear_parameter (linear, extents, t);
//		if ((t[0] <= 0.0 && t[1] <= 0.0) || (t[0] >= 1.0 && t[1] >= 1.0))
//		    return TRUE;
//	    }
//	}
//    } else {
//	cairo_radial_pattern_t *radial = (cairo_radial_pattern_t *) gradient;
//	/* degenerate radial gradients are clear */
//	if (_radial_pattern_is_degenerate (radial))
//	    return TRUE;
//	/* TODO: check actual intersection */
//    }
//
//    for (i = 0; i < gradient->n_stops; i++)
//	if (! CAIRO_COLOR_IS_CLEAR (&gradient->stops[i].color))
//	    return FALSE;
//
    return TRUE;
}

cairo_bool_t
_cairo_pattern_is_clear (const cairo_pattern_t *abstract_pattern)
{
    const cairo_pattern_union_t *pattern;

    if (abstract_pattern->has_component_alpha)
	return FALSE;

    pattern = (cairo_pattern_union_t *) abstract_pattern;
    switch (pattern->type) {
    case CAIRO_PATTERN_TYPE_SOLID:
	return CAIRO_COLOR_IS_CLEAR (&pattern->solid.color);
    case CAIRO_PATTERN_TYPE_SURFACE:
	return _surface_is_clear (&pattern->surface);
    case CAIRO_PATTERN_TYPE_LINEAR:
    case CAIRO_PATTERN_TYPE_RADIAL:
	return _gradient_is_clear (&pattern->gradient.base, NULL);
    }

    ASSERT_NOT_REACHED;
    return FALSE;
}
/**
 * _cairo_gradient_pattern_is_solid
 *
 * Convenience function to determine whether a gradient pattern is
 * a solid color within the given extents. In this case the color
 * argument is initialized to the color the pattern represents.
 * This functions doesn't handle completely transparent gradients,
 * thus it should be called only after _cairo_pattern_is_clear has
 * returned FALSE.
 *
 * Return value: %TRUE if the pattern is a solid color.
 **/
cairo_bool_t
_cairo_gradient_pattern_is_solid (const cairo_gradient_pattern_t *gradient,
				  const cairo_rectangle_int_t *extents,
				  cairo_color_t *color)
{
//    unsigned int i;
//
//    assert (gradient->base.type == CAIRO_PATTERN_TYPE_LINEAR ||
//	    gradient->base.type == CAIRO_PATTERN_TYPE_RADIAL);
//
//    /* TODO: radial */
//    if (gradient->base.type == CAIRO_PATTERN_TYPE_LINEAR) {
//	cairo_linear_pattern_t *linear = (cairo_linear_pattern_t *) gradient;
//	if (_linear_pattern_is_degenerate (linear)) {
//	    _gradient_color_average (gradient, color);
//	    return TRUE;
//	}
//
//	if (gradient->base.extend == CAIRO_EXTEND_NONE) {
//	    double t[2];
//
//	    /* We already know that the pattern is not clear, thus if some
//	     * part of it is clear, the whole is not solid.
//	     */
//
//	    if (extents == NULL)
//		return FALSE;
//
//	    _extents_to_linear_parameter (linear, extents, t);
//	    if (t[0] < 0.0 || t[1] > 1.0)
//		return FALSE;
//	}
//    } else
//	return FALSE;
//
//    for (i = 1; i < gradient->n_stops; i++)
//	if (! _cairo_color_stop_equal (&gradient->stops[0].color,
//				       &gradient->stops[i].color))
//	    return FALSE;
//
//    _cairo_color_init_rgba (color,
//			    gradient->stops[0].color.red,
//			    gradient->stops[0].color.green,
//			    gradient->stops[0].color.blue,
//			    gradient->stops[0].color.alpha);
//
    return TRUE;
}
void
_cairo_pattern_init_static_copy (cairo_pattern_t	*pattern,
				 const cairo_pattern_t *other)
{
    int size;

    assert (other->status == CAIRO_STATUS_SUCCESS);

    switch (other->type) {
    default:
	ASSERT_NOT_REACHED;
    case CAIRO_PATTERN_TYPE_SOLID:
	size = sizeof (cairo_solid_pattern_t);
	break;
    case CAIRO_PATTERN_TYPE_SURFACE:
	size = sizeof (cairo_surface_pattern_t);
	break;
    case CAIRO_PATTERN_TYPE_LINEAR:
	size = sizeof (cairo_linear_pattern_t);
	break;
    case CAIRO_PATTERN_TYPE_RADIAL:
	size = sizeof (cairo_radial_pattern_t);
	break;
    }

    memcpy (pattern, other, size);

    CAIRO_REFERENCE_COUNT_INIT (&pattern->ref_count, 0);
    _cairo_user_data_array_init (&pattern->user_data);
}
/**
 * _cairo_pattern_is_opaque_solid
 *
 * Convenience function to determine whether a pattern is an opaque
 * (alpha==1.0) solid color pattern. This is done by testing whether
 * the pattern's alpha value when converted to a byte is 255, so if a
 * backend actually supported deep alpha channels this function might
 * not do the right thing.
 *
 * Return value: %TRUE if the pattern is an opaque, solid color.
 **/
cairo_bool_t
_cairo_pattern_is_opaque_solid (const cairo_pattern_t *pattern)
{
    cairo_solid_pattern_t *solid;

    if (pattern->type != CAIRO_PATTERN_TYPE_SOLID)
	return FALSE;

    solid = (cairo_solid_pattern_t *) pattern;

    return CAIRO_COLOR_IS_OPAQUE (&solid->color);
}
/**
 * _cairo_pattern_analyze_filter:
 * @pattern: surface pattern
 * @pad_out: location to store necessary padding in the source image, or %NULL
 * Returns: the optimized #cairo_filter_t to use with @pattern.
 *
 * Analyze the filter to determine how much extra needs to be sampled
 * from the source image to account for the filter radius and whether
 * we can optimize the filter to a simpler value.
 *
 * XXX: We don't actually have any way of querying the backend for
 *      the filter radius, so we just guess base on what we know that
 *      backends do currently (see bug #10508)
 */
cairo_filter_t
_cairo_pattern_analyze_filter (const cairo_pattern_t	*pattern,
			       double			*pad_out)
{
    double pad;
    cairo_filter_t optimized_filter;

    switch (pattern->filter) {
    case CAIRO_FILTER_GOOD:
    case CAIRO_FILTER_BEST:
    case CAIRO_FILTER_BILINEAR:
	/* If source pixels map 1:1 onto destination pixels, we do
	 * not need to filter (and do not want to filter, since it
	 * will cause blurriness)
	 */
	if (_cairo_matrix_is_pixel_exact (&pattern->matrix)) {
	    pad = 0.;
	    optimized_filter = CAIRO_FILTER_NEAREST;
	} else {
	    /* 0.5 is enough for a bilinear filter. It's possible we
	     * should defensively use more for CAIRO_FILTER_BEST, but
	     * without a single example, it's hard to know how much
	     * more would be defensive...
	     */
	    pad = 0.5;
	    optimized_filter = pattern->filter;
	}
	break;

    case CAIRO_FILTER_FAST:
    case CAIRO_FILTER_NEAREST:
    case CAIRO_FILTER_GAUSSIAN:
    default:
	pad = 0.;
	optimized_filter = pattern->filter;
	break;
    }

    if (pad_out)
	*pad_out = pad;

    return optimized_filter;
}

/**
 * _cairo_pattern_get_extents:
 *
 * Return the "target-space" extents of @pattern in @extents.
 *
 * For unbounded patterns, the @extents will be initialized with
 * "infinite" extents, (minimum and maximum fixed-point values).
 *
 * XXX: Currently, bounded gradient patterns will also return
 * "infinite" extents, though it would be possible to optimize these
 * with a little more work.
 **/
void
_cairo_pattern_get_extents (const cairo_pattern_t         *pattern,
			    cairo_rectangle_int_t         *extents)
{
    double x1, y1, x2, y2;
    cairo_status_t status;

    switch (pattern->type) {
    case CAIRO_PATTERN_TYPE_SOLID:
	goto UNBOUNDED;

    case CAIRO_PATTERN_TYPE_SURFACE:
	{
	    cairo_rectangle_int_t surface_extents;
	    const cairo_surface_pattern_t *surface_pattern =
		(const cairo_surface_pattern_t *) pattern;
	    cairo_surface_t *surface = surface_pattern->surface;
	    double pad;

	    if (! _cairo_surface_get_extents (surface, &surface_extents))
		goto UNBOUNDED;

	    if (surface_extents.width == 0 || surface_extents.height == 0)
		goto EMPTY;

	    if (pattern->extend != CAIRO_EXTEND_NONE)
		goto UNBOUNDED;

	    /* The filter can effectively enlarge the extents of the
	     * pattern, so extend as necessary.
	     */
	    _cairo_pattern_analyze_filter (&surface_pattern->base, &pad);
	    x1 = surface_extents.x - pad;
	    y1 = surface_extents.y - pad;
	    x2 = surface_extents.x + (int) surface_extents.width  + pad;
	    y2 = surface_extents.y + (int) surface_extents.height + pad;
	}
	break;

    case CAIRO_PATTERN_TYPE_RADIAL:
	{
	    const cairo_radial_pattern_t *radial =
		(const cairo_radial_pattern_t *) pattern;
	    double cx1, cy1;
	    double cx2, cy2;
	    double r, D;

	    if (radial->r1 == 0 && radial->r2 == 0)
		goto EMPTY;

	    cx1 = _cairo_fixed_to_double (radial->c1.x);
	    cy1 = _cairo_fixed_to_double (radial->c1.y);
	    r = _cairo_fixed_to_double (radial->r1);
	    x1 = cx1 - r; x2 = cx1 + r;
	    y1 = cy1 - r; y2 = cy1 + r;

	    cx2 = _cairo_fixed_to_double (radial->c2.x);
	    cy2 = _cairo_fixed_to_double (radial->c2.y);
	    r = fabs (_cairo_fixed_to_double (radial->r2));

	    if (pattern->extend != CAIRO_EXTEND_NONE)
		goto UNBOUNDED;

	    /* We need to be careful, as if the circles are not
	     * self-contained, then the solution is actually unbounded.
	     */
	    D = (cx1-cx2)*(cx1-cx2) + (cy1-cy2)*(cy1-cy2);
	    if (D > r*r - 1e-5)
		goto UNBOUNDED;

	    if (cx2 - r < x1)
		x1 = cx2 - r;
	    if (cx2 + r > x2)
		x2 = cx2 + r;

	    if (cy2 - r < y1)
		y1 = cy2 - r;
	    if (cy2 + r > y2)
		y2 = cy2 + r;
	}
	break;

    case CAIRO_PATTERN_TYPE_LINEAR:
	{
	    const cairo_linear_pattern_t *linear =
		(const cairo_linear_pattern_t *) pattern;

	    if (pattern->extend != CAIRO_EXTEND_NONE)
		goto UNBOUNDED;

	    if (linear->p1.x == linear->p2.x && linear->p1.y == linear->p2.y)
		goto EMPTY;

	    if (pattern->matrix.xy != 0. || pattern->matrix.yx != 0.)
		goto UNBOUNDED;

	    if (linear->p1.x == linear->p2.x) {
		x1 = -HUGE_VAL;
		x2 = HUGE_VAL;
		y1 = _cairo_fixed_to_double (MIN (linear->p1.y, linear->p2.y));
		y2 = _cairo_fixed_to_double (MAX (linear->p1.y, linear->p2.y));
	    } else if (linear->p1.y == linear->p2.y) {
		x1 = _cairo_fixed_to_double (MIN (linear->p1.x, linear->p2.x));
		x2 = _cairo_fixed_to_double (MAX (linear->p1.x, linear->p2.x));
		y1 = -HUGE_VAL;
		y2 = HUGE_VAL;
	    } else {
		goto  UNBOUNDED;
	    }
	}
	break;

    default:
	ASSERT_NOT_REACHED;
    }

    if (_cairo_matrix_is_translation (&pattern->matrix)) {
	x1 -= pattern->matrix.x0; x2 -= pattern->matrix.x0;
	y1 -= pattern->matrix.y0; y2 -= pattern->matrix.y0;
    } else {
	cairo_matrix_t imatrix;

	imatrix = pattern->matrix;
	status = cairo_matrix_invert (&imatrix);
	/* cairo_pattern_set_matrix ensures the matrix is invertible */
	assert (status == CAIRO_STATUS_SUCCESS);

	_cairo_matrix_transform_bounding_box (&imatrix,
					      &x1, &y1, &x2, &y2,
					      NULL);
    }

    x1 = floor (x1);
    if (x1 < CAIRO_RECT_INT_MIN)
	x1 = CAIRO_RECT_INT_MIN;
    y1 = floor (y1);
    if (y1 < CAIRO_RECT_INT_MIN)
	y1 = CAIRO_RECT_INT_MIN;

    x2 = ceil (x2);
    if (x2 > CAIRO_RECT_INT_MAX)
	x2 = CAIRO_RECT_INT_MAX;
    y2 = ceil (y2);
    if (y2 > CAIRO_RECT_INT_MAX)
	y2 = CAIRO_RECT_INT_MAX;

    extents->x = x1; extents->width  = x2 - x1;
    extents->y = y1; extents->height = y2 - y1;
    return;

  UNBOUNDED:
    /* unbounded patterns -> 'infinite' extents */
    _cairo_unbounded_rectangle_init (extents);
    return;

  EMPTY:
    extents->x = extents->y = 0;
    extents->width = extents->height = 0;
    return;
}

cairo_pattern_t *
_cairo_pattern_create_solid (const cairo_color_t *color)
{
    cairo_solid_pattern_t *pattern;

    pattern =
	_freed_pool_get (&freed_pattern_pool[CAIRO_PATTERN_TYPE_SOLID]);
    if (unlikely (pattern == NULL)) {
	/* None cached, need to create a new pattern. */
	pattern = malloc (sizeof (cairo_solid_pattern_t));
	if (unlikely (pattern == NULL)) {
//	    _cairo_error_throw (CAIRO_STATUS_NO_MEMORY);
	    return (cairo_pattern_t *) &_cairo_pattern_nil;
	}
    }

    _cairo_pattern_init_solid (pattern, color);
    CAIRO_REFERENCE_COUNT_INIT (&pattern->base.ref_count, 1);

    return &pattern->base;
}

/**
 * cairo_pattern_create_rgb:
 * @red: red component of the color
 * @green: green component of the color
 * @blue: blue component of the color
 *
 * Creates a new #cairo_pattern_t corresponding to an opaque color.  The
 * color components are floating point numbers in the range 0 to 1.
 * If the values passed in are outside that range, they will be
 * clamped.
 *
 * Return value: the newly created #cairo_pattern_t if successful, or
 * an error pattern in case of no memory.  The caller owns the
 * returned object and should call cairo_pattern_destroy() when
 * finished with it.
 *
 * This function will always return a valid pointer, but if an error
 * occurred the pattern status will be set to an error.  To inspect
 * the status of a pattern use cairo_pattern_status().
 **/
cairo_pattern_t *
cairo_pattern_create_rgb (double red, double green, double blue)
{
    cairo_color_t color;

    red   = _cairo_restrict_value (red,   0.0, 1.0);
    green = _cairo_restrict_value (green, 0.0, 1.0);
    blue  = _cairo_restrict_value (blue,  0.0, 1.0);

    _cairo_color_init_rgb (&color, red, green, blue);

//    CAIRO_MUTEX_INITIALIZE ();
//    HURTYOU

    return _cairo_pattern_create_solid (&color);
}

void
_cairo_pattern_fini (cairo_pattern_t *pattern)
{
    _cairo_user_data_array_fini (&pattern->user_data);

    switch (pattern->type) {
    case CAIRO_PATTERN_TYPE_SOLID:
	break;
    case CAIRO_PATTERN_TYPE_SURFACE: {
	cairo_surface_pattern_t *surface_pattern =
	    (cairo_surface_pattern_t *) pattern;

	cairo_surface_destroy (surface_pattern->surface);
    } break;
    case CAIRO_PATTERN_TYPE_LINEAR:
    case CAIRO_PATTERN_TYPE_RADIAL: {
	cairo_gradient_pattern_t *gradient =
	    (cairo_gradient_pattern_t *) pattern;

	if (gradient->stops && gradient->stops != gradient->stops_embedded)
	    free (gradient->stops);
    } break;
    }

//#if HAVE_VALGRIND
//    switch (pattern->type) {
//    case CAIRO_PATTERN_TYPE_SOLID:
//	VALGRIND_MAKE_MEM_NOACCESS (pattern, sizeof (cairo_solid_pattern_t));
//	break;
//    case CAIRO_PATTERN_TYPE_SURFACE:
//	VALGRIND_MAKE_MEM_NOACCESS (pattern, sizeof (cairo_surface_pattern_t));
//	break;
//    case CAIRO_PATTERN_TYPE_LINEAR:
//	VALGRIND_MAKE_MEM_NOACCESS (pattern, sizeof (cairo_linear_pattern_t));
//	break;
//    case CAIRO_PATTERN_TYPE_RADIAL:
//	VALGRIND_MAKE_MEM_NOACCESS (pattern, sizeof (cairo_radial_pattern_t));
//	break;
//    }
//#endif
}

/**
 * cairo_pattern_destroy:
 * @pattern: a #cairo_pattern_t
 *
 * Decreases the reference count on @pattern by one. If the result is
 * zero, then @pattern and all associated resources are freed.  See
 * cairo_pattern_reference().
 **/
void
cairo_pattern_destroy (cairo_pattern_t *pattern)
{
    cairo_pattern_type_t type;

    if (pattern == NULL ||
	    CAIRO_REFERENCE_COUNT_IS_INVALID (&pattern->ref_count))
	return;
//
//    assert (CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&pattern->ref_count));
//
//    if (! _cairo_reference_count_dec_and_test (&pattern->ref_count))
//	return;
//
    type = pattern->type;
    _cairo_pattern_fini (pattern);

    /* maintain a small cache of freed patterns */
    _freed_pool_put (&freed_pattern_pool[type], pattern);
}

