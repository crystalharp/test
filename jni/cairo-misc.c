
#include "cairoint.h"
/**
 * _cairo_operator_bounded_by_mask:
 * @op: a #cairo_operator_t
 *
 * A bounded operator is one where mask pixel
 * of zero results in no effect on the destination image.
 *
 * Unbounded operators often require special handling; if you, for
 * example, draw trapezoids with an unbounded operator, the effect
 * extends past the bounding box of the trapezoids.
 *
 * Return value: %TRUE if the operator is bounded by the mask operand
 **/
cairo_bool_t
_cairo_operator_bounded_by_mask (cairo_operator_t op)
{
    switch (op) {
    case CAIRO_OPERATOR_CLEAR:
    case CAIRO_OPERATOR_SOURCE:
    case CAIRO_OPERATOR_OVER:
    case CAIRO_OPERATOR_ATOP:
    case CAIRO_OPERATOR_DEST:
    case CAIRO_OPERATOR_DEST_OVER:
    case CAIRO_OPERATOR_DEST_OUT:
    case CAIRO_OPERATOR_XOR:
    case CAIRO_OPERATOR_ADD:
    case CAIRO_OPERATOR_SATURATE:
    case CAIRO_OPERATOR_MULTIPLY:
    case CAIRO_OPERATOR_SCREEN:
    case CAIRO_OPERATOR_OVERLAY:
    case CAIRO_OPERATOR_DARKEN:
    case CAIRO_OPERATOR_LIGHTEN:
    case CAIRO_OPERATOR_COLOR_DODGE:
    case CAIRO_OPERATOR_COLOR_BURN:
    case CAIRO_OPERATOR_HARD_LIGHT:
    case CAIRO_OPERATOR_SOFT_LIGHT:
    case CAIRO_OPERATOR_DIFFERENCE:
    case CAIRO_OPERATOR_EXCLUSION:
    case CAIRO_OPERATOR_HSL_HUE:
    case CAIRO_OPERATOR_HSL_SATURATION:
    case CAIRO_OPERATOR_HSL_COLOR:
    case CAIRO_OPERATOR_HSL_LUMINOSITY:
	return TRUE;
    case CAIRO_OPERATOR_OUT:
    case CAIRO_OPERATOR_IN:
    case CAIRO_OPERATOR_DEST_IN:
    case CAIRO_OPERATOR_DEST_ATOP:
	return FALSE;
    }

    ASSERT_NOT_REACHED;
    return FALSE;
}

