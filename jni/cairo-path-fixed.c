#include "cairoint.h"
#include "cairo-path-fixed-private.h"
#include "cairo-slope-private.h"

#define cairo_path_head(path__) (&(path__)->buf.base)
#define cairo_path_tail(path__) cairo_path_buf_prev (cairo_path_head (path__))
#define cairo_path_buf_next(pos__) \
    cairo_list_entry ((pos__)->link.next, cairo_path_buf_t, link)
#define cairo_path_buf_prev(pos__) \
    cairo_list_entry ((pos__)->link.prev, cairo_path_buf_t, link)

static void
_cairo_path_buf_destroy (cairo_path_buf_t *buf);

void
_cairo_path_fixed_init (cairo_path_fixed_t *path)
{
//    VG (VALGRIND_MAKE_MEM_UNDEFINED (path, sizeof (cairo_path_fixed_t)));

    cairo_list_init (&path->buf.base.link);

    path->buf.base.num_ops = 0;
    path->buf.base.num_points = 0;
    path->buf.base.size_ops = ARRAY_LENGTH (path->buf.op);
    path->buf.base.size_points = ARRAY_LENGTH (path->buf.points);
    path->buf.base.op = path->buf.op;
    path->buf.base.points = path->buf.points;

    path->current_point.x = 0;
    path->current_point.y = 0;
    path->last_move_point = path->current_point;
    path->has_last_move_point = FALSE;
    path->has_current_point = FALSE;
    path->has_curve_to = FALSE;
    path->is_rectilinear = TRUE;
    path->maybe_fill_region = TRUE;
    path->is_empty_fill = TRUE;

    path->extents.p1.x = path->extents.p1.y = INT_MAX;
    path->extents.p2.x = path->extents.p2.y = INT_MIN;
}

void
_cairo_path_fixed_fini (cairo_path_fixed_t *path)
{
    cairo_path_buf_t *buf;

    buf = cairo_path_buf_next (cairo_path_head (path));
    while (buf != cairo_path_head (path)) {
	cairo_path_buf_t *this = buf;
	buf = cairo_path_buf_next (buf);
	_cairo_path_buf_destroy (this);
    }

//    VG (VALGRIND_MAKE_MEM_NOACCESS (path, sizeof (cairo_path_fixed_t)));
}

static cairo_status_t
_cairo_path_fixed_add (cairo_path_fixed_t  *path,
		       cairo_path_op_t	    op,
		       const cairo_point_t *points,
		       int		    num_points);

static void
_cairo_path_fixed_add_buf (cairo_path_fixed_t *path,
			   cairo_path_buf_t   *buf);

static cairo_path_buf_t *
_cairo_path_buf_create (int size_ops, int size_points);

static void
_cairo_path_buf_add_op (cairo_path_buf_t *buf,
			cairo_path_op_t   op);

static void
_cairo_path_buf_add_points (cairo_path_buf_t       *buf,
			    const cairo_point_t    *points,
			    int		            num_points);

static cairo_status_t
_cairo_path_fixed_add (cairo_path_fixed_t   *path,
		       cairo_path_op_t	     op,
		       const cairo_point_t  *points,
		       int		     num_points)
{
    cairo_path_buf_t *buf = cairo_path_tail (path);

    if (buf->num_ops + 1 > buf->size_ops ||
	buf->num_points + num_points > buf->size_points)
    {
	buf = _cairo_path_buf_create (buf->num_ops * 2, buf->num_points * 2);
//	if (unlikely (buf == NULL))
//	    return _cairo_error (CAIRO_STATUS_NO_MEMORY);

	_cairo_path_fixed_add_buf (path, buf);
    }

    if (WATCH_PATH) {
	const char *op_str[] = {
	    "move-to",
	    "line-to",
	    "curve-to",
	    "close-path",
	};
	char buf[1024];
	int len = 0;
	int i;

	len += snprintf (buf + len, sizeof (buf), "[");
	for (i = 0; i < num_points; i++) {
	    if (i != 0)
		len += snprintf (buf + len, sizeof (buf), " ");
	    len += snprintf (buf + len, sizeof (buf), "(%f, %f)",
			     _cairo_fixed_to_double (points[i].x),
			     _cairo_fixed_to_double (points[i].y));
	}
	len += snprintf (buf + len, sizeof (buf), "]");

	fprintf (stderr,
		 "_cairo_path_fixed_add (%s, %s)\n",
		 op_str[(int) op], buf);
    }

    _cairo_path_buf_add_op (buf, op);
    _cairo_path_buf_add_points (buf, points, num_points);

    return CAIRO_STATUS_SUCCESS;
}

