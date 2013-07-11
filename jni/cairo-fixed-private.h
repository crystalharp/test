#ifndef CAIRO_FIXED_PRIVATE_H
#define CAIRO_FIXED_PRIVATE_H

#include "cairo-wideint-private.h"
#include "cairo-fixed-type-private.h"

static inline cairo_fixed_t
_cairo_fixed_from_int (int i)
{
    return i << CAIRO_FIXED_FRAC_BITS;
}

#define CAIRO_FIXED_ONE_DOUBLE ((double)(1 << CAIRO_FIXED_FRAC_BITS))
#define CAIRO_FIXED_FRAC_MASK  ((cairo_fixed_t)(((cairo_fixed_unsigned_t)(-1)) >> (CAIRO_FIXED_BITS - CAIRO_FIXED_FRAC_BITS)))

#define CAIRO_FIXED_ONE        ((cairo_fixed_t)(1 << CAIRO_FIXED_FRAC_BITS))
/* This is the "magic number" approach to converting a double into fixed
 * point as described here:
 *
 * http://www.stereopsis.com/sree/fpu2006.html (an overview)
 * http://www.d6.com/users/checker/pdfs/gdmfp.pdf (in detail)
 *
 * The basic idea is to add a large enough number to the double that the
 * literal floating point is moved up to the extent that it forces the
 * double's value to be shifted down to the bottom of the mantissa (to make
 * room for the large number being added in). Since the mantissa is, at a
 * given moment in time, a fixed point integer itself, one can convert a
 * float to various fixed point representations by moving around the point
 * of a floating point number through arithmetic operations. This behavior
 * is reliable on most modern platforms as it is mandated by the IEEE-754
 * standard for floating point arithmetic.
 *
 * For our purposes, a "magic number" must be carefully selected that is
 * both large enough to produce the desired point-shifting effect, and also
 * has no lower bits in its representation that would interfere with our
 * value at the bottom of the mantissa. The magic number is calculated as
 * follows:
 *
 *          (2 ^ (MANTISSA_SIZE - FRACTIONAL_SIZE)) * 1.5
 *
 * where in our case:
 *  - MANTISSA_SIZE for 64-bit doubles is 52
 *  - FRACTIONAL_SIZE for 16.16 fixed point is 16
 *
 * Although this approach provides a very large speedup of this function
 * on a wide-array of systems, it does come with two caveats:
 *
 * 1) It uses banker's rounding as opposed to arithmetic rounding.
 * 2) It doesn't function properly if the FPU is in single-precision
 *    mode.
 */

/* The 16.16 number must always be available */
#define CAIRO_MAGIC_NUMBER_FIXED_16_16 (103079215104.0)

#if CAIRO_FIXED_BITS <= 32
#define CAIRO_MAGIC_NUMBER_FIXED ((1LL << (52 - CAIRO_FIXED_FRAC_BITS)) * 1.5)

/* For 32-bit fixed point numbers */
static inline cairo_fixed_t
_cairo_fixed_from_double (double d)
{
    union {
        double d;
        int i[2];
    } u;

    u.d = d + CAIRO_MAGIC_NUMBER_FIXED;
#ifdef FLOAT_WORDS_BIGENDIAN
    return u.i[1];
#else
    return u.i[0];
#endif
}

#else
# error Please define a magic number for your fixed point type!
# error See cairo-fixed-private.h for details.
#endif
static inline double
_cairo_fixed_to_double (cairo_fixed_t f)
{
    return ((double) f) / CAIRO_FIXED_ONE_DOUBLE;
}


static inline int
_cairo_fixed_is_integer (cairo_fixed_t f)
{
    return (f & CAIRO_FIXED_FRAC_MASK) == 0;
}
static inline int
_cairo_fixed_integer_floor (cairo_fixed_t f)
{
    if (f >= 0)
        return f >> CAIRO_FIXED_FRAC_BITS;
    else
        return -((-f - 1) >> CAIRO_FIXED_FRAC_BITS) - 1;
}
static inline int
_cairo_fixed_integer_ceil (cairo_fixed_t f)
{
    if (f > 0)
	return ((f - 1)>>CAIRO_FIXED_FRAC_BITS) + 1;
    else
	return - (-f >> CAIRO_FIXED_FRAC_BITS);
}
static inline cairo_fixed_t
_cairo_fixed_mul_div_floor (cairo_fixed_t a, cairo_fixed_t b, cairo_fixed_t c)
{
    return _cairo_int64_32_div (_cairo_int32x32_64_mul (a, b), c);
}


static inline cairo_fixed_t
_cairo_edge_compute_intersection_y_for_x (const cairo_point_t *p1,
					  const cairo_point_t *p2,
					  cairo_fixed_t x)
{
    cairo_fixed_t y, dx;

    if (x == p1->x)
	return p1->y;
    if (x == p2->x)
	return p2->y;

    y = p1->y;
    dx = p2->x - p1->x;
    if (dx != 0)
	y += _cairo_fixed_mul_div_floor (x - p1->x, p2->y - p1->y, dx);

    return y;
}

static inline cairo_fixed_t
_cairo_edge_compute_intersection_x_for_y (const cairo_point_t *p1,
					  const cairo_point_t *p2,
					  cairo_fixed_t y)
{
    cairo_fixed_t x, dy;

    if (y == p1->y)
	return p1->x;
    if (y == p2->y)
	return p2->x;

    x = p1->x;
    dy = p2->y - p1->y;
    if (dy != 0)
	x += _cairo_fixed_mul_div_floor (y - p1->y, p2->x - p1->x, dy);

    return x;
}
static inline int
_cairo_fixed_integer_part (cairo_fixed_t f)
{
    return f >> CAIRO_FIXED_FRAC_BITS;
}
static inline int
_cairo_fixed_integer_round_down (cairo_fixed_t f)
{
    return _cairo_fixed_integer_part (f + CAIRO_FIXED_FRAC_MASK/2);
}
static inline int
_cairo_fixed_fractional_part (cairo_fixed_t f)
{
    return f & CAIRO_FIXED_FRAC_MASK;
}

#endif /* CAIRO_FIXED_PRIVATE_H */
