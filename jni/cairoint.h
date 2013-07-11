
#ifndef _CAIROINT_H_
#define _CAIROINT_H_
#include "pixman.h"
#include "tk_cairo.h"
#include <assert.h>
#include <stdlib.h>
#include <math.h>
#include <limits.h>
#include <string.h>
#include <stdio.h>
#include <stddef.h>
#include "cairo-compiler-private.h"
#include "tk_log.h"

#include "cairo-spans-private.h"
#include "cairo-fixed-private.h"
#include "cairo-malloc-private.h"
#include "cairo-wideint-private.h"
#include "cairo-types-private.h"
#include "cairo-surface-private.h"
#include "cairo-scaled-font-private.h"
#include "cairo-reference-count-private.h"
#ifndef FALSE
#define FALSE 0
#endif

#ifndef TRUE
#define TRUE 1
#endif
#undef MIN
#define MIN(a, b) ((a) < (b) ? (a) : (b))

#undef MAX
#define MAX(a, b) ((a) > (b) ? (a) : (b))

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#ifndef M_SQRT2
#define M_SQRT2 1.41421356237309504880
#endif
#ifndef M_SQRT1_2
#define M_SQRT1_2 0.707106781186547524400844362104849039
#endif

/* pixman-required stride alignment in bytes. */
#define CAIRO_STRIDE_ALIGNMENT (sizeof (uint32_t))
#define CAIRO_STRIDE_FOR_WIDTH_BPP(w,bpp) \
   ((((bpp)*(w)+7)/8 + CAIRO_STRIDE_ALIGNMENT-1) & -CAIRO_STRIDE_ALIGNMENT)

#define _cairo_polygon_status(P) ((cairo_polygon_t *) (P))->status
#define CAIRO_ALPHA_SHORT_IS_OPAQUE(alpha) ((alpha) >= 0xff00)
#define CAIRO_ALPHA_SHORT_IS_CLEAR(alpha) ((alpha) <= 0x00ff)
#define CAIRO_COLOR_IS_CLEAR(color) CAIRO_ALPHA_SHORT_IS_CLEAR ((color)->alpha_short)
#define CAIRO_COLOR_IS_OPAQUE(color) CAIRO_ALPHA_SHORT_IS_OPAQUE ((color)->alpha_short)
#define CAIRO_GSTATE_LINE_WIDTH_DEFAULT	2.0
#define CAIRO_GSTATE_LINE_CAP_DEFAULT	CAIRO_LINE_CAP_BUTT
#define CAIRO_GSTATE_LINE_JOIN_DEFAULT	CAIRO_LINE_JOIN_MITER
#define CAIRO_GSTATE_MITER_LIMIT_DEFAULT	10.0

#define CAIRO_GSTATE_TOLERANCE_DEFAULT	0.1
#define CAIRO_GSTATE_FILL_RULE_DEFAULT	CAIRO_FILL_RULE_WINDING
#define CAIRO_GSTATE_OPERATOR_DEFAULT	CAIRO_OPERATOR_OVER
#define CAIRO_GSTATE_DEFAULT_FONT_SIZE  10.0
#define CAIRO_SURFACE_RESOLUTION_DEFAULT 72.0
#define CAIRO_SURFACE_FALLBACK_RESOLUTION_DEFAULT 300.0
#define CAIRO_EXTEND_GRADIENT_DEFAULT CAIRO_EXTEND_PAD
#define CAIRO_FILTER_DEFAULT CAIRO_FILTER_GOOD
#define CAIRO_EXTEND_SURFACE_DEFAULT CAIRO_EXTEND_NONE
cairo_private cairo_status_t
_cairo_error (cairo_status_t status);

extern const cairo_private cairo_solid_pattern_t _cairo_pattern_black;
#define ASSERT_NOT_REACHED		\
do {					\
    assert (!"reached");		\
} while (0)

#undef  ARRAY_LENGTH
#define ARRAY_LENGTH(__array) ((int) (sizeof (__array) / sizeof (__array[0])))

#if defined (__GNUC__)
#define cairo_container_of(ptr, type, member) ({ \
    const __typeof__ (((type *) 0)->member) *mptr__ = (ptr); \
    (type *) ((char *) mptr__ - offsetof (type, member)); \
})
#else
#define cairo_container_of(ptr, type, member) \
    ((type *)((char *) (ptr) - (char *) &((type *)0)->member))
