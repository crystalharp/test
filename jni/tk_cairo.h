#ifndef CAIRO_H
#define CAIRO_H

#ifndef cairo_public
# if defined (_MSC_VER) && ! defined (CAIRO_WIN32_STATIC_BUILD)
#  define cairo_public __declspec(dllimport)
# else
#  define cairo_public
# endif
#endif

/**
 * cairo_bool_t:
 *
 * #cairo_bool_t is used for boolean values. Returns of type
 * #cairo_bool_t will always be either 0 or 1, but testing against
 * these values explicitly is not encouraged; just use the
 * value as a boolean condition.
 *
 * <informalexample><programlisting>
 *  if (cairo_in_stroke (cr, x, y)) {
 *      /<!-- -->* do something *<!-- -->/
 *  }
 * </programlisting></informalexample>
 **/
typedef int cairo_bool_t;

/**
 * cairo_t:
 *
 * A #cairo_t contains the current state of the rendering device,
 * including coordinates of yet to be drawn shapes.
 *
 * Cairo contexts, as #cairo_t objects are named, are central to
 * cairo and all drawing with cairo is always done to a #cairo_t
 * object.
 *
 * Memory management of #cairo_t is done with
 * cairo_reference() and cairo_destroy().
 **/
typedef struct _cairo cairo_t;
/**
 * cairo_surface_t:
 *
 * A #cairo_surface_t represents an image, either as the destination
 * of a drawing operation or as source when drawing onto another
 * surface.  To draw to a #cairo_surface_t, create a cairo context
 * with the surface as the target, using cairo_create().
 *
 * There are different subtypes of #cairo_surface_t for
 * different drawing backends; for example, cairo_image_surface_create()
 * creates a bitmap image in memory.
 * The type of a surface can be queried with cairo_surface_get_type().
 *
 * The initial contents of a surface after creation depend upon the manner
 * of its creation. If cairo creates the surface and backing storage for
 * the user, it will be initially cleared; for example,
 * cairo_image_surface_create() and cairo_surface_create_similar().
 * Alternatively, if the user passes in a reference to some backing storage
 * and asks cairo to wrap that in a #cairo_surface_t, then the contents are
 * not modified; for example, cairo_image_surface_create_for_data() and
 * cairo_xlib_surface_create().
 *
 * Memory management of #cairo_surface_t is done with
 * cairo_surface_reference() and cairo_surface_destroy().
 **/
typedef struct _cairo_surface cairo_surface_t;
/**
 * cairo_device_t:
 *
 * A #cairo_device_t represents the driver interface for drawing
 * operations to a #cairo_surface_t.  There are different subtypes of
 * #cairo_device_t for different drawing backends; for example,
 * cairo_xcb_device_create() creates a device that wraps the connection
 * to an X Windows System using the XCB library.
 *
 * The type of a device can be queried with cairo_device_get_type().
 *
 * Memory management of #cairo_device_t is done with
 * cairo_device_reference() and cairo_device_destroy().
 *
 * Since: 1.10
 **/
typedef struct _cairo_device cairo_device_t;

/**
 * cairo_matrix_t:
 * @xx: xx component of the affine transformation
 * @yx: yx component of the affine transformation
 * @xy: xy component of the affine transformation
 * @yy: yy component of the affine transformation
 * @x0: X translation component of the affine transformation
 * @y0: Y translation component of the affine transformation
 *
 * A #cairo_matrix_t holds an affine transformation, such as a scale,
 * rotation, shear, or a combination of those. The transformation of
 * a point (x, y) is given by:
 * <programlisting>
 *     x_new = xx * x + xy * y + x0;
 *     y_new = yx * x + yy * y + y0;
 * </programlisting>
 **/
typedef struct _cairo_matrix {
    double xx; double yx;
    double xy; double yy;
    double x0; double y0;
} cairo_matrix_t;

/**
 * cairo_extend_t:
 * @CAIRO_EXTEND_NONE: pixels outside of the source pattern
 *   are fully transparent
 * @CAIRO_EXTEND_REPEAT: the pattern is tiled by repeating
 * @CAIRO_EXTEND_REFLECT: the pattern is tiled by reflecting
 *   at the edges (Implemented for surface patterns since 1.6)
 * @CAIRO_EXTEND_PAD: pixels outside of the pattern copy
 *   the closest pixel from the source (Since 1.2; but only
 *   implemented for surface patterns since 1.6)
 *
 * #cairo_extend_t is used to describe how pattern color/alpha will be
 * determined for areas "outside" the pattern's natural area, (for
 * example, outside the surface bounds or outside the gradient
 * geometry).
 *
 * The default extend mode is %CAIRO_EXTEND_NONE for surface patterns
 * and %CAIRO_EXTEND_PAD for gradient patterns.
 *
 * New entries may be added in future versions.
 **/
typedef enum _cairo_extend {
    CAIRO_EXTEND_NONE,
    CAIRO_EXTEND_REPEAT,
    CAIRO_EXTEND_REFLECT,
    CAIRO_EXTEND_PAD
} cairo_extend_t;

