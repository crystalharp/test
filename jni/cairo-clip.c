#include "cairo-clip-private.h"
#include "cairo-composite-rectangles-private.h"
#include "cairoint.h"

static void
_cairo_clip_path_destroy (cairo_clip_path_t *clip_path)
{
//    assert (CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&clip_path->ref_count));
//
//    if (! _cairo_reference_count_dec_and_test (&clip_path->ref_count))
//	return;
//
//    _cairo_path_fixed_fini (&clip_path->path);
//    if (clip_path->region != NULL)
//	cairo_region_destroy (clip_path->region);
//    if (clip_path->surface != NULL)
//	cairo_surface_destroy (clip_path->surface);
//
//    if (clip_path->prev != NULL)
//	_cairo_clip_path_destroy (clip_path->prev);
//
//    _freed_pool_put (&clip_path_pool, clip_path);
}
static cairo_clip_path_t *
_cairo_clip_path_reference (cairo_clip_path_t *clip_path)
{
    assert (CAIRO_REFERENCE_COUNT_HAS_REFERENCE (&clip_path->ref_count));
    clip_path->ref_count.ref_count++;
    return clip_path;
}

static const cairo_rectangle_int_t _cairo_empty_rectangle_int = { 0, 0, 0, 0 };
const cairo_rectangle_int_t *
_cairo_clip_get_extents (const cairo_clip_t *clip)
{
    if (clip->all_clipped)
	return &_cairo_empty_rectangle_int;

    if (clip->path == NULL)
	return NULL;

    return &clip->path->extents;
}

void
_cairo_clip_init (cairo_clip_t *clip)
{
    clip->all_clipped = FALSE;
    clip->path = NULL;
}

cairo_clip_t *
_cairo_clip_init_copy (cairo_clip_t *clip, cairo_clip_t *other)
{
    if (other != NULL) {
	clip->all_clipped = other->all_clipped;
	if (other->path == NULL) {
	    clip->path = NULL;
	    if (! clip->all_clipped)
		clip = NULL;
	} else {
	    clip->path = _cairo_clip_path_reference (other->path);
	}
    } else {
	_cairo_clip_init (clip);
	clip = NULL;
    }

    return clip;
}

void
_cairo_clip_reset (cairo_clip_t *clip)
{
    clip->all_clipped = FALSE;
    if (clip->path != NULL) {
	_cairo_clip_path_destroy (clip->path);
	clip->path = NULL;
    }
}

cairo_bool_t
_cairo_clip_contains_rectangle (cairo_clip_t *clip,
				const cairo_rectangle_int_t *rect)
{
    cairo_clip_path_t *clip_path;

    if (clip == NULL)
	return FALSE;

    clip_path = clip->path;
    if (clip_path->extents.x > rect->x ||
	clip_path->extents.y > rect->y ||
	clip_path->extents.x + clip_path->extents.width  < rect->x + rect->width ||
	clip_path->extents.y + clip_path->extents.height < rect->y + rect->height)
    {
	return FALSE;
    }

    do {
	cairo_box_t box;

	if ((clip_path->flags & CAIRO_CLIP_PATH_IS_BOX) == 0)
	    return FALSE;

	if (! _cairo_path_fixed_is_box (&clip_path->path, &box))
	    return FALSE;

	if (box.p1.x > _cairo_fixed_from_int (rect->x) ||
	    box.p1.y > _cairo_fixed_from_int (rect->y) ||
	    box.p2.x < _cairo_fixed_from_int (rect->x + rect->width) ||
	    box.p2.y < _cairo_fixed_from_int (rect->y + rect->height))
	{
	    return FALSE;
	}
    } while ((clip_path = clip_path->prev) != NULL);

    return TRUE;
}