#endif
typedef struct _cairo_traps {
    cairo_status_t status;

    const cairo_box_t *limits;
    int num_limits;

    unsigned int maybe_region : 1; /* hint: 0 implies that it cannot be */
    unsigned int has_intersections : 1;
    unsigned int is_rectilinear : 1;
    unsigned int is_rectangular : 1;

    int num_traps;
    int traps_size;
    cairo_trapezoid_t *traps;
    cairo_trapezoid_t  traps_embedded[16];
} cairo_traps_t;

typedef struct _cairo_stroke_face {
    cairo_point_t ccw;
    cairo_point_t point;
    cairo_point_t cw;
    cairo_slope_t dev_vector;
    cairo_point_double_t usr_vector;
} cairo_stroke_face_t;

typedef struct {
    const cairo_user_data_key_t *key;
    void *user_data;
    cairo_destroy_func_t destroy;
} cairo_user_data_slot_t;

cairo_private void
_cairo_user_data_array_init (cairo_user_data_array_t *array);

cairo_private void
_cairo_user_data_array_fini (cairo_user_data_array_t *array);

cairo_private void
_cairo_stroke_style_init (cairo_stroke_style_t *style);
cairo_private void
_cairo_stroke_style_max_distance_from_path (const cairo_stroke_style_t *style,
                                            const cairo_matrix_t *ctm,
                                            double *dx, double *dy);
cairo_private cairo_status_t
_cairo_path_fixed_close_path (cairo_path_fixed_t *path);

cairo_private void
_cairo_stroke_style_fini (cairo_stroke_style_t *style);
cairo_private cairo_status_t
_cairo_path_fixed_stroke_to_polygon (const cairo_path_fixed_t	*path,
				     const cairo_stroke_style_t	*stroke_style,
				     const cairo_matrix_t	*ctm,
				     const cairo_matrix_t	*ctm_inverse,
				     double		 tolerance,
				     cairo_polygon_t	*polygon);
cairo_private cairo_int_status_t
_cairo_path_fixed_stroke_rectilinear_to_boxes (const cairo_path_fixed_t	*path,
					       const cairo_stroke_style_t	*stroke_style,
					       const cairo_matrix_t	*ctm,
					       cairo_boxes_t		*boxes);

/* cairo-matrix.c */
cairo_private void
_cairo_matrix_get_affine (const cairo_matrix_t *matrix,
			  double *xx, double *yx,
			  double *xy, double *yy,
			  double *x0, double *y0);

cairo_private cairo_bool_t
_cairo_matrix_has_unity_scale (const cairo_matrix_t *matrix);
cairo_private cairo_bool_t
_cairo_matrix_is_identity (const cairo_matrix_t *matrix) cairo_pure;
cairo_private double
_cairo_matrix_compute_determinant (const cairo_matrix_t *matrix) cairo_pure;
cairo_private double
_cairo_matrix_transformed_circle_major_axis (const cairo_matrix_t *matrix,
					     double radius) cairo_pure;
cairo_private void
_cairo_matrix_transform_bounding_box (const cairo_matrix_t *matrix,
				      double *x1, double *y1,
				      double *x2, double *y2,
				      cairo_bool_t *is_tight);
cairo_private cairo_bool_t
_cairo_matrix_is_translation (const cairo_matrix_t *matrix) cairo_pure;
cairo_private cairo_bool_t
_cairo_matrix_is_pixel_exact (const cairo_matrix_t *matrix) cairo_pure;

static inline void
_cairo_unbounded_rectangle_init (cairo_rectangle_int_t *rect)
{
    rect->x = CAIRO_RECT_INT_MIN;
    rect->y = CAIRO_RECT_INT_MIN;
    rect->width = CAIRO_RECT_INT_MAX - CAIRO_RECT_INT_MIN;
    rect->height = CAIRO_RECT_INT_MAX - CAIRO_RECT_INT_MIN;
}

/* cairo-pen.c */
cairo_private cairo_status_t
_cairo_pen_init (cairo_pen_t	*pen,
		 double		 radius,
		 double		 tolerance,
		 const cairo_matrix_t	*ctm);
