#include "cairoint.h"

#include "cairo-surface-fallback-private.h"

static unsigned int
_cairo_surface_allocate_unique_id (void)
{
    static cairo_atomic_int_t unique_id;

    if (++unique_id == 0)
	unique_id = 1;
    return unique_id;
}
/**
 * SECTION:cairo-surface
 * @Title: cairo_surface_t
 * @Short_Description: Base class for surfaces
 * @See_Also: #cairo_t, #cairo_pattern_t
 *
 * #cairo_surface_t is the abstract type representing all different drawing
 * targets that cairo can render to.  The actual drawings are
 * performed using a cairo <firstterm>context</firstterm>.
 *
 * A cairo surface is created by using <firstterm>backend</firstterm>-specific
 * constructors, typically of the form
 * cairo_<emphasis>backend</emphasis>_surface_create().
 *
 * Most surface types allow accessing the surface without using Cairo
 * functions. If you do this, keep in mind that it is mandatory that you call
 * cairo_surface_flush() before reading from or writing to the surface and that
 * you must use cairo_surface_mark_dirty() after modifying it.
 * <example>
 * <title>Directly modifying an image surface</title>
 * <programlisting>
 * void
 * modify_image_surface (cairo_surface_t *surface)
 * {
 *   unsigned char *data;
 *   int width, height, stride;
 *
 *   // flush to ensure all writing to the image was done
 *   cairo_surface_flush (surface);
 *
 *   // modify the image
 *   data = cairo_image_surface_get_data (surface);
 *   width = cairo_image_surface_get_width (surface);
 *   height = cairo_image_surface_get_height (surface);
 *   stride = cairo_image_surface_get_stride (surface);
 *   modify_image_data (data, width, height, stride);
 *
 *   // mark the image dirty so Cairo clears its caches.
 *   cairo_surface_mark_dirty (surface);
 * }
 * </programlisting>
 * </example>
 * Note that for other surface types it might be necessary to acquire the
 * surface's device first. See cairo_device_acquire() for a discussion of
 * devices.
 */

#define DEFINE_NIL_SURFACE(status, name)			\
const cairo_surface_t name = {					\
    NULL,				/* backend */		\
    NULL,				/* device */		\
    CAIRO_SURFACE_TYPE_IMAGE,		/* type */		\
    CAIRO_CONTENT_COLOR,		/* content */		\
    CAIRO_REFERENCE_COUNT_INVALID,	/* ref_count */		\
    status,				/* status */		\
    0,					/* unique id */		\
    FALSE,				/* finished */		\
    TRUE,				/* is_clear */		\
    FALSE,				/* has_font_options */	\
    FALSE,				/* owns_device */	\
    { 0, 0, 0, NULL, },			/* user_data */		\
    { 0, 0, 0, NULL, },			/* mime_data */         \
    { 1.0, 0.0, 0.0, 1.0, 0.0, 0.0 },   /* device_transform */	\
    { 1.0, 0.0,	0.0, 1.0, 0.0, 0.0 },	/* device_transform_inverse */	\
    { NULL, NULL },			/* device_transform_observers */ \
    0.0,				/* x_resolution */	\
    0.0,				/* y_resolution */	\
    0.0,				/* x_fallback_resolution */	\
    0.0,				/* y_fallback_resolution */	\
    NULL,				/* snapshot_of */	\
    NULL,				/* snapshot_detach */	\
    { NULL, NULL },			/* snapshots */		\
    { NULL, NULL },			/* snapshot */		\
    { CAIRO_ANTIALIAS_DEFAULT,		/* antialias */		\
      CAIRO_SUBPIXEL_ORDER_DEFAULT,	/* subpixel_order */	\
      CAIRO_LCD_FILTER_DEFAULT,		/* lcd_filter */	\
      CAIRO_HINT_STYLE_DEFAULT,		/* hint_style */	\
      CAIRO_HINT_METRICS_DEFAULT	/* hint_metrics */	\
    }					/* font_options */	\
}

/* XXX error object! */

