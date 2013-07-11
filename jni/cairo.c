#include "cairoint.h"
#include "cairo-private.h"
#include "tk_cairo.h"

#define _context_get() malloc (sizeof (cairo_t))
#define _context_put(cr) free (cr)

static const cairo_t _cairo_nil = {
  CAIRO_REFERENCE_COUNT_INVALID,	/* ref_count */
  CAIRO_STATUS_NO_MEMORY,	/* status */
  { 0, 0, 0, NULL },		/* user_data */
  NULL,				/* gstate */
  {{ 0 }, { 0 }},		/* gstate_tail */
  NULL,				/* gstate_freelist */
  {{				/* path */
    { 0, 0 },			/* last_move_point */
    { 0, 0 },			/* current point */
    FALSE,			/* has_current_point */
    FALSE,			/* has_last_move_point */
    FALSE,			/* has_curve_to */
    FALSE,			/* is_box */
    FALSE,			/* maybe_fill_region */
    TRUE,			/* is_empty_fill */
    { {0, 0}, {0, 0}},		/* extents */
    {{{NULL,NULL}}}		/* link */
  }}
};

static const cairo_t _cairo_nil__null_pointer = {
  CAIRO_REFERENCE_COUNT_INVALID,	/* ref_count */
  CAIRO_STATUS_NULL_POINTER,	/* status */
  { 0, 0, 0, NULL },		/* user_data */
  NULL,				/* gstate */
  {{ 0 }, { 0 }},		/* gstate_tail */
  NULL,				/* gstate_freelist */
  {{				/* path */
    { 0, 0 },			/* last_move_point */
    { 0, 0 },			/* current point */
    FALSE,			/* has_current_point */
    FALSE,			/* has_last_move_point */
    FALSE,			/* has_curve_to */
    FALSE,			/* is_box */
    FALSE,			/* maybe_fill_region */
    TRUE,			/* is_empty_fill */
    { {0, 0}, {0, 0}},		/* extents */
    {{{NULL,NULL}}}		/* link */
  }}
};

/**
 * _cairo_error:
 * @status: a status value indicating an error, (eg. not
 * %CAIRO_STATUS_SUCCESS)
 *
 * Checks that status is an error status, but does nothing else.
 *
 * All assignments of an error status to any user-visible object
 * within the cairo application should result in a call to
 * _cairo_error().
 *
 * The purpose of this function is to allow the user to set a
 * breakpoint in _cairo_error() to generate a stack trace for when the
 * user causes cairo to detect an error.
 *
 * Return value: the error status.
 **/
cairo_status_t
_cairo_error (cairo_status_t status)
{
//    CAIRO_ENSURE_UNIQUE;
//    assert (_cairo_status_is_error (status));

    return status;
}

/* XXX This should disappear in favour of a common pool of error objects. */
static cairo_t *_cairo_nil__objects[CAIRO_STATUS_LAST_STATUS + 1];
static cairo_t *
_cairo_create_in_error (cairo_status_t status)
{
    cairo_t *cr;

    assert (status != CAIRO_STATUS_SUCCESS);

    /* special case OOM in order to avoid another allocation */
    switch ((int) status) {
    case CAIRO_STATUS_NO_MEMORY:
	return (cairo_t *) &_cairo_nil;
    case CAIRO_STATUS_NULL_POINTER:
	return (cairo_t *) &_cairo_nil__null_pointer;
    }

//    CAIRO_MUTEX_LOCK (_cairo_error_mutex);
    cr = _cairo_nil__objects[status];
    if (cr == NULL) {
	cr = malloc (sizeof (cairo_t));
	if (unlikely (cr == NULL)) {
//	    CAIRO_MUTEX_UNLOCK (_cairo_error_mutex);
//	    _cairo_error_throw (CAIRO_STATUS_NO_MEMORY);
	    return (cairo_t *) &_cairo_nil;
	}

	*cr = _cairo_nil;
	cr->status = status;
	_cairo_nil__objects[status] = cr;
    }
 //   CAIRO_MUTEX_UNLOCK (_cairo_error_mutex);

    return cr;
}

