#include "cairoint.h"
#include "cairo-clip-private.h"
#include "cairo-boxes-private.h"
#include "cairo-composite-rectangles-private.h"

/* XXX: I think we should fix pixman to match the names/order of the
 * cairo operators, but that will likely be better done at the same
 * time the X server is ported to pixman, (which will change a lot of
 * things in pixman I think).
 */
static pixman_op_t
_pixman_operator (cairo_operator_t op)
{
    switch (op) {
    case CAIRO_OPERATOR_CLEAR:
	return PIXMAN_OP_CLEAR;

    case CAIRO_OPERATOR_SOURCE:
	return PIXMAN_OP_SRC;
    case CAIRO_OPERATOR_OVER:
	return PIXMAN_OP_OVER;
    case CAIRO_OPERATOR_IN:
	return PIXMAN_OP_IN;
    case CAIRO_OPERATOR_OUT:
	return PIXMAN_OP_OUT;
    case CAIRO_OPERATOR_ATOP:
	return PIXMAN_OP_ATOP;

    case CAIRO_OPERATOR_DEST:
	return PIXMAN_OP_DST;
    case CAIRO_OPERATOR_DEST_OVER:
	return PIXMAN_OP_OVER_REVERSE;
    case CAIRO_OPERATOR_DEST_IN:
	return PIXMAN_OP_IN_REVERSE;
    case CAIRO_OPERATOR_DEST_OUT:
	return PIXMAN_OP_OUT_REVERSE;
    case CAIRO_OPERATOR_DEST_ATOP:
	return PIXMAN_OP_ATOP_REVERSE;

    case CAIRO_OPERATOR_XOR:
	return PIXMAN_OP_XOR;
    case CAIRO_OPERATOR_ADD:
	return PIXMAN_OP_ADD;
    case CAIRO_OPERATOR_SATURATE:
	return PIXMAN_OP_SATURATE;

    case CAIRO_OPERATOR_MULTIPLY:
	return PIXMAN_OP_MULTIPLY;
    case CAIRO_OPERATOR_SCREEN:
	return PIXMAN_OP_SCREEN;
    case CAIRO_OPERATOR_OVERLAY:
	return PIXMAN_OP_OVERLAY;
    case CAIRO_OPERATOR_DARKEN:
	return PIXMAN_OP_DARKEN;
    case CAIRO_OPERATOR_LIGHTEN:
	return PIXMAN_OP_LIGHTEN;
    case CAIRO_OPERATOR_COLOR_DODGE:
	return PIXMAN_OP_COLOR_DODGE;
    case CAIRO_OPERATOR_COLOR_BURN:
	return PIXMAN_OP_COLOR_BURN;
    case CAIRO_OPERATOR_HARD_LIGHT:
	return PIXMAN_OP_HARD_LIGHT;
    case CAIRO_OPERATOR_SOFT_LIGHT:
	return PIXMAN_OP_SOFT_LIGHT;
    case CAIRO_OPERATOR_DIFFERENCE:
	return PIXMAN_OP_DIFFERENCE;
    case CAIRO_OPERATOR_EXCLUSION:
	return PIXMAN_OP_EXCLUSION;
    case CAIRO_OPERATOR_HSL_HUE:
	return PIXMAN_OP_HSL_HUE;
    case CAIRO_OPERATOR_HSL_SATURATION:
	return PIXMAN_OP_HSL_SATURATION;
    case CAIRO_OPERATOR_HSL_COLOR:
	return PIXMAN_OP_HSL_COLOR;
    case CAIRO_OPERATOR_HSL_LUMINOSITY:
	return PIXMAN_OP_HSL_LUMINOSITY;

    default:
	ASSERT_NOT_REACHED;
	return PIXMAN_OP_OVER;
    }
}

cairo_format_t
_cairo_format_from_pixman_format (pixman_format_code_t pixman_format)
{
    switch (pixman_format) {
    case PIXMAN_a8r8g8b8:
	return CAIRO_FORMAT_ARGB32;
    case PIXMAN_x8r8g8b8:
	return CAIRO_FORMAT_RGB24;
    case PIXMAN_a8:
	return CAIRO_FORMAT_A8;
    case PIXMAN_a1:
	return CAIRO_FORMAT_A1;
    case PIXMAN_r5g6b5:
	return CAIRO_FORMAT_RGB16_565;
    case PIXMAN_a8b8g8r8: case PIXMAN_x8b8g8r8: case PIXMAN_r8g8b8:
    case PIXMAN_b8g8r8:   case PIXMAN_b5g6r5:
    case PIXMAN_a1r5g5b5: case PIXMAN_x1r5g5b5: case PIXMAN_a1b5g5r5:
    case PIXMAN_x1b5g5r5: case PIXMAN_a4r4g4b4: case PIXMAN_x4r4g4b4:
    case PIXMAN_a4b4g4r4: case PIXMAN_x4b4g4r4: case PIXMAN_r3g3b2:
    case PIXMAN_b2g3r3:   case PIXMAN_a2r2g2b2: case PIXMAN_a2b2g2r2:
    case PIXMAN_c8:       case PIXMAN_g8:       case PIXMAN_x4a4:
    case PIXMAN_a4:       case PIXMAN_r1g2b1:   case PIXMAN_b1g2r1:
    case PIXMAN_a1r1g1b1: case PIXMAN_a1b1g1r1: case PIXMAN_c4:
    case PIXMAN_g4:       case PIXMAN_g1:
    case PIXMAN_yuy2:     case PIXMAN_yv12:
    case PIXMAN_b8g8r8x8:
    case PIXMAN_b8g8r8a8:
    case PIXMAN_x2b10g10r10:
    case PIXMAN_a2b10g10r10:
    case PIXMAN_x2r10g10b10:
    case PIXMAN_a2r10g10b10:
    default:
	return CAIRO_FORMAT_INVALID;
    }

    return CAIRO_FORMAT_INVALID;
}

int
_cairo_format_bits_per_pixel (cairo_format_t format)
{
    switch (format) {
    case CAIRO_FORMAT_ARGB32:
	return 32;
    case CAIRO_FORMAT_RGB24:
	return 32;
    case CAIRO_FORMAT_RGB16_565:
	return 16;
    case CAIRO_FORMAT_A8:
	return 8;
    case CAIRO_FORMAT_A1:
	return 1;
    case CAIRO_FORMAT_INVALID:
    default:
	ASSERT_NOT_REACHED;
	return 0;
    }
}

pixman_format_code_t
_cairo_format_to_pixman_format_code (cairo_format_t format)
{
    pixman_format_code_t ret;
    switch (format) {
    case CAIRO_FORMAT_A1:
	ret = PIXMAN_a1;
	break;
    case CAIRO_FORMAT_A8:
	ret = PIXMAN_a8;
	break;
    case CAIRO_FORMAT_RGB24:
	ret = PIXMAN_x8r8g8b8;
	break;
    case CAIRO_FORMAT_RGB16_565:
	ret = PIXMAN_r5g6b5;
	break;
    case CAIRO_FORMAT_ARGB32:
    case CAIRO_FORMAT_INVALID:
    default:
	ret = PIXMAN_a8r8g8b8;
	break;
    }
    return ret;
}
/* Limit on the width / height of an image surface in pixels.  This is
 * mainly determined by coordinates of things sent to pixman at the
 * moment being in 16.16 format. */
#define MAX_IMAGE_SIZE 32767

static cairo_bool_t
_cairo_image_surface_is_size_valid (int width, int height)
{
    return 0 <= width  &&  width <= MAX_IMAGE_SIZE &&
	   0 <= height && height <= MAX_IMAGE_SIZE;
}
cairo_content_t
_cairo_content_from_pixman_format (pixman_format_code_t pixman_format)
{
    cairo_content_t content;

    content = 0;
    if (PIXMAN_FORMAT_RGB (pixman_format))
	content |= CAIRO_CONTENT_COLOR;
    if (PIXMAN_FORMAT_A (pixman_format))
	content |= CAIRO_CONTENT_ALPHA;

    return content;
}