static DEFINE_NIL_SURFACE(CAIRO_STATUS_NO_MEMORY, _cairo_surface_nil);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_SURFACE_TYPE_MISMATCH, _cairo_surface_nil_surface_type_mismatch);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_INVALID_STATUS, _cairo_surface_nil_invalid_status);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_INVALID_CONTENT, _cairo_surface_nil_invalid_content);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_INVALID_FORMAT, _cairo_surface_nil_invalid_format);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_INVALID_VISUAL, _cairo_surface_nil_invalid_visual);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_FILE_NOT_FOUND, _cairo_surface_nil_file_not_found);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_TEMP_FILE_ERROR, _cairo_surface_nil_temp_file_error);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_READ_ERROR, _cairo_surface_nil_read_error);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_WRITE_ERROR, _cairo_surface_nil_write_error);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_INVALID_STRIDE, _cairo_surface_nil_invalid_stride);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_INVALID_SIZE, _cairo_surface_nil_invalid_size);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_DEVICE_TYPE_MISMATCH, _cairo_surface_nil_device_type_mismatch);
static DEFINE_NIL_SURFACE(CAIRO_STATUS_DEVICE_ERROR, _cairo_surface_nil_device_error);

cairo_bool_t
_cairo_surface_has_device_transform (cairo_surface_t *surface)
{
    return ! _cairo_matrix_is_identity (&surface->device_transform);
}

/**
 * _cairo_surface_set_error:
 * @surface: a surface
 * @status: a status value indicating an error
 *
 * Atomically sets surface->status to @status and calls _cairo_error;
 * Does nothing if status is %CAIRO_STATUS_SUCCESS or any of the internal
 * status values.
 *
 * All assignments of an error status to surface->status should happen
 * through _cairo_surface_set_error(). Note that due to the nature of
 * the atomic operation, it is not safe to call this function on the
 * nil objects.
 *
 * The purpose of this function is to allow the user to set a
 * breakpoint in _cairo_error() to generate a stack trace for when the
 * user causes cairo to detect an error.
 *
 * Return value: the error status.
 **/
cairo_status_t
_cairo_surface_set_error (cairo_surface_t *surface,
			  cairo_status_t status)
{
    if (status == CAIRO_INT_STATUS_NOTHING_TO_DO)
	status = CAIRO_STATUS_SUCCESS;

    if (status == CAIRO_STATUS_SUCCESS || status >= CAIRO_INT_STATUS_UNSUPPORTED)
	return status;

    /* Don't overwrite an existing error. This preserves the first
     * error, which is the most significant. */
//    _cairo_status_set_error (&surface->status, status);

    return status;//_cairo_error (status);
}

static cairo_bool_t
_cairo_surface_has_mime_data (cairo_surface_t *surface)
{
    return surface->mime_data.num_elements != 0;
}

static void
_cairo_surface_detach_mime_data (cairo_surface_t *surface)
{
    if (! _cairo_surface_has_mime_data (surface))
	return;

    _cairo_user_data_array_fini (&surface->mime_data);
    _cairo_user_data_array_init (&surface->mime_data);
}

void
_cairo_surface_detach_snapshot (cairo_surface_t *snapshot)
{
    assert (snapshot->snapshot_of != NULL);

    snapshot->snapshot_of = NULL;
    cairo_list_del (&snapshot->snapshot);

    if (snapshot->snapshot_detach != NULL)
	snapshot->snapshot_detach (snapshot);

    cairo_surface_destroy (snapshot);
}
cairo_surface_t *
_cairo_surface_has_snapshot (cairo_surface_t *surface,
			     const cairo_surface_backend_t *backend)
{
    cairo_surface_t *snapshot;

    cairo_list_foreach_entry (snapshot, cairo_surface_t,
			      &surface->snapshots, snapshot)
    {
	/* XXX is_similar? */
	if (snapshot->backend == backend)
	    return snapshot;
    }

    return NULL;
}
static cairo_bool_t
_cairo_surface_has_snapshots (cairo_surface_t *surface)
{
    return ! cairo_list_is_empty (&surface->snapshots);
}