cairo_private void
_cairo_pen_fini (cairo_pen_t *pen);

cairo_private int
_cairo_pen_find_active_cw_vertex_index (const cairo_pen_t *pen,
					const cairo_slope_t *slope);

cairo_private int
_cairo_pen_find_active_ccw_vertex_index (const cairo_pen_t *pen,
					 const cairo_slope_t *slope);

/* cairo-surface.c */
cairo_private void
_cairo_surface_detach_snapshot (cairo_surface_t *snapshot);
cairo_private cairo_surface_t *
_cairo_surface_has_snapshot (cairo_surface_t *surface,
			     const cairo_surface_backend_t *backend);
cairo_private cairo_status_t
_cairo_surface_set_error (cairo_surface_t	*surface,
			  cairo_status_t	 status);
cairo_private cairo_bool_t
_cairo_surface_has_device_transform (cairo_surface_t *surface) cairo_pure;
cairo_private cairo_bool_t
_cairo_surface_get_extents (cairo_surface_t         *surface,
			    cairo_rectangle_int_t   *extents);
cairo_private cairo_status_t
_cairo_surface_paint (cairo_surface_t	*surface,
		      cairo_operator_t	 op,
		      const cairo_pattern_t *source,
		      cairo_clip_t	    *clip);

cairo_private cairo_surface_t *
_cairo_surface_create_similar_scratch (cairo_surface_t *other,
				       cairo_content_t	content,
				       int		width,
				       int		height);

cairo_private cairo_status_t
_cairo_surface_stroke (cairo_surface_t		*surface,
		       cairo_operator_t		 op,
		       const cairo_pattern_t	*source,
		       cairo_path_fixed_t	*path,
		       const cairo_stroke_style_t	*style,
		       const cairo_matrix_t		*ctm,
		       const cairo_matrix_t		*ctm_inverse,
		       double			 tolerance,
		       cairo_antialias_t	 antialias,
		       cairo_clip_t		*clip);
cairo_private cairo_status_t
_cairo_surface_fill (cairo_surface_t	*surface,
		     cairo_operator_t	 op,
		     const cairo_pattern_t *source,
		     cairo_path_fixed_t	*path,
		     cairo_fill_rule_t	 fill_rule,
		     double		 tolerance,
		     cairo_antialias_t	 antialias,
		     cairo_clip_t	*clip);

cairo_private cairo_status_t
_cairo_surface_acquire_source_image (cairo_surface_t         *surface,
				     cairo_image_surface_t  **image_out,
				     void                   **image_extra);
cairo_private void
_cairo_surface_init (cairo_surface_t			*surface,
		     const cairo_surface_backend_t	*backend,
		     cairo_device_t			*device,
		     cairo_content_t			 content);

cairo_private cairo_surface_t *
_cairo_surface_create_in_error (cairo_status_t status);
cairo_private void
_cairo_surface_release_source_image (cairo_surface_t        *surface,
				     cairo_image_surface_t  *image,
				     void                   *image_extra);
cairo_private cairo_bool_t
_cairo_pattern_is_clear (const cairo_pattern_t *pattern);
cairo_private void
_cairo_pattern_transform (cairo_pattern_t      *pattern,
			  const cairo_matrix_t *ctm_inverse);
cairo_private void
_cairo_pattern_init_solid (cairo_solid_pattern_t	*pattern,
			   const cairo_color_t		*color);
cairo_private void
_cairo_pattern_init_static_copy (cairo_pattern_t	*pattern,
				 const cairo_pattern_t *other);
cairo_private cairo_bool_t
_cairo_pattern_is_opaque_solid (const cairo_pattern_t *pattern);
cairo_private cairo_bool_t
_cairo_gradient_pattern_is_solid (const cairo_gradient_pattern_t *gradient,
				  const cairo_rectangle_int_t *extents,
				  cairo_color_t *color);
cairo_private void
_cairo_pattern_get_extents (const cairo_pattern_t	    *pattern,
			    cairo_rectangle_int_t           *extents);

struct _cairo_image_surface {
    cairo_surface_t base;

    pixman_format_code_t pixman_format;
    cairo_format_t format;
    unsigned char *data;

    int width;
    int height;
    int stride;
    int depth;

