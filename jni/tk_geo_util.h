//
//  tk_geo_util.h
//  tigermap
//
//  Created by Chen Ming on 13-6-14.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_geo_util_h
#define tigermap_tk_geo_util_h

#include "tk_types.h"

#define LEVEL_MAX 19
#define LEVEL_MIN 5
#define LEVEL_SKIP 9

/* earth radius */
#define MATH_PI  3.141592653589793238
#define EARTH_RADIUS 6378137


#define TK_ENV_NO_OVERLAP( rect1, rect2) \
(((rect1).right < (rect2).left) || ((rect1).top > (rect2).bottom) \
|| ((rect1).left > (rect2).right) || ((rect1).bottom < (rect2).top))

#define TK_POINT_IN_ENV( pt, rect) (pt->x >= rect.left && pt->x <= rect.right \
&& pt->y >= rect.top && pt->y <= rect.bottom)

int tk_vector_cross(const tk_point_t *p1, const tk_point_t *p2, const tk_point_t *p3);
int tk_is_point_in_polygon(const tk_point_t *polygon, int point_num, const tk_point_t *point);
tk_bool_t tk_is_point_in_region(int rid, const tk_point_t *point);
tk_bool_t tk_vector_rect_cross(const tk_point_t *p1, const tk_point_t *p2, const tk_envelope_t *rect);

int tk_morton_equal(int morton_tile, int morton_file, int morton_level);
int tk_get_morton_code(int x, int y);

//Liang-Barsky Line Clipping  Test
int tk_lb_clip_test(float p, float q, float *u1, float *u2);

typedef enum _tk_rect_relation {
    TK_RECT_DISJOINT,
    TK_RECT_INTERSECT,
    TK_RECT_COVER,
    TK_RECT_EMBODY,
} tk_rect_relation_t;

tk_rect_relation_t tk_geo_get_rect_relation(tk_envelope_t rect1, tk_envelope_t rect2);

void tk_get_mercator_tile_box(int tile_size_bit, int tile_x, int tile_y, int zoom, tk_envelope_t *tile_mercator_box);

tk_bool_t tk_is_tile_adjacent(tk_base_tile_data_t *tile1, tk_base_tile_data_t *tile2);

#endif
