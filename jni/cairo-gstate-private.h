#ifndef CAIRO_GSTATE_PRIVATE_H
#define CAIRO_GSTATE_PRIVATE_H

#include "cairo-clip-private.h"

struct _cairo_gstate {
    cairo_operator_t op;

    double tolerance;
    cairo_antialias_t antialias;

    cairo_stroke_style_t stroke_style;

    cairo_fill_rule_t fill_rule;

    cairo_font_face_t *font_face;
//    cairo_scaled_font_t *scaled_font;	/* Specific to the current CTM */
//    cairo_scaled_font_t *previous_scaled_font;	/* holdover */
    cairo_matrix_t font_matrix;
    cairo_font_options_t font_options;

    cairo_clip_t clip;

    cairo_surface_t *target;		/* The target to which all rendering is directed */
    cairo_surface_t *parent_target;	/* The previous target which was receiving rendering */
    cairo_surface_t *original_target;	/* The original target the initial gstate was created with */

    /* the user is allowed to update the device after we have cached the matrices... */
    cairo_observer_t device_transform_observer;

    cairo_matrix_t ctm;
    cairo_matrix_t ctm_inverse;
    cairo_matrix_t source_ctm_inverse; /* At the time ->source was set */
    cairo_bool_t is_identity;

    cairo_pattern_t *source;

    struct _cairo_gstate *next;
};

/* cairo-gstate.c */
cairo_private cairo_status_t
_cairo_gstate_init (cairo_gstate_t  *gstate,
		    cairo_surface_t *target);

cairo_private cairo_status_t
_cairo_gstate_stroke (cairo_gstate_t *gstate, cairo_path_fixed_t *path);

cairo_private void
_cairo_gstate_fini (cairo_gstate_t *gstate);

cairo_private cairo_status_t
_cairo_gstate_save (cairo_gstate_t **gstate, cairo_gstate_t **freelist);

cairo_private cairo_status_t
_cairo_gstate_restore (cairo_gstate_t **gstate, cairo_gstate_t **freelist);

cairo_private cairo_status_t
_cairo_gstate_set_line_width (cairo_gstate_t *gstate, double width);

cairo_private void
_do_cairo_gstate_user_to_backend (cairo_gstate_t *gstate, double *x, double *y);

static inline void
_cairo_gstate_user_to_backend (cairo_gstate_t *gstate, double *x, double *y)
{
    if (! gstate->is_identity)
	_do_cairo_gstate_user_to_backend (gstate, x, y);
}

cairo_private cairo_status_t
_cairo_gstate_set_source (cairo_gstate_t *gstate, cairo_pattern_t *source);

cairo_private cairo_status_t
_cairo_gstate_fill (cairo_gstate_t *gstate, cairo_path_fixed_t *path);
cairo_status_t
_cairo_gstate_set_line_join (cairo_gstate_t *gstate, cairo_line_join_t line_join);

#endif