/**
 * cairo_create:
 * @target: target surface for the context
 *
 * Creates a new #cairo_t with all graphics state parameters set to
 * default values and with @target as a target surface. The target
 * surface should be constructed with a backend-specific function such
 * as cairo_image_surface_create() (or any other
 * cairo_<emphasis>backend</emphasis>_surface_create() variant).
 *
 * This function references @target, so you can immediately
 * call cairo_surface_destroy() on it if you don't need to
 * maintain a separate reference to it.
 *
 * Return value: a newly allocated #cairo_t with a reference
 *  count of 1. The initial reference count should be released
 *  with cairo_destroy() when you are done using the #cairo_t.
 *  This function never returns %NULL. If memory cannot be
 *  allocated, a special #cairo_t object will be returned on
 *  which cairo_status() returns %CAIRO_STATUS_NO_MEMORY.
 *  You can use this object normally, but no drawing will
 *  be done.
 **/
cairo_t *
cairo_create (cairo_surface_t *target)
{
    cairo_t *cr;
    cairo_status_t status;

    if (unlikely (target == NULL))
	return _cairo_create_in_error (_cairo_error (CAIRO_STATUS_NULL_POINTER));
    if (unlikely (target->status))
	return _cairo_create_in_error (target->status);

    cr = _context_get ();
    if (unlikely (cr == NULL))
	return _cairo_create_in_error (_cairo_error (CAIRO_STATUS_NO_MEMORY));

    CAIRO_REFERENCE_COUNT_INIT (&cr->ref_count, 1);

    cr->status = CAIRO_STATUS_SUCCESS;

    _cairo_user_data_array_init (&cr->user_data);
    _cairo_path_fixed_init (cr->path);

    cr->gstate = &cr->gstate_tail[0];
    cr->gstate_freelist = &cr->gstate_tail[1];
    cr->gstate_tail[1].next = NULL;

    status = _cairo_gstate_init (cr->gstate, target);
    if (unlikely (status)) {
	_context_put (cr);
	cr = _cairo_create_in_error (status);
    }

    return cr;
}
//slim_hidden_def (cairo_create);

/**
 * cairo_new_path:
 * @cr: a cairo context
 *
 * Clears the current path. After this call there will be no path and
 * no current point.
 **/
void
cairo_new_path (cairo_t *cr)
{
    if (unlikely (cr->status))
	return;

    _cairo_path_fixed_fini (cr->path);
    _cairo_path_fixed_init (cr->path);
}
//slim_hidden_def(cairo_new_path);

/**
 * _cairo_set_error:
 * @cr: a cairo context
 * @status: a status value indicating an error
 *
 * Atomically sets cr->status to @status and calls _cairo_error;
 * Does nothing if status is %CAIRO_STATUS_SUCCESS.
 *
 * All assignments of an error status to cr->status should happen
 * through _cairo_set_error(). Note that due to the nature of the atomic
 * operation, it is not safe to call this function on the nil objects.
 *
 * The purpose of this function is to allow the user to set a
 * breakpoint in _cairo_error() to generate a stack trace for when the
 * user causes cairo to detect an error.
 **/
static void
_cairo_set_error (cairo_t *cr, cairo_status_t status)
{
    /* Don't overwrite an existing error. This preserves the first
     * error, which is the most significant. */
//    _cairo_status_set_error (&cr->status, _cairo_error (status));
}

/**
 * cairo_set_line_width:
 * @cr: a #cairo_t
 * @width: a line width
 *
 * Sets the current line width within the cairo context. The line
 * width value specifies the diameter of a pen that is circular in
 * user space, (though device-space pen may be an ellipse in general
 * due to scaling/shear/rotation of the CTM).
 *
 * Note: When the description above refers to user space and CTM it
 * refers to the user space and CTM in effect at the time of the
 * stroking operation, not the user space and CTM in effect at the
 * time of the call to cairo_set_line_width(). The simplest usage
 * makes both of these spaces identical. That is, if there is no
 * change to the CTM between a call to cairo_set_line_width() and the
 * stroking operation, then one can just pass user-space values to
 * cairo_set_line_width() and ignore this note.
 *
 * As with the other stroke parameters, the current line width is
 * examined by cairo_stroke(), cairo_stroke_extents(), and
 * cairo_stroke_to_path(), but does not have any effect during path
 * construction.
 *
 * The default line width value is 2.0.
 **/
void
cairo_set_line_width (cairo_t *cr, double width)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    if (width < 0.)
	width = 0.;

    status = _cairo_gstate_set_line_width (cr->gstate, width);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}
