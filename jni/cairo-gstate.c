#include "cairo-gstate-private.h"
#include "cairoint.h"

static void
_cairo_gstate_copy_pattern (cairo_pattern_t *pattern,
			    const cairo_pattern_t *original)
{
    /* First check if the we can replace the original with a much simpler
     * pattern. For example, gradients that are uniform or just have a single
     * stop can sometimes be replaced with a solid.
     */

    if (_cairo_pattern_is_clear (original)) {
        _cairo_pattern_init_solid ((cairo_solid_pattern_t *) pattern,
				   CAIRO_COLOR_TRANSPARENT);
	return;
    }

    if (original->type == CAIRO_PATTERN_TYPE_LINEAR ||
	original->type == CAIRO_PATTERN_TYPE_RADIAL)
    {
        cairo_color_t color;
	if (_cairo_gradient_pattern_is_solid ((cairo_gradient_pattern_t *) original,
					      NULL,
					      &color))
	{
	    _cairo_pattern_init_solid ((cairo_solid_pattern_t *) pattern,
				       &color);
	    return;
	}
    }

    _cairo_pattern_init_static_copy (pattern, original);
}

static void
_cairo_gstate_copy_transformed_pattern (cairo_gstate_t  *gstate,
					cairo_pattern_t *pattern,
					const cairo_pattern_t *original,
					const cairo_matrix_t  *ctm_inverse)
{
    _cairo_gstate_copy_pattern (pattern, original);

    /* apply device_transform first so that it is transformed by ctm_inverse */
    if (original->type == CAIRO_PATTERN_TYPE_SURFACE) {
//	cairo_surface_pattern_t *surface_pattern;
//	cairo_surface_t *surface;
//
//        surface_pattern = (cairo_surface_pattern_t *) original;
//        surface = surface_pattern->surface;
//
//	if (_cairo_surface_has_device_transform (surface))
//	    _cairo_pattern_transform (pattern, &surface->device_transform);
        ;
    }

    if (! _cairo_matrix_is_identity (ctm_inverse))
	_cairo_pattern_transform (pattern, ctm_inverse);

    if (_cairo_surface_has_device_transform (gstate->target)) {
        _cairo_pattern_transform (pattern,
                                  &gstate->target->device_transform_inverse);
    }
}

static void
_cairo_gstate_copy_transformed_source (cairo_gstate_t   *gstate,
				       cairo_pattern_t  *pattern)
{
    _cairo_gstate_copy_transformed_pattern (gstate, pattern,
					    gstate->source,
					    &gstate->source_ctm_inverse);
}

cairo_status_t
_cairo_gstate_init (cairo_gstate_t  *gstate,
		    cairo_surface_t *target)
{
    cairo_status_t status;

//    VG (VALGRIND_MAKE_MEM_UNDEFINED (gstate, sizeof (cairo_gstate_t)));

    gstate->next = NULL;

    gstate->op = CAIRO_GSTATE_OPERATOR_DEFAULT;

    gstate->tolerance = CAIRO_GSTATE_TOLERANCE_DEFAULT;
    gstate->antialias = CAIRO_ANTIALIAS_DEFAULT;

    _cairo_stroke_style_init (&gstate->stroke_style);

    gstate->fill_rule = CAIRO_GSTATE_FILL_RULE_DEFAULT;

    gstate->font_face = NULL;
//    gstate->scaled_font = NULL;
//    gstate->previous_scaled_font = NULL;
//
//    cairo_matrix_init_scale (&gstate->font_matrix,
//			     CAIRO_GSTATE_DEFAULT_FONT_SIZE,
//			     CAIRO_GSTATE_DEFAULT_FONT_SIZE);
//
//    _cairo_font_options_init_default (&gstate->font_options);
//
    _cairo_clip_init (&gstate->clip);

    gstate->target = cairo_surface_reference (target);
    gstate->parent_target = NULL;
    gstate->original_target = cairo_surface_reference (target);

//    gstate->device_transform_observer.callback = _cairo_gstate_update_device_transform;
    cairo_list_add (&gstate->device_transform_observer.link,
		    &gstate->target->device_transform_observers);

//    gstate->is_identity = _cairo_matrix_is_identity (&gstate->target->device_transform);
    cairo_matrix_init_identity (&gstate->ctm);
    gstate->ctm_inverse = gstate->ctm;
    gstate->source_ctm_inverse = gstate->ctm;

    gstate->source = (cairo_pattern_t *) &_cairo_pattern_black.base;

    /* Now that the gstate is fully initialized and ready for the eventual
     * _cairo_gstate_fini(), we can check for errors (and not worry about
     * the resource deallocation). */
    status = target->status;
    if (unlikely (status))
	return status;

    status = gstate->source->status;
    if (unlikely (status))
	return status;

    return CAIRO_STATUS_SUCCESS;
}