    pixman_image_t *pixman_image;

    unsigned owns_data : 1;
    unsigned transparency : 2;
};

cairo_private cairo_bool_t
_cairo_rectangle_intersect (cairo_rectangle_int_t *dst,
			    const cairo_rectangle_int_t *src);

cairo_private void
_cairo_box_round_to_rectangle (const cairo_box_t     *box,
			       cairo_rectangle_int_t *rectangle);
cairo_private void
_cairo_boxes_get_extents (const cairo_box_t *boxes,
			  int num_boxes,
			  cairo_box_t *extents);
cairo_private void
_cairo_box_from_rectangle (cairo_box_t                 *box,
			   const cairo_rectangle_int_t *rectangle);

struct _cairo_surface_backend {
    cairo_surface_type_t type;

    cairo_surface_t *
    (*create_similar)		(void			*surface,
				 cairo_content_t	 content,
				 int			 width,
				 int			 height);

    cairo_warn cairo_status_t
    (*finish)			(void			*surface);

    cairo_warn cairo_status_t
    (*acquire_source_image)	(void                    *abstract_surface,
				 cairo_image_surface_t  **image_out,
				 void                   **image_extra);

    void
    (*release_source_image)	(void                   *abstract_surface,
				 cairo_image_surface_t  *image,
				 void                   *image_extra);

    cairo_warn cairo_status_t
    (*acquire_dest_image)       (void                    *abstract_surface,
				 cairo_rectangle_int_t   *interest_rect,
				 cairo_image_surface_t  **image_out,
				 cairo_rectangle_int_t   *image_rect,
				 void                   **image_extra);

    void
    (*release_dest_image)       (void                    *abstract_surface,
				 cairo_rectangle_int_t   *interest_rect,
				 cairo_image_surface_t   *image,
				 cairo_rectangle_int_t   *image_rect,
				 void                    *image_extra);

    /* Create a new surface (@clone_out) with the following
     * characteristics:
     *
     * 1. It is as compatible as possible with @surface (in terms of
     *    efficiency)
     *
     * 2. It has the same contents as @src within the given rectangle.
     *
     * 3. The offset of the similar surface with respect to the original
     *    surface is returned in the clone_offset vector.
     *    - if you clone the entire surface, this vector is zero.
     *    - if you clone (src_x, src_y)x(w, h) the vector is (src_x, src_y);
     */
    cairo_warn cairo_status_t
    (*clone_similar)            (void                   *surface,
				 cairo_surface_t        *src,
				 int                     src_x,
				 int                     src_y,
				 int                     width,
				 int                     height,
				 int                    *clone_offset_x,
				 int                    *clone_offset_y,
				 cairo_surface_t       **clone_out);

    /* XXX remove to a separate cairo_surface_compositor_t */
    /* XXX: dst should be the first argument for consistency */
    cairo_warn cairo_int_status_t
    (*composite)		(cairo_operator_t	 op,
				 const cairo_pattern_t	*src,
				 const cairo_pattern_t	*mask,
				 void			*dst,
				 int			 src_x,
				 int			 src_y,
				 int			 mask_x,
				 int			 mask_y,
				 int			 dst_x,
				 int			 dst_y,
				 unsigned int		 width,
				 unsigned int		 height,
				 cairo_region_t		*clip_region);

    cairo_warn cairo_int_status_t
    (*fill_rectangles)		(void			 *surface,
				 cairo_operator_t	  op,
				 const cairo_color_t     *color,
				 cairo_rectangle_int_t   *rects,
				 int			  num_rects);

    /* XXX: dst should be the first argument for consistency */
    cairo_warn cairo_int_status_t
    (*composite_trapezoids)	(cairo_operator_t	 op,
				 const cairo_pattern_t	*pattern,
				 void			*dst,
				 cairo_antialias_t	 antialias,
				 int			 src_x,
				 int			 src_y,
				 int			 dst_x,
				 int			 dst_y,
				 unsigned int		 width,
				 unsigned int		 height,
				 cairo_trapezoid_t	*traps,
				 int			 num_traps,
				 cairo_region_t		*region);

/*     cairo_warn cairo_span_renderer_t *
 *     (*create_span_renderer)	(cairo_operator_t			 op,
 * 				 const cairo_pattern_t			*pattern,
 *                                  void					*dst,
 *                                  cairo_antialias_t			 antialias,
 *                                  const cairo_composite_rectangles_t *rects,
 * 				 cairo_region_t *clip_region);
 * 
 */
    cairo_warn cairo_bool_t
    (*check_span_renderer)	(cairo_operator_t			 op,
				 const cairo_pattern_t			*pattern,
                                 void					*dst,
                                 cairo_antialias_t			 antialias);