/**
 * cairo_pattern_t:
 *
 * A #cairo_pattern_t represents a source when drawing onto a
 * surface. There are different subtypes of #cairo_pattern_t,
 * for different types of sources; for example,
 * cairo_pattern_create_rgb() creates a pattern for a solid
 * opaque color.
 *
 * Other than various cairo_pattern_create_<emphasis>type</emphasis>()
 * functions, some of the pattern types can be implicitly created
 * using various cairo_set_source_<emphasis>type</emphasis>() functions;
 * for example cairo_set_source_rgb().
 *
 * The type of a pattern can be queried with cairo_pattern_get_type().
 *
 * Memory management of #cairo_pattern_t is done with
 * cairo_pattern_reference() and cairo_pattern_destroy().
 **/
typedef struct _cairo_pattern cairo_pattern_t;

/**
 * cairo_destroy_func_t:
 * @data: The data element being destroyed.
 *
 * #cairo_destroy_func_t the type of function which is called when a
 * data element is destroyed. It is passed the pointer to the data
 * element and should free any memory and resources allocated for it.
 **/
typedef void (*cairo_destroy_func_t) (void *data);

/**
 * cairo_user_data_key_t:
 * @unused: not used; ignore.
 *
 * #cairo_user_data_key_t is used for attaching user data to cairo
 * data structures.  The actual contents of the struct is never used,
 * and there is no need to initialize the object; only the unique
 * address of a #cairo_data_key_t object is used.  Typically, you
 * would just use the address of a static #cairo_data_key_t object.
 **/
typedef struct _cairo_user_data_key {
    int unused;
} cairo_user_data_key_t;

/**
 * cairo_pattern_type_t:
 * @CAIRO_PATTERN_TYPE_SOLID: The pattern is a solid (uniform)
 * color. It may be opaque or translucent.
 * @CAIRO_PATTERN_TYPE_SURFACE: The pattern is a based on a surface (an image).
 * @CAIRO_PATTERN_TYPE_LINEAR: The pattern is a linear gradient.
 * @CAIRO_PATTERN_TYPE_RADIAL: The pattern is a radial gradient.
 *
 * #cairo_pattern_type_t is used to describe the type of a given pattern.
 *
 * The type of a pattern is determined by the function used to create
 * it. The cairo_pattern_create_rgb() and cairo_pattern_create_rgba()
 * functions create SOLID patterns. The remaining
 * cairo_pattern_create<!-- --> functions map to pattern types in obvious
 * ways.
 *
 * The pattern type can be queried with cairo_pattern_get_type()
 *
 * Most #cairo_pattern_t functions can be called with a pattern of any
 * type, (though trying to change the extend or filter for a solid
 * pattern will have no effect). A notable exception is
 * cairo_pattern_add_color_stop_rgb() and
 * cairo_pattern_add_color_stop_rgba() which must only be called with
 * gradient patterns (either LINEAR or RADIAL). Otherwise the pattern
 * will be shutdown and put into an error state.
 *
 * New entries may be added in future versions.
 *
 * Since: 1.2
 **/
typedef enum _cairo_pattern_type {
    CAIRO_PATTERN_TYPE_SOLID,
    CAIRO_PATTERN_TYPE_SURFACE,
    CAIRO_PATTERN_TYPE_LINEAR,
    CAIRO_PATTERN_TYPE_RADIAL
} cairo_pattern_type_t;

/**
 * cairo_filter_t:
 * @CAIRO_FILTER_FAST: A high-performance filter, with quality similar
 *     to %CAIRO_FILTER_NEAREST
 * @CAIRO_FILTER_GOOD: A reasonable-performance filter, with quality
 *     similar to %CAIRO_FILTER_BILINEAR
 * @CAIRO_FILTER_BEST: The highest-quality available, performance may
 *     not be suitable for interactive use.
 * @CAIRO_FILTER_NEAREST: Nearest-neighbor filtering
 * @CAIRO_FILTER_BILINEAR: Linear interpolation in two dimensions
 * @CAIRO_FILTER_GAUSSIAN: This filter value is currently
 *     unimplemented, and should not be used in current code.
 *
 * #cairo_filter_t is used to indicate what filtering should be
 * applied when reading pixel values from patterns. See
 * cairo_pattern_set_source() for indicating the desired filter to be
 * used with a particular pattern.
 */
typedef enum _cairo_filter {
    CAIRO_FILTER_FAST,
    CAIRO_FILTER_GOOD,
    CAIRO_FILTER_BEST,
    CAIRO_FILTER_NEAREST,
    CAIRO_FILTER_BILINEAR,
    CAIRO_FILTER_GAUSSIAN
} cairo_filter_t;

