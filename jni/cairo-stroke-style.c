#include "cairoint.h"
/*
 * For a stroke in the given style, compute the maximum distance
 * from the path that vertices could be generated.  In the case
 * of rotation in the ctm, the distance will not be exact.
 */
void
_cairo_stroke_style_max_distance_from_path (const cairo_stroke_style_t *style,
                                            const cairo_matrix_t *ctm,
                                            double *dx, double *dy)
{
    double style_expansion = 0.5;

    if (style->line_cap == CAIRO_LINE_CAP_SQUARE)
	style_expansion = M_SQRT1_2;

    if (style->line_join == CAIRO_LINE_JOIN_MITER &&
	style_expansion < M_SQRT2 * style->miter_limit)
    {
	style_expansion = M_SQRT2 * style->miter_limit;
    }

    style_expansion *= style->line_width;

    *dx = style_expansion * hypot (ctm->xx, ctm->xy);
    *dy = style_expansion * hypot (ctm->yy, ctm->yx);
}

void
_cairo_stroke_style_init (cairo_stroke_style_t *style)
{
//    VG (VALGRIND_MAKE_MEM_UNDEFINED (style, sizeof (cairo_stroke_style_t)));

    style->line_width = CAIRO_GSTATE_LINE_WIDTH_DEFAULT;
    style->line_cap = CAIRO_GSTATE_LINE_CAP_DEFAULT;
    style->line_join = CAIRO_GSTATE_LINE_JOIN_DEFAULT;
    style->miter_limit = CAIRO_GSTATE_MITER_LIMIT_DEFAULT;

    style->dash = NULL;
    style->num_dashes = 0;
    style->dash_offset = 0.0;
}

void
_cairo_stroke_style_fini (cairo_stroke_style_t *style)
{
    if (style->dash) {
	free (style->dash);
	style->dash = NULL;
    }
    style->num_dashes = 0;

//    VG (VALGRIND_MAKE_MEM_NOACCESS (style, sizeof (cairo_stroke_style_t)));
}