cairo_bool_t
_cairo_clip_contains_extents (cairo_clip_t *clip,
			      const cairo_composite_rectangles_t *extents)
{
    const cairo_rectangle_int_t *rect;

    if (clip == NULL)
	return FALSE;

    rect = extents->is_bounded ? &extents->bounded : &extents->unbounded;
    return _cairo_clip_contains_rectangle (clip, rect);
}
static void
intersect_with_boxes (cairo_composite_rectangles_t *extents,
		      cairo_box_t *boxes,
		      int num_boxes)
{
    cairo_rectangle_int_t rect;
    cairo_box_t box;
    cairo_bool_t is_empty;

    box.p1.x = box.p1.y = INT_MIN;
    box.p2.x = box.p2.y = INT_MAX;
    while (num_boxes--) {
	if (boxes->p1.x < box.p1.x)
	    box.p1.x = boxes->p1.x;
	if (boxes->p1.y < box.p1.y)
	    box.p1.y = boxes->p1.y;

	if (boxes->p2.x > box.p2.x)
	    box.p2.x = boxes->p2.x;
	if (boxes->p2.y > box.p2.y)
	    box.p2.y = boxes->p2.y;
    }

    _cairo_box_round_to_rectangle (&box, &rect);
    is_empty = _cairo_rectangle_intersect (&extents->bounded, &rect);
    is_empty = _cairo_rectangle_intersect (&extents->unbounded, &rect);
}
static inline cairo_bool_t
_clip_paths_are_rectilinear (cairo_clip_path_t *clip_path)
{
    while (clip_path != NULL) {
	if (! clip_path->path.is_rectilinear)
	    return FALSE;

	clip_path = clip_path->prev;
    }

    return TRUE;
}

static cairo_status_t
_rectilinear_clip_to_boxes (const cairo_path_fixed_t *path,
			    cairo_fill_rule_t fill_rule,
			    cairo_box_t **boxes,
			    int *num_boxes,
			    int *size_boxes)
{
//    cairo_polygon_t polygon;
//    cairo_traps_t traps;
//    cairo_status_t status;

//    _cairo_traps_init (&traps);
//    _cairo_traps_limit (&traps, *boxes, *num_boxes);
//
//    _cairo_polygon_init (&polygon);
//    _cairo_polygon_limit (&polygon, *boxes, *num_boxes);
//
//    status = _cairo_path_fixed_fill_rectilinear_to_traps (path,
//							  fill_rule,
//							  &traps);
//    if (unlikely (_cairo_status_is_error (status)))
//	goto CLEANUP;
//    if (status == CAIRO_STATUS_SUCCESS)
//	goto BOXES;
//
//    /* tolerance will be ignored as the path is rectilinear */
//    status = _cairo_path_fixed_fill_to_polygon (path, 0., &polygon);
//    if (unlikely (status))
//	goto CLEANUP;
//
//    if (polygon.num_edges == 0) {
//	*num_boxes = 0;
//    } else {
//	status = _cairo_bentley_ottmann_tessellate_rectilinear_polygon (&traps,
//									&polygon,
//									fill_rule);
//	if (likely (status == CAIRO_STATUS_SUCCESS)) {
//	    int i;
//
//          BOXES:
//	    i = *size_boxes;
//	    if (i < 0)
//		i = -i;
//
//	    if (traps.num_traps > i) {
//		cairo_box_t *new_boxes;
//		int new_size;
//
//		new_size = pot (traps.num_traps);
//		new_boxes = _cairo_malloc_ab (new_size, sizeof (cairo_box_t));
//		if (unlikely (new_boxes == NULL)) {
//		    status = _cairo_error (CAIRO_STATUS_NO_MEMORY);
//		    goto CLEANUP;
//		}
//
//		if (*size_boxes > 0)
//		    free (*boxes);
//
//		*boxes = new_boxes;
//		*size_boxes = new_size;
//	    }
//
//	    for (i = 0; i < traps.num_traps; i++) {
//		(*boxes)[i].p1.x = traps.traps[i].left.p1.x;
//		(*boxes)[i].p1.y = traps.traps[i].top;
//		(*boxes)[i].p2.x = traps.traps[i].right.p1.x;
//		(*boxes)[i].p2.y = traps.traps[i].bottom;
//	    }
//	    *num_boxes = i;
//	}
//    }
//
//  CLEANUP:
//    _cairo_polygon_fini (&polygon);
//    _cairo_traps_fini (&traps);
//
//    return status;
    return CAIRO_STATUS_SUCCESS; 
}