cairo_surface_t *
_cairo_image_surface_create_for_pixman_image (pixman_image_t		*pixman_image,
					      pixman_format_code_t	 pixman_format)
{
    cairo_image_surface_t *surface;
    int width = pixman_image_get_width (pixman_image);
    int height = pixman_image_get_height (pixman_image);

    surface = malloc (sizeof (cairo_image_surface_t));
    if (unlikely (surface == NULL))
	return _cairo_surface_create_in_error (CAIRO_STATUS_NO_MEMORY);

    _cairo_surface_init (&surface->base,
			 &_cairo_image_surface_backend,
			 NULL, /* device */
			 _cairo_content_from_pixman_format (pixman_format));

    surface->pixman_image = pixman_image;

    surface->pixman_format = pixman_format;
    surface->format = _cairo_format_from_pixman_format (pixman_format);
    surface->data = (uint8_t *) pixman_image_get_data (pixman_image);
    surface->owns_data = FALSE;
    surface->transparency = CAIRO_IMAGE_UNKNOWN;

    surface->width = width;
    surface->height = height;
    surface->stride = pixman_image_get_stride (pixman_image);
    surface->depth = pixman_image_get_depth (pixman_image);

    return &surface->base;
}

cairo_surface_t *
_cairo_image_surface_create_with_pixman_format (unsigned char		*data,
						pixman_format_code_t	 pixman_format,
						int			 width,
						int			 height,
						int			 stride)
{
    cairo_surface_t *surface;
    pixman_image_t *pixman_image;

    if (! _cairo_image_surface_is_size_valid (width, height))
    {
	return _cairo_surface_create_in_error (CAIRO_STATUS_INVALID_SIZE);
    }

    pixman_image = pixman_image_create_bits (pixman_format, width, height,
					     (uint32_t *) data, stride);

    if (unlikely (pixman_image == NULL))
	return _cairo_surface_create_in_error (CAIRO_STATUS_NO_MEMORY);

    surface = _cairo_image_surface_create_for_pixman_image (pixman_image,
							    pixman_format);
    if (unlikely (surface->status)) {
	pixman_image_unref (pixman_image);
	return surface;
    }

    /* we can not make any assumptions about the initial state of user data */
    surface->is_clear = data == NULL;
    return surface;
}

/**
 * cairo_image_surface_create:
 * @format: format of pixels in the surface to create
 * @width: width of the surface, in pixels
 * @height: height of the surface, in pixels
 *
 * Creates an image surface of the specified format and
 * dimensions. Initially the surface contents are all
 * 0. (Specifically, within each pixel, each color or alpha channel
 * belonging to format will be 0. The contents of bits within a pixel,
 * but not belonging to the given format are undefined).
 *
 * Return value: a pointer to the newly created surface. The caller
 * owns the surface and should call cairo_surface_destroy() when done
 * with it.
 *
 * This function always returns a valid pointer, but it will return a
 * pointer to a "nil" surface if an error such as out of memory
 * occurs. You can use cairo_surface_status() to check for this.
 **/
cairo_surface_t *
cairo_image_surface_create ( cairo_format_t	format,
			    int			width,
			    int			height)
{
    pixman_format_code_t pixman_format;

    if (! CAIRO_FORMAT_VALID (format))
	return _cairo_surface_create_in_error (CAIRO_STATUS_INVALID_FORMAT);

    pixman_format = _cairo_format_to_pixman_format_code (format);

    return _cairo_image_surface_create_with_pixman_format (NULL, pixman_format,
							   width, height, -1);
}
/**
 * cairo_format_stride_for_width:
 * @format: A #cairo_format_t value
 * @width: The desired width of an image surface to be created.
 *
 * This function provides a stride value that will respect all
 * alignment requirements of the accelerated image-rendering code
 * within cairo. Typical usage will be of the form:
 *
 * <informalexample><programlisting>
 * int stride;
 * unsigned char *data;
 * #cairo_surface_t *surface;
 *
 * stride = cairo_format_stride_for_width (format, width);
 * data = malloc (stride * height);
 * surface = cairo_image_surface_create_for_data (data, format,
 *						  width, height,
 *						  stride);
 * </programlisting></informalexample>
 *
 * Return value: the appropriate stride to use given the desired
 * format and width, or -1 if either the format is invalid or the width
 * too large.
 *
 * Since: 1.6
 **/
int
cairo_format_stride_for_width (cairo_format_t	format,
			       int		width)
{
    int bpp;

//    if (! CAIRO_FORMAT_VALID (format)) {
//	_cairo_error_throw (CAIRO_STATUS_INVALID_FORMAT);
//	return -1;
//    }
//    HURTYOU

    bpp = _cairo_format_bits_per_pixel (format);
    if ((unsigned) (width) >= (INT32_MAX - 7) / (unsigned) (bpp))
	return -1;

    return CAIRO_STRIDE_FOR_WIDTH_BPP (width, bpp);
}

/**
 * cairo_image_surface_create_for_data:
 * @data: a pointer to a buffer supplied by the application in which
 *     to write contents. This pointer must be suitably aligned for any
 *     kind of variable, (for example, a pointer returned by malloc).
 * @format: the format of pixels in the buffer
 * @width: the width of the image to be stored in the buffer
 * @height: the height of the image to be stored in the buffer
 * @stride: the number of bytes between the start of rows in the
 *     buffer as allocated. This value should always be computed by
 *     cairo_format_stride_for_width() before allocating the data
 *     buffer.
 *
 * Creates an image surface for the provided pixel data. The output
 * buffer must be kept around until the #cairo_surface_t is destroyed
 * or cairo_surface_finish() is called on the surface.  The initial
 * contents of @data will be used as the initial image contents; you
 * must explicitly clear the buffer, using, for example,
 * cairo_rectangle() and cairo_fill() if you want it cleared.
 *
 * Note that the stride may be larger than
 * width*bytes_per_pixel to provide proper alignment for each pixel
 * and row. This alignment is required to allow high-performance rendering
 * within cairo. The correct way to obtain a legal stride value is to
 * call cairo_format_stride_for_width() with the desired format and
 * maximum image width value, and then use the resulting stride value
 * to allocate the data and to create the image surface. See
 * cairo_format_stride_for_width() for example code.
 *
 * Return value: a pointer to the newly created surface. The caller
 * owns the surface and should call cairo_surface_destroy() when done
 * with it.
 *
 * This function always returns a valid pointer, but it will return a
 * pointer to a "nil" surface in the case of an error such as out of
 * memory or an invalid stride value. In case of invalid stride value
 * the error status of the returned surface will be
 * %CAIRO_STATUS_INVALID_STRIDE.  You can use
 * cairo_surface_status() to check for this.
 *
 * See cairo_surface_set_user_data() for a means of attaching a
 * destroy-notification fallback to the surface if necessary.
 **/
cairo_surface_t *
cairo_image_surface_create_for_data (unsigned char     *data,
				     cairo_format_t	format,
				     int		width,
				     int		height,
				     int		stride)
{
    pixman_format_code_t pixman_format;
    int minstride;

    if (! CAIRO_FORMAT_VALID (format))
	return _cairo_surface_create_in_error (_cairo_error (CAIRO_STATUS_INVALID_FORMAT));

    if ((stride & (CAIRO_STRIDE_ALIGNMENT-1)) != 0)
	return _cairo_surface_create_in_error (_cairo_error (CAIRO_STATUS_INVALID_STRIDE));

    if (! _cairo_image_surface_is_size_valid (width, height))
	return _cairo_surface_create_in_error (_cairo_error (CAIRO_STATUS_INVALID_SIZE));

    minstride = cairo_format_stride_for_width (format, width);
    if (stride < 0) {
	if (stride > -minstride) {
	    return _cairo_surface_create_in_error (_cairo_error (CAIRO_STATUS_INVALID_STRIDE));
	}
    } else {
	if (stride < minstride) {
	    return _cairo_surface_create_in_error (_cairo_error (CAIRO_STATUS_INVALID_STRIDE));
	}
    }

    pixman_format = _cairo_format_to_pixman_format_code (format);
    return _cairo_image_surface_create_with_pixman_format (data,
							   pixman_format,
							   width, height,
							   stride);
}

//slim_hidden_def (cairo_image_surface_create);

cairo_format_t
_cairo_format_from_content (cairo_content_t content)
{
    switch (content) {
    case CAIRO_CONTENT_COLOR:
	return CAIRO_FORMAT_RGB24;
    case CAIRO_CONTENT_ALPHA:
	return CAIRO_FORMAT_A8;
    case CAIRO_CONTENT_COLOR_ALPHA:
	return CAIRO_FORMAT_ARGB32;
    }

    ASSERT_NOT_REACHED;
    return CAIRO_FORMAT_INVALID;
}