static void
_cairo_path_buf_destroy (cairo_path_buf_t *buf)
{
    free (buf);
}

static void
_cairo_path_buf_add_op (cairo_path_buf_t *buf,
			cairo_path_op_t	  op)
{
    buf->op[buf->num_ops++] = op;
}

static void
_cairo_path_buf_add_points (cairo_path_buf_t       *buf,
			    const cairo_point_t    *points,
			    int		            num_points)
{
    memcpy (buf->points + buf->num_points,
	    points,
	    sizeof (points[0]) * num_points);
    buf->num_points += num_points;
}

cairo_status_t
_cairo_path_fixed_interpret (const cairo_path_fixed_t		*path,
			     cairo_direction_t			 dir,
			     cairo_path_fixed_move_to_func_t	*move_to,
			     cairo_path_fixed_line_to_func_t	*line_to,
			     cairo_path_fixed_curve_to_func_t	*curve_to,
			     cairo_path_fixed_close_path_func_t	*close_path,
			     void				*closure)
{
    const uint8_t num_args[] = {
	1, /* cairo_path_move_to */
	1, /* cairo_path_op_line_to */
	3, /* cairo_path_op_curve_to */
	0, /* cairo_path_op_close_path */
    };
    cairo_status_t status;
    const cairo_path_buf_t *buf, *first;
    cairo_bool_t forward = (dir == CAIRO_DIRECTION_FORWARD);
    int step = forward ? 1 : -1;

    buf = first = forward ? cairo_path_head (path) : cairo_path_tail (path);
    do {
	cairo_point_t *points;
	int start, stop, i;

	if (forward) {
	    start = 0;
	    stop = buf->num_ops;
	    points = buf->points;
	} else {
	    start = buf->num_ops - 1;
	    stop = -1;
	    points = buf->points + buf->num_points;
	}

	for (i = start; i != stop; i += step) {
	    cairo_path_op_t op = buf->op[i];

	    if (! forward)
		points -= num_args[(int) op];

	    switch (op) {
	    case CAIRO_PATH_OP_MOVE_TO:
		status = (*move_to) (closure, &points[0]);
		break;
	    case CAIRO_PATH_OP_LINE_TO:
		status = (*line_to) (closure, &points[0]);
		break;
	    case CAIRO_PATH_OP_CURVE_TO:
		status = (*curve_to) (closure, &points[0], &points[1], &points[2]);
		break;
	    default:
		ASSERT_NOT_REACHED;
	    case CAIRO_PATH_OP_CLOSE_PATH:
		status = (*close_path) (closure);
		break;
	    }
	    if (unlikely (status))
		return status;

	    if (forward)
		points += num_args[(int) op];
	}
    } while ((buf = forward ? cairo_path_buf_next (buf) : cairo_path_buf_prev (buf)) != first);

    return CAIRO_STATUS_SUCCESS;
}

static void
_cairo_path_fixed_add_buf (cairo_path_fixed_t *path,
			   cairo_path_buf_t   *buf)
{
    cairo_list_add_tail (&buf->link, &cairo_path_head (path)->link);
}

static cairo_path_buf_t *
_cairo_path_buf_create (int size_ops, int size_points)
{
    cairo_path_buf_t *buf;

    /* adjust size_ops to ensure that buf->points is naturally aligned */
    size_ops += sizeof (double) - ((sizeof (cairo_path_buf_t) + size_ops) % sizeof (double));
    buf = _cairo_malloc_ab_plus_c (size_points, sizeof (cairo_point_t), size_ops + sizeof (cairo_path_buf_t));
    if (buf) {
	buf->num_ops = 0;
	buf->num_points = 0;
	buf->size_ops = size_ops;
	buf->size_points = size_points;

	buf->op = (cairo_path_op_t *) (buf + 1);
	buf->points = (cairo_point_t *) (buf->op + size_ops);
    }

    return buf;
}

static cairo_path_op_t
_cairo_path_last_op (cairo_path_fixed_t *path)
{
    cairo_path_buf_t *buf;

    buf = cairo_path_tail (path);
    if (buf->num_ops == 0)
	return -1;

    return buf->op[buf->num_ops - 1];
}