/**
 * cairo_status_t:
 * @CAIRO_STATUS_SUCCESS: no error has occurred
 * @CAIRO_STATUS_NO_MEMORY: out of memory
 * @CAIRO_STATUS_INVALID_RESTORE: cairo_restore() called without matching cairo_save()
 * @CAIRO_STATUS_INVALID_POP_GROUP: no saved group to pop, i.e. cairo_pop_group() without matching cairo_push_group()
 * @CAIRO_STATUS_NO_CURRENT_POINT: no current point defined
 * @CAIRO_STATUS_INVALID_MATRIX: invalid matrix (not invertible)
 * @CAIRO_STATUS_INVALID_STATUS: invalid value for an input #cairo_status_t
 * @CAIRO_STATUS_NULL_POINTER: %NULL pointer
 * @CAIRO_STATUS_INVALID_STRING: input string not valid UTF-8
 * @CAIRO_STATUS_INVALID_PATH_DATA: input path data not valid
 * @CAIRO_STATUS_READ_ERROR: error while reading from input stream
 * @CAIRO_STATUS_WRITE_ERROR: error while writing to output stream
 * @CAIRO_STATUS_SURFACE_FINISHED: target surface has been finished
 * @CAIRO_STATUS_SURFACE_TYPE_MISMATCH: the surface type is not appropriate for the operation
 * @CAIRO_STATUS_PATTERN_TYPE_MISMATCH: the pattern type is not appropriate for the operation
 * @CAIRO_STATUS_INVALID_CONTENT: invalid value for an input #cairo_content_t
 * @CAIRO_STATUS_INVALID_FORMAT: invalid value for an input #cairo_format_t
 * @CAIRO_STATUS_INVALID_VISUAL: invalid value for an input Visual*
 * @CAIRO_STATUS_FILE_NOT_FOUND: file not found
 * @CAIRO_STATUS_INVALID_DASH: invalid value for a dash setting
 * @CAIRO_STATUS_INVALID_DSC_COMMENT: invalid value for a DSC comment (Since 1.2)
 * @CAIRO_STATUS_INVALID_INDEX: invalid index passed to getter (Since 1.4)
 * @CAIRO_STATUS_CLIP_NOT_REPRESENTABLE: clip region not representable in desired format (Since 1.4)
 * @CAIRO_STATUS_TEMP_FILE_ERROR: error creating or writing to a temporary file (Since 1.6)
 * @CAIRO_STATUS_INVALID_STRIDE: invalid value for stride (Since 1.6)
 * @CAIRO_STATUS_FONT_TYPE_MISMATCH: the font type is not appropriate for the operation (Since 1.8)
 * @CAIRO_STATUS_USER_FONT_IMMUTABLE: the user-font is immutable (Since 1.8)
 * @CAIRO_STATUS_USER_FONT_ERROR: error occurred in a user-font callback function (Since 1.8)
 * @CAIRO_STATUS_NEGATIVE_COUNT: negative number used where it is not allowed (Since 1.8)
 * @CAIRO_STATUS_INVALID_CLUSTERS: input clusters do not represent the accompanying text and glyph array (Since 1.8)
 * @CAIRO_STATUS_INVALID_SLANT: invalid value for an input #cairo_font_slant_t (Since 1.8)
 * @CAIRO_STATUS_INVALID_WEIGHT: invalid value for an input #cairo_font_weight_t (Since 1.8)
 * @CAIRO_STATUS_INVALID_SIZE: invalid value (typically too big) for the size of the input (surface, pattern, etc.) (Since 1.10)
 * @CAIRO_STATUS_USER_FONT_NOT_IMPLEMENTED: user-font method not implemented (Since 1.10)
 * @CAIRO_STATUS_DEVICE_TYPE_MISMATCH: the device type is not appropriate for the operation (Since 1.10)
 * @CAIRO_STATUS_DEVICE_ERROR: an operation to the device caused an unspecified error (Since 1.10)
 * @CAIRO_STATUS_LAST_STATUS: this is a special value indicating the number of
 *   status values defined in this enumeration.  When using this value, note
 *   that the version of cairo at run-time may have additional status values
 *   defined than the value of this symbol at compile-time. (Since 1.10)
 *
 * #cairo_status_t is used to indicate errors that can occur when
 * using Cairo. In some cases it is returned directly by functions.
 * but when using #cairo_t, the last error, if any, is stored in
 * the context and can be retrieved with cairo_status().
 *
 * New entries may be added in future versions.  Use cairo_status_to_string()
 * to get a human-readable representation of an error message.
 **/
typedef enum _cairo_status {
    CAIRO_STATUS_SUCCESS = 0,

    CAIRO_STATUS_NO_MEMORY,
    CAIRO_STATUS_INVALID_RESTORE,
    CAIRO_STATUS_INVALID_POP_GROUP,
    CAIRO_STATUS_NO_CURRENT_POINT,
    CAIRO_STATUS_INVALID_MATRIX,
    CAIRO_STATUS_INVALID_STATUS,
    CAIRO_STATUS_NULL_POINTER,
    CAIRO_STATUS_INVALID_STRING,
    CAIRO_STATUS_INVALID_PATH_DATA,
    CAIRO_STATUS_READ_ERROR,
    CAIRO_STATUS_WRITE_ERROR,
    CAIRO_STATUS_SURFACE_FINISHED,
    CAIRO_STATUS_SURFACE_TYPE_MISMATCH,
    CAIRO_STATUS_PATTERN_TYPE_MISMATCH,
    CAIRO_STATUS_INVALID_CONTENT,
    CAIRO_STATUS_INVALID_FORMAT,
    CAIRO_STATUS_INVALID_VISUAL,
    CAIRO_STATUS_FILE_NOT_FOUND,
    CAIRO_STATUS_INVALID_DASH,
    CAIRO_STATUS_INVALID_DSC_COMMENT,
    CAIRO_STATUS_INVALID_INDEX,
    CAIRO_STATUS_CLIP_NOT_REPRESENTABLE,
    CAIRO_STATUS_TEMP_FILE_ERROR,
    CAIRO_STATUS_INVALID_STRIDE,
    CAIRO_STATUS_FONT_TYPE_MISMATCH,
    CAIRO_STATUS_USER_FONT_IMMUTABLE,
    CAIRO_STATUS_USER_FONT_ERROR,
    CAIRO_STATUS_NEGATIVE_COUNT,
    CAIRO_STATUS_INVALID_CLUSTERS,
    CAIRO_STATUS_INVALID_SLANT,
    CAIRO_STATUS_INVALID_WEIGHT,
    CAIRO_STATUS_INVALID_SIZE,
    CAIRO_STATUS_USER_FONT_NOT_IMPLEMENTED,
    CAIRO_STATUS_DEVICE_TYPE_MISMATCH,
    CAIRO_STATUS_DEVICE_ERROR,

    CAIRO_STATUS_LAST_STATUS
} cairo_status_t;