/* A convenience function for when one needs to coerce an image
 * surface to an alternate format. */
cairo_image_surface_t *
_cairo_image_surface_coerce (cairo_image_surface_t *surface)
{
    return _cairo_image_surface_coerce_to_format (surface,
		                                  _cairo_format_from_content (surface->base.content));
        
}

/* A convenience function for when one needs to coerce an image
 * surface to an alternate format. */
cairo_image_surface_t *
_cairo_image_surface_coerce_to_format (cairo_image_surface_t *surface,
			               cairo_format_t	      format)
{
    cairo_image_surface_t *clone;
    cairo_status_t status;

    status = surface->base.status;
    if (unlikely (status))
	return (cairo_image_surface_t *)_cairo_surface_create_in_error (status);

    if (surface->format == format)
	return (cairo_image_surface_t *)cairo_surface_reference(&surface->base);

    clone = (cairo_image_surface_t *)
	cairo_image_surface_create (format, surface->width, surface->height);
    if (unlikely (clone->base.status))
	return clone;

//    pixman_image_composite32 (PIXMAN_OP_SRC,
//                              surface->pixman_image, NULL, clone->pixman_image,
//                              0, 0,
//                              0, 0,
//                              0, 0,
//                              surface->width, surface->height);
    clone->base.is_clear = FALSE;

    clone->base.device_transform =
	surface->base.device_transform;
    clone->base.device_transform_inverse =
	surface->base.device_transform_inverse;

    return clone;
}
typedef struct {
    cairo_polygon_t		*polygon;
    cairo_fill_rule_t		 fill_rule;
    cairo_antialias_t		 antialias;
} composite_spans_info_t;
/* low level compositor */
typedef cairo_status_t
(*image_draw_func_t) (void				*closure,
		      pixman_image_t			*dst,
		      pixman_format_code_t		 dst_format,
		      cairo_operator_t			 op,
		      const cairo_pattern_t		*src,
		      int				 dst_x,
		      int				 dst_y,
		      const cairo_rectangle_int_t	*extents,
		      cairo_region_t			*clip_region);


static cairo_bool_t
can_reduce_alpha_op (cairo_operator_t op)
{
    int iop = op;
    switch (iop) {
    case CAIRO_OPERATOR_OVER:
    case CAIRO_OPERATOR_SOURCE:
    case CAIRO_OPERATOR_ADD:
	return TRUE;
    default:
	return FALSE;
    }
}
static cairo_bool_t
reduce_alpha_op (cairo_image_surface_t *dst,
		 cairo_operator_t op,
		 const cairo_pattern_t *pattern)
{
    return dst->base.is_clear &&
	   dst->base.content == CAIRO_CONTENT_ALPHA &&
	   _cairo_pattern_is_opaque_solid (pattern) &&
	   can_reduce_alpha_op (op);
}

static cairo_status_t
_clip_and_composite (cairo_image_surface_t	*dst,
		     cairo_operator_t		 op,
		     const cairo_pattern_t	*src,
		     image_draw_func_t		 draw_func,
		     void			*draw_closure,
		     cairo_composite_rectangles_t*extents,
		     cairo_clip_t		*clip)
{
    cairo_status_t status;
    cairo_region_t *clip_region = NULL;
    cairo_bool_t need_clip_surface = FALSE;

    if (clip != NULL) {
        printf("should not be here in _clip_and_composite\n");
//	status = _cairo_clip_get_region (clip, &clip_region);
//	if (unlikely (status == CAIRO_INT_STATUS_NOTHING_TO_DO))
//	    return CAIRO_STATUS_SUCCESS;
////	if (unlikely (_cairo_status_is_error (status)))
////	    return status;
//
//	need_clip_surface = status == CAIRO_INT_STATUS_UNSUPPORTED;
//
//	if (clip_region != NULL) {
//	    cairo_rectangle_int_t rect;
//	    cairo_bool_t is_empty;
//
//	    cairo_region_get_extents (clip_region, &rect);
//	    is_empty = ! _cairo_rectangle_intersect (&extents->unbounded, &rect);
//	    if (unlikely (is_empty))
//		return CAIRO_STATUS_SUCCESS;
//
//	    is_empty = ! _cairo_rectangle_intersect (&extents->bounded, &rect);
//	    if (unlikely (is_empty && extents->is_bounded))
//		return CAIRO_STATUS_SUCCESS;
//
//	    if (cairo_region_num_rectangles (clip_region) == 1)
//		clip_region = NULL;
//	}
    }

    if (clip_region != NULL) {
        printf("should not be here in _clip_and_composite\n");
//	status = _cairo_image_surface_set_clip_region (dst, clip_region);
//	if (unlikely (status))
//	    return status;
    }

    if (reduce_alpha_op (dst, op, src)) {
	op = CAIRO_OPERATOR_ADD;
	src = NULL;
    }

    if (op == CAIRO_OPERATOR_SOURCE) {
        printf("should not be here in _clip_and_composite\n");
//	status = _clip_and_composite_source (clip, src,
//					     draw_func, draw_closure,
//					     dst, &extents->bounded);
    } else {
	if (op == CAIRO_OPERATOR_CLEAR) {
	    src = NULL;
	    op = CAIRO_OPERATOR_DEST_OUT;
	}

	if (need_clip_surface) {
        printf("should not be here in _clip_and_composite\n");
//	    if (extents->is_bounded) {
//		status = _clip_and_composite_with_mask (clip, op, src,
//							draw_func, draw_closure,
//							dst, &extents->bounded);
//	    } else {
//		status = _clip_and_composite_combine (clip, op, src,
//						      draw_func, draw_closure,
//						      dst, &extents->bounded);
//	    }
	} else {
	    status = draw_func (draw_closure,
				dst->pixman_image, dst->pixman_format,
				op, src,
				0, 0,
				&extents->bounded,
				clip_region);
	}
    }

    if (status == CAIRO_STATUS_SUCCESS && ! extents->is_bounded) {
        printf("should not be here in _clip_and_composite\n");
//	status = _cairo_image_surface_fixup_unbounded (dst, extents,
//						       need_clip_surface ? clip : NULL);
    }

    if (clip_region != NULL)
        printf("should not be here in _clip_and_composite\n");
//	_cairo_image_surface_unset_clip_region (dst);

    return status;
}


typedef struct _cairo_image_surface_span_renderer {
    cairo_span_renderer_t base;

    uint8_t *mask_data;
    uint32_t mask_stride;
} cairo_image_surface_span_renderer_t;

static cairo_status_t
_cairo_image_surface_span (void *abstract_renderer,
			   int y, int height,
			   const cairo_half_open_span_t *spans,
			   unsigned num_spans)
{
    cairo_image_surface_span_renderer_t *renderer = abstract_renderer;
    uint8_t *row;
    unsigned i;

    if (num_spans == 0)
	return CAIRO_STATUS_SUCCESS;

    /* XXX will it be quicker to repeat the sparse memset,
     * or perform a simpler memcpy?
     * The fairly dense spiral benchmarks suggests that the sparse
     * memset is a win there as well.
     */
    row = renderer->mask_data + y * renderer->mask_stride;
    do {
	for (i = 0; i < num_spans - 1; i++) {
	    if (! spans[i].coverage)
		continue;

	    /* We implement setting rendering the most common single
	     * pixel wide span case to avoid the overhead of a memset
	     * call.  Open coding setting longer spans didn't show a
	     * noticeable improvement over memset. */
	    if (spans[i+1].x == spans[i].x + 1) {
		row[spans[i].x] = spans[i].coverage;
	    } else {
		memset (row + spans[i].x,
			spans[i].coverage,
			spans[i+1].x - spans[i].x);
	    }
	}
	row += renderer->mask_stride;
    } while (--height);

    return CAIRO_STATUS_SUCCESS;
}

#if HAS_ATOMIC_OPS
static pixman_image_t *__pixman_transparent_image;
static pixman_image_t *__pixman_black_image;
static pixman_image_t *__pixman_white_image;