static inline void
_cairo_path_fixed_extents_add (cairo_path_fixed_t *path,
			       const cairo_point_t *point)
{
    if (point->x < path->extents.p1.x)
	path->extents.p1.x = point->x;
    if (point->y < path->extents.p1.y)
	path->extents.p1.y = point->y;

    if (point->x > path->extents.p2.x)
	path->extents.p2.x = point->x;
    if (point->y > path->extents.p2.y)
	path->extents.p2.y = point->y;
}

cairo_status_t
_cairo_path_fixed_move_to (cairo_path_fixed_t  *path,
			   cairo_fixed_t	x,
			   cairo_fixed_t	y)
{
    cairo_status_t status;
    cairo_point_t point;

    point.x = x;
    point.y = y;

    /* If the previous op was also a MOVE_TO, then just change its
     * point rather than adding a new op. */
    if (_cairo_path_last_op (path) == CAIRO_PATH_OP_MOVE_TO) {
	cairo_path_buf_t *buf;

	buf = cairo_path_tail (path);
	buf->points[buf->num_points - 1] = point;
    } else {
	status = _cairo_path_fixed_add (path, CAIRO_PATH_OP_MOVE_TO, &point, 1);
	if (unlikely (status))
	    return status;

	if (path->has_current_point && path->is_rectilinear) {
	    /* a move-to is first an implicit close */
	    path->is_rectilinear = path->current_point.x == path->last_move_point.x ||
				   path->current_point.y == path->last_move_point.y;
	    path->maybe_fill_region &= path->is_rectilinear;
	}
	if (path->maybe_fill_region) {
	    path->maybe_fill_region =
		_cairo_fixed_is_integer (path->last_move_point.x) &&
		_cairo_fixed_is_integer (path->last_move_point.y);
	}
    }

    path->current_point = point;
    path->last_move_point = point;
    path->has_last_move_point = TRUE;
    path->has_current_point = TRUE;

    return CAIRO_STATUS_SUCCESS;
}

cairo_status_t
_cairo_path_fixed_line_to (cairo_path_fixed_t *path,
			   cairo_fixed_t	x,
			   cairo_fixed_t	y)
{
    cairo_status_t status;
    cairo_point_t point;

    point.x = x;
    point.y = y;

    /* When there is not yet a current point, the line_to operation
     * becomes a move_to instead. Note: We have to do this by
     * explicitly calling into _cairo_path_fixed_move_to to ensure
     * that the last_move_point state is updated properly.
     */
    if (! path->has_current_point)
	return _cairo_path_fixed_move_to (path, point.x, point.y);

    /* If the previous op was but the initial MOVE_TO and this segment
     * is degenerate, then we can simply skip this point. Note that
     * a move-to followed by a degenerate line-to is a valid path for
     * stroking, but at all other times is simply a degenerate segment.
     */
    if (_cairo_path_last_op (path) != CAIRO_PATH_OP_MOVE_TO) {
	if (x == path->current_point.x && y == path->current_point.y)
	    return CAIRO_STATUS_SUCCESS;
    }

    /* If the previous op was also a LINE_TO with the same gradient,
     * then just change its end-point rather than adding a new op.
     */
    if (_cairo_path_last_op (path) == CAIRO_PATH_OP_LINE_TO) {
	cairo_path_buf_t *buf;
	const cairo_point_t *p;

	buf = cairo_path_tail (path);
	if (likely (buf->num_points >= 2)) {
	    p = &buf->points[buf->num_points-2];
	} else {
	    cairo_path_buf_t *prev_buf = cairo_path_buf_prev (buf);
	    p = &prev_buf->points[prev_buf->num_points - (2 - buf->num_points)];
	}

	if (p->x == path->current_point.x && p->y == path->current_point.y) {
	    /* previous line element was degenerate, replace */
	    buf->points[buf->num_points - 1] = point;
	    goto FLAGS;
	} else {
	    cairo_slope_t prev, self;

	    _cairo_slope_init (&prev, p, &path->current_point);
	    _cairo_slope_init (&self, &path->current_point, &point);
	    if (_cairo_slope_equal (&prev, &self) &&
		/* cannot trim anti-parallel segments whilst stroking */
		! _cairo_slope_backwards (&prev, &self))
	    {
		buf->points[buf->num_points - 1] = point;
		goto FLAGS;
	    }
	}
    }

    status = _cairo_path_fixed_add (path, CAIRO_PATH_OP_LINE_TO, &point, 1);
    if (unlikely (status))
	return status;

  FLAGS:
    if (path->is_rectilinear) {
	path->is_rectilinear = path->current_point.x == x ||
			       path->current_point.y == y;
	path->maybe_fill_region &= path->is_rectilinear;
    }
    if (path->maybe_fill_region) {
	path->maybe_fill_region = _cairo_fixed_is_integer (x) &&
				  _cairo_fixed_is_integer (y);
    }
    if (path->is_empty_fill) {
	path->is_empty_fill = path->current_point.x == x &&
			      path->current_point.y == y;
    }

    path->current_point = point;
    if (path->has_last_move_point) {
	_cairo_path_fixed_extents_add (path, &path->last_move_point);
	path->has_last_move_point = FALSE;
    }
    _cairo_path_fixed_extents_add (path, &point);
    return CAIRO_STATUS_SUCCESS;
}