static cairo_int_status_t
_cairo_clip_path_to_boxes (cairo_clip_path_t *clip_path,
			   cairo_box_t **boxes,
			   int *count)
{
    int size = -*count;
    int num_boxes = 0;
    cairo_status_t status;

    if (clip_path->region != NULL) {
	int num_rects, n;

	num_rects = cairo_region_num_rectangles (clip_path->region);
	if (num_rects > -size) {
	    cairo_box_t *new_boxes;

	    new_boxes = _cairo_malloc_ab (num_rects, sizeof (cairo_box_t));
	    if (unlikely (new_boxes == NULL))
		return CAIRO_STATUS_NO_MEMORY;

	    *boxes = new_boxes;
	}

	for (n = 0; n < num_rects; n++) {
	    cairo_rectangle_int_t rect;

	    cairo_region_get_rectangle (clip_path->region, n, &rect);
	    (*boxes)[n].p1.x = _cairo_fixed_from_int (rect.x);
	    (*boxes)[n].p1.y = _cairo_fixed_from_int (rect.y);
	    (*boxes)[n].p2.x = _cairo_fixed_from_int (rect.x + rect.width);
	    (*boxes)[n].p2.y = _cairo_fixed_from_int (rect.y + rect.height);
	}

	*count = num_rects;
	return CAIRO_STATUS_SUCCESS;
    }

    /* keep it simple at first */
    if (! _clip_paths_are_rectilinear (clip_path))
	return CAIRO_INT_STATUS_UNSUPPORTED;

    assert (-size >= 1);
    if (_cairo_path_fixed_is_box (&clip_path->path, *boxes)) {
	num_boxes = 1;
    } else {
	status = _rectilinear_clip_to_boxes (&clip_path->path,
					     clip_path->fill_rule,
					     boxes, &num_boxes, &size);
	if (unlikely (status))
	    return status;
    }

    while (num_boxes > 0 && (clip_path = clip_path->prev) != NULL) {
	cairo_box_t box;

	if (clip_path->region != NULL) {
//	    status = _region_clip_to_boxes (clip_path->region,
//					    boxes, &num_boxes, &size);
	    if (unlikely (status))
		return status;

	    break;
	} else if (_cairo_path_fixed_is_box (&clip_path->path, &box)) {
	    int i, j;

	    for (i = j = 0; i < num_boxes; i++) {
		if (j != i)
		    (*boxes)[j] = (*boxes)[i];

		if (box.p1.x > (*boxes)[j].p1.x)
		    (*boxes)[j].p1.x = box.p1.x;
		if (box.p2.x < (*boxes)[j].p2.x)
		    (*boxes)[j].p2.x = box.p2.x;

		if (box.p1.y > (*boxes)[j].p1.y)
		    (*boxes)[j].p1.y = box.p1.y;
		if (box.p2.y < (*boxes)[j].p2.y)
		    (*boxes)[j].p2.y = box.p2.y;

		j += (*boxes)[j].p2.x > (*boxes)[j].p1.x &&
		     (*boxes)[j].p2.y > (*boxes)[j].p1.y;
	    }

	    num_boxes = j;
	} else {
	    status = _rectilinear_clip_to_boxes (&clip_path->path,
						 clip_path->fill_rule,
						 boxes, &num_boxes, &size);
	    if (unlikely (status))
		return status;
	}
    }

    *count = num_boxes;
    return CAIRO_STATUS_SUCCESS;
}