static pixman_image_t *
_pixman_transparent_image (void)
{
    pixman_image_t *image;

    image = __pixman_transparent_image;
    if (unlikely (image == NULL)) {
	pixman_color_t color;

	color.red   = 0x00;
	color.green = 0x00;
	color.blue  = 0x00;
	color.alpha = 0x00;

	image = pixman_image_create_solid_fill (&color);
	if (unlikely (image == NULL))
	    return NULL;

//	if (_cairo_atomic_ptr_cmpxchg (&__pixman_transparent_image,
//				       NULL, image))
//	{
//	    pixman_image_ref (image);
//	}
//    HURTYOU
    } else {
	pixman_image_ref (image);
    }

    return image;
}

static pixman_image_t *
_pixman_black_image (void)
{
    pixman_image_t *image;

    image = __pixman_black_image;
    if (unlikely (image == NULL)) {
	pixman_color_t color;

	color.red   = 0x00;
	color.green = 0x00;
	color.blue  = 0x00;
	color.alpha = 0xffff;

	image = pixman_image_create_solid_fill (&color);
	if (unlikely (image == NULL))
	    return NULL;

//	if (_cairo_atomic_ptr_cmpxchg (&__pixman_black_image,
//				       NULL, image))
//	{
//	    pixman_image_ref (image);
//	}
//	HURTYOU
    } else {
	pixman_image_ref (image);
    }

    return image;
}

static pixman_image_t *
_pixman_white_image (void)
{
    pixman_image_t *image;

    image = __pixman_white_image;
    if (unlikely (image == NULL)) {
	pixman_color_t color;

	color.red   = 0xffff;
	color.green = 0xffff;
	color.blue  = 0xffff;
	color.alpha = 0xffff;

	image = pixman_image_create_solid_fill (&color);
	if (unlikely (image == NULL))
	    return NULL;

//	if (_cairo_atomic_ptr_cmpxchg (&__pixman_white_image,
//				       NULL, image))
//	{
//	    pixman_image_ref (image);
//	}
//	HURTYOU
    } else {
	pixman_image_ref (image);
    }

    return image;
}
#else
static pixman_image_t *
_pixman_transparent_image (void)
{
    return _pixman_image_for_solid (&_cairo_pattern_clear);
}
static pixman_image_t *
_pixman_black_image (void)
{
    return _pixman_image_for_solid (&_cairo_pattern_black);
}
static pixman_image_t *
_pixman_white_image (void)
{
    return _pixman_image_for_solid (&_cairo_pattern_white);
}
#endif
static uint32_t
hars_petruska_f54_1_random (void)
{
#define rol(x,k) ((x << k) | (x >> (32-k)))
    static uint32_t x;
    return x = (x ^ rol (x, 5) ^ rol (x, 24)) + 0x37798849;
#undef rol
}
static struct {
    cairo_color_t color;
    pixman_image_t *image;
} cache[16];
static int n_cached;

static pixman_image_t *
_pixman_image_for_solid (const cairo_solid_pattern_t *pattern)
{
    pixman_color_t color;
    pixman_image_t *image;
//    int i;

#if HAS_ATOMIC_OPS
    if (pattern->color.alpha_short <= 0x00ff)
	return _pixman_transparent_image ();

    if (pattern->color.alpha_short >= 0xff00) {
	if (pattern->color.red_short <= 0x00ff &&
	    pattern->color.green_short <= 0x00ff &&
	    pattern->color.blue_short <= 0x00ff)
	{
	    return _pixman_black_image ();
	}

	if (pattern->color.red_short >= 0xff00 &&
	    pattern->color.green_short >= 0xff00 &&
	    pattern->color.blue_short >= 0xff00)
	{
	    return _pixman_white_image ();
	}
    }
#endif
//    printf("I'm here, Error!\n");

//    CAIRO_MUTEX_LOCK (_cairo_image_solid_cache_mutex);
//    for (i = 0; i < n_cached; i++) {
//	if (_cairo_color_equal (&cache[i].color, &pattern->color)) {
//	    image = pixman_image_ref (cache[i].image);
//	    goto UNLOCK;
//	}
//    }
//
    color.red   = pattern->color.red_short;
    color.green = pattern->color.green_short;
    color.blue  = pattern->color.blue_short;
    color.alpha = pattern->color.alpha_short;

    image = pixman_image_create_solid_fill (&color);
//    if (image == NULL)
//	goto UNLOCK;
//
//    if (n_cached < ARRAY_LENGTH (cache)) {
//	i = n_cached++;
//    } else {
//	i = hars_petruska_f54_1_random () % ARRAY_LENGTH (cache);
//	pixman_image_unref (cache[i].image);
//    }
//    cache[i].image = pixman_image_ref (image);
//    cache[i].color = pattern->color;
//
//UNLOCK:
//    CAIRO_MUTEX_UNLOCK (_cairo_image_solid_cache_mutex);
    return image;
}


static pixman_image_t *
_pixman_image_for_pattern (const cairo_pattern_t *pattern,
			   cairo_bool_t is_mask,
			   const cairo_rectangle_int_t *extents,
			   int *tx, int *ty)
{
    *tx = *ty = 0;

    if (pattern == NULL)
	return _pixman_white_image ();

    switch (pattern->type) {
    default:
	ASSERT_NOT_REACHED;
    case CAIRO_PATTERN_TYPE_SOLID:
	return _pixman_image_for_solid ((const cairo_solid_pattern_t *) pattern);

//    case CAIRO_PATTERN_TYPE_RADIAL:
//    case CAIRO_PATTERN_TYPE_LINEAR:
////	return _pixman_image_for_gradient ((const cairo_gradient_pattern_t *) pattern,
//					   extents, tx, ty);
//
//    case CAIRO_PATTERN_TYPE_SURFACE:
//	return _pixman_image_for_surface ((const cairo_surface_pattern_t *) pattern,
//					  is_mask, extents, tx, ty);
//					  HURTYOU
    }
}

static cairo_status_t
_composite_spans (void                          *closure,
		  pixman_image_t		*dst,
		  pixman_format_code_t		 dst_format,
		  cairo_operator_t               op,
		  const cairo_pattern_t         *pattern,
		  int                            dst_x,
		  int                            dst_y,
		  const cairo_rectangle_int_t   *extents,
		  cairo_region_t		*clip_region)
{
    uint8_t mask_buf[CAIRO_STACK_BUFFER_SIZE];
    composite_spans_info_t *info = closure;
    cairo_image_surface_span_renderer_t renderer;
#if USE_BOTOR_SCAN_CONVERTER
    cairo_box_t box;
    cairo_botor_scan_converter_t converter;
#else
    cairo_scan_converter_t *converter;
#endif
    pixman_image_t *mask;
    cairo_status_t status;

#if USE_BOTOR_SCAN_CONVERTER
    box.p1.x = _cairo_fixed_from_int (extents->x);
    box.p1.y = _cairo_fixed_from_int (extents->y);
    box.p2.x = _cairo_fixed_from_int (extents->x + extents->width);
    box.p2.y = _cairo_fixed_from_int (extents->y + extents->height);
    _cairo_botor_scan_converter_init (&converter, &box, info->fill_rule);
    status = converter.base.add_polygon (&converter.base, info->polygon);
#else
    converter = _cairo_tor_scan_converter_create (extents->x, extents->y,
						  extents->x + extents->width,
						  extents->y + extents->height,
						  info->fill_rule);
    status = converter->add_polygon (converter, info->polygon);
#endif
    if (unlikely (status))
	goto CLEANUP_CONVERTER;

    /* TODO: support rendering to A1 surfaces (or: go add span
     * compositing to pixman.) */

    if (pattern == NULL &&
	dst_format == PIXMAN_a8 &&
	op == CAIRO_OPERATOR_SOURCE)
    {
	mask = dst;
	dst = NULL;
    }
    else
    {
	int stride = CAIRO_STRIDE_FOR_WIDTH_BPP (extents->width, 8);
	uint8_t *data = mask_buf;

	if (extents->height * stride <= (int) sizeof (mask_buf))
	    memset (data, 0, extents->height * stride);
	else
	    data = NULL, stride = 0;

	mask = pixman_image_create_bits (PIXMAN_a8,
					 extents->width,
					 extents->height,
					 (uint32_t *) data,
					 stride);
	if (unlikely (mask == NULL)) {
	    status = _cairo_error (CAIRO_STATUS_NO_MEMORY);
	    goto CLEANUP_CONVERTER;
	}
    }

    renderer.base.render_rows = _cairo_image_surface_span;
    renderer.mask_stride = pixman_image_get_stride (mask);
    renderer.mask_data = (uint8_t *) pixman_image_get_data (mask);
    if (dst != NULL)
	renderer.mask_data -= extents->y * renderer.mask_stride + extents->x;
    else
	renderer.mask_data -= dst_y * renderer.mask_stride + dst_x;

#if USE_BOTOR_SCAN_CONVERTER
    status = converter.base.generate (&converter.base, &renderer.base);
#else
    status = converter->generate (converter, &renderer.base);
#endif
    if (unlikely (status))
	goto CLEANUP_RENDERER;

    if (dst != NULL) {
	pixman_image_t *src;
	int src_x, src_y;

	src = _pixman_image_for_pattern (pattern, FALSE, extents, &src_x, &src_y);
	if (unlikely (src == NULL)) {
	    status = _cairo_error (CAIRO_STATUS_NO_MEMORY);
	    goto CLEANUP_RENDERER;
	}

	pixman_image_composite32 (_pixman_operator (op), src, mask, dst,
                                  extents->x + src_x, extents->y + src_y,
                                  0, 0, /* mask.x, mask.y */
                                  extents->x - dst_x, extents->y - dst_y,
                                  extents->width, extents->height);
	pixman_image_unref (src);
    }

 CLEANUP_RENDERER:
    if (dst != NULL)
	pixman_image_unref (mask);
 CLEANUP_CONVERTER:
#if USE_BOTOR_SCAN_CONVERTER
    converter.base.destroy (&converter.base);
#else
    converter->destroy (converter);
#endif
    return status;
}