static inline void
_canonical_box (cairo_box_t *box,
		const cairo_point_t *p1,
		const cairo_point_t *p2)
{
    if (p1->x <= p2->x) {
	box->p1.x = p1->x;
	box->p2.x = p2->x;
    } else {
	box->p1.x = p2->x;
	box->p2.x = p1->x;
    }

    if (p1->y <= p2->y) {
	box->p1.y = p1->y;
	box->p2.y = p2->y;
    } else {
	box->p1.y = p2->y;
	box->p2.y = p1->y;
    }
}

/*
 * Check whether the given path contains a single rectangle.
 */
cairo_bool_t
_cairo_path_fixed_is_box (const cairo_path_fixed_t *path,
			  cairo_box_t *box)
{
    const cairo_path_buf_t *buf = cairo_path_head (path);

    if (! path->is_rectilinear)
	return FALSE;

    /* Do we have the right number of ops? */
    if (buf->num_ops < 4 || buf->num_ops > 6)
	return FALSE;

    /* Check whether the ops are those that would be used for a rectangle */
    if (buf->op[0] != CAIRO_PATH_OP_MOVE_TO ||
	buf->op[1] != CAIRO_PATH_OP_LINE_TO ||
	buf->op[2] != CAIRO_PATH_OP_LINE_TO ||
	buf->op[3] != CAIRO_PATH_OP_LINE_TO)
    {
	return FALSE;
    }

    /* we accept an implicit close for filled paths */
    if (buf->num_ops > 4) {
	/* Now, there are choices. The rectangle might end with a LINE_TO
	 * (to the original point), but this isn't required. If it
	 * doesn't, then it must end with a CLOSE_PATH. */
	if (buf->op[4] == CAIRO_PATH_OP_LINE_TO) {
	    if (buf->points[4].x != buf->points[0].x ||
		buf->points[4].y != buf->points[0].y)
		return FALSE;
	} else if (buf->op[4] != CAIRO_PATH_OP_CLOSE_PATH) {
	    return FALSE;
	}

	if (buf->num_ops == 6) {
	    /* A trailing CLOSE_PATH or MOVE_TO is ok */
	    if (buf->op[5] != CAIRO_PATH_OP_MOVE_TO &&
		buf->op[5] != CAIRO_PATH_OP_CLOSE_PATH)
		return FALSE;
	}
    }

    /* Ok, we may have a box, if the points line up */
    if (buf->points[0].y == buf->points[1].y &&
	buf->points[1].x == buf->points[2].x &&
	buf->points[2].y == buf->points[3].y &&
	buf->points[3].x == buf->points[0].x)
    {
	_canonical_box (box, &buf->points[0], &buf->points[2]);
	return TRUE;
    }

    if (buf->points[0].x == buf->points[1].x &&
	buf->points[1].y == buf->points[2].y &&
	buf->points[2].x == buf->points[3].x &&
	buf->points[3].y == buf->points[0].y)
    {
	_canonical_box (box, &buf->points[0], &buf->points[2]);
	return TRUE;
    }

    return FALSE;
}