/**
 * cairo_content_t:
 * @CAIRO_CONTENT_COLOR: The surface will hold color content only.
 * @CAIRO_CONTENT_ALPHA: The surface will hold alpha content only.
 * @CAIRO_CONTENT_COLOR_ALPHA: The surface will hold color and alpha content.
 *
 * #cairo_content_t is used to describe the content that a surface will
 * contain, whether color information, alpha information (translucence
 * vs. opacity), or both.
 *
 * Note: The large values here are designed to keep #cairo_content_t
 * values distinct from #cairo_format_t values so that the
 * implementation can detect the error if users confuse the two types.
 **/
typedef enum _cairo_content {
    CAIRO_CONTENT_COLOR		= 0x1000,
    CAIRO_CONTENT_ALPHA		= 0x2000,
    CAIRO_CONTENT_COLOR_ALPHA	= 0x3000
} cairo_content_t;

cairo_public void
cairo_set_line_width (cairo_t *cr, double width);
cairo_public void
cairo_line_to (cairo_t *cr, double x, double y);
cairo_public void
cairo_move_to (cairo_t *cr, double x, double y);
cairo_public void
cairo_stroke (cairo_t *cr);

cairo_public void
cairo_stroke_preserve (cairo_t *cr);

/* Functions for manipulating state objects */
cairo_public cairo_t *
cairo_create (cairo_surface_t *target);
cairo_public void
cairo_destroy (cairo_t *cr);
cairo_public void
cairo_surface_destroy (cairo_surface_t *surface);

/*  #if CAIRO_HAS_PNG_FUNCTIONS*/
cairo_public cairo_status_t
cairo_surface_write_to_png (cairo_surface_t	*surface,
			    const char		*filename);
/*#endif*/

cairo_public void
cairo_matrix_transform_point (const cairo_matrix_t *matrix,
			      double *x, double *y);
cairo_public void
cairo_matrix_multiply (cairo_matrix_t	    *result,
		       const cairo_matrix_t *a,
		       const cairo_matrix_t *b);
cairo_public void
cairo_matrix_transform_distance (const cairo_matrix_t *matrix,
				 double *dx, double *dy);
cairo_public cairo_status_t
cairo_matrix_invert (cairo_matrix_t *matrix);

/* Image-surface functions */

/**
 * cairo_format_t:
 * @CAIRO_FORMAT_INVALID: no such format exists or is supported.
 * @CAIRO_FORMAT_ARGB32: each pixel is a 32-bit quantity, with
 *   alpha in the upper 8 bits, then red, then green, then blue.
 *   The 32-bit quantities are stored native-endian. Pre-multiplied
 *   alpha is used. (That is, 50% transparent red is 0x80800000,
 *   not 0x80ff0000.)
 * @CAIRO_FORMAT_RGB24: each pixel is a 32-bit quantity, with
 *   the upper 8 bits unused. Red, Green, and Blue are stored
 *   in the remaining 24 bits in that order.
 * @CAIRO_FORMAT_A8: each pixel is a 8-bit quantity holding
 *   an alpha value.
 * @CAIRO_FORMAT_A1: each pixel is a 1-bit quantity holding
 *   an alpha value. Pixels are packed together into 32-bit
 *   quantities. The ordering of the bits matches the
 *   endianess of the platform. On a big-endian machine, the
 *   first pixel is in the uppermost bit, on a little-endian
 *   machine the first pixel is in the least-significant bit.
 * @CAIRO_FORMAT_RGB16_565: each pixel is a 16-bit quantity
 *   with red in the upper 5 bits, then green in the middle
 *   6 bits, and blue in the lower 5 bits.
 *
 * #cairo_format_t is used to identify the memory format of
 * image data.
 *
 * New entries may be added in future versions.
 **/
typedef enum _cairo_format {
    CAIRO_FORMAT_INVALID   = -1,
    CAIRO_FORMAT_ARGB32    = 0,
    CAIRO_FORMAT_RGB24     = 1,
    CAIRO_FORMAT_A8        = 2,
    CAIRO_FORMAT_A1        = 3,
    CAIRO_FORMAT_RGB16_565 = 4
} cairo_format_t;

cairo_public cairo_surface_t *
cairo_image_surface_create (cairo_format_t	format,
			    int			width,
			    int			height);
cairo_public cairo_surface_t *
cairo_image_surface_create_for_data (unsigned char	       *data,
				     cairo_format_t		format,
				     int			width,
				     int			height,
				     int			stride);

/**
 * cairo_antialias_t:
 * @CAIRO_ANTIALIAS_DEFAULT: Use the default antialiasing for
 *   the subsystem and target device
 * @CAIRO_ANTIALIAS_NONE: Use a bilevel alpha mask
 * @CAIRO_ANTIALIAS_GRAY: Perform single-color antialiasing (using
 *  shades of gray for black text on a white background, for example).
 * @CAIRO_ANTIALIAS_SUBPIXEL: Perform antialiasing by taking
 *  advantage of the order of subpixel elements on devices
 *  such as LCD panels
 *
 * Specifies the type of antialiasing to do when rendering text or shapes.
 **/