static cairo_status_t
_clip_and_composite_polygon (cairo_image_surface_t *dst,
			     cairo_operator_t op,
			     const cairo_pattern_t *src,
			     cairo_polygon_t *polygon,
			     cairo_fill_rule_t fill_rule,
			     cairo_antialias_t antialias,
			     cairo_composite_rectangles_t *extents,
			     cairo_clip_t *clip)
{
    cairo_status_t status;

    if (polygon->num_edges == 0) {
//	cairo_traps_t traps;
//
//	if (extents->is_bounded)
//	    return CAIRO_STATUS_SUCCESS;
//
//	_cairo_traps_init (&traps);
//	status = _clip_and_composite_trapezoids (dst, op, src,
//						 &traps, antialias,
//						 extents, clip);
//	_cairo_traps_fini (&traps);
//
//	return status;
    printf("should not be here in _clip_and_composite_polygon!\n");
    return CAIRO_STATUS_SUCCESS;
    }

    _cairo_box_round_to_rectangle (&polygon->extents, &extents->mask);
    if (! _cairo_rectangle_intersect (&extents->bounded, &extents->mask))
	return CAIRO_STATUS_SUCCESS;

    if (antialias != CAIRO_ANTIALIAS_NONE) {
	composite_spans_info_t info;

	info.polygon = polygon;
	info.fill_rule = fill_rule;
	info.antialias = antialias;

	status = _clip_and_composite (dst, op, src,
				      _composite_spans, &info,
				      extents, clip);
    } else {
    printf("should not be here in _clip_and_composite_polygon!\n");
//	cairo_traps_t traps;
//
//	_cairo_traps_init (&traps);
//
//	/* Fall back to trapezoid fills. */
//	status = _cairo_bentley_ottmann_tessellate_polygon (&traps,
//							    polygon,
//							    fill_rule);
//	if (likely (status == CAIRO_STATUS_SUCCESS)) {
//	    status = _clip_and_composite_trapezoids (dst, op, src,
//						     &traps, antialias,
//						     extents, clip);
//	}
//
//	_cairo_traps_fini (&traps);
    }

    return status;
}

typedef struct {
    cairo_trapezoid_t *traps;
    int num_traps;
    cairo_antialias_t antialias;
} composite_traps_info_t;

static inline uint32_t
color_to_uint32 (const cairo_color_t *color)
{
    return
        (color->alpha_short >> 8 << 24) |
        (color->red_short >> 8 << 16)   |
        (color->green_short & 0xff00)   |
        (color->blue_short >> 8);
}

static inline cairo_bool_t
color_to_pixel (const cairo_color_t	*color,
                pixman_format_code_t	 format,
                uint32_t		*pixel)
{
    uint32_t c;

    if (!(format == PIXMAN_a8r8g8b8     ||
          format == PIXMAN_x8r8g8b8     ||
          format == PIXMAN_a8b8g8r8     ||
          format == PIXMAN_x8b8g8r8     ||
          format == PIXMAN_b8g8r8a8     ||
          format == PIXMAN_b8g8r8x8     ||
          format == PIXMAN_r5g6b5       ||
          format == PIXMAN_b5g6r5       ||
          format == PIXMAN_a8))
    {
	return FALSE;
    }

    c = color_to_uint32 (color);

    if (PIXMAN_FORMAT_TYPE (format) == PIXMAN_TYPE_ABGR) {
	c = ((c & 0xff000000) >>  0) |
	    ((c & 0x00ff0000) >> 16) |
	    ((c & 0x0000ff00) >>  0) |
	    ((c & 0x000000ff) << 16);
    }

    if (PIXMAN_FORMAT_TYPE (format) == PIXMAN_TYPE_BGRA) {
	c = ((c & 0xff000000) >> 24) |
	    ((c & 0x00ff0000) >>  8) |
	    ((c & 0x0000ff00) <<  8) |
	    ((c & 0x000000ff) << 24);
    }

    if (format == PIXMAN_a8) {
	c = c >> 24;
    } else if (format == PIXMAN_r5g6b5 || format == PIXMAN_b5g6r5) {
	c = ((((c) >> 3) & 0x001f) |
	     (((c) >> 5) & 0x07e0) |
	     (((c) >> 8) & 0xf800));
    }

    *pixel = c;
    return TRUE;
}

static inline cairo_bool_t
pattern_to_pixel (const cairo_solid_pattern_t *solid,
		  cairo_operator_t op,
		  pixman_format_code_t format,
		  uint32_t *pixel)
{
    if (op == CAIRO_OPERATOR_CLEAR) {
	*pixel = 0;
	return TRUE;
    }

    if (solid->base.type != CAIRO_PATTERN_TYPE_SOLID)
	return FALSE;

    if (op == CAIRO_OPERATOR_OVER) {
	if (solid->color.alpha_short >= 0xff00)
	    op = CAIRO_OPERATOR_SOURCE;
    }

    if (op != CAIRO_OPERATOR_SOURCE)
	return FALSE;

    return color_to_pixel (&solid->color, format, pixel);
}
typedef struct _fill_span {
    cairo_span_renderer_t base;

    uint8_t *mask_data;
    pixman_image_t *src, *dst, *mask;
} fill_span_renderer_t;

static cairo_status_t
_fill_span (void *abstract_renderer,
	    int y, int height,
	    const cairo_half_open_span_t *spans,
	    unsigned num_spans)
{
    fill_span_renderer_t *renderer = abstract_renderer;
    uint8_t *row;
    unsigned i;

    if (num_spans == 0)
	return CAIRO_STATUS_SUCCESS;

    row = renderer->mask_data - spans[0].x;
    for (i = 0; i < num_spans - 1; i++) {
	/* We implement setting the most common single pixel wide
	 * span case to avoid the overhead of a memset call.
	 * Open coding setting longer spans didn't show a
	 * noticeable improvement over memset.
	 */
	if (spans[i+1].x == spans[i].x + 1) {
	    row[spans[i].x] = spans[i].coverage;
	} else {
	    memset (row + spans[i].x,
		    spans[i].coverage,
		    spans[i+1].x - spans[i].x);
	}
    }

    do {
	pixman_image_composite32 (PIXMAN_OP_OVER,
                                  renderer->src, renderer->mask, renderer->dst,
                                  0, 0, 0, 0,
                                  spans[0].x, y++,
                                  spans[i].x - spans[0].x, 1);
    } while (--height);

    return CAIRO_STATUS_SUCCESS;
}