    cairo_warn cairo_int_status_t
    (*copy_page)		(void			*surface);

    cairo_warn cairo_int_status_t
    (*show_page)		(void			*surface);

    /* Get the extents of the current surface. For many surface types
     * this will be as simple as { x=0, y=0, width=surface->width,
     * height=surface->height}.
     *
     * If this function is not implemented, or if it returns
     * FALSE the surface is considered to be
     * boundless and infinite bounds are used for it.
     */
    cairo_warn cairo_bool_t
    (*get_extents)		(void			 *surface,
				 cairo_rectangle_int_t   *extents);

    /*
     * This is an optional entry to let the surface manage its own glyph
     * resources. If null, render against this surface, using image
     * surfaces as glyphs.
     */
/*     cairo_warn cairo_int_status_t
    (*old_show_glyphs)		(cairo_scaled_font_t	        *font,
				 cairo_operator_t		 op,
				 const cairo_pattern_t		*pattern,
				 void				*surface,
				 int				 source_x,
				 int				 source_y,
				 int				 dest_x,
				 int				 dest_y,
				 unsigned int			 width,
				 unsigned int			 height,
				 cairo_glyph_t			*glyphs,
				 int				 num_glyphs,
				 cairo_region_t			*clip_region);
*/
    void
    (*get_font_options)         (void                  *surface,
				 cairo_font_options_t  *options);

    cairo_warn cairo_status_t
    (*flush)                    (void                  *surface);

    cairo_warn cairo_status_t
    (*mark_dirty_rectangle)     (void                  *surface,
				 int                    x,
				 int                    y,
				 int                    width,
				 int                    height);

 //   void
//    (*scaled_font_fini)		(cairo_scaled_font_t   *scaled_font);

 //   void
  //  (*scaled_glyph_fini)	(cairo_scaled_glyph_t	*scaled_glyph,
	//			 cairo_scaled_font_t	*scaled_font);

    /* OK, I'm starting over somewhat by defining the 5 top-level
     * drawing operators for the surface backend here with consistent
     * naming and argument-order conventions. */
    cairo_warn cairo_int_status_t
    (*paint)			(void			*surface,
				 cairo_operator_t	 op,
				 const cairo_pattern_t	*source,
				 cairo_clip_t		*clip);

    cairo_warn cairo_int_status_t
    (*mask)			(void			*surface,
				 cairo_operator_t	 op,
				 const cairo_pattern_t	*source,
				 const cairo_pattern_t	*mask,
				 cairo_clip_t		*clip);

    cairo_warn cairo_int_status_t
    (*stroke)			(void			*surface,
				 cairo_operator_t	 op,
				 const cairo_pattern_t	*source,
				 cairo_path_fixed_t	*path,
				 const cairo_stroke_style_t	*style,
				 const cairo_matrix_t	*ctm,
				 const cairo_matrix_t	*ctm_inverse,
				 double			 tolerance,
				 cairo_antialias_t	 antialias,
				 cairo_clip_t		*clip);

    cairo_warn cairo_int_status_t
    (*fill)			(void			*surface,
				 cairo_operator_t	 op,
				 const cairo_pattern_t	*source,
				 cairo_path_fixed_t	*path,
				 cairo_fill_rule_t	 fill_rule,
				 double			 tolerance,
				 cairo_antialias_t	 antialias,
				 cairo_clip_t           *clip);

   /*  cairo_warn cairo_int_status_t
    (*show_glyphs)		(void			*surface,
				 cairo_operator_t	 op,
				 const cairo_pattern_t	*source,
				 cairo_glyph_t		*glyphs,
				 int			 num_glyphs,
				 cairo_scaled_font_t	*scaled_font,
				 cairo_clip_t           *clip,
				 int			*remaining_glyphs);
*/
    cairo_surface_t *
    (*snapshot)			(void			*surface);

