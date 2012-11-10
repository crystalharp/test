
#ifndef CAIRO_TYPES_PRIVATE_H
#define CAIRO_TYPES_PRIVATE_H

#include "tk_cairo.h"
#include "cairo-fixed-type-private.h"
#include "cairo-list-private.h"
#include "cairo-reference-count-private.h"

#define CAIRO_RECT_INT_MIN (INT_MIN >> CAIRO_FIXED_FRAC_BITS)
#define CAIRO_RECT_INT_MAX (INT_MAX >> CAIRO_FIXED_FRAC_BITS)

typedef struct _cairo_composite_rectangles cairo_composite_rectangles_t;
typedef struct _cairo_color_stop cairo_color_stop_t;
typedef struct _cairo_boxes_t cairo_boxes_t;
typedef struct _cairo_hash_entry cairo_hash_entry_t;
typedef struct _cairo_array cairo_array_t;
typedef struct _cairo_solid_pattern cairo_solid_pattern_t;
typedef struct _cairo_clip_path cairo_clip_path_t;
typedef struct _cairo_clip cairo_clip_t;
typedef struct _cairo_color cairo_color_t;
typedef struct _cairo_image_surface cairo_image_surface_t;
typedef struct _cairo_surface_backend cairo_surface_backend_t;
typedef struct _cairo_gstate cairo_gstate_t;
typedef cairo_array_t cairo_user_data_array_t;

typedef struct _cairo_observer cairo_observer_t;
typedef struct _cairo_path_fixed cairo_path_fixed_t;
typedef enum _cairo_image_transparency {
    CAIRO_IMAGE_IS_OPAQUE,
    CAIRO_IMAGE_HAS_BILEVEL_ALPHA,
    CAIRO_IMAGE_HAS_ALPHA,
    CAIRO_IMAGE_UNKNOWN
} cairo_image_transparency_t;

typedef struct _cairo_slope {
    cairo_fixed_t dx;
    cairo_fixed_t dy;
} cairo_slope_t, cairo_distance_t;

typedef struct _cairo_point_double {
    double x;
    double y;
} cairo_point_double_t;


typedef cairo_warn cairo_status_t
(*cairo_spline_add_point_func_t) (void *closure,
				  const cairo_point_t *point);

typedef struct _cairo_spline_knots {
    cairo_point_t a, b, c, d;
} cairo_spline_knots_t;

typedef struct _cairo_spline {
    cairo_spline_add_point_func_t add_point_func;
    void *closure;

    cairo_spline_knots_t knots;

    cairo_slope_t initial_slope;
    cairo_slope_t final_slope;

    cairo_bool_t has_point;
    cairo_point_t last_point;
} cairo_spline_t;


typedef enum _cairo_direction {
    CAIRO_DIRECTION_FORWARD,
    CAIRO_DIRECTION_REVERSE
} cairo_direction_t;

typedef struct _cairo_pen_vertex {
    cairo_point_t point;

    cairo_slope_t slope_ccw;
    cairo_slope_t slope_cw;
} cairo_pen_vertex_t;

typedef struct _cairo_pen {
    double radius;
    double tolerance;

    int num_vertices;
    cairo_pen_vertex_t *vertices;
    cairo_pen_vertex_t  vertices_embedded[32];
} cairo_pen_t;

typedef struct _cairo_stroke_style {
    double		 line_width;
    cairo_line_cap_t	 line_cap;
    cairo_line_join_t	 line_join;
    double		 miter_limit;
    double		*dash;
    unsigned int	 num_dashes;
    double		 dash_offset;
} cairo_stroke_style_t;


struct _cairo_observer {
    cairo_list_t link;
    void (*callback) (cairo_observer_t *self, void *arg);
};

/**
 * cairo_hash_entry_t:
 *
 * A #cairo_hash_entry_t contains both a key and a value for
 * #cairo_hash_table_t. User-derived types for #cairo_hash_entry_t must
 * be type-compatible with this structure (eg. they must have an
 * unsigned long as the first parameter. The easiest way to get this
 * is to use:
 *
 * 	typedef _my_entry {
 *	    cairo_hash_entry_t base;
 *	    ... Remainder of key and value fields here ..
 *	} my_entry_t;
 *
 * which then allows a pointer to my_entry_t to be passed to any of
 * the #cairo_hash_table_t functions as follows without requiring a cast:
 *
 *	_cairo_hash_table_insert (hash_table, &my_entry->base);
 *
 * IMPORTANT: The caller is reponsible for initializing
 * my_entry->base.hash with a hash code derived from the key. The
 * essential property of the hash code is that keys_equal must never
 * return %TRUE for two keys that have different hashes. The best hash
 * code will reduce the frequency of two keys with the same code for
 * which keys_equal returns %FALSE.
 *
 * Which parts of the entry make up the "key" and which part make up
 * the value are entirely up to the caller, (as determined by the
 * computation going into base.hash as well as the keys_equal
 * function). A few of the #cairo_hash_table_t functions accept an entry
 * which will be used exclusively as a "key", (indicated by a
 * parameter name of key). In these cases, the value-related fields of
 * the entry need not be initialized if so desired.
 **/
struct _cairo_hash_entry {
    unsigned long hash;
};

struct _cairo_array {
    unsigned int size;
    unsigned int num_elements;
    unsigned int element_size;
    char **elements;

    cairo_bool_t is_snapshot;
};