static void
_cairo_surface_detach_snapshots (cairo_surface_t *surface)
{
    while (_cairo_surface_has_snapshots (surface)) {
	_cairo_surface_detach_snapshot (cairo_list_first_entry (&surface->snapshots,
								cairo_surface_t,
								snapshot));
    }
}
/**
 * cairo_surface_reference:
 * @surface: a #cairo_surface_t
 *
 * Increases the reference count on @surface by one. This prevents
 * @surface from being destroyed until a matching call to
 * cairo_surface_destroy() is made.
 *
 * The number of references to a #cairo_surface_t can be get using
 * cairo_surface_get_reference_count().
 *
 * Return value: the referenced #cairo_surface_t.
 **/
cairo_surface_t *
cairo_surface_reference (cairo_surface_t *surface)
{
    if (surface == NULL ||
	    CAIRO_REFERENCE_COUNT_IS_INVALID (&surface->ref_count))
	return surface;

    assert (CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&surface->ref_count));

    surface->ref_count.ref_count++;

    return surface;
}
//slim_hidden_def (cairo_surface_reference);
/**
 * cairo_surface_finish:
 * @surface: the #cairo_surface_t to finish
 *
 * This function finishes the surface and drops all references to
 * external resources.  For example, for the Xlib backend it means
 * that cairo will no longer access the drawable, which can be freed.
 * After calling cairo_surface_finish() the only valid operations on a
 * surface are getting and setting user, referencing and
 * destroying, and flushing and finishing it.
 * Further drawing to the surface will not affect the
 * surface but will instead trigger a %CAIRO_STATUS_SURFACE_FINISHED
 * error.
 *
 * When the last call to cairo_surface_destroy() decreases the
 * reference count to zero, cairo will call cairo_surface_finish() if
 * it hasn't been called already, before freeing the resources
 * associated with the surface.
 **/
void
cairo_surface_finish (cairo_surface_t *surface)
{
    cairo_status_t status;

    if (surface == NULL)
	return;

    if (CAIRO_REFERENCE_COUNT_IS_INVALID (&surface->ref_count))
	return;

    if (surface->finished)
	return;

    /* update the snapshots *before* we declare the surface as finished */
    _cairo_surface_detach_snapshots (surface);
    if (surface->snapshot_of != NULL)
	_cairo_surface_detach_snapshot (surface);

    cairo_surface_flush (surface);
    surface->finished = TRUE;

    /* call finish even if in error mode */
    if (surface->backend->finish) {
	status = surface->backend->finish (surface);
	if (unlikely (status))
	    status = status;//_cairo_surface_set_error (surface, status);
    }
}

/**
 * cairo_surface_destroy:
 * @surface: a #cairo_surface_t
 *
 * Decreases the reference count on @surface by one. If the result is
 * zero, then @surface and all associated resources are freed.  See
 * cairo_surface_reference().
 **/
void
cairo_surface_destroy (cairo_surface_t *surface)
{
    if (surface == NULL ||
	    CAIRO_REFERENCE_COUNT_IS_INVALID (&surface->ref_count))
	return;

    assert (CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&surface->ref_count));
    surface->ref_count.ref_count--;
    if (surface->ref_count.ref_count != 0)
	return;

    assert (surface->snapshot_of == NULL);

    if (! surface->finished)
	cairo_surface_finish (surface);

    /* paranoid check that nobody took a reference whilst finishing */
//    assert (! CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&surface->ref_count));

    _cairo_user_data_array_fini (&surface->user_data);
    _cairo_user_data_array_fini (&surface->mime_data);