typedef enum _cairo_antialias {
    CAIRO_ANTIALIAS_DEFAULT,
    CAIRO_ANTIALIAS_NONE,
    CAIRO_ANTIALIAS_GRAY,
    CAIRO_ANTIALIAS_SUBPIXEL
} cairo_antialias_t;

/**
 * cairo_fill_rule_t:
 * @CAIRO_FILL_RULE_WINDING: If the path crosses the ray from
 * left-to-right, counts +1. If the path crosses the ray
 * from right to left, counts -1. (Left and right are determined
 * from the perspective of looking along the ray from the starting
 * point.) If the total count is non-zero, the point will be filled.
 * @CAIRO_FILL_RULE_EVEN_ODD: Counts the total number of
 * intersections, without regard to the orientation of the contour. If
 * the total number of intersections is odd, the point will be
 * filled.
 *
 * #cairo_fill_rule_t is used to select how paths are filled. For both
 * fill rules, whether or not a point is included in the fill is
 * determined by taking a ray from that point to infinity and looking
 * at intersections with the path. The ray can be in any direction,
 * as long as it doesn't pass through the end point of a segment
 * or have a tricky intersection such as intersecting tangent to the path.
 * (Note that filling is not actually implemented in this way. This
 * is just a description of the rule that is applied.)
 *
 * The default fill rule is %CAIRO_FILL_RULE_WINDING.
 *
 * New entries may be added in future versions.
 **/
typedef enum _cairo_fill_rule {
    CAIRO_FILL_RULE_WINDING,
    CAIRO_FILL_RULE_EVEN_ODD
} cairo_fill_rule_t;

/**
 * cairo_surface_type_t:
 * @CAIRO_SURFACE_TYPE_IMAGE: The surface is of type image
 * @CAIRO_SURFACE_TYPE_PDF: The surface is of type pdf
 * @CAIRO_SURFACE_TYPE_PS: The surface is of type ps
 * @CAIRO_SURFACE_TYPE_XLIB: The surface is of type xlib
 * @CAIRO_SURFACE_TYPE_XCB: The surface is of type xcb
 * @CAIRO_SURFACE_TYPE_GLITZ: The surface is of type glitz
 * @CAIRO_SURFACE_TYPE_QUARTZ: The surface is of type quartz
 * @CAIRO_SURFACE_TYPE_WIN32: The surface is of type win32
 * @CAIRO_SURFACE_TYPE_BEOS: The surface is of type beos
 * @CAIRO_SURFACE_TYPE_DIRECTFB: The surface is of type directfb
 * @CAIRO_SURFACE_TYPE_SVG: The surface is of type svg
 * @CAIRO_SURFACE_TYPE_OS2: The surface is of type os2
 * @CAIRO_SURFACE_TYPE_WIN32_PRINTING: The surface is a win32 printing surface
 * @CAIRO_SURFACE_TYPE_QUARTZ_IMAGE: The surface is of type quartz_image
 * @CAIRO_SURFACE_TYPE_SCRIPT: The surface is of type script, since 1.10
 * @CAIRO_SURFACE_TYPE_QT: The surface is of type Qt, since 1.10
 * @CAIRO_SURFACE_TYPE_RECORDING: The surface is of type recording, since 1.10
 * @CAIRO_SURFACE_TYPE_VG: The surface is a OpenVG surface, since 1.10
 * @CAIRO_SURFACE_TYPE_GL: The surface is of type OpenGL, since 1.10
 * @CAIRO_SURFACE_TYPE_DRM: The surface is of type Direct Render Manager, since 1.10
 * @CAIRO_SURFACE_TYPE_TEE: The surface is of type 'tee' (a multiplexing surface), since 1.10
 * @CAIRO_SURFACE_TYPE_XML: The surface is of type XML (for debugging), since 1.10
 * @CAIRO_SURFACE_TYPE_SKIA: The surface is of type Skia, since 1.10
 * @CAIRO_SURFACE_TYPE_SUBSURFACE: The surface is a subsurface created with
 *   cairo_surface_create_for_rectangle(), since 1.10
 *
 * #cairo_surface_type_t is used to describe the type of a given
 * surface. The surface types are also known as "backends" or "surface
 * backends" within cairo.
 *
 * The type of a surface is determined by the function used to create
 * it, which will generally be of the form cairo_<emphasis>type</emphasis>_surface_create(),
 * (though see cairo_surface_create_similar() as well).
 *
 * The surface type can be queried with cairo_surface_get_type()
 *
 * The various #cairo_surface_t functions can be used with surfaces of
 * any type, but some backends also provide type-specific functions
 * that must only be called with a surface of the appropriate
 * type. These functions have names that begin with
 * cairo_<emphasis>type</emphasis>_surface<!-- --> such as cairo_image_surface_get_width().
 *
 * The behavior of calling a type-specific function with a surface of
 * the wrong type is undefined.
 *
 * New entries may be added in future versions.
 *
 * Since: 1.2
 **/