cairo_int_status_t
_cairo_clip_get_boxes (cairo_clip_t *clip,
		       cairo_box_t **boxes,
		       int *count)
{
    cairo_int_status_t status;

    if (clip->all_clipped)
	return CAIRO_INT_STATUS_NOTHING_TO_DO;

    assert (clip->path != NULL);

    status = _cairo_clip_path_to_boxes (clip->path, boxes, count);
    if (status)
	return status;

    if (*count == 0) {
//	_cairo_clip_set_all_clipped (clip);
	return CAIRO_INT_STATUS_NOTHING_TO_DO;
    }

    return CAIRO_STATUS_SUCCESS;
}
cairo_status_t
_cairo_clip_rectangle (cairo_clip_t       *clip,
		       const cairo_rectangle_int_t *rectangle)
{
    if (clip->all_clipped)
	return CAIRO_STATUS_SUCCESS;

    if (rectangle->width == 0 || rectangle->height == 0) {
//	_cairo_clip_set_all_clipped (clip);
	return CAIRO_STATUS_SUCCESS;
    }

    /* if a smaller clip has already been set, ignore the new path */
    if (clip->path != NULL) {
	if (rectangle->x <= clip->path->extents.x &&
	    rectangle->y <= clip->path->extents.y &&
	    rectangle->x + rectangle->width  >= clip->path->extents.x + clip->path->extents.width &&
	    rectangle->y + rectangle->height >= clip->path->extents.y + clip->path->extents.height)
	{
	    return CAIRO_STATUS_SUCCESS;
	}
    }

    return CAIRO_STATUS_SUCCESS;//_cairo_clip_intersect_rectangle (clip, rectangle);
}
static cairo_bool_t
box_is_aligned (const cairo_box_t *box)
{
    return
	_cairo_fixed_is_integer (box->p1.x) &&
	_cairo_fixed_is_integer (box->p1.y) &&
	_cairo_fixed_is_integer (box->p2.x) &&
	_cairo_fixed_is_integer (box->p2.y);
}
cairo_int_status_t
_cairo_clip_get_region (cairo_clip_t *clip,
			cairo_region_t **region)
{
//    cairo_int_status_t status;
//
//    if (clip->all_clipped)
//	goto CLIPPED;
//
//    assert (clip->path != NULL);
//
//    status = _cairo_clip_path_to_region (clip->path);
//    if (status)
//	return status;
//
//    if (cairo_region_is_empty (clip->path->region)) {
//	_cairo_clip_set_all_clipped (clip);
//	goto CLIPPED;
//    }
//
//    if (region)
//	*region = clip->path->region;
    return CAIRO_STATUS_SUCCESS;
//
//  CLIPPED:
//    if (region)
//	*region = NULL;
//    return CAIRO_INT_STATUS_NOTHING_TO_DO;
}

cairo_status_t
_cairo_clip_to_boxes (cairo_clip_t **clip,
		      cairo_composite_rectangles_t *extents,
		      cairo_box_t **boxes,
		      int *num_boxes)
{
    cairo_status_t status;
    const cairo_rectangle_int_t *rect;

    rect = extents->is_bounded ? &extents->bounded : &extents->unbounded;

    if (*clip == NULL)
	goto EXTENTS;

    status = _cairo_clip_rectangle (*clip, rect);
    if (unlikely (status))
	return status;

    status = _cairo_clip_get_boxes (*clip, boxes, num_boxes);
    switch ((int) status) {
    case CAIRO_STATUS_SUCCESS:
	intersect_with_boxes (extents, *boxes, *num_boxes);
	if (rect->width == 0 || rect->height == 0 ||
	    extents->is_bounded ||
	    (*num_boxes == 1 && box_is_aligned (*boxes)))
	{
	    *clip = NULL;
	}
	goto DONE;

    case CAIRO_INT_STATUS_UNSUPPORTED:
	goto EXTENTS;

    default:
	return status;
    }

  EXTENTS:
    status = CAIRO_STATUS_SUCCESS;
    _cairo_box_from_rectangle (&(*boxes)[0], rect);
    *num_boxes = 1;
  DONE:
    return status;
}