/**
 * cairo_lcd_filter_t:
 * @CAIRO_LCD_FILTER_DEFAULT: Use the default LCD filter for
 *   font backend and target device
 * @CAIRO_LCD_FILTER_NONE: Do not perform LCD filtering
 * @CAIRO_LCD_FILTER_INTRA_PIXEL: Intra-pixel filter
 * @CAIRO_LCD_FILTER_FIR3: FIR filter with a 3x3 kernel
 * @CAIRO_LCD_FILTER_FIR5: FIR filter with a 5x5 kernel
 *
 * The LCD filter specifies the low-pass filter applied to LCD-optimized
 * bitmaps generated with an antialiasing mode of %CAIRO_ANTIALIAS_SUBPIXEL.
 *
 * Note: This API was temporarily made available in the public
 * interface during the 1.7.x development series, but was made private
 * before 1.8.
 **/
typedef enum _cairo_lcd_filter {
    CAIRO_LCD_FILTER_DEFAULT,
    CAIRO_LCD_FILTER_NONE,
    CAIRO_LCD_FILTER_INTRA_PIXEL,
    CAIRO_LCD_FILTER_FIR3,
    CAIRO_LCD_FILTER_FIR5
} cairo_lcd_filter_t;

struct _cairo_font_options {
    cairo_antialias_t antialias;
    cairo_subpixel_order_t subpixel_order;
    cairo_lcd_filter_t lcd_filter;
    cairo_hint_style_t hint_style;
    cairo_hint_metrics_t hint_metrics;
};

/* Sure wish C had a real enum type so that this would be distinct
 * from #cairo_status_t. Oh well, without that, I'll use this bogus 100
 * offset.  We want to keep it fit in int8_t as the compiler may choose
 * that for #cairo_status_t */
typedef enum _cairo_int_status {
    CAIRO_INT_STATUS_UNSUPPORTED = 100,
    CAIRO_INT_STATUS_DEGENERATE,
    CAIRO_INT_STATUS_NOTHING_TO_DO,
    CAIRO_INT_STATUS_FLATTEN_TRANSPARENCY,
    CAIRO_INT_STATUS_IMAGE_FALLBACK,
    CAIRO_INT_STATUS_ANALYZE_RECORDING_SURFACE_PATTERN,

    CAIRO_INT_STATUS_LAST_STATUS
} cairo_int_status_t;

/* XXX: Right now, the _cairo_color structure puts unpremultiplied
   color in the doubles and premultiplied color in the shorts. Yes,
   this is crazy insane, (but at least we don't export this
   madness). I'm still working on a cleaner API, but in the meantime,
   at least this does prevent precision loss in color when changing
   alpha. */
struct _cairo_color {
    double red;
    double green;
    double blue;
    double alpha;

    unsigned short red_short;
    unsigned short green_short;
    unsigned short blue_short;
    unsigned short alpha_short;
};
struct _cairo_color_stop {
    /* unpremultiplied */
    double red;
    double green;
    double blue;
    double alpha;

    /* unpremultipled, for convenience */
    uint16_t red_short;
    uint16_t green_short;
    uint16_t blue_short;
    uint16_t alpha_short;
};

typedef struct _cairo_line {
    cairo_point_t p1;
    cairo_point_t p2;
} cairo_line_t, cairo_box_t;

typedef struct _cairo_trapezoid {
    cairo_fixed_t top, bottom;
    cairo_line_t left, right;
} cairo_trapezoid_t;

struct _cairo_pattern {
    cairo_pattern_type_t	type;
    cairo_reference_count_t	ref_count;
    cairo_status_t		status;
    cairo_user_data_array_t	user_data;

    cairo_matrix_t		matrix;
    cairo_filter_t		filter;
    cairo_extend_t		extend;

    cairo_bool_t		has_component_alpha;
};

struct _cairo_solid_pattern {
    cairo_pattern_t base;
    cairo_color_t color;
};

typedef struct _cairo_surface_pattern {
    cairo_pattern_t base;

    cairo_surface_t *surface;
} cairo_surface_pattern_t;

typedef struct _cairo_gradient_stop {
    double offset;
    cairo_color_stop_t color;
} cairo_gradient_stop_t;

typedef struct _cairo_gradient_pattern {
    cairo_pattern_t base;

    unsigned int	    n_stops;
    unsigned int	    stops_size;
    cairo_gradient_stop_t  *stops;
    cairo_gradient_stop_t   stops_embedded[2];
} cairo_gradient_pattern_t;

typedef struct _cairo_linear_pattern {
    cairo_gradient_pattern_t base;

    cairo_point_t p1;
    cairo_point_t p2;
} cairo_linear_pattern_t;

typedef struct _cairo_radial_pattern {
    cairo_gradient_pattern_t base;

    cairo_point_t c1;
    cairo_fixed_t r1;
    cairo_point_t c2;
    cairo_fixed_t r2;
} cairo_radial_pattern_t;

typedef union {
    cairo_gradient_pattern_t base;

    cairo_linear_pattern_t linear;
    cairo_radial_pattern_t radial;
} cairo_gradient_pattern_union_t;

typedef union {
    cairo_pattern_type_t	    type;
    cairo_pattern_t		    base;

    cairo_solid_pattern_t	    solid;
    cairo_surface_pattern_t	    surface;
    cairo_gradient_pattern_union_t  gradient;
} cairo_pattern_union_t;

typedef struct _cairo_edge {
    cairo_line_t line;
    int top, bottom;
    int dir;
} cairo_edge_t;

typedef struct _cairo_polygon {
    cairo_status_t status;

    cairo_point_t first_point;
    cairo_point_t last_point;
    cairo_point_t current_point;
    cairo_slope_t current_edge;
    cairo_bool_t has_current_point;
    cairo_bool_t has_current_edge;

    cairo_box_t extents;
    cairo_box_t limit;
    const cairo_box_t *limits;
    int num_limits;

    int num_edges;
    int edges_size;
    cairo_edge_t *edges;
    cairo_edge_t  edges_embedded[32];
} cairo_polygon_t;

#endif