cairo_status_t
_cairo_path_fixed_close_path (cairo_path_fixed_t *path)
{
    cairo_status_t status;

    if (! path->has_current_point)
	return CAIRO_STATUS_SUCCESS;

    /* If the previous op was also a LINE_TO back to the start, discard it */
    if (_cairo_path_last_op (path) == CAIRO_PATH_OP_LINE_TO) {
	if (path->current_point.x == path->last_move_point.x &&
	    path->current_point.y == path->last_move_point.y)
	{
	    cairo_path_buf_t *buf;
	    cairo_point_t *p;

	    buf = cairo_path_tail (path);
	    if (likely (buf->num_points >= 2)) {
		p = &buf->points[buf->num_points-2];
	    } else {
		cairo_path_buf_t *prev_buf = cairo_path_buf_prev (buf);
		p = &prev_buf->points[prev_buf->num_points - (2 - buf->num_points)];
	    }

	    path->current_point = *p;
	    buf->num_ops--;
	    buf->num_points--;
	}
    }

    status = _cairo_path_fixed_add (path, CAIRO_PATH_OP_CLOSE_PATH, NULL, 0);
    if (unlikely (status))
	return status;

    return _cairo_path_fixed_move_to (path,
				      path->last_move_point.x,
				      path->last_move_point.y);
}
#define cairo_path_foreach_buf_start(pos__, path__) \
    pos__ = cairo_path_head (path__); do
#define cairo_path_foreach_buf_end(pos__, path__) \
    while ((pos__ = cairo_path_buf_next (pos__)) !=  cairo_path_head (path__))

cairo_bool_t
_cairo_path_fixed_equal (const cairo_path_fixed_t *a,
			 const cairo_path_fixed_t *b)
{
    const cairo_path_buf_t *buf_a, *buf_b;
    const cairo_path_op_t *ops_a, *ops_b;
    const cairo_point_t *points_a, *points_b;
    int num_points_a, num_ops_a;
    int num_points_b, num_ops_b;

    if (a == b)
	return TRUE;

    /* use the flags to quickly differentiate based on contents */
    if (a->is_empty_fill != b->is_empty_fill ||
	a->has_curve_to != b->has_curve_to ||
	a->maybe_fill_region != b->maybe_fill_region ||
	a->is_rectilinear != b->is_rectilinear)
    {
	return FALSE;
    }

    if (a->extents.p1.x != b->extents.p1.x ||
	a->extents.p1.y != b->extents.p1.y ||
	a->extents.p2.x != b->extents.p2.x ||
	a->extents.p2.y != b->extents.p2.y)
    {
	return FALSE;
    }

    num_ops_a = num_points_a = 0;
    cairo_path_foreach_buf_start (buf_a, a) {
	num_ops_a    += buf_a->num_ops;
	num_points_a += buf_a->num_points;
    } cairo_path_foreach_buf_end (buf_a, a);

    num_ops_b = num_points_b = 0;
    cairo_path_foreach_buf_start (buf_b, b) {
	num_ops_b    += buf_b->num_ops;
	num_points_b += buf_b->num_points;
    } cairo_path_foreach_buf_end (buf_b, b);

    if (num_ops_a == 0 && num_ops_b == 0)
	return TRUE;

    if (num_ops_a != num_ops_b || num_points_a != num_points_b)
	return FALSE;

    buf_a = cairo_path_head (a);
    num_points_a = buf_a->num_points;
    num_ops_a = buf_a->num_ops;
    ops_a = buf_a->op;
    points_a = buf_a->points;

    buf_b = cairo_path_head (b);
    num_points_b = buf_b->num_points;
    num_ops_b = buf_b->num_ops;
    ops_b = buf_b->op;
    points_b = buf_b->points;

    while (TRUE) {
	int num_ops = MIN (num_ops_a, num_ops_b);
	int num_points = MIN (num_points_a, num_points_b);

	if (memcmp (ops_a, ops_b, num_ops * sizeof (cairo_path_op_t)))
	    return FALSE;
	if (memcmp (points_a, points_b, num_points * sizeof (cairo_point_t)))
	    return FALSE;

	num_ops_a -= num_ops;
	ops_a += num_ops;
	num_points_a -= num_points;
	points_a += num_points;
	if (num_ops_a == 0 || num_points_a == 0) {
	    if (num_ops_a || num_points_a)
		return FALSE;

	    buf_a = cairo_path_buf_next (buf_a);
	    if (buf_a == cairo_path_head (a))
		break;

	    num_points_a = buf_a->num_points;
	    num_ops_a = buf_a->num_ops;
	    ops_a = buf_a->op;
	    points_a = buf_a->points;
	}

	num_ops_b -= num_ops;
	ops_b += num_ops;
	num_points_b -= num_points;
	points_b += num_points;
	if (num_ops_b == 0 || num_points_b == 0) {
	    if (num_ops_b || num_points_b)
		return FALSE;

	    buf_b = cairo_path_buf_next (buf_b);
	    if (buf_b == cairo_path_head (b))
		break;

	    num_points_b = buf_b->num_points;
	    num_ops_b = buf_b->num_ops;
	    ops_b = buf_b->op;
	    points_b = buf_b->points;
	}
    }

    return TRUE;
}