//slim_hidden_def (cairo_set_line_width);

/**
 * cairo_move_to:
 * @cr: a cairo context
 * @x: the X coordinate of the new position
 * @y: the Y coordinate of the new position
 *
 * Begin a new sub-path. After this call the current point will be (@x,
 * @y).
 **/
void
cairo_move_to (cairo_t *cr, double x, double y)
{
    cairo_status_t status;
    cairo_fixed_t x_fixed, y_fixed;

    if (unlikely (cr->status))
	return;

    _cairo_gstate_user_to_backend (cr->gstate, &x, &y);
    x_fixed = _cairo_fixed_from_double (x);
    y_fixed = _cairo_fixed_from_double (y);

    status = _cairo_path_fixed_move_to (cr->path, x_fixed, y_fixed);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}
//slim_hidden_def(cairo_move_to);

/**
 * cairo_line_to:
 * @cr: a cairo context
 * @x: the X coordinate of the end of the new line
 * @y: the Y coordinate of the end of the new line
 *
 * Adds a line to the path from the current point to position (@x, @y)
 * in user-space coordinates. After this call the current point
 * will be (@x, @y).
 *
 * If there is no current point before the call to cairo_line_to()
 * this function will behave as cairo_move_to(@cr, @x, @y).
 **/
void
cairo_line_to (cairo_t *cr, double x, double y)
{
    cairo_status_t status;
    cairo_fixed_t x_fixed, y_fixed;

    if (unlikely (cr->status))
	return;

    _cairo_gstate_user_to_backend (cr->gstate, &x, &y);
    x_fixed = _cairo_fixed_from_double (x);
    y_fixed = _cairo_fixed_from_double (y);

    status = _cairo_path_fixed_line_to (cr->path, x_fixed, y_fixed);
    if (unlikely (status))
	    _cairo_set_error (cr, status);
}
//slim_hidden_def (cairo_line_to);

/**
 * cairo_stroke:
 * @cr: a cairo context
 *
 * A drawing operator that strokes the current path according to the
 * current line width, line join, line cap, and dash settings. After
 * cairo_stroke(), the current path will be cleared from the cairo
 * context. See cairo_set_line_width(), cairo_set_line_join(),
 * cairo_set_line_cap(), cairo_set_dash(), and
 * cairo_stroke_preserve().
 *
 * Note: Degenerate segments and sub-paths are treated specially and
 * provide a useful result. These can result in two different
 * situations:
 *
 * 1. Zero-length "on" segments set in cairo_set_dash(). If the cap
 * style is %CAIRO_LINE_CAP_ROUND or %CAIRO_LINE_CAP_SQUARE then these
 * segments will be drawn as circular dots or squares respectively. In
 * the case of %CAIRO_LINE_CAP_SQUARE, the orientation of the squares
 * is determined by the direction of the underlying path.
 *
 * 2. A sub-path created by cairo_move_to() followed by either a
 * cairo_close_path() or one or more calls to cairo_line_to() to the
 * same coordinate as the cairo_move_to(). If the cap style is
 * %CAIRO_LINE_CAP_ROUND then these sub-paths will be drawn as circular
 * dots. Note that in the case of %CAIRO_LINE_CAP_SQUARE a degenerate
 * sub-path will not be drawn at all, (since the correct orientation
 * is indeterminate).
 *
 * In no case will a cap style of %CAIRO_LINE_CAP_BUTT cause anything
 * to be drawn in the case of either degenerate segments or sub-paths.
 **/
void
cairo_stroke (cairo_t *cr)
{
    cairo_stroke_preserve (cr);

    cairo_new_path (cr);
}
//slim_hidden_def(cairo_stroke);

/**
 * cairo_stroke_preserve:
 * @cr: a cairo context
 *
 * A drawing operator that strokes the current path according to the
 * current line width, line join, line cap, and dash settings. Unlike
 * cairo_stroke(), cairo_stroke_preserve() preserves the path within the
 * cairo context.
 *
 * See cairo_set_line_width(), cairo_set_line_join(),
 * cairo_set_line_cap(), cairo_set_dash(), and
 * cairo_stroke_preserve().
 **/