void
_cairo_gstate_fini (cairo_gstate_t *gstate)
{
    _cairo_stroke_style_fini (&gstate->stroke_style);

//    cairo_font_face_destroy (gstate->font_face);
    gstate->font_face = NULL;

//    cairo_scaled_font_destroy (gstate->previous_scaled_font);
//    gstate->previous_scaled_font = NULL;

//    cairo_scaled_font_destroy (gstate->scaled_font);
//    gstate->scaled_font = NULL;

//    _cairo_clip_reset (&gstate->clip);

//    cairo_list_del (&gstate->device_transform_observer.link);

    cairo_surface_destroy (gstate->target);
    gstate->target = NULL;

    cairo_surface_destroy (gstate->parent_target);
    gstate->parent_target = NULL;

    cairo_surface_destroy (gstate->original_target);
    gstate->original_target = NULL;

//    cairo_pattern_destroy (gstate->source);
    gstate->source = NULL;

//    VG (VALGRIND_MAKE_MEM_NOACCESS (gstate, sizeof (cairo_gstate_t)));
}

/**
 * _cairo_gstate_restore:
 * @gstate: input/output gstate pointer
 *
 * Reverses the effects of one _cairo_gstate_save() call.
 **/
cairo_status_t
_cairo_gstate_restore (cairo_gstate_t **gstate, cairo_gstate_t **freelist)
{
    cairo_gstate_t *top;

    top = *gstate;
//    if (top->next == NULL)
//	return _cairo_error (CAIRO_STATUS_INVALID_RESTORE);

    *gstate = top->next;

    _cairo_gstate_fini (top);
//    VG (VALGRIND_MAKE_MEM_UNDEFINED (&top->next, sizeof (cairo_gstate_t *)));
    top->next = *freelist;
    *freelist = top;

    return CAIRO_STATUS_SUCCESS;
}

/* We need to take a copy of the clip so that the lower layers may modify it
 * by, perhaps, intersecting it with the operation extents and other paths.
 */
#define _gstate_get_clip(G, C) _cairo_clip_init_copy ((C), &(G)->clip)

cairo_status_t
_cairo_gstate_stroke (cairo_gstate_t *gstate, cairo_path_fixed_t *path)
{
    cairo_pattern_union_t source_pattern;
    cairo_stroke_style_t style;
//    double dash[2];
    cairo_clip_t clip;
    cairo_status_t status;

    if (unlikely (gstate->source->status))
	return gstate->source->status;

    if (gstate->op == CAIRO_OPERATOR_DEST)
	return CAIRO_STATUS_SUCCESS;

    if (gstate->stroke_style.line_width <= 0.0)
	return CAIRO_STATUS_SUCCESS;

//    if (_clipped (gstate))
//	return CAIRO_STATUS_SUCCESS;
//
    memcpy (&style, &gstate->stroke_style, sizeof (gstate->stroke_style));
//    if (_cairo_stroke_style_dash_can_approximate (&gstate->stroke_style, &gstate->ctm, gstate->tolerance)) {
//        style.dash = dash;
//        _cairo_stroke_style_dash_approximate (&gstate->stroke_style, &gstate->ctm, gstate->tolerance,
//					      &style.dash_offset,
//					      style.dash,
//					      &style.num_dashes);
//    }
//
    _cairo_gstate_copy_transformed_source (gstate, &source_pattern.base);

    status = _cairo_surface_stroke (gstate->target,
				    gstate->op,
				    &source_pattern.base,
				    path,
				    &style,
				    &gstate->ctm,
				    &gstate->ctm_inverse,
				    gstate->tolerance,
				    gstate->antialias,
				    _gstate_get_clip (gstate, &clip));
    _cairo_clip_fini (&clip);

    return status;
}

cairo_status_t
_cairo_gstate_set_line_width (cairo_gstate_t *gstate, double width)
{
    gstate->stroke_style.line_width = width;

    return CAIRO_STATUS_SUCCESS;
}

void
_do_cairo_gstate_user_to_backend (cairo_gstate_t *gstate, double *x, double *y)
{
    cairo_matrix_transform_point (&gstate->ctm, x, y);
    cairo_matrix_transform_point (&gstate->target->device_transform, x, y);
}

cairo_status_t
_cairo_gstate_set_source (cairo_gstate_t  *gstate,
			  cairo_pattern_t *source)
{
    if (source->status)
	return source->status;

//    source = cairo_pattern_reference (source);
    cairo_pattern_destroy (gstate->source);
    gstate->source = source;
    gstate->source_ctm_inverse = gstate->ctm_inverse;

    return CAIRO_STATUS_SUCCESS;
}

static cairo_bool_t
_clipped (cairo_gstate_t *gstate)
{
    cairo_rectangle_int_t extents;

    if (gstate->clip.all_clipped)
	return TRUE;

    /* XXX consider applying a surface clip? */

    if (gstate->clip.path == NULL)
	return FALSE;

    if (_cairo_surface_get_extents (gstate->target, &extents)) {
	if (extents.width == 0 || extents.height == 0)
	    return TRUE;

	if (! _cairo_rectangle_intersect (&extents,
					  &gstate->clip.path->extents))
	{
	    return TRUE;
	}
    }

    /* perform a simple query to exclude trivial all-clipped cases */
    return _cairo_clip_get_region (&gstate->clip, NULL) == CAIRO_INT_STATUS_NOTHING_TO_DO;
}

