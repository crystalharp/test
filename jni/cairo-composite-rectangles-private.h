#ifndef CAIRO_COMPOSITE_RECTANGLES_PRIVATE_H
#define CAIRO_COMPOSITE_RECTANGLES_PRIVATE_H

#include "cairo-types-private.h"

/* Rectangles that take part in a composite operation.
 *
 * The source and mask track the extents of the respective patterns in device
 * space. The unbounded rectangle is essentially the clip rectangle. And the
 * intersection of all is the bounded rectangle, which is the minimum extents
 * the operation may require. Whether or not the operation is actually bounded
 * is tracked in the is_bounded boolean.
 *
 */
struct _cairo_composite_rectangles {
    cairo_rectangle_int_t source;
    cairo_rectangle_int_t mask;
    cairo_rectangle_int_t bounded; /* dst */
    cairo_rectangle_int_t unbounded; /* clip */
    uint32_t is_bounded;
};

cairo_private cairo_int_status_t
_cairo_composite_rectangles_init_for_paint (cairo_composite_rectangles_t *extents,
					 int surface_width, int surface_height,
					 cairo_operator_t	 op,
					 const cairo_pattern_t	*source,
					 cairo_clip_t		*clip);

cairo_private cairo_int_status_t
_cairo_composite_rectangles_init_for_mask (cairo_composite_rectangles_t *extents,
					int surface_width, int surface_height,
					cairo_operator_t	 op,
					const cairo_pattern_t	*source,
					const cairo_pattern_t	*mask,
					cairo_clip_t		*clip);

cairo_private cairo_int_status_t
_cairo_composite_rectangles_init_for_stroke (cairo_composite_rectangles_t *extents,
					     int surface_width, int surface_height,
					     cairo_operator_t	 op,
					     const cairo_pattern_t	*source,
					     cairo_path_fixed_t	*path,
					     const cairo_stroke_style_t	*style,
					     const cairo_matrix_t	*ctm,
					     cairo_clip_t		*clip);

cairo_private cairo_int_status_t
_cairo_composite_rectangles_init_for_fill (cairo_composite_rectangles_t *extents,
					   int surface_width, int surface_height,
					   cairo_operator_t	 op,
					   const cairo_pattern_t	*source,
					   cairo_path_fixed_t	*path,
					   cairo_clip_t		*clip);

#endif /* CAIRO_COMPOSITE_RECTANGLES_PRIVATE_H */