    cairo_bool_t
    (*is_similar)		(void			*surface_a,
	                         void			*surface_b);

    cairo_warn cairo_int_status_t
    (*fill_stroke)		(void			*surface,
				 cairo_operator_t	 fill_op,
				 const cairo_pattern_t	*fill_source,
				 cairo_fill_rule_t	 fill_rule,
				 double			 fill_tolerance,
				 cairo_antialias_t	 fill_antialias,
				 cairo_path_fixed_t	*path,
				 cairo_operator_t	 stroke_op,
				 const cairo_pattern_t	*stroke_source,
				 const cairo_stroke_style_t	*stroke_style,
				 const cairo_matrix_t	*stroke_ctm,
				 const cairo_matrix_t	*stroke_ctm_inverse,
				 double			 stroke_tolerance,
				 cairo_antialias_t	 stroke_antialias,
				 cairo_clip_t		*clip);

    cairo_surface_t *
    (*create_solid_pattern_surface)
			        (void			      *surface,
				 const cairo_solid_pattern_t  *solid_pattern);

    cairo_bool_t
    (*can_repaint_solid_pattern_surface)
			        (void			      *surface,
				 const cairo_solid_pattern_t  *solid_pattern);

    cairo_bool_t
    (*has_show_text_glyphs)	(void			    *surface);
/* 
    cairo_warn cairo_int_status_t
    (*show_text_glyphs)		(void			    *surface,
				 cairo_operator_t	     op,
				 const cairo_pattern_t	    *source,
				 const char		    *utf8,
				 int			     utf8_len,
				 cairo_glyph_t		    *glyphs,
				 int			     num_glyphs,
				 const cairo_text_cluster_t *clusters,
				 int			     num_clusters,
				 cairo_text_cluster_flags_t  cluster_flags,
				 cairo_scaled_font_t	    *scaled_font,
				 cairo_clip_t               *clip);
*/
};

extern const cairo_private cairo_surface_backend_t _cairo_image_surface_backend;

struct _cairo_font_face {
    /* hash_entry must be first */
    cairo_hash_entry_t hash_entry;
    cairo_status_t status;
    cairo_reference_count_t ref_count;
    cairo_user_data_array_t user_data;
   // const cairo_font_face_backend_t *backend;
};

/* cairo-image-surface.c */

/* XXX: In cairo 1.2.0 we added a new %CAIRO_FORMAT_RGB16_565 but
 * neglected to adjust this macro. The net effect is that it's
 * impossible to externally create an image surface with this
 * format. This is perhaps a good thing since we also neglected to fix
 * up things like cairo_surface_write_to_png() for the new format
 * (-Wswitch-enum will tell you where). Is it obvious that format was
 * added in haste?
 *
 * The reason for the new format was to allow the xlib backend to be
 * used on X servers with a 565 visual. So the new format did its job
 * for that, even without being considered "valid" for the sake of
 * things like cairo_image_surface_create().
 *
 * Since 1.2.0 we ran into the same situtation with X servers with BGR
 * visuals. This time we invented #cairo_internal_format_t instead,
 * (see it for more discussion).
 *
 * The punchline is that %CAIRO_FORMAT_VALID must not conside any
 * internal format to be valid. Also we need to decide if the
 * RGB16_565 should be moved to instead be an internal format. If so,
 * this macro need not change for it. (We probably will need to leave
 * an RGB16_565 value in the header files for the sake of code that
 * might have that value in it.)
 *
 * If we do decide to start fully supporting RGB16_565 as an external
 * format, then %CAIRO_FORMAT_VALID needs to be adjusted to include
 * it. But that should not happen before all necessary code is fixed
 * to support it (at least cairo_surface_write_to_png() and a few spots
 * in cairo-xlib-surface.c--again see -Wswitch-enum).
 */
#define CAIRO_FORMAT_VALID(format) ((format) >= CAIRO_FORMAT_ARGB32 &&		\
                                    (format) <= CAIRO_FORMAT_RGB16_565)
cairo_private cairo_image_surface_t *
_cairo_image_surface_coerce (cairo_image_surface_t	*surface);

cairo_private cairo_image_surface_t *
_cairo_image_surface_coerce_to_format (cairo_image_surface_t	*surface,
			               cairo_format_t		 format);