void
_cairo_path_fixed_iter_init (cairo_path_fixed_iter_t *iter,
			     const cairo_path_fixed_t *path)
{
    iter->first = iter->buf = cairo_path_head (path);
    iter->n_op = 0;
    iter->n_point = 0;
}

static cairo_bool_t
_cairo_path_fixed_iter_next_op (cairo_path_fixed_iter_t *iter)
{
    if (++iter->n_op >= iter->buf->num_ops) {
	iter->buf = cairo_path_buf_next (iter->buf);
	if (iter->buf == iter->first) {
	    iter->buf = NULL;
	    return FALSE;
	}

	iter->n_op = 0;
	iter->n_point = 0;
    }

    return TRUE;
}

cairo_bool_t
_cairo_path_fixed_iter_is_fill_box (cairo_path_fixed_iter_t *_iter,
				    cairo_box_t *box)
{
    cairo_point_t points[5];
    cairo_path_fixed_iter_t iter;

    if (_iter->buf == NULL)
	return FALSE;

    iter = *_iter;

    if (iter.n_op == iter.buf->num_ops &&
	! _cairo_path_fixed_iter_next_op (&iter))
    {
	return FALSE;
    }

    /* Check whether the ops are those that would be used for a rectangle */
    if (iter.buf->op[iter.n_op] != CAIRO_PATH_OP_MOVE_TO)
	return FALSE;
    points[0] = iter.buf->points[iter.n_point++];
    if (! _cairo_path_fixed_iter_next_op (&iter))
	return FALSE;

    if (iter.buf->op[iter.n_op] != CAIRO_PATH_OP_LINE_TO)
	return FALSE;
    points[1] = iter.buf->points[iter.n_point++];
    if (! _cairo_path_fixed_iter_next_op (&iter))
	return FALSE;

    if (iter.buf->op[iter.n_op] != CAIRO_PATH_OP_LINE_TO)
	return FALSE;
    points[2] = iter.buf->points[iter.n_point++];
    if (! _cairo_path_fixed_iter_next_op (&iter))
	return FALSE;

    if (iter.buf->op[iter.n_op] != CAIRO_PATH_OP_LINE_TO)
	return FALSE;
    points[3] = iter.buf->points[iter.n_point++];
    if (! _cairo_path_fixed_iter_next_op (&iter))
	return FALSE;

    /* Now, there are choices. The rectangle might end with a LINE_TO
     * (to the original point), but this isn't required. If it
     * doesn't, then it must end with a CLOSE_PATH (which may be implicit). */
    if (iter.buf->op[iter.n_op] == CAIRO_PATH_OP_LINE_TO)
    {
	points[4] = iter.buf->points[iter.n_point++];
	if (points[4].x != points[0].x || points[4].y != points[0].y)
	    return FALSE;
    }
    else if (! (iter.buf->op[iter.n_op] == CAIRO_PATH_OP_CLOSE_PATH ||
		iter.buf->op[iter.n_op] == CAIRO_PATH_OP_MOVE_TO))
    {
	return FALSE;
    }
    if (! _cairo_path_fixed_iter_next_op (&iter))
	return FALSE;

    /* Ok, we may have a box, if the points line up */
    if (points[0].y == points[1].y &&
	points[1].x == points[2].x &&
	points[2].y == points[3].y &&
	points[3].x == points[0].x)
    {
	box->p1 = points[0];
	box->p2 = points[2];
	*_iter = iter;
	return TRUE;
    }

    if (points[0].x == points[1].x &&
	points[1].y == points[2].y &&
	points[2].x == points[3].x &&
	points[3].y == points[0].y)
    {
	box->p1 = points[1];
	box->p2 = points[3];
	*_iter = iter;
	return TRUE;
    }

    return FALSE;
}

cairo_bool_t
_cairo_path_fixed_iter_at_end (const cairo_path_fixed_iter_t *iter)
{
    if (iter->buf == NULL)
	return TRUE;

    if (iter->n_op == iter->buf->num_ops)
	return TRUE;

    if (iter->buf->op[iter->n_op] == CAIRO_PATH_OP_MOVE_TO &&
	iter->buf->num_ops == iter->n_op + 1)
    {
	return TRUE;
    }

    return FALSE;
}
