#include "cairo-boxes-private.h"
#include "cairo-path-fixed-private.h"
#include "cairo-region-private.h"

#include "cairoint.h"

typedef struct cairo_filler {
    double tolerance;
    cairo_polygon_t *polygon;
} cairo_filler_t;

static void
_cairo_filler_init (cairo_filler_t *filler,
		    double tolerance,
		    cairo_polygon_t *polygon)
{
    filler->tolerance = tolerance;
    filler->polygon = polygon;
}

static void
_cairo_filler_fini (cairo_filler_t *filler)
{
}

static cairo_status_t
_cairo_filler_move_to (void *closure,
		       const cairo_point_t *point)
{
    cairo_filler_t *filler = closure;
    cairo_polygon_t *polygon = filler->polygon;

    return _cairo_polygon_close (polygon) ||
           _cairo_polygon_move_to (polygon, point);
}

static cairo_status_t
_cairo_filler_line_to (void *closure,
		       const cairo_point_t *point)
{
    cairo_filler_t *filler = closure;
    return _cairo_polygon_line_to (filler->polygon, point);
}

static cairo_status_t
_cairo_filler_curve_to (void *closure,
			const cairo_point_t *b,
			const cairo_point_t *c,
			const cairo_point_t *d)
{
    cairo_filler_t *filler = closure;
    cairo_spline_t spline;

    if (! _cairo_spline_init (&spline,
			      _cairo_filler_line_to, filler,
			      &filler->polygon->current_point, b, c, d))
    {
	return _cairo_filler_line_to (closure, d);
    }

    return _cairo_spline_decompose (&spline, filler->tolerance);
}

static cairo_status_t
_cairo_filler_close_path (void *closure)
{
    cairo_filler_t *filler = closure;
    return _cairo_polygon_close (filler->polygon);
}

static cairo_status_t
_cairo_path_fixed_fill_rectilinear_tessellate_to_boxes (const cairo_path_fixed_t *path,
							cairo_fill_rule_t fill_rule,
							cairo_boxes_t *boxes)
{
    cairo_polygon_t polygon;
    cairo_status_t status;

    _cairo_polygon_init (&polygon);
    if (boxes->num_limits) {
	_cairo_polygon_limit (&polygon, boxes->limits, boxes->num_limits);
	boxes->num_limits = 0;
    }

    /* tolerance will be ignored as the path is rectilinear */
    status = _cairo_path_fixed_fill_to_polygon (path, 0., &polygon);
    if (likely (status == CAIRO_STATUS_SUCCESS)) {
	status =
	    _cairo_bentley_ottmann_tessellate_rectilinear_polygon_to_boxes (&polygon,
									    fill_rule,
									    boxes);
    }

    _cairo_polygon_fini (&polygon);

    return status;
}

cairo_status_t
_cairo_path_fixed_fill_rectilinear_to_boxes (const cairo_path_fixed_t *path,
					     cairo_fill_rule_t fill_rule,
					     cairo_boxes_t *boxes)
{
    cairo_path_fixed_iter_t iter;
    cairo_status_t status;
    cairo_box_t box;

    if (_cairo_path_fixed_is_box (path, &box))
	return _cairo_boxes_add (boxes, &box);

    _cairo_path_fixed_iter_init (&iter, path);
    while (_cairo_path_fixed_iter_is_fill_box (&iter, &box)) {
	if (box.p1.y == box.p2.y || box.p1.x == box.p2.x)
	    continue;

	if (box.p1.y > box.p2.y) {
	    cairo_fixed_t t;

	    t = box.p1.y;
	    box.p1.y = box.p2.y;
	    box.p2.y = t;

	    t = box.p1.x;
	    box.p1.x = box.p2.x;
	    box.p2.x = t;
	}

	status = _cairo_boxes_add (boxes, &box);
	if (unlikely (status))
	    return status;
    }

    if (_cairo_path_fixed_iter_at_end (&iter))
	return _cairo_bentley_ottmann_tessellate_boxes (boxes, fill_rule, boxes);

    /* path is not rectangular, try extracting clipped rectilinear edges */
    _cairo_boxes_clear (boxes);
    return _cairo_path_fixed_fill_rectilinear_tessellate_to_boxes (path,
								   fill_rule,
								   boxes);
}

cairo_status_t
_cairo_path_fixed_fill_to_polygon (const cairo_path_fixed_t *path,
				   double tolerance,
				   cairo_polygon_t *polygon)
{
    cairo_filler_t filler;
    cairo_status_t status;

    _cairo_filler_init (&filler, tolerance, polygon);

    status = _cairo_path_fixed_interpret (path,
					  CAIRO_DIRECTION_FORWARD,
					  _cairo_filler_move_to,
					  _cairo_filler_line_to,
					  _cairo_filler_curve_to,
					  _cairo_filler_close_path,
					  &filler);
    if (unlikely (status))
	return status;

    status = _cairo_polygon_close (polygon);
    _cairo_filler_fini (&filler);

    return status;
}