typedef enum _cairo_surface_type {
    CAIRO_SURFACE_TYPE_IMAGE,
    CAIRO_SURFACE_TYPE_PDF,
    CAIRO_SURFACE_TYPE_PS,
    CAIRO_SURFACE_TYPE_XLIB,
    CAIRO_SURFACE_TYPE_XCB,
    CAIRO_SURFACE_TYPE_GLITZ,
    CAIRO_SURFACE_TYPE_QUARTZ,
    CAIRO_SURFACE_TYPE_WIN32,
    CAIRO_SURFACE_TYPE_BEOS,
    CAIRO_SURFACE_TYPE_DIRECTFB,
    CAIRO_SURFACE_TYPE_SVG,
    CAIRO_SURFACE_TYPE_OS2,
    CAIRO_SURFACE_TYPE_WIN32_PRINTING,
    CAIRO_SURFACE_TYPE_QUARTZ_IMAGE,
    CAIRO_SURFACE_TYPE_SCRIPT,
    CAIRO_SURFACE_TYPE_QT,
    CAIRO_SURFACE_TYPE_RECORDING,
    CAIRO_SURFACE_TYPE_VG,
    CAIRO_SURFACE_TYPE_GL,
    CAIRO_SURFACE_TYPE_DRM,
    CAIRO_SURFACE_TYPE_TEE,
    CAIRO_SURFACE_TYPE_XML,
    CAIRO_SURFACE_TYPE_SKIA,
    CAIRO_SURFACE_TYPE_SUBSURFACE
} cairo_surface_type_t;

/**
 * cairo_font_options_t:
 *
 * An opaque structure holding all options that are used when
 * rendering fonts.
 *
 * Individual features of a #cairo_font_options_t can be set or
 * accessed using functions named
 * cairo_font_options_set_<emphasis>feature_name</emphasis> and
 * cairo_font_options_get_<emphasis>feature_name</emphasis>, like
 * cairo_font_options_set_antialias() and
 * cairo_font_options_get_antialias().
 *
 * New features may be added to a #cairo_font_options_t in the
 * future.  For this reason, cairo_font_options_copy(),
 * cairo_font_options_equal(), cairo_font_options_merge(), and
 * cairo_font_options_hash() should be used to copy, check
 * for equality, merge, or compute a hash value of
 * #cairo_font_options_t objects.
 **/
typedef struct _cairo_font_options cairo_font_options_t;
/**
 * cairo_font_face_t:
 *
 * A #cairo_font_face_t specifies all aspects of a font other
 * than the size or font matrix (a font matrix is used to distort
 * a font by sheering it or scaling it unequally in the two
 * directions) . A font face can be set on a #cairo_t by using
 * cairo_set_font_face(); the size and font matrix are set with
 * cairo_set_font_size() and cairo_set_font_matrix().
 *
 * There are various types of font faces, depending on the
 * <firstterm>font backend</firstterm> they use. The type of a
 * font face can be queried using cairo_font_face_get_type().
 *
 * Memory management of #cairo_font_face_t is done with
 * cairo_font_face_reference() and cairo_font_face_destroy().
 **/
typedef struct _cairo_font_face cairo_font_face_t;

/**
 * cairo_subpixel_order_t:
 * @CAIRO_SUBPIXEL_ORDER_DEFAULT: Use the default subpixel order for
 *   for the target device
 * @CAIRO_SUBPIXEL_ORDER_RGB: Subpixel elements are arranged horizontally
 *   with red at the left
 * @CAIRO_SUBPIXEL_ORDER_BGR:  Subpixel elements are arranged horizontally
 *   with blue at the left
 * @CAIRO_SUBPIXEL_ORDER_VRGB: Subpixel elements are arranged vertically
 *   with red at the top
 * @CAIRO_SUBPIXEL_ORDER_VBGR: Subpixel elements are arranged vertically
 *   with blue at the top
 *
 * The subpixel order specifies the order of color elements within
 * each pixel on the display device when rendering with an
 * antialiasing mode of %CAIRO_ANTIALIAS_SUBPIXEL.
 **/
typedef enum _cairo_subpixel_order {
    CAIRO_SUBPIXEL_ORDER_DEFAULT,
    CAIRO_SUBPIXEL_ORDER_RGB,
    CAIRO_SUBPIXEL_ORDER_BGR,
    CAIRO_SUBPIXEL_ORDER_VRGB,
    CAIRO_SUBPIXEL_ORDER_VBGR
} cairo_subpixel_order_t;

/**
 * cairo_hint_style_t:
 * @CAIRO_HINT_STYLE_DEFAULT: Use the default hint style for
 *   font backend and target device
 * @CAIRO_HINT_STYLE_NONE: Do not hint outlines
 * @CAIRO_HINT_STYLE_SLIGHT: Hint outlines slightly to improve
 *   contrast while retaining good fidelity to the original
 *   shapes.
 * @CAIRO_HINT_STYLE_MEDIUM: Hint outlines with medium strength
 *   giving a compromise between fidelity to the original shapes
 *   and contrast
 * @CAIRO_HINT_STYLE_FULL: Hint outlines to maximize contrast
 *
 * Specifies the type of hinting to do on font outlines. Hinting
 * is the process of fitting outlines to the pixel grid in order
 * to improve the appearance of the result. Since hinting outlines
 * involves distorting them, it also reduces the faithfulness
 * to the original outline shapes. Not all of the outline hinting
 * styles are supported by all font backends.
 *
 * New entries may be added in future versions.
 **/
typedef enum _cairo_hint_style {
    CAIRO_HINT_STYLE_DEFAULT,
    CAIRO_HINT_STYLE_NONE,
    CAIRO_HINT_STYLE_SLIGHT,
    CAIRO_HINT_STYLE_MEDIUM,
    CAIRO_HINT_STYLE_FULL
} cairo_hint_style_t;

