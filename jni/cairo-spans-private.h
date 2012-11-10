#ifndef CAIRO_SPANS_PRIVATE_H
#define CAIRO_SPANS_PRIVATE_H
#include "cairo-types-private.h"

/* A structure representing an open-ended horizontal span of constant
 * pixel coverage. */
typedef struct _cairo_half_open_span {
    /* The inclusive x-coordinate of the start of the span. */
    int x;

    /* The pixel coverage for the pixels to the right. */
    int coverage;
} cairo_half_open_span_t;

/* Span renderer interface. Instances of renderers are provided by
 * surfaces if they want to composite spans instead of trapezoids. */
typedef struct _cairo_span_renderer cairo_span_renderer_t;
struct _cairo_span_renderer {
    /* Private status variable. */
    cairo_status_t status;

    /* Called to destroy the renderer. */
    cairo_destroy_func_t	destroy;

    /* Render the spans on row y of the destination by whatever compositing
     * method is required. */
    cairo_warn cairo_status_t
    (*render_rows) (void *abstract_renderer,
		    int y, int height,
		    const cairo_half_open_span_t	*coverages,
		    unsigned num_coverages);

    /* Called after all rows have been rendered to perform whatever
     * final rendering step is required.  This function is called just
     * once before the renderer is destroyed. */
    cairo_status_t (*finish) (void *abstract_renderer);
};

/* Scan converter interface. */
typedef struct _cairo_scan_converter cairo_scan_converter_t;
struct _cairo_scan_converter {
    /* Destroy this scan converter. */
    cairo_destroy_func_t	destroy;

    /* Add a single edge to the converter. */
    cairo_status_t (*add_edge) (void		    *abstract_converter,
				const cairo_point_t *p1,
				const cairo_point_t *p2,
				int top, int bottom,
				int dir);

    /* Add a polygon (set of edges) to the converter. */
    cairo_status_t (*add_polygon) (void		    *abstract_converter,
				   const cairo_polygon_t  *polygon);

    /* Generates coverage spans for rows for the added edges and calls
     * the renderer function for each row. After generating spans the
     * only valid thing to do with the converter is to destroy it. */
    cairo_status_t (*generate) (void			*abstract_converter,
				cairo_span_renderer_t	*renderer);

    /* Private status. Read with _cairo_scan_converter_status(). */
    cairo_status_t status;
};

/* Scan converter constructors. */

cairo_private cairo_scan_converter_t *
_cairo_tor_scan_converter_create (int			xmin,
				  int			ymin,
				  int			xmax,
				  int			ymax,
				  cairo_fill_rule_t	fill_rule);

/* Scan converter constructors. */
typedef struct _cairo_rectangular_scan_converter {
    cairo_scan_converter_t base;

    int xmin, xmax;
    int ymin, ymax;

    struct _cairo_rectangular_scan_converter_chunk {
	struct _cairo_rectangular_scan_converter_chunk *next;
	void *base;
	int count;
	int size;
    } chunks, *tail;
    char buf[CAIRO_STACK_BUFFER_SIZE];
    int num_rectangles;
} cairo_rectangular_scan_converter_t;

cairo_private cairo_scan_converter_t *
_cairo_tor_scan_converter_create (int			xmin,
				  int			ymin,
				  int			xmax,
				  int			ymax,
				  cairo_fill_rule_t	fill_rule);
cairo_private void
_cairo_rectangular_scan_converter_init (cairo_rectangular_scan_converter_t *self,
					const cairo_rectangle_int_t *extents);
cairo_private cairo_status_t
_cairo_rectangular_scan_converter_add_box (cairo_rectangular_scan_converter_t *self,
					   const cairo_box_t *box,
					   int dir);

#endif /* CAIRO_SPANS_PRIVATE_H */