//    if (surface->owns_device)
//        cairo_device_destroy (surface->device);

    free (surface);
}
static cairo_status_t
_pattern_has_error (const cairo_pattern_t *pattern)
{
    const cairo_surface_pattern_t *spattern;

    if (unlikely (pattern->status))
	return pattern->status;

    if (pattern->type != CAIRO_PATTERN_TYPE_SURFACE)
	return CAIRO_STATUS_SUCCESS;

    spattern = (const cairo_surface_pattern_t *) pattern;
    if (unlikely (spattern->surface->status))
	return spattern->surface->status;

    if (unlikely (spattern->surface->finished))
	return CAIRO_STATUS_SURFACE_FINISHED;//_cairo_error (CAIRO_STATUS_SURFACE_FINISHED);

    return CAIRO_STATUS_SUCCESS;
}
static void
_cairo_surface_begin_modification (cairo_surface_t *surface)
{
    assert (surface->status == CAIRO_STATUS_SUCCESS);
    assert (! surface->finished);
    assert (surface->snapshot_of == NULL);

    _cairo_surface_detach_snapshots (surface);
    _cairo_surface_detach_mime_data (surface);
}

//slim_hidden_def(cairo_surface_destroy);
cairo_status_t
_cairo_surface_stroke (cairo_surface_t		*surface,
		       cairo_operator_t		 op,
		       const cairo_pattern_t	*source,
		       cairo_path_fixed_t	*path,
		       const cairo_stroke_style_t	*stroke_style,
		       const cairo_matrix_t		*ctm,
		       const cairo_matrix_t		*ctm_inverse,
		       double			 tolerance,
		       cairo_antialias_t	 antialias,
		       cairo_clip_t		*clip)
{
    cairo_status_t status;

    if (unlikely (surface->status))
	return surface->status;

//    if (clip && clip->all_clipped)
//	return CAIRO_STATUS_SUCCESS;
//
    if (op == CAIRO_OPERATOR_CLEAR && surface->is_clear)
	return CAIRO_STATUS_SUCCESS;

    if (op == CAIRO_OPERATOR_OVER &&
	_cairo_pattern_is_clear (source))
    {
	return CAIRO_STATUS_SUCCESS;
    }

    status = _pattern_has_error (source);
    if (unlikely (status))
	return status;

    _cairo_surface_begin_modification (surface);

    if (surface->backend->stroke != NULL) {
	status = surface->backend->stroke (surface, op, source,
					   path, stroke_style,
					   ctm, ctm_inverse,
					   tolerance, antialias,
					   clip);

	if (status != CAIRO_INT_STATUS_UNSUPPORTED)
            goto FINISH;
    }
    status = _cairo_surface_fallback_stroke (surface, op, source,
                                             path, stroke_style,
                                             ctm, ctm_inverse,
                                             tolerance, antialias,
					     clip);

 FINISH:
    surface->is_clear = FALSE;

    return _cairo_surface_set_error (surface, status);
}
/**
 * _cairo_surface_release_source_image:
 * @surface: a #cairo_surface_t
 * @image_extra: same as return from the matching _cairo_surface_acquire_source_image()
 *
 * Releases any resources obtained with _cairo_surface_acquire_source_image()
 **/
void
_cairo_surface_release_source_image (cairo_surface_t        *surface,
				     cairo_image_surface_t  *image,
				     void                   *image_extra)
{
//    assert (!surface->finished);

    if (surface->backend->release_source_image)
	surface->backend->release_source_image (surface, image, image_extra);
}

/**
 * _cairo_surface_acquire_source_image:
 * @surface: a #cairo_surface_t
 * @image_out: location to store a pointer to an image surface that
 *    has identical contents to @surface. This surface could be @surface
 *    itself, a surface held internal to @surface, or it could be a new
 *    surface with a copy of the relevant portion of @surface.
 * @image_extra: location to store image specific backend data
 *
 * Gets an image surface to use when drawing as a fallback when drawing with
 * @surface as a source. _cairo_surface_release_source_image() must be called
 * when finished.
 *
 * Return value: %CAIRO_STATUS_SUCCESS if an image was stored in @image_out.
 * %CAIRO_INT_STATUS_UNSUPPORTED if an image cannot be retrieved for the specified
 * surface. Or %CAIRO_STATUS_NO_MEMORY.
 **/