/* avoid using region code to re-validate boxes */
static cairo_status_t
_fill_unaligned_boxes (cairo_image_surface_t *dst,
		       const cairo_pattern_t *pattern,
		       uint32_t pixel,
		       const cairo_boxes_t *boxes,
		       const cairo_composite_rectangles_t *extents)
{
    uint8_t buf[CAIRO_STACK_BUFFER_SIZE];
    fill_span_renderer_t renderer;
    cairo_rectangular_scan_converter_t converter;
    const struct _cairo_boxes_chunk *chunk;
    cairo_status_t status;
    int i;

    /* XXX
     * using composite for fill:
     *   spiral-box-nonalign-evenodd-fill.512    2201957    2.202
     *   spiral-box-nonalign-nonzero-fill.512     336726    0.337
     *   spiral-box-pixalign-evenodd-fill.512     352256    0.352
     *   spiral-box-pixalign-nonzero-fill.512     147056    0.147
     * using fill:
     *   spiral-box-nonalign-evenodd-fill.512    3174565    3.175
     *   spiral-box-nonalign-nonzero-fill.512     182710    0.183
     *   spiral-box-pixalign-evenodd-fill.512     353863    0.354
     *   spiral-box-pixalign-nonzero-fill.512     147402    0.147
     *
     * cairo-perf-trace seems to favour using fill.
     */

    renderer.base.render_rows = _fill_span;
    renderer.dst = dst->pixman_image;

    if ((unsigned) extents->bounded.width <= sizeof (buf)) {
	renderer.mask = pixman_image_create_bits (PIXMAN_a8,
						  extents->bounded.width, 1,
						  (uint32_t *) buf,
						  sizeof (buf));
    } else {
	renderer.mask = pixman_image_create_bits (PIXMAN_a8,
						  extents->bounded.width, 1,
						  NULL,  0);
    }
    if (unlikely (renderer.mask == NULL))
	return _cairo_error (CAIRO_STATUS_NO_MEMORY);

    renderer.mask_data = (uint8_t *) pixman_image_get_data (renderer.mask);

    renderer.src = _pixman_image_for_solid ((const cairo_solid_pattern_t *) pattern);
    if (unlikely (renderer.src == NULL)) {
	status = _cairo_error (CAIRO_STATUS_NO_MEMORY);
	goto CLEANUP_MASK;
    }

    _cairo_rectangular_scan_converter_init (&converter, &extents->bounded);

    /* first blit any aligned part of the boxes */
    for (chunk = &boxes->chunks; chunk != NULL; chunk = chunk->next) {
	const cairo_box_t *box = chunk->base;

	for (i = 0; i < chunk->count; i++) {
	    int x1 = _cairo_fixed_integer_ceil (box[i].p1.x);
	    int y1 = _cairo_fixed_integer_ceil (box[i].p1.y);
	    int x2 = _cairo_fixed_integer_floor (box[i].p2.x);
	    int y2 = _cairo_fixed_integer_floor (box[i].p2.y);

	    if (x2 > x1 && y2 > y1) {
		cairo_box_t b;

		pixman_fill ((uint32_t *) dst->data,
			     dst->stride / sizeof (uint32_t),
			     PIXMAN_FORMAT_BPP (dst->pixman_format),
			     x1, y1, x2 - x1, y2 - y1,
			     pixel);

		/*
		 * Corners have to be included only once if the rects
		 * are passed to the rectangular scan converter
		 * because it can only handle disjoint rectangles.
		*/

		/* top (including top-left and top-right corners) */
		if (! _cairo_fixed_is_integer (box[i].p1.y)) {
		    b.p1.x = box[i].p1.x;
		    b.p1.y = box[i].p1.y;
		    b.p2.x = box[i].p2.x;
		    b.p2.y = _cairo_fixed_from_int (y1);

		    status = _cairo_rectangular_scan_converter_add_box (&converter, &b, 1);
		    if (unlikely (status))
			goto CLEANUP_CONVERTER;
		}

		/* left (no corners) */
		if (! _cairo_fixed_is_integer (box[i].p1.x)) {
		    b.p1.x = box[i].p1.x;
		    b.p1.y = _cairo_fixed_from_int (y1);
		    b.p2.x = _cairo_fixed_from_int (x1);
		    b.p2.y = _cairo_fixed_from_int (y2);

		    status = _cairo_rectangular_scan_converter_add_box (&converter, &b, 1);
		    if (unlikely (status))
			goto CLEANUP_CONVERTER;
		}

		/* right (no corners) */
		if (! _cairo_fixed_is_integer (box[i].p2.x)) {
		    b.p1.x = _cairo_fixed_from_int (x2);
		    b.p1.y = _cairo_fixed_from_int (y1);
		    b.p2.x = box[i].p2.x;
		    b.p2.y = _cairo_fixed_from_int (y2);

		    status = _cairo_rectangular_scan_converter_add_box (&converter, &b, 1);
		    if (unlikely (status))
			goto CLEANUP_CONVERTER;
		}

		/* bottom (including bottom-left and bottom-right corners) */
		if (! _cairo_fixed_is_integer (box[i].p2.y)) {
		    b.p1.x = box[i].p1.x;
		    b.p1.y = _cairo_fixed_from_int (y2);
		    b.p2.x = box[i].p2.x;
		    b.p2.y = box[i].p2.y;

		    status = _cairo_rectangular_scan_converter_add_box (&converter, &b, 1);
		    if (unlikely (status))
			goto CLEANUP_CONVERTER;
		}
	    } else {
		status = _cairo_rectangular_scan_converter_add_box (&converter, &box[i], 1);
		if (unlikely (status))
		    goto CLEANUP_CONVERTER;
	    }
	}
    }

    status = converter.base.generate (&converter.base, &renderer.base);

  CLEANUP_CONVERTER:
    converter.base.destroy (&converter.base);
    pixman_image_unref (renderer.src);
  CLEANUP_MASK:
    pixman_image_unref (renderer.mask);

    return status;
}
static cairo_status_t
_composite_unaligned_boxes (cairo_image_surface_t *dst,
			    cairo_operator_t op,
			    const cairo_pattern_t *pattern,
			    const cairo_boxes_t *boxes,
			    const cairo_composite_rectangles_t *extents)
{
    uint8_t buf[CAIRO_STACK_BUFFER_SIZE];
    cairo_image_surface_span_renderer_t renderer;
    cairo_rectangular_scan_converter_t converter;
    pixman_image_t *mask, *src;
    cairo_status_t status;
    const struct _cairo_boxes_chunk *chunk;
    int i, src_x, src_y;

    i = CAIRO_STRIDE_FOR_WIDTH_BPP (extents->bounded.width, 8) * extents->bounded.height;
    if ((unsigned) i <= sizeof (buf)) {
	mask = pixman_image_create_bits (PIXMAN_a8,
					 extents->bounded.width,
					 extents->bounded.height,
					 (uint32_t *) buf,
					 CAIRO_STRIDE_FOR_WIDTH_BPP (extents->bounded.width, 8));
	memset (buf, 0, i);
    } else {
	mask = pixman_image_create_bits (PIXMAN_a8,
					 extents->bounded.width,
					 extents->bounded.height,
					 NULL,  0);
    }
    if (unlikely (mask == NULL))
	return _cairo_error (CAIRO_STATUS_NO_MEMORY);

    renderer.base.render_rows = _cairo_image_surface_span;
    renderer.mask_stride = pixman_image_get_stride (mask);
    renderer.mask_data = (uint8_t *) pixman_image_get_data (mask);
    renderer.mask_data -= extents->bounded.y * renderer.mask_stride + extents->bounded.x;

    _cairo_rectangular_scan_converter_init (&converter, &extents->bounded);

    for (chunk = &boxes->chunks; chunk != NULL; chunk = chunk->next) {
	const cairo_box_t *box = chunk->base;

	for (i = 0; i < chunk->count; i++) {
	    status = _cairo_rectangular_scan_converter_add_box (&converter, &box[i], 1);
	    if (unlikely (status))
		goto CLEANUP;
	}
    }

    status = converter.base.generate (&converter.base, &renderer.base);
    if (unlikely (status))
	goto CLEANUP;

    src = _pixman_image_for_pattern (pattern, FALSE, &extents->bounded, &src_x, &src_y);
    if (unlikely (src == NULL)) {
	status =  _cairo_error (CAIRO_STATUS_NO_MEMORY);
	goto CLEANUP;
    }

    pixman_image_composite32 (_pixman_operator (op),
                              src, mask, dst->pixman_image,
                              extents->bounded.x + src_x, extents->bounded.y + src_y,
                              0, 0,
                              extents->bounded.x, extents->bounded.y,
                              extents->bounded.width, extents->bounded.height);
    pixman_image_unref (src);

  CLEANUP:
    converter.base.destroy (&converter.base);
    pixman_image_unref (mask);

    return status;
}