void
cairo_stroke_preserve (cairo_t *cr)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    status = _cairo_gstate_stroke (cr->gstate, cr->path);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}
//slim_hidden_def(cairo_stroke_preserve);

/**
 * cairo_destroy:
 * @cr: a #cairo_t
 *
 * Decreases the reference count on @cr by one. If the result
 * is zero, then @cr and all associated resources are freed.
 * See cairo_reference().
 **/
void
cairo_destroy (cairo_t *cr)
{
    if (cr == NULL || CAIRO_REFERENCE_COUNT_IS_INVALID (&cr->ref_count))
	return;

    assert (CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&cr->ref_count));

 //   if (! _cairo_reference_count_dec_and_test (&cr->ref_count))
//	return;

    while (cr->gstate != &cr->gstate_tail[0]) {
        if (_cairo_gstate_restore (&cr->gstate, &cr->gstate_freelist))
	        break;
        }

    _cairo_gstate_fini (cr->gstate);
    cr->gstate_freelist = cr->gstate_freelist->next; /* skip over tail[1] */
    while (cr->gstate_freelist != NULL) {
	cairo_gstate_t *gstate = cr->gstate_freelist;
	cr->gstate_freelist = gstate->next;
	free (gstate);
    }

    _cairo_path_fixed_fini (cr->path);

    _cairo_user_data_array_fini (&cr->user_data);

    /* mark the context as invalid to protect against misuse */
    cr->status = CAIRO_STATUS_NULL_POINTER;

    _context_put (cr);
}
//slim_hidden_def (cairo_destroy);

static cairo_bool_t
_current_source_matches_solid (cairo_t *cr,
			       double red,
			       double green,
			       double blue,
			       double alpha)
{
    const cairo_pattern_t *current;
    cairo_color_t color;

    current = cr->gstate->source;
    if (current->type != CAIRO_PATTERN_TYPE_SOLID)
	return FALSE;

    red   = _cairo_restrict_value (red,   0.0, 1.0);
    green = _cairo_restrict_value (green, 0.0, 1.0);
    blue  = _cairo_restrict_value (blue,  0.0, 1.0);
    alpha = _cairo_restrict_value (alpha, 0.0, 1.0);

    _cairo_color_init_rgba (&color, red, green, blue, alpha);
    return _cairo_color_equal (&color,
			       &((cairo_solid_pattern_t *) current)->color);
}

/**
 * cairo_set_source
 * @cr: a cairo context
 * @source: a #cairo_pattern_t to be used as the source for
 * subsequent drawing operations.
 *
 * Sets the source pattern within @cr to @source. This pattern
 * will then be used for any subsequent drawing operation until a new
 * source pattern is set.
 *
 * Note: The pattern's transformation matrix will be locked to the
 * user space in effect at the time of cairo_set_source(). This means
 * that further modifications of the current transformation matrix
 * will not affect the source pattern. See cairo_pattern_set_matrix().
 *
 * The default source pattern is a solid pattern that is opaque black,
 * (that is, it is equivalent to cairo_set_source_rgb(cr, 0.0, 0.0,
 * 0.0)).
 **/
void
cairo_set_source (cairo_t *cr, cairo_pattern_t *source)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    if (source == NULL) {
	_cairo_set_error (cr, CAIRO_STATUS_NULL_POINTER);
	return;
    }

    if (source->status) {
	_cairo_set_error (cr, source->status);
	return;
    }

    status = _cairo_gstate_set_source (cr->gstate, source);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}

/**
 * cairo_set_source_rgb
 * @cr: a cairo context
 * @red: red component of color
 * @green: green component of color
 * @blue: blue component of color
 *
 * Sets the source pattern within @cr to an opaque color. This opaque
 * color will then be used for any subsequent drawing operation until
 * a new source pattern is set.
 *
 * The color components are floating point numbers in the range 0 to
 * 1. If the values passed in are outside that range, they will be
 * clamped.
 *
 * The default source pattern is opaque black, (that is, it is
 * equivalent to cairo_set_source_rgb(cr, 0.0, 0.0, 0.0)).
 **/