cairo_private cairo_format_t
_cairo_format_from_content (cairo_content_t content) cairo_const;

cairo_private cairo_image_transparency_t
_cairo_image_analyze_transparency (cairo_image_surface_t      *image);

/* cairo-path-fixed.c */
cairo_private void
_cairo_path_fixed_init (cairo_path_fixed_t *path);

cairo_private void
_cairo_path_fixed_fini (cairo_path_fixed_t *path);

cairo_private cairo_status_t
_cairo_path_fixed_move_to (cairo_path_fixed_t  *path,
			   cairo_fixed_t	x,
			   cairo_fixed_t	y);

cairo_private cairo_status_t
_cairo_path_fixed_line_to (cairo_path_fixed_t *path,
			   cairo_fixed_t	x,
			   cairo_fixed_t	y);
cairo_private cairo_bool_t
_cairo_path_fixed_is_box (const cairo_path_fixed_t *path,
                          cairo_box_t *box);
typedef cairo_status_t
(cairo_path_fixed_move_to_func_t) (void		 *closure,
				   const cairo_point_t *point);

typedef cairo_status_t
(cairo_path_fixed_line_to_func_t) (void		 *closure,
				   const cairo_point_t *point);

typedef cairo_status_t
(cairo_path_fixed_curve_to_func_t) (void	  *closure,
				    const cairo_point_t *p0,
				    const cairo_point_t *p1,
				    const cairo_point_t *p2);

typedef cairo_status_t
(cairo_path_fixed_close_path_func_t) (void *closure);

cairo_private cairo_status_t
_cairo_path_fixed_interpret (const cairo_path_fixed_t	  *path,
		       cairo_direction_t		   dir,
		       cairo_path_fixed_move_to_func_t	  *move_to,
		       cairo_path_fixed_line_to_func_t	  *line_to,
		       cairo_path_fixed_curve_to_func_t	  *curve_to,
		       cairo_path_fixed_close_path_func_t *close_path,
		       void				  *closure);
cairo_private void
_cairo_path_fixed_approximate_stroke_extents (const cairo_path_fixed_t *path,
					      const cairo_stroke_style_t *style,
					      const cairo_matrix_t *ctm,
					      cairo_rectangle_int_t *extents);
cairo_private void
_cairo_path_fixed_approximate_fill_extents (const cairo_path_fixed_t *path,
					    cairo_rectangle_int_t *extents);

/* cairo-traps.c */
#define _cairo_traps_status(T) (T)->status
cairo_private void
_cairo_traps_init (cairo_traps_t *traps);

cairo_private void
_cairo_traps_limit (cairo_traps_t	*traps,
		    const cairo_box_t	*boxes,
		    int			 num_boxes);
cairo_private void
_cairo_traps_fini (cairo_traps_t *traps);

cairo_private cairo_status_t
_cairo_traps_tessellate_rectangle (cairo_traps_t *traps,
				   const cairo_point_t *top_left,
				   const cairo_point_t *bottom_right);
cairo_private cairo_status_t
_cairo_bentley_ottmann_tessellate_boxes (const cairo_boxes_t *in,
					 cairo_fill_rule_t fill_rule,
					 cairo_boxes_t *out);
cairo_private void
_cairo_traps_add_trap (cairo_traps_t *traps,
		       cairo_fixed_t top, cairo_fixed_t bottom,
		       cairo_line_t *left, cairo_line_t *right);
/* cairo-polygon.c */
cairo_private void
_cairo_polygon_init (cairo_polygon_t *polygon);
cairo_private cairo_status_t
_cairo_polygon_add_external_edge (void *polygon,
				  const cairo_point_t *p1,
				  const cairo_point_t *p2);

cairo_private void
_cairo_polygon_limit (cairo_polygon_t	*polygon,
		      const cairo_box_t *boxes,
		      int		 num_boxes);

cairo_private void
_cairo_polygon_fini (cairo_polygon_t *polygon);
cairo_private cairo_status_t
_cairo_polygon_move_to (cairo_polygon_t *polygon,
			const cairo_point_t *point);

cairo_private cairo_status_t
_cairo_polygon_line_to (cairo_polygon_t *polygon,
			const cairo_point_t *point);

