#include "cairo-surface-fallback-private.h"

#include "cairoint.h"
cairo_status_t
_cairo_surface_fallback_stroke (cairo_surface_t		*surface,
				cairo_operator_t	 op,
				const cairo_pattern_t	*source,
				cairo_path_fixed_t	*path,
				const cairo_stroke_style_t	*stroke_style,
				const cairo_matrix_t		*ctm,
				const cairo_matrix_t		*ctm_inverse,
				double			 tolerance,
				cairo_antialias_t	 antialias,
				cairo_clip_t		*clip)
{
    return CAIRO_STATUS_NO_MEMORY;
    /*  
    cairo_polygon_t polygon;
    cairo_traps_t traps;
    cairo_box_t boxes_stack[32], *clip_boxes = boxes_stack;
    int num_boxes = ARRAY_LENGTH (boxes_stack);
    cairo_composite_rectangles_t extents;
    cairo_rectangle_int_t rect;
    cairo_status_t status;

    if (!_cairo_surface_get_extents (surface, &rect))
        ASSERT_NOT_REACHED;

    status = _cairo_composite_rectangles_init_for_stroke (&extents,
							  rect.width,
							  rect.height,
							  op, source,
							  path, stroke_style, ctm,
							  clip);
    if (unlikely (status))
	return status;

    if (_cairo_clip_contains_extents (clip, &extents))
	clip = NULL;

    status = _cairo_clip_to_boxes (&clip, &extents, &clip_boxes, &num_boxes);
    if (unlikely (status))
	return status;

    _cairo_polygon_init (&polygon);
    _cairo_polygon_limit (&polygon, clip_boxes, num_boxes);

    _cairo_traps_init (&traps);
    _cairo_traps_limit (&traps, clip_boxes, num_boxes);

    if (path->is_rectilinear) {
	status = _cairo_path_fixed_stroke_rectilinear_to_traps (path,
								stroke_style,
								ctm,
								&traps);
	if (likely (status == CAIRO_STATUS_SUCCESS))
	    goto DO_TRAPS;

	if (_cairo_status_is_error (status))
	    goto CLEANUP;
    }

    status = _cairo_path_fixed_stroke_to_polygon (path,
						  stroke_style,
						  ctm, ctm_inverse,
						  tolerance,
						  &polygon);
    if (unlikely (status))
	goto CLEANUP;

    if (polygon.num_edges == 0)
	goto DO_TRAPS;

    if (_cairo_operator_bounded_by_mask (op)) {
	_cairo_box_round_to_rectangle (&polygon.extents, &extents.mask);
	if (! _cairo_rectangle_intersect (&extents.bounded, &extents.mask))
	    goto CLEANUP;
    }

    // Fall back to trapezoid fills. */
 /*    status = _cairo_bentley_ottmann_tessellate_polygon (&traps,
							&polygon,
							CAIRO_FILL_RULE_WINDING);
    if (unlikely (status))
	goto CLEANUP;

  DO_TRAPS:
    status = _clip_and_composite_trapezoids (source, op, surface,
					     &traps, antialias,
					     clip,
                                             extents.is_bounded ? &extents.bounded : &extents.unbounded);
  CLEANUP:
    _cairo_traps_fini (&traps);
    _cairo_polygon_fini (&polygon);
    if (clip_boxes != boxes_stack)
	free (clip_boxes);

    return status;*/
}

cairo_status_t
_cairo_surface_fallback_fill (cairo_surface_t		*surface,
			      cairo_operator_t		 op,
			      const cairo_pattern_t	*source,
			      cairo_path_fixed_t	*path,
			      cairo_fill_rule_t		 fill_rule,
			      double			 tolerance,
			      cairo_antialias_t		 antialias,
			      cairo_clip_t		*clip)
{
    printf("You Should not be here:_cairo_surface_fallback_fill!\n");
    return CAIRO_STATUS_NO_MEMORY;
//    cairo_polygon_t polygon;
//    cairo_traps_t traps;
//    cairo_box_t boxes_stack[32], *clip_boxes = boxes_stack;
//    int num_boxes = ARRAY_LENGTH (boxes_stack);
//    cairo_bool_t is_rectilinear;
//    cairo_composite_rectangles_t extents;
//    cairo_rectangle_int_t rect;
//    cairo_status_t status;
//
//    if (!_cairo_surface_get_extents (surface, &rect))
//        ASSERT_NOT_REACHED;
//
//    status = _cairo_composite_rectangles_init_for_fill (&extents,
//							rect.width,
//							rect.height,
//							op, source, path,
//							clip);
//    if (unlikely (status))
//	return status;
//
//    if (_cairo_clip_contains_extents (clip, &extents))
//	clip = NULL;
//
//    status = _cairo_clip_to_boxes (&clip, &extents, &clip_boxes, &num_boxes);
//    if (unlikely (status))
//	return status;
//
//    _cairo_traps_init (&traps);
//    _cairo_traps_limit (&traps, clip_boxes, num_boxes);
//
//    _cairo_polygon_init (&polygon);
//    _cairo_polygon_limit (&polygon, clip_boxes, num_boxes);
//
//    if (path->is_empty_fill)
//	goto DO_TRAPS;
//
//    is_rectilinear = _cairo_path_fixed_is_rectilinear_fill (path);
//    if (is_rectilinear) {
//	status = _cairo_path_fixed_fill_rectilinear_to_traps (path,
//							      fill_rule,
//							      &traps);
//	if (likely (status == CAIRO_STATUS_SUCCESS))
//	    goto DO_TRAPS;
//
//	if (_cairo_status_is_error (status))
//	    goto CLEANUP;
//    }
//
//    status = _cairo_path_fixed_fill_to_polygon (path, tolerance, &polygon);
//    if (unlikely (status))
//	goto CLEANUP;
//
//    if (polygon.num_edges == 0)
//	goto DO_TRAPS;
//
//    if (_cairo_operator_bounded_by_mask (op)) {
//	_cairo_box_round_to_rectangle (&polygon.extents, &extents.mask);
//	if (! _cairo_rectangle_intersect (&extents.bounded, &extents.mask))
//	    goto CLEANUP;
//    }
//
//    if (is_rectilinear) {
//	status = _cairo_bentley_ottmann_tessellate_rectilinear_polygon (&traps,
//									&polygon,
//									fill_rule);
//	if (likely (status == CAIRO_STATUS_SUCCESS))
//	    goto DO_TRAPS;
//
//	if (unlikely (_cairo_status_is_error (status)))
//	    goto CLEANUP;
//    }
//
//    /* Fall back to trapezoid fills. */
//    status = _cairo_bentley_ottmann_tessellate_polygon (&traps,
//							&polygon,
//							fill_rule);
//    if (unlikely (status))
//	goto CLEANUP;
//
//  DO_TRAPS:
//    status = _clip_and_composite_trapezoids (source, op, surface,
//					     &traps, antialias,
//					     clip,
//                                             extents.is_bounded ? &extents.bounded : &extents.unbounded);
//  CLEANUP:
//    _cairo_traps_fini (&traps);
//    _cairo_polygon_fini (&polygon);
//    if (clip_boxes != boxes_stack)
//	free (clip_boxes);
//
//    return status;
}