static cairo_operator_t
_reduce_op (cairo_gstate_t *gstate)
{
    cairo_operator_t op;
    const cairo_pattern_t *pattern;

    op = gstate->op;
    if (op != CAIRO_OPERATOR_SOURCE)
	return op;

    pattern = gstate->source;
    if (pattern->type == CAIRO_PATTERN_TYPE_SOLID) {
	const cairo_solid_pattern_t *solid = (cairo_solid_pattern_t *) pattern;
	if (solid->color.alpha_short <= 0x00ff) {
	    op = CAIRO_OPERATOR_CLEAR;
	} else if ((gstate->target->content & CAIRO_CONTENT_ALPHA) == 0) {
	    if ((solid->color.red_short |
		 solid->color.green_short |
		 solid->color.blue_short) <= 0x00ff)
	    {
		op = CAIRO_OPERATOR_CLEAR;
	    }
	}
    } else if (pattern->type == CAIRO_PATTERN_TYPE_SURFACE) {
	const cairo_surface_pattern_t *surface = (cairo_surface_pattern_t *) pattern;
	if (surface->surface->is_clear &&
	    surface->surface->content & CAIRO_CONTENT_ALPHA)
	{
	    op = CAIRO_OPERATOR_CLEAR;
	}
    } else {
	const cairo_gradient_pattern_t *gradient = (cairo_gradient_pattern_t *) pattern;
	if (gradient->n_stops == 0)
	    op = CAIRO_OPERATOR_CLEAR;
    }

    return op;
}

cairo_status_t
_cairo_gstate_fill (cairo_gstate_t *gstate, cairo_path_fixed_t *path)
{
    cairo_clip_t clip;
    cairo_status_t status;

    if (unlikely (gstate->source->status))
	return gstate->source->status;

    if (gstate->op == CAIRO_OPERATOR_DEST)
	return CAIRO_STATUS_SUCCESS;

    if (_clipped (gstate))
	return CAIRO_STATUS_SUCCESS;

    if (_cairo_path_fixed_fill_is_empty (path)) {
	if (_cairo_operator_bounded_by_mask (gstate->op))
	    return CAIRO_STATUS_SUCCESS;

	status = _cairo_surface_paint (gstate->target,
				       CAIRO_OPERATOR_CLEAR,
				       &_cairo_pattern_clear.base,
				       _gstate_get_clip (gstate, &clip));
    } else {
	cairo_pattern_union_t source_pattern;
	const cairo_pattern_t *pattern;
	cairo_operator_t op;
	cairo_rectangle_int_t extents;
	cairo_box_t box;

	op = _reduce_op (gstate);
	if (op == CAIRO_OPERATOR_CLEAR) {
	    pattern = &_cairo_pattern_clear.base;
	} else {
	    _cairo_gstate_copy_transformed_source (gstate, &source_pattern.base);
	    pattern = &source_pattern.base;
	}

	/* Toolkits often paint the entire background with a fill */
	if (_cairo_surface_get_extents (gstate->target, &extents) &&
	    _cairo_path_fixed_is_box (path, &box) &&
	    box.p1.x <= _cairo_fixed_from_int (extents.x) &&
	    box.p1.y <= _cairo_fixed_from_int (extents.y) &&
	    box.p2.x >= _cairo_fixed_from_int (extents.x + extents.width) &&
	    box.p2.y >= _cairo_fixed_from_int (extents.y + extents.height))
	{
	    status = _cairo_surface_paint (gstate->target, op, pattern,
					   _gstate_get_clip (gstate, &clip));
	}
	else
	{
	    status = _cairo_surface_fill (gstate->target, op, pattern,
					  path,
					  gstate->fill_rule,
					  gstate->tolerance,
					  gstate->antialias,
					  _gstate_get_clip (gstate, &clip));
	}
    }

    _cairo_clip_fini (&clip);

    return status;
}

cairo_status_t
_cairo_gstate_set_line_join (cairo_gstate_t *gstate, cairo_line_join_t line_join)
{
    gstate->stroke_style.line_join = line_join;

    return CAIRO_STATUS_SUCCESS;
}


cairo_status_t
_cairo_gstate_set_line_cap (cairo_gstate_t *gstate, cairo_line_cap_t line_cap)
{
    gstate->stroke_style.line_cap = line_cap;

    return CAIRO_STATUS_SUCCESS;
}



cairo_status_t
_cairo_gstate_set_fill_rule (cairo_gstate_t *gstate, cairo_fill_rule_t fill_rule)
{
    gstate->fill_rule = fill_rule;

    return CAIRO_STATUS_SUCCESS;
}