cairo_status_t
_cairo_surface_acquire_source_image (cairo_surface_t         *surface,
				     cairo_image_surface_t  **image_out,
				     void                   **image_extra)
{
    cairo_status_t status;

    if (surface->status)
	return surface->status;

    assert (!surface->finished);

    if (surface->backend->acquire_source_image == NULL)
	return CAIRO_INT_STATUS_UNSUPPORTED;

    status = surface->backend->acquire_source_image (surface,
						     image_out, image_extra);
    if (unlikely (status))
	return _cairo_surface_set_error (surface, status);

//    _cairo_debug_check_image_surface_is_defined (&(*image_out)->base);

    return CAIRO_STATUS_SUCCESS;
}

void
_cairo_surface_init (cairo_surface_t			*surface,
		     const cairo_surface_backend_t	*backend,
		     cairo_device_t			*device,
		     cairo_content_t			 content)
{
//    CAIRO_MUTEX_INITIALIZE ();

    surface->backend = backend;
//    surface->device = cairo_device_reference (device);
    surface->content = content;
    surface->type = backend->type;

    CAIRO_REFERENCE_COUNT_INIT (&surface->ref_count, 1);
    surface->status = CAIRO_STATUS_SUCCESS;
    surface->unique_id = _cairo_surface_allocate_unique_id ();
    surface->finished = FALSE;
    surface->is_clear = FALSE;
    surface->owns_device = (device != NULL);

    _cairo_user_data_array_init (&surface->user_data);
    _cairo_user_data_array_init (&surface->mime_data);

    cairo_matrix_init_identity (&surface->device_transform);
    cairo_matrix_init_identity (&surface->device_transform_inverse);
    cairo_list_init (&surface->device_transform_observers);

    surface->x_resolution = CAIRO_SURFACE_RESOLUTION_DEFAULT;
    surface->y_resolution = CAIRO_SURFACE_RESOLUTION_DEFAULT;

    surface->x_fallback_resolution = CAIRO_SURFACE_FALLBACK_RESOLUTION_DEFAULT;
    surface->y_fallback_resolution = CAIRO_SURFACE_FALLBACK_RESOLUTION_DEFAULT;

    cairo_list_init (&surface->snapshots);
    surface->snapshot_of = NULL;

    surface->has_font_options = FALSE;
}