static cairo_status_t
_composite_boxes (cairo_image_surface_t *dst,
		  cairo_operator_t op,
		  const cairo_pattern_t *pattern,
		  cairo_boxes_t *boxes,
		  cairo_antialias_t antialias,
		  cairo_clip_t *clip,
		  const cairo_composite_rectangles_t *extents)
{
    cairo_region_t *clip_region = NULL;
    cairo_bool_t need_clip_mask = FALSE;
    cairo_status_t status;
    struct _cairo_boxes_chunk *chunk;
    uint32_t pixel;
    int i;

    if (clip != NULL) {
	status = _cairo_clip_get_region (clip, &clip_region);
	need_clip_mask = status == CAIRO_INT_STATUS_UNSUPPORTED;
	if (need_clip_mask &&
	    (op == CAIRO_OPERATOR_SOURCE || ! extents->is_bounded))
	{
	    return CAIRO_INT_STATUS_UNSUPPORTED;
	}

	if (clip_region != NULL && cairo_region_num_rectangles (clip_region) == 1)
	    clip_region = NULL;
    }

    if (antialias != CAIRO_ANTIALIAS_NONE) {
	if (! boxes->is_pixel_aligned) {
	    if (need_clip_mask)
		return CAIRO_INT_STATUS_UNSUPPORTED;

	    if (pattern_to_pixel ((cairo_solid_pattern_t *) pattern, op,
				  dst->pixman_format, &pixel))
	    {
		return _fill_unaligned_boxes (dst, pattern, pixel, boxes, extents);
	    }
	    else
	    {
		return _composite_unaligned_boxes (dst, op, pattern, boxes, extents);
	    }
	}
    }

    status = CAIRO_STATUS_SUCCESS;
    if (! need_clip_mask &&
	pattern_to_pixel ((cairo_solid_pattern_t *) pattern, op, dst->pixman_format,
			  &pixel))
    {
	for (chunk = &boxes->chunks; chunk != NULL; chunk = chunk->next) {
	    cairo_box_t *box = chunk->base;

	    for (i = 0; i < chunk->count; i++) {
		int x1 = _cairo_fixed_integer_round_down (box[i].p1.x);
		int y1 = _cairo_fixed_integer_round_down (box[i].p1.y);
		int x2 = _cairo_fixed_integer_round_down (box[i].p2.x);
		int y2 = _cairo_fixed_integer_round_down (box[i].p2.y);

		if (x2 == x1 || y2 == y1)
		    continue;

		pixman_fill ((uint32_t *) dst->data, dst->stride / sizeof (uint32_t),
			     PIXMAN_FORMAT_BPP (dst->pixman_format),
			     x1, y1, x2 - x1, y2 - y1,
			     pixel);
	    }
	}
    }
    else
    {
	pixman_image_t *src = NULL, *mask = NULL;
	int src_x, src_y, mask_x = 0, mask_y = 0;
	pixman_op_t pixman_op = _pixman_operator (op);

	if (need_clip_mask) {
//	    cairo_surface_t *clip_surface;
//	    int clip_x, clip_y;
//
//	    clip_surface = _cairo_clip_get_surface (clip, &dst->base, &clip_x, &clip_y);
//	    if (unlikely (clip_surface->status))
//		return clip_surface->status;
//
//	    mask_x = -clip_x;
//	    mask_y = -clip_y;
//
//	    if (op == CAIRO_OPERATOR_CLEAR) {
//		pattern = NULL;
//		pixman_op = PIXMAN_OP_OUT_REVERSE;
//	    }
//
//	    mask = ((cairo_image_surface_t *) clip_surface)->pixman_image;
//	    HURTYOU
    printf("Should not be here:need_clip_mask\n");
    ;
	}

	if (pattern != NULL) {
	    src = _pixman_image_for_pattern (pattern, FALSE, &extents->bounded, &src_x, &src_y);
	    if (unlikely (src == NULL))
		return _cairo_error (CAIRO_STATUS_NO_MEMORY);
	} else {
	    src = mask;
	    src_x = mask_x;
	    src_y = mask_y;
	    mask = NULL;
	}

	for (chunk = &boxes->chunks; chunk != NULL; chunk = chunk->next) {
	    const cairo_box_t *box = chunk->base;

	    for (i = 0; i < chunk->count; i++) {
		int x1 = _cairo_fixed_integer_round_down (box[i].p1.x);
		int y1 = _cairo_fixed_integer_round_down (box[i].p1.y);
		int x2 = _cairo_fixed_integer_round_down (box[i].p2.x);
		int y2 = _cairo_fixed_integer_round_down (box[i].p2.y);

		if (x2 == x1 || y2 == y1)
		    continue;

		pixman_image_composite32 (pixman_op,
                                          src, mask, dst->pixman_image,
                                          x1 + src_x,  y1 + src_y,
                                          x1 + mask_x, y1 + mask_y,
                                          x1, y1,
                                          x2 - x1, y2 - y1);
	    }
	}

	if (pattern != NULL)
	    pixman_image_unref (src);

	if (! extents->is_bounded) {
//	    status =
//		_cairo_image_surface_fixup_unbounded_boxes (dst, extents,
//							    clip_region, boxes);
//							    HURTYOU
    printf("Should not be here:! extents->is_bounded \n");							    
	}
    }

    return status;
}

static cairo_status_t
_clip_and_composite_boxes (cairo_image_surface_t *dst,
			   cairo_operator_t op,
			   const cairo_pattern_t *src,
			   cairo_boxes_t *boxes,
			   cairo_antialias_t antialias,
			   cairo_composite_rectangles_t *extents,
			   cairo_clip_t *clip)
{
    cairo_traps_t traps;
    cairo_status_t status;
    composite_traps_info_t info;

    if (boxes->num_boxes == 0 && extents->is_bounded)
	return CAIRO_STATUS_SUCCESS;

    /* Use a fast path if the boxes are pixel aligned */
    status = _composite_boxes (dst, op, src, boxes, antialias, clip, extents);
    if (status != CAIRO_INT_STATUS_UNSUPPORTED)
	return status;

    printf("Should not be here:status != CAIRO_INT_STATUS_UNSUPPORTED!\n");
//    /* Otherwise render via a mask and composite in the usual fashion.  */
//    status = _cairo_traps_init_boxes (&traps, boxes);
//    if (unlikely (status))
//	return status;
//
//    info.num_traps = traps.num_traps;
//    info.traps = traps.traps;
//    info.antialias = antialias;
//    status = _clip_and_composite (dst, op, src,
//				  _composite_traps, &info,
//				  extents, clip);
//
//    _cairo_traps_fini (&traps);
    return status;
}

static cairo_int_status_t
_cairo_image_surface_stroke (void			*abstract_surface,
			     cairo_operator_t		 op,
			     const cairo_pattern_t	*source,
			     cairo_path_fixed_t		*path,
			     const cairo_stroke_style_t	*style,
			     const cairo_matrix_t	*ctm,
			     const cairo_matrix_t	*ctm_inverse,
			     double			 tolerance,
			     cairo_antialias_t		 antialias,
			     cairo_clip_t		*clip)
{
    cairo_image_surface_t *surface = abstract_surface;
    cairo_composite_rectangles_t extents;
    cairo_box_t boxes_stack[32], *clip_boxes = boxes_stack;
    int num_boxes = ARRAY_LENGTH (boxes_stack);
    cairo_clip_t local_clip;
    cairo_bool_t have_clip = FALSE;
    cairo_status_t status;

    status = _cairo_composite_rectangles_init_for_stroke (&extents,
							  surface->width,
							  surface->height,
							  op, source,
							  path, style, ctm,
							  clip);
    if (unlikely (status))
	return status;

    if (_cairo_clip_contains_extents (clip, &extents))
	clip = NULL;

    if (clip != NULL) {
	clip = _cairo_clip_init_copy (&local_clip, clip);
	have_clip = TRUE;
    }

    status = _cairo_clip_to_boxes (&clip, &extents, &clip_boxes, &num_boxes);
    if (unlikely (status)) {
	if (have_clip)
	    _cairo_clip_fini (&local_clip);

	return status;
    }

    status = CAIRO_INT_STATUS_UNSUPPORTED;
    if (path->is_rectilinear) {
	cairo_boxes_t boxes;

	_cairo_boxes_init (&boxes);
	_cairo_boxes_limit (&boxes, clip_boxes, num_boxes);

	status = _cairo_path_fixed_stroke_rectilinear_to_boxes (path,
								style,
								ctm,
								&boxes);
	if (likely (status == CAIRO_STATUS_SUCCESS)) {
	    status = _clip_and_composite_boxes (surface, op, source,
						&boxes, antialias,
						&extents, clip);
	}

	_cairo_boxes_fini (&boxes);
    }

    if (status == CAIRO_INT_STATUS_UNSUPPORTED) {
	cairo_polygon_t polygon;

	_cairo_polygon_init (&polygon);
	_cairo_polygon_limit (&polygon, clip_boxes, num_boxes);

	status = _cairo_path_fixed_stroke_to_polygon (path,
						      style,
						      ctm, ctm_inverse,
						      tolerance,
						      &polygon);
	if (likely (status == CAIRO_STATUS_SUCCESS)) {
	    status = _clip_and_composite_polygon (surface, op, source, &polygon,
						  CAIRO_FILL_RULE_WINDING, antialias,
						  &extents, clip);
	}

	_cairo_polygon_fini (&polygon);
    }

    if (clip_boxes != boxes_stack)
	free (clip_boxes);

    if (have_clip)
	_cairo_clip_fini (&local_clip);

    return status;
}