cairo_private cairo_status_t
_cairo_polygon_close (cairo_polygon_t *polygon);

enum {
    CAIRO_OPERATOR_BOUND_BY_MASK = 1 << 1,
    CAIRO_OPERATOR_BOUND_BY_SOURCE = 1 << 2,
};

cairo_private uint32_t
_cairo_operator_bounded_by_either (cairo_operator_t op) cairo_const;

/* cairo-color.c */
typedef enum {
    CAIRO_STOCK_WHITE,
    CAIRO_STOCK_BLACK,
    CAIRO_STOCK_TRANSPARENT,
    CAIRO_STOCK_NUM_COLORS,
} cairo_stock_t;

cairo_private const cairo_color_t *
_cairo_stock_color (cairo_stock_t stock) cairo_pure;

#define CAIRO_COLOR_WHITE       _cairo_stock_color (CAIRO_STOCK_WHITE)
#define CAIRO_COLOR_BLACK       _cairo_stock_color (CAIRO_STOCK_BLACK)
#define CAIRO_COLOR_TRANSPARENT _cairo_stock_color (CAIRO_STOCK_TRANSPARENT)

cairo_private uint16_t
_cairo_color_double_to_short (double d) cairo_const;

cairo_private void
_cairo_color_init (cairo_color_t *color);

cairo_private void
_cairo_color_init_rgb (cairo_color_t *color,
		       double red, double green, double blue);

cairo_private void
_cairo_color_init_rgba (cairo_color_t *color,
			double red, double green, double blue,
			double alpha);

cairo_private void
_cairo_color_multiply_alpha (cairo_color_t *color,
			     double	    alpha);

cairo_private void
_cairo_color_get_rgba (cairo_color_t *color,
		       double	     *red,
		       double	     *green,
		       double	     *blue,
		       double	     *alpha);

cairo_private void
_cairo_color_get_rgba_premultiplied (cairo_color_t *color,
				     double	   *red,
				     double	   *green,
				     double	   *blue,
				     double	   *alpha);

cairo_private cairo_bool_t
_cairo_color_equal (const cairo_color_t *color_a,
                    const cairo_color_t *color_b) cairo_pure;

cairo_private cairo_bool_t
_cairo_color_stop_equal (const cairo_color_stop_t *color_a,
			 const cairo_color_stop_t *color_b) cairo_pure;

cairo_private cairo_content_t
_cairo_color_get_content (const cairo_color_t *color) cairo_pure;

static inline double cairo_const
_cairo_restrict_value (double value, double min, double max)
{
    if (value < min)
	return min;
    else if (value > max)
	return max;
    else
	return value;
}

cairo_private cairo_bool_t
_cairo_operator_bounded_by_mask (cairo_operator_t op) cairo_const;

extern const cairo_private cairo_solid_pattern_t _cairo_pattern_clear;

cairo_private cairo_status_t
_cairo_path_fixed_fill_rectilinear_to_boxes (const cairo_path_fixed_t *path,
					     cairo_fill_rule_t fill_rule,
					     cairo_boxes_t *boxes);
cairo_private cairo_status_t
_cairo_path_fixed_fill_to_polygon (const cairo_path_fixed_t *path,
				   double              tolerance,
				   cairo_polygon_t      *polygon);
/* cairo-spline.c */
cairo_private cairo_bool_t
_cairo_spline_init (cairo_spline_t *spline,
		    cairo_spline_add_point_func_t add_point_func,
		    void *closure,
		    const cairo_point_t *a, const cairo_point_t *b,
		    const cairo_point_t *c, const cairo_point_t *d);

cairo_private cairo_status_t
_cairo_spline_decompose (cairo_spline_t *spline, double tolerance);

cairo_private cairo_status_t
_cairo_spline_bound (cairo_spline_add_point_func_t add_point_func,
		     void *closure,
		     const cairo_point_t *p0, const cairo_point_t *p1,
		     const cairo_point_t *p2, const cairo_point_t *p3);

cairo_private cairo_status_t
_cairo_bentley_ottmann_tessellate_rectilinear_polygon_to_boxes (const cairo_polygon_t *polygon,
								cairo_fill_rule_t fill_rule,
								cairo_boxes_t *boxes);

#endif