/**
 * cairo_hint_metrics_t:
 * @CAIRO_HINT_METRICS_DEFAULT: Hint metrics in the default
 *  manner for the font backend and target device
 * @CAIRO_HINT_METRICS_OFF: Do not hint font metrics
 * @CAIRO_HINT_METRICS_ON: Hint font metrics
 *
 * Specifies whether to hint font metrics; hinting font metrics
 * means quantizing them so that they are integer values in
 * device space. Doing this improves the consistency of
 * letter and line spacing, however it also means that text
 * will be laid out differently at different zoom factors.
 **/
typedef enum _cairo_hint_metrics {
    CAIRO_HINT_METRICS_DEFAULT,
    CAIRO_HINT_METRICS_OFF,
    CAIRO_HINT_METRICS_ON
} cairo_hint_metrics_t;


/* Region functions */

/**
 * cairo_region_t:
 *
 * A #cairo_region_t represents a set of integer-aligned rectangles.
 *
 * It allows set-theoretical operations like cairo_region_union() and
 * cairo_region_intersect() to be performed on them.
 *
 * Memory management of #cairo_region_t is done with
 * cairo_region_reference() and cairo_region_destroy().
 *
 * Since: 1.10
 **/
typedef struct _cairo_region cairo_region_t;

/**
 * cairo_rectangle_int_t:
 * @x: X coordinate of the left side of the rectangle
 * @y: Y coordinate of the the top side of the rectangle
 * @width: width of the rectangle
 * @height: height of the rectangle
 *
 * A data structure for holding a rectangle with integer coordinates.
 *
 * Since: 1.10
 **/

typedef struct _cairo_rectangle_int {
    int x, y;
    int width, height;
} cairo_rectangle_int_t;

cairo_public int
cairo_region_num_rectangles (const cairo_region_t *region);
cairo_public void
cairo_region_get_rectangle (const cairo_region_t  *region,
			    int                    nth,
			    cairo_rectangle_int_t *rectangle);

/* Modify state */

/**
 * cairo_operator_t:
 * @CAIRO_OPERATOR_CLEAR: clear destination layer (bounded)
 * @CAIRO_OPERATOR_SOURCE: replace destination layer (bounded)
 * @CAIRO_OPERATOR_OVER: draw source layer on top of destination layer
 * (bounded)
 * @CAIRO_OPERATOR_IN: draw source where there was destination content
 * (unbounded)
 * @CAIRO_OPERATOR_OUT: draw source where there was no destination
 * content (unbounded)
 * @CAIRO_OPERATOR_ATOP: draw source on top of destination content and
 * only there
 * @CAIRO_OPERATOR_DEST: ignore the source
 * @CAIRO_OPERATOR_DEST_OVER: draw destination on top of source
 * @CAIRO_OPERATOR_DEST_IN: leave destination only where there was
 * source content (unbounded)
 * @CAIRO_OPERATOR_DEST_OUT: leave destination only where there was no
 * source content
 * @CAIRO_OPERATOR_DEST_ATOP: leave destination on top of source content
 * and only there (unbounded)
 * @CAIRO_OPERATOR_XOR: source and destination are shown where there is only
 * one of them
 * @CAIRO_OPERATOR_ADD: source and destination layers are accumulated
 * @CAIRO_OPERATOR_SATURATE: like over, but assuming source and dest are
 * disjoint geometries
 * @CAIRO_OPERATOR_MULTIPLY: source and destination layers are multiplied.
 * This causes the result to be at least as dark as the darker inputs.
 * @CAIRO_OPERATOR_SCREEN: source and destination are complemented and
 * multiplied. This causes the result to be at least as light as the lighter
 * inputs.
 * @CAIRO_OPERATOR_OVERLAY: multiplies or screens, depending on the
 * lightness of the destination color.
 * @CAIRO_OPERATOR_DARKEN: replaces the destination with the source if it
 * is darker, otherwise keeps the source.
 * @CAIRO_OPERATOR_LIGHTEN: replaces the destination with the source if it
 * is lighter, otherwise keeps the source.
 * @CAIRO_OPERATOR_COLOR_DODGE: brightens the destination color to reflect
 * the source color.
 * @CAIRO_OPERATOR_COLOR_BURN: darkens the destination color to reflect
 * the source color.
 * @CAIRO_OPERATOR_HARD_LIGHT: Multiplies or screens, dependant on source
 * color.
 * @CAIRO_OPERATOR_SOFT_LIGHT: Darkens or lightens, dependant on source
 * color.
 * @CAIRO_OPERATOR_DIFFERENCE: Takes the difference of the source and
 * destination color.
 * @CAIRO_OPERATOR_EXCLUSION: Produces an effect similar to difference, but
 * with lower contrast.
 * @CAIRO_OPERATOR_HSL_HUE: Creates a color with the hue of the source
 * and the saturation and luminosity of the target.
 * @CAIRO_OPERATOR_HSL_SATURATION: Creates a color with the saturation
 * of the source and the hue and luminosity of the target. Painting with
 * this mode onto a gray area prduces no change.
 * @CAIRO_OPERATOR_HSL_COLOR: Creates a color with the hue and saturation
 * of the source and the luminosity of the target. This preserves the gray
 * levels of the target and is useful for coloring monochrome images or
 * tinting color images.
 * @CAIRO_OPERATOR_HSL_LUMINOSITY: Creates a color with the luminosity of
 * the source and the hue and saturation of the target. This produces an
 * inverse effect to @CAIRO_OPERATOR_HSL_COLOR.
 *
 * #cairo_operator_t is used to set the compositing operator for all cairo
 * drawing operations.
 *
 * The default operator is %CAIRO_OPERATOR_OVER.
 *
 * The operators marked as <firstterm>unbounded</firstterm> modify their
 * destination even outside of the mask layer (that is, their effect is not
 * bound by the mask layer).  However, their effect can still be limited by
 * way of clipping.
 *
 * To keep things simple, the operator descriptions here
 * document the behavior for when both source and destination are either fully
 * transparent or fully opaque.  The actual implementation works for
 * translucent layers too.
 * For a more detailed explanation of the effects of each operator, including
 * the mathematical definitions, see
 * <ulink url="http://cairographics.org/operators/">http://cairographics.org/operators/</ulink>.
 **/