static cairo_status_t
_cairo_image_surface_acquire_source_image (void                    *abstract_surface,
					   cairo_image_surface_t  **image_out,
					   void                   **image_extra)
{
    *image_out = abstract_surface;
    *image_extra = NULL;

    return CAIRO_STATUS_SUCCESS;
}
static void
_cairo_image_surface_release_source_image (void                   *abstract_surface,
					   cairo_image_surface_t  *image,
					   void                   *image_extra)
{
}

static cairo_clip_path_t *
_clip_get_single_path (cairo_clip_t *clip)
{
    cairo_clip_path_t *iter = clip->path;
    cairo_clip_path_t *path = NULL;

    do {
	if ((iter->flags & CAIRO_CLIP_PATH_IS_BOX) == 0) {
	    if (path != NULL)
		return FALSE;

	    path = iter;
	}
	iter = iter->prev;
    } while (iter != NULL);

    return path;
}

static cairo_int_status_t
_cairo_image_surface_fill (void				*abstract_surface,
			   cairo_operator_t		 op,
			   const cairo_pattern_t	*source,
			   cairo_path_fixed_t		*path,
			   cairo_fill_rule_t		 fill_rule,
			   double			 tolerance,
			   cairo_antialias_t		 antialias,
			   cairo_clip_t			*clip)
{
    cairo_image_surface_t *surface = abstract_surface;
    cairo_composite_rectangles_t extents;
    cairo_box_t boxes_stack[32], *clip_boxes = boxes_stack;
    cairo_clip_t local_clip;
    cairo_bool_t have_clip = FALSE;
    int num_boxes = ARRAY_LENGTH (boxes_stack);
    cairo_status_t status;

    status = _cairo_composite_rectangles_init_for_fill (&extents,
							surface->width,
							surface->height,
							op, source, path,
							clip);
    if (unlikely (status))
	return status;

    if (_cairo_clip_contains_extents (clip, &extents))
	clip = NULL;

    if (extents.is_bounded && clip != NULL) {
	cairo_clip_path_t *clip_path;

	if (((clip_path = _clip_get_single_path (clip)) != NULL) &&
	    _cairo_path_fixed_equal (&clip_path->path, path))
	{
	    clip = NULL;
	}
    }

    if (clip != NULL) {
	clip = _cairo_clip_init_copy (&local_clip, clip);
	have_clip = TRUE;
    }

    status = _cairo_clip_to_boxes (&clip, &extents, &clip_boxes, &num_boxes);
    if (unlikely (status)) {
	if (have_clip)
	    _cairo_clip_fini (&local_clip);

	return status;
    }

    if (_cairo_path_fixed_is_rectilinear_fill (path)) {
	cairo_boxes_t boxes;

	_cairo_boxes_init (&boxes);
	_cairo_boxes_limit (&boxes, clip_boxes, num_boxes);

	status = _cairo_path_fixed_fill_rectilinear_to_boxes (path,
							      fill_rule,
							      &boxes);
	if (likely (status == CAIRO_STATUS_SUCCESS)) {
	    status = _clip_and_composite_boxes (surface, op, source,
						&boxes, antialias,
						&extents, clip);
	}

	_cairo_boxes_fini (&boxes);
    } else {
	cairo_polygon_t polygon;

	assert (! path->is_empty_fill);

	_cairo_polygon_init (&polygon);
	_cairo_polygon_limit (&polygon, clip_boxes, num_boxes);

	status = _cairo_path_fixed_fill_to_polygon (path, tolerance, &polygon);
	if (likely (status == CAIRO_STATUS_SUCCESS)) {
	    status = _clip_and_composite_polygon (surface, op, source, &polygon,
						  fill_rule, antialias,
						  &extents, clip);
	}

	_cairo_polygon_fini (&polygon);
    }

    if (clip_boxes != boxes_stack)
	free (clip_boxes);

    if (have_clip)
	_cairo_clip_fini (&local_clip);

    return status;
}

const cairo_surface_backend_t _cairo_image_surface_backend = {
    CAIRO_SURFACE_TYPE_IMAGE,
    NULL, //_cairo_image_surface_create_similar,
    NULL, //_cairo_image_surface_finish,
    _cairo_image_surface_acquire_source_image,
    _cairo_image_surface_release_source_image,
    NULL, //_cairo_image_surface_acquire_dest_image,
    NULL, //_cairo_image_surface_release_dest_image,
    NULL, //_cairo_image_surface_clone_similar,
    NULL, //_cairo_image_surface_composite,
    NULL, //_cairo_image_surface_fill_rectangles,
    NULL, //_cairo_image_surface_composite_trapezoids,
//    NULL, //_cairo_image_surface_create_span_renderer,
    NULL, //_cairo_image_surface_check_span_renderer,

    NULL, /* copy_page */
    NULL, /* show_page */
    NULL, //_cairo_image_surface_get_extents,
//    NULL, /* old_show_glyphs */
    NULL, //_cairo_image_surface_get_font_options,
    NULL, /* flush */
    NULL, /* mark dirty */
//    NULL, /* font_fini */
//    NULL, /* glyph_fini */

    NULL, //_cairo_image_surface_paint,
    NULL, //_cairo_image_surface_mask,
    _cairo_image_surface_stroke,
    _cairo_image_surface_fill,
    NULL, //_cairo_image_surface_glyphs,
//    NULL, /* show_text_glyphs */
    NULL, /* snapshot */
    NULL, /* is_similar */
};
cairo_image_transparency_t
_cairo_image_analyze_transparency (cairo_image_surface_t      *image)
{
    int x, y;

    if (image->transparency != CAIRO_IMAGE_UNKNOWN)
	return image->transparency;

    if ((image->base.content & CAIRO_CONTENT_ALPHA) == 0)
	return image->transparency = CAIRO_IMAGE_IS_OPAQUE;

    if ((image->base.content & CAIRO_CONTENT_COLOR) == 0) {
	if (image->format == CAIRO_FORMAT_A1)
	    return image->transparency = CAIRO_IMAGE_HAS_BILEVEL_ALPHA;
	else
	    return image->transparency = CAIRO_IMAGE_HAS_ALPHA;
    }

    if (image->format == CAIRO_FORMAT_RGB16_565) {
	image->transparency = CAIRO_IMAGE_IS_OPAQUE;
	return CAIRO_IMAGE_IS_OPAQUE;
    }

    if (image->format != CAIRO_FORMAT_ARGB32)
	return image->transparency = CAIRO_IMAGE_HAS_ALPHA;

    image->transparency = CAIRO_IMAGE_IS_OPAQUE;
    for (y = 0; y < image->height; y++) {
	uint32_t *pixel = (uint32_t *) (image->data + y * image->stride);

	for (x = 0; x < image->width; x++, pixel++) {
	    int a = (*pixel & 0xff000000) >> 24;
	    if (a > 0 && a < 255) {
		return image->transparency = CAIRO_IMAGE_HAS_ALPHA;
	    } else if (a == 0) {
		image->transparency = CAIRO_IMAGE_HAS_BILEVEL_ALPHA;
	    }
	}
    }

    return image->transparency;
}