cairo_surface_t *
_cairo_surface_create_in_error (cairo_status_t status)
{
    switch (status) {
    case CAIRO_STATUS_NO_MEMORY:
	return (cairo_surface_t *) &_cairo_surface_nil;
    case CAIRO_STATUS_SURFACE_TYPE_MISMATCH:
	return (cairo_surface_t *) &_cairo_surface_nil_surface_type_mismatch;
    case CAIRO_STATUS_INVALID_STATUS:
	return (cairo_surface_t *) &_cairo_surface_nil_invalid_status;
    case CAIRO_STATUS_INVALID_CONTENT:
	return (cairo_surface_t *) &_cairo_surface_nil_invalid_content;
    case CAIRO_STATUS_INVALID_FORMAT:
	return (cairo_surface_t *) &_cairo_surface_nil_invalid_format;
    case CAIRO_STATUS_INVALID_VISUAL:
	return (cairo_surface_t *) &_cairo_surface_nil_invalid_visual;
    case CAIRO_STATUS_READ_ERROR:
	return (cairo_surface_t *) &_cairo_surface_nil_read_error;
    case CAIRO_STATUS_WRITE_ERROR:
	return (cairo_surface_t *) &_cairo_surface_nil_write_error;
    case CAIRO_STATUS_FILE_NOT_FOUND:
	return (cairo_surface_t *) &_cairo_surface_nil_file_not_found;
    case CAIRO_STATUS_TEMP_FILE_ERROR:
	return (cairo_surface_t *) &_cairo_surface_nil_temp_file_error;
    case CAIRO_STATUS_INVALID_STRIDE:
	return (cairo_surface_t *) &_cairo_surface_nil_invalid_stride;
    case CAIRO_STATUS_INVALID_SIZE:
	return (cairo_surface_t *) &_cairo_surface_nil_invalid_size;
    case CAIRO_STATUS_DEVICE_TYPE_MISMATCH:
	return (cairo_surface_t *) &_cairo_surface_nil_device_type_mismatch;
    case CAIRO_STATUS_DEVICE_ERROR:
	return (cairo_surface_t *) &_cairo_surface_nil_device_error;
    case CAIRO_STATUS_SUCCESS:
    case CAIRO_STATUS_LAST_STATUS:
	ASSERT_NOT_REACHED;
	/* fall-through */
    case CAIRO_STATUS_INVALID_RESTORE:
    case CAIRO_STATUS_INVALID_POP_GROUP:
    case CAIRO_STATUS_NO_CURRENT_POINT:
    case CAIRO_STATUS_INVALID_MATRIX:
    case CAIRO_STATUS_NULL_POINTER:
    case CAIRO_STATUS_INVALID_STRING:
    case CAIRO_STATUS_INVALID_PATH_DATA:
    case CAIRO_STATUS_SURFACE_FINISHED:
    case CAIRO_STATUS_PATTERN_TYPE_MISMATCH:
    case CAIRO_STATUS_INVALID_DASH:
    case CAIRO_STATUS_INVALID_DSC_COMMENT:
    case CAIRO_STATUS_INVALID_INDEX:
    case CAIRO_STATUS_CLIP_NOT_REPRESENTABLE:
    case CAIRO_STATUS_FONT_TYPE_MISMATCH:
    case CAIRO_STATUS_USER_FONT_IMMUTABLE:
    case CAIRO_STATUS_USER_FONT_ERROR:
    case CAIRO_STATUS_NEGATIVE_COUNT:
    case CAIRO_STATUS_INVALID_CLUSTERS:
    case CAIRO_STATUS_INVALID_SLANT:
    case CAIRO_STATUS_INVALID_WEIGHT:
    case CAIRO_STATUS_USER_FONT_NOT_IMPLEMENTED:
    default:
	;//_cairo_error_throw (CAIRO_STATUS_NO_MEMORY);
	return (cairo_surface_t *) &_cairo_surface_nil;
    }
}
/**
 * cairo_surface_flush:
 * @surface: a #cairo_surface_t
 *
 * Do any pending drawing for the surface and also restore any
 * temporary modifications cairo has made to the surface's
 * state. This function must be called before switching from
 * drawing on the surface with cairo to drawing on it directly
 * with native APIs. If the surface doesn't support direct access,
 * then this function does nothing.
 **/
void
cairo_surface_flush (cairo_surface_t *surface)
{
    cairo_status_t status;

    if (surface->status)
	return;

    if (surface->finished)
	return;

    /* update the current snapshots *before* the user updates the surface */
    _cairo_surface_detach_snapshots (surface);

    if (surface->backend->flush) {
	status = surface->backend->flush (surface);
	if (unlikely (status))
	    status = _cairo_surface_set_error (surface, status);
    }
}
/**
 * _cairo_surface_get_extents:
 * @surface: the #cairo_surface_t to fetch extents for
 *
 * This function returns a bounding box for the surface.  The surface
 * bounds are defined as a region beyond which no rendering will
 * possibly be recorded, in other words, it is the maximum extent of
 * potentially usable coordinates.
 *
 * For vector surfaces, (PDF, PS, SVG and recording-surfaces), the surface
 * might be conceived as unbounded, but we force the user to provide a
 * maximum size at the time of surface_create. So get_extents uses
 * that size.
 *
 * Note: The coordinates returned are in "backend" space rather than
 * "surface" space. That is, they are relative to the true (0,0)
 * origin rather than the device_transform origin. This might seem a
 * bit inconsistent with other #cairo_surface_t interfaces, but all
 * current callers are within the surface layer where backend space is
 * desired.
 *
 * This behavior would have to be changed is we ever exported a public
 * variant of this function.
 */