typedef enum _cairo_operator {
    CAIRO_OPERATOR_CLEAR,

    CAIRO_OPERATOR_SOURCE,
    CAIRO_OPERATOR_OVER,
    CAIRO_OPERATOR_IN,
    CAIRO_OPERATOR_OUT,
    CAIRO_OPERATOR_ATOP,

    CAIRO_OPERATOR_DEST,
    CAIRO_OPERATOR_DEST_OVER,
    CAIRO_OPERATOR_DEST_IN,
    CAIRO_OPERATOR_DEST_OUT,
    CAIRO_OPERATOR_DEST_ATOP,

    CAIRO_OPERATOR_XOR,
    CAIRO_OPERATOR_ADD,
    CAIRO_OPERATOR_SATURATE,

    CAIRO_OPERATOR_MULTIPLY,
    CAIRO_OPERATOR_SCREEN,
    CAIRO_OPERATOR_OVERLAY,
    CAIRO_OPERATOR_DARKEN,
    CAIRO_OPERATOR_LIGHTEN,
    CAIRO_OPERATOR_COLOR_DODGE,
    CAIRO_OPERATOR_COLOR_BURN,
    CAIRO_OPERATOR_HARD_LIGHT,
    CAIRO_OPERATOR_SOFT_LIGHT,
    CAIRO_OPERATOR_DIFFERENCE,
    CAIRO_OPERATOR_EXCLUSION,
    CAIRO_OPERATOR_HSL_HUE,
    CAIRO_OPERATOR_HSL_SATURATION,
    CAIRO_OPERATOR_HSL_COLOR,
    CAIRO_OPERATOR_HSL_LUMINOSITY
} cairo_operator_t;

/**
 * cairo_line_cap_t:
 * @CAIRO_LINE_CAP_BUTT: start(stop) the line exactly at the start(end) point
 * @CAIRO_LINE_CAP_ROUND: use a round ending, the center of the circle is the end point
 * @CAIRO_LINE_CAP_SQUARE: use squared ending, the center of the square is the end point
 *
 * Specifies how to render the endpoints of the path when stroking.
 *
 * The default line cap style is %CAIRO_LINE_CAP_BUTT.
 **/
typedef enum _cairo_line_cap {
    CAIRO_LINE_CAP_BUTT,
    CAIRO_LINE_CAP_ROUND,
    CAIRO_LINE_CAP_SQUARE
} cairo_line_cap_t;

/**
 * cairo_line_join_t:
 * @CAIRO_LINE_JOIN_MITER: use a sharp (angled) corner, see
 * cairo_set_miter_limit()
 * @CAIRO_LINE_JOIN_ROUND: use a rounded join, the center of the circle is the
 * joint point
 * @CAIRO_LINE_JOIN_BEVEL: use a cut-off join, the join is cut off at half
 * the line width from the joint point
 *
 * Specifies how to render the junction of two lines when stroking.
 *
 * The default line join style is %CAIRO_LINE_JOIN_MITER.
 **/
typedef enum _cairo_line_join {
    CAIRO_LINE_JOIN_MITER,
    CAIRO_LINE_JOIN_ROUND,
    CAIRO_LINE_JOIN_BEVEL
} cairo_line_join_t;
/* Matrix functions */

cairo_public void
cairo_matrix_init (cairo_matrix_t *matrix,
		   double  xx, double  yx,
		   double  xy, double  yy,
		   double  x0, double  y0);

cairo_public void
cairo_matrix_init_identity (cairo_matrix_t *matrix);

/* Backend device manipulation */

cairo_public cairo_device_t *
cairo_device_reference (cairo_device_t *device);

cairo_public void
cairo_device_destroy (cairo_device_t *device);

/* Surface manipulation */
cairo_public cairo_surface_t *
cairo_surface_reference (cairo_surface_t *surface);
cairo_public void
cairo_surface_flush (cairo_surface_t *surface);
cairo_public void
cairo_set_source_rgb (cairo_t *cr, double red, double green, double blue);
cairo_public void
cairo_set_source_rgba (cairo_t *cr,
                       double red, double green, double blue,
                                   double alpha);

/* Pattern creation functions */

cairo_public cairo_pattern_t *
cairo_pattern_create_rgb (double red, double green, double blue);
cairo_public void
cairo_pattern_destroy (cairo_pattern_t *pattern);

cairo_public void
cairo_fill (cairo_t *cr);

cairo_public void
cairo_fill_preserve (cairo_t *cr);
cairo_public void
cairo_close_path (cairo_t *cr);

#endif