void
cairo_set_source_rgb (cairo_t *cr, double red, double green, double blue)
{
    cairo_pattern_t *pattern;

    if (unlikely (cr->status))
	return;

    if (_current_source_matches_solid (cr, red, green, blue, 1.))
	return;

    /* push the current pattern to the freed lists */
    cairo_set_source (cr, (cairo_pattern_t *) &_cairo_pattern_black);

    pattern = cairo_pattern_create_rgb (red, green, blue);
    cairo_set_source (cr, pattern);
//    cairo_pattern_destroy (pattern);
}

/**
 * cairo_close_path:
 * @cr: a cairo context
 *
 * Adds a line segment to the path from the current point to the
 * beginning of the current sub-path, (the most recent point passed to
 * cairo_move_to()), and closes this sub-path. After this call the
 * current point will be at the joined endpoint of the sub-path.
 *
 * The behavior of cairo_close_path() is distinct from simply calling
 * cairo_line_to() with the equivalent coordinate in the case of
 * stroking. When a closed sub-path is stroked, there are no caps on
 * the ends of the sub-path. Instead, there is a line join connecting
 * the final and initial segments of the sub-path.
 *
 * If there is no current point before the call to cairo_close_path(),
 * this function will have no effect.
 *
 * Note: As of cairo version 1.2.4 any call to cairo_close_path() will
 * place an explicit MOVE_TO element into the path immediately after
 * the CLOSE_PATH element, (which can be seen in cairo_copy_path() for
 * example). This can simplify path processing in some cases as it may
 * not be necessary to save the "last move_to point" during processing
 * as the MOVE_TO immediately after the CLOSE_PATH will provide that
 * point.
 **/
void
cairo_close_path (cairo_t *cr)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    status = _cairo_path_fixed_close_path (cr->path);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}

/**
 * cairo_fill:
 * @cr: a cairo context
 *
 * A drawing operator that fills the current path according to the
 * current fill rule, (each sub-path is implicitly closed before being
 * filled). After cairo_fill(), the current path will be cleared from
 * the cairo context. See cairo_set_fill_rule() and
 * cairo_fill_preserve().
 **/
void
cairo_fill (cairo_t *cr)
{
    cairo_fill_preserve (cr);

    cairo_new_path (cr);
}

/**
 * cairo_fill_preserve:
 * @cr: a cairo context
 *
 * A drawing operator that fills the current path according to the
 * current fill rule, (each sub-path is implicitly closed before being
 * filled). Unlike cairo_fill(), cairo_fill_preserve() preserves the
 * path within the cairo context.
 *
 * See cairo_set_fill_rule() and cairo_fill().
 **/
void
cairo_fill_preserve (cairo_t *cr)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    status = _cairo_gstate_fill (cr->gstate, cr->path);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}
/**
 * cairo_set_line_join:
 * @cr: a cairo context
 * @line_join: a line join style
 *
 * Sets the current line join style within the cairo context. See
 * #cairo_line_join_t for details about how the available line join
 * styles are drawn.
 *
 * As with the other stroke parameters, the current line join style is
 * examined by cairo_stroke(), cairo_stroke_extents(), and
 * cairo_stroke_to_path(), but does not have any effect during path
 * construction.
 *
 * The default line join style is %CAIRO_LINE_JOIN_MITER.
 **/
void
cairo_set_line_join (cairo_t *cr, cairo_line_join_t line_join)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    status = _cairo_gstate_set_line_join (cr->gstate, line_join);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}



void
cairo_set_line_cap (cairo_t *cr, cairo_line_cap_t line_cap)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    status = _cairo_gstate_set_line_cap (cr->gstate, line_cap);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}



/**
 * cairo_set_fill_rule:
 * @cr: a #cairo_t
 * @fill_rule: a fill rule, specified as a #cairo_fill_rule_t
 *
 * Set the current fill rule within the cairo context. The fill rule
 * is used to determine which regions are inside or outside a complex
 * (potentially self-intersecting) path. The current fill rule affects
 * both cairo_fill() and cairo_clip(). See #cairo_fill_rule_t for details
 * on the semantics of each available fill rule.
 *
 * The default fill rule is %CAIRO_FILL_RULE_WINDING.
 **/
void
cairo_set_fill_rule (cairo_t *cr, cairo_fill_rule_t fill_rule)
{
    cairo_status_t status;

    if (unlikely (cr->status))
	return;

    status = _cairo_gstate_set_fill_rule (cr->gstate, fill_rule);
    if (unlikely (status))
	_cairo_set_error (cr, status);
}