cairo_bool_t
_cairo_surface_get_extents (cairo_surface_t         *surface,
			    cairo_rectangle_int_t   *extents)
{
    cairo_bool_t bounded;

    bounded = FALSE;
    if (surface->backend->get_extents != NULL)
	bounded = surface->backend->get_extents (surface, extents);

    if (! bounded)
	_cairo_unbounded_rectangle_init (extents);

    return bounded;
}
cairo_surface_t *
_cairo_surface_create_similar_scratch (cairo_surface_t *other,
				       cairo_content_t	content,
				       int		width,
				       int		height)
{
    cairo_surface_t *surface;

//    if (unlikely (other->status))
//	return _cairo_surface_create_in_error (other->status);
//
//    if (other->backend->create_similar == NULL)
//	return NULL;
//
//    surface = other->backend->create_similar (other,
//					      content, width, height);
//    if (surface == NULL || surface->status)
//	return surface;
//
//    _cairo_surface_copy_similar_properties (surface, other);
//
//    return surface;
//    HURTYOU
    printf("should not be here:_cairo_surface_create_similar_scratch\n");
	return _cairo_surface_create_in_error (CAIRO_STATUS_NO_MEMORY);
}
cairo_status_t
_cairo_surface_paint (cairo_surface_t	*surface,
		      cairo_operator_t	 op,
		      const cairo_pattern_t *source,
		      cairo_clip_t	    *clip)
{
//    cairo_status_t status;
//
//    if (unlikely (surface->status))
//	return surface->status;
//
//    if (clip && clip->all_clipped)
//	return CAIRO_STATUS_SUCCESS;
//
//    if (op == CAIRO_OPERATOR_CLEAR && surface->is_clear)
//	return CAIRO_STATUS_SUCCESS;
//
//    if (op == CAIRO_OPERATOR_OVER &&
//	_cairo_pattern_is_clear (source))
//    {
//	return CAIRO_STATUS_SUCCESS;
//    }
//
//    status = _pattern_has_error (source);
//    if (unlikely (status))
//	return status;
//
//    _cairo_surface_begin_modification (surface);
//
//    if (surface->backend->paint != NULL) {
//	status = surface->backend->paint (surface, op, source, clip);
//	if (status != CAIRO_INT_STATUS_UNSUPPORTED)
//            goto FINISH;
//    }
//
//    status = _cairo_surface_fallback_paint (surface, op, source, clip);
//
// FINISH:
//    surface->is_clear = op == CAIRO_OPERATOR_CLEAR && clip == NULL;
//
//    return _cairo_surface_set_error (surface, status);
//    HURTYOU
    printf("should not be here:_cairo_surface_paint\n");
    return CAIRO_STATUS_SUCCESS;
}

cairo_status_t
_cairo_surface_fill (cairo_surface_t	*surface,
		     cairo_operator_t	 op,
		     const cairo_pattern_t *source,
		     cairo_path_fixed_t	*path,
		     cairo_fill_rule_t	 fill_rule,
		     double		 tolerance,
		     cairo_antialias_t	 antialias,
		     cairo_clip_t	*clip)
{
    cairo_status_t status;

    if (unlikely (surface->status))
	return surface->status;

//    if (clip && clip->all_clipped)
//	return CAIRO_STATUS_SUCCESS;

    if (op == CAIRO_OPERATOR_CLEAR && surface->is_clear)
	return CAIRO_STATUS_SUCCESS;

    if (op == CAIRO_OPERATOR_OVER &&
	_cairo_pattern_is_clear (source))
    {
	return CAIRO_STATUS_SUCCESS;
    }

    status = _pattern_has_error (source);
    if (unlikely (status))
	return status;

    _cairo_surface_begin_modification (surface);

    if (surface->backend->fill != NULL) {
	status = surface->backend->fill (surface, op, source,
					 path, fill_rule,
					 tolerance, antialias,
					 clip);

	if (status != CAIRO_INT_STATUS_UNSUPPORTED)
            goto FINISH;
    }

    status = _cairo_surface_fallback_fill (surface, op, source,
                                           path, fill_rule,
                                           tolerance, antialias,
					   clip);

 FINISH:
    surface->is_clear = FALSE;

    return _cairo_surface_set_error (surface, status);
}