static cairo_surface_t *
_cairo_clip_path_get_surface (cairo_clip_path_t *clip_path,
			      cairo_surface_t *target,
			      int *tx, int *ty)
{
//    const cairo_rectangle_int_t *clip_extents = &clip_path->extents;
//    cairo_bool_t need_translate;
//    cairo_surface_t *surface;
//    cairo_clip_path_t *prev;
//    cairo_status_t status;
//
//    while (clip_path->prev != NULL &&
//	   clip_path->flags & CAIRO_CLIP_PATH_IS_BOX &&
//	   clip_path->path.maybe_fill_region)
//    {
//	clip_path = clip_path->prev;
//    }
//
//    clip_extents = &clip_path->extents;
//    if (clip_path->surface != NULL &&
//	clip_path->surface->backend == target->backend)
//    {
//	*tx = clip_extents->x;
//	*ty = clip_extents->y;
//	return clip_path->surface;
//    }
//
//    surface = _cairo_surface_create_similar_scratch (target,
//						     CAIRO_CONTENT_ALPHA,
//						     clip_extents->width,
//						     clip_extents->height);
//    if (surface == NULL) {
//	surface = cairo_image_surface_create (CAIRO_FORMAT_A8,
//					      clip_extents->width,
//					      clip_extents->height);
//    }
//    if (unlikely (surface->status))
//	return surface;
//
//    need_translate = clip_extents->x | clip_extents->y;
//    if (clip_path->flags & CAIRO_CLIP_PATH_IS_BOX &&
//	clip_path->path.maybe_fill_region)
//    {
//	status = _cairo_surface_paint (surface,
//				       CAIRO_OPERATOR_SOURCE,
//				       &_cairo_pattern_white.base,
//				       NULL);
//	if (unlikely (status))
//	    goto BAIL;
//    }
//    else
//    {
//	status = _cairo_surface_paint (surface,
//				       CAIRO_OPERATOR_CLEAR,
//				       &_cairo_pattern_clear.base,
//				       NULL);
//	if (unlikely (status))
//	    goto BAIL;
//
//	if (need_translate) {
//	    _cairo_path_fixed_translate (&clip_path->path,
//					 _cairo_fixed_from_int (-clip_extents->x),
//					 _cairo_fixed_from_int (-clip_extents->y));
//	}
//	status = _cairo_surface_fill (surface,
//				      CAIRO_OPERATOR_ADD,
//				      &_cairo_pattern_white.base,
//				      &clip_path->path,
//				      clip_path->fill_rule,
//				      clip_path->tolerance,
//				      clip_path->antialias,
//				      NULL);
//	if (need_translate) {
//	    _cairo_path_fixed_translate (&clip_path->path,
//					 _cairo_fixed_from_int (clip_extents->x),
//					 _cairo_fixed_from_int (clip_extents->y));
//	}
//
//	if (unlikely (status))
//	    goto BAIL;
//    }
//
//    prev = clip_path->prev;
//    while (prev != NULL) {
//	if (prev->flags & CAIRO_CLIP_PATH_IS_BOX &&
//	    prev->path.maybe_fill_region)
//	{
//	    /* a simple box only affects the extents */
//	}
//	else if (prev->path.is_rectilinear ||
//		prev->surface == NULL ||
//		prev->surface->backend != target->backend)
//	{
//	    if (need_translate) {
//		_cairo_path_fixed_translate (&prev->path,
//					     _cairo_fixed_from_int (-clip_extents->x),
//					     _cairo_fixed_from_int (-clip_extents->y));
//	    }
//	    status = _cairo_surface_fill (surface,
//					  CAIRO_OPERATOR_IN,
//					  &_cairo_pattern_white.base,
//					  &prev->path,
//					  prev->fill_rule,
//					  prev->tolerance,
//					  prev->antialias,
//					  NULL);
//	    if (need_translate) {
//		_cairo_path_fixed_translate (&prev->path,
//					     _cairo_fixed_from_int (clip_extents->x),
//					     _cairo_fixed_from_int (clip_extents->y));
//	    }
//
//	    if (unlikely (status))
//		goto BAIL;
//	}
//	else
//	{
//	    cairo_surface_pattern_t pattern;
//	    cairo_surface_t *prev_surface;
//	    int prev_tx, prev_ty;
//
//	    prev_surface = _cairo_clip_path_get_surface (prev, target, &prev_tx, &prev_ty);
//	    status = prev_surface->status;
//	    if (unlikely (status))
//		goto BAIL;
//
//	    _cairo_pattern_init_for_surface (&pattern, prev_surface);
//	    pattern.base.filter = CAIRO_FILTER_NEAREST;
//	    cairo_matrix_init_translate (&pattern.base.matrix,
//					 clip_extents->x - prev_tx,
//					 clip_extents->y - prev_ty);
//	    status = _cairo_surface_paint (surface,
//					   CAIRO_OPERATOR_IN,
//					   &pattern.base,
//					   NULL);
//	    _cairo_pattern_fini (&pattern.base);
//
//	    if (unlikely (status))
//		goto BAIL;
//
//	    break;
//	}
//
//	prev = prev->prev;
//    }
//
//    *tx = clip_extents->x;
//    *ty = clip_extents->y;
//    cairo_surface_destroy (clip_path->surface);
//    return clip_path->surface = surface;
//
//  BAIL:
//    cairo_surface_destroy (surface);
//    return _cairo_surface_create_in_error (status);
//    HURTYOU
    printf("Should not be here!\n");
    return _cairo_surface_create_in_error (CAIRO_STATUS_NO_MEMORY);
}

cairo_surface_t *
_cairo_clip_get_surface (cairo_clip_t *clip, cairo_surface_t *target, int *tx, int *ty)
{
    /* XXX is_clear -> all_clipped */
    assert (clip->path != NULL);
    return _cairo_clip_path_get_surface (clip->path, target, tx, ty);
}
