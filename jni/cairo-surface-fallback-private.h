#ifndef CAIRO_SURFACE_FALLBACK_PRIVATE_H
#define CAIRO_SURFACE_FALLBACK_PRIVATE_H

#include "cairoint.h"

cairo_private cairo_status_t
_cairo_surface_fallback_stroke (cairo_surface_t		*surface,
				cairo_operator_t	 op,
				const cairo_pattern_t	*source,
				cairo_path_fixed_t	*path,
				const cairo_stroke_style_t	*stroke_style,
				const cairo_matrix_t		*ctm,
				const cairo_matrix_t		*ctm_inverse,
				double			 tolerance,
				cairo_antialias_t	 antialias,
				cairo_clip_t		*clip);
cairo_private cairo_status_t
_cairo_surface_fallback_fill (cairo_surface_t		*surface,
			      cairo_operator_t		 op,
			      const cairo_pattern_t	*source,
			      cairo_path_fixed_t	*path,
			      cairo_fill_rule_t		 fill_rule,
			      double			 tolerance,
			      cairo_antialias_t		 antialias,
			      cairo_clip_t		*clip);

#endif
