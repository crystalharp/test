//
//  tk_geo_util.c
//  tigermap
//
//  Created by Chen Ming on 13-6-14.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <math.h>
#include "tk_geo_util.h"
#include "tk_types.h"
#include "tk_util.h"
#include "tk_tile_data.h"
#include "tk_global_info.h"


//顺时针返回正，逆时针返回负，共线返回0(y轴向下)
int tk_vector_cross(const tk_point_t *p1, const tk_point_t *p2, const tk_point_t *p3) {
    long long result = (p2->x - p1->x) * (long long)(p3->y - p2->y) - (p3->x - p2->x) * (long long)(p2->y - p1->y);//防止溢出
    if (result > 0) {
        return 1;
    }
    if (result < 0) {
        return -1;
    }
    return 0;
}

//polygon是环形点集，即polygon[0] == polygon[n-1]
int tk_is_point_in_polygon(const tk_point_t *polygon, int point_num, const tk_point_t *point) {
    int i;
    int inside, redo;
    tk_point_t pt = *point;
    
    for (i = 0; i < point_num; ++i) {
        if (polygon[i].x == pt.x &&    // 是否在顶点上
            polygon[i].y == pt.y ) {
            return 1;
        }
    }
    redo = 1;
    // y轴向下，x轴向右
    while (redo) {
        redo = 0;
        inside = 0;
        for (i = 0; i < point_num - 1; i++) {
            if (polygon[i].y < polygon[i + 1].y && polygon[i].y < pt.y && pt.y < polygon[i + 1].y) {
                if (pt.x <= polygon[i].x || pt.x <= polygon[i + 1].x) {
                    int clock = tk_vector_cross(polygon + i, &pt, polygon + i + 1);
                    if (clock < 0) {         //点i->pt->i+1为逆时针，则pt在线的左侧
                        inside = !inside;
                    }
                    else if (clock == 0) {    // 在线上
                        return 1;
                    }
                }
            }
            else if (polygon[i + 1].y < polygon[i].y && polygon[i + 1].y < pt.y && pt.y < polygon[i].y) {
                if (pt.x <= polygon[i].x || pt.x <= polygon[i + 1].x) {
                    int clock = tk_vector_cross(polygon + i + 1, &pt, polygon + i);
                    if (clock < 0) {         //点i + 1->pt->i为逆时针，则pt在线的左侧
                        inside = !inside;
                    }
                    else if (clock == 0) {    // 在线上
                        return 1;
                    }
                }
            }
            else if ( polygon[i].y ==  polygon[i + 1].y && // 在水平的边界线上
                     pt.y == polygon[i].y &&
                     ( (polygon[i].x < pt.x && pt.x < polygon[i + 1].x) ||
                      (polygon[i + 1].x < pt.x && pt.x < polygon[i].x) ) ) {
                         return 1;
                     }
            else if ( pt.y == polygon[i].y) {
                if (pt.x < polygon[i].x) {   // 交点在顶点上
                    polygon[i].y > polygon[i + 1].y ? --pt.y : ++pt.y;
                    redo = 1;
                    break;
                }
            }
        }
    }
    return inside;
}

tk_bool_t tk_is_point_in_region(int rid, const tk_point_t *point) {
    int i, buf_idx = 0;
    tk_bool_t inside;
    tk_point_t pt = *point;
    tk_point_t p1, p2;
    const unsigned char *buf = tk_global_info.region_polygon_buf + tk_global_info.reg_bounds[rid].offset;
    int point_num = buf[buf_idx ++];
    //    if (rid == 103) {
    //        LOG_INFO("in debug");
    //    }
    // y轴向下，x轴向右
    inside = 0;
    //    if (rid == 103) {
    //        LOG_INFO("p[%i].x = %i, p[%i].y = %i", 0, p1.x, 0, p1.y);
    //    }
    for (i = 0; i < point_num; i++) {
        p2.level_code = (buf[buf_idx] >> 7) & 0x1f;
        p2.x = ((buf[buf_idx] & 0x7f) << 24) + (buf[buf_idx + 1] << 16) + (buf[buf_idx + 2] << 8) + buf[buf_idx + 3];
        p2.y = (buf[buf_idx + 4] << 24) + (buf[buf_idx + 5] << 16) + (buf[buf_idx + 6] << 8) + buf[buf_idx + 7];
        buf_idx += 8;
        if (p2.level_code) {
//            if (inside) {
//                return inside;
//            }
            p1 = p2;
            continue;
        }
        //        if (rid == 103) {
        //            LOG_INFO("p[%i].x = %i, p[%i].y = %i", i, p2.x, i, p2.y);
        //        }
        if (p1.y < p2.y && p1.y < pt.y && pt.y < p2.y) {
            if (pt.x >= p1.x || pt.x >= p2.x) {
                int clock = tk_vector_cross(&p1, &pt, &p2);
                if (clock > 0) {         //点i->pt->i+1为顺时针，则pt在线的右侧
                    inside = !inside;
                }
                else if (clock == 0) {    // 在线上
                    return TK_YES;
                }
            }
        }
        else if (p2.y < p1.y && p2.y < pt.y && pt.y < p1.y) {
            if (pt.x >= p1.x || pt.x >= p2.x) {
                int clock = tk_vector_cross(&p2, &pt, &p1);
                if (clock > 0) {         //点i + 1->pt->i为顺时针，则pt在线的右侧
                    inside = !inside;
                }
                else if (clock == 0) {    // 在线上
                    return TK_YES;
                }
            }
        }
        else if ( p1.y == p2.y && // 在水平的边界线上
                 pt.y == p1.y &&
                 ( (p1.x < pt.x && pt.x < p2.x) ||
                  (p2.x < pt.x && pt.x < p1.x) ) ) {
                     return TK_YES;
                 }
        else if (p1.y != p2.y) {
            if( (pt.y == p1.y && pt.x > p1.x)){   // 交点在顶点上
                inside = !inside;
                if (pt.y > p2.y) {
                    --pt.y;
                }
                else {
                    ++pt.y;
                }
            }
        }
        p1 = p2;
    }
    return inside;
}

tk_bool_t tk_vector_rect_cross(const tk_point_t *p1, const tk_point_t *p2, const tk_envelope_t *rect) {
    tk_point_t rect_point;
    if ((p1->x < rect->left && p2->x < rect->left) ||
        (p1->x > rect->right && p2->x > rect->right) ||
        (p1->y < rect->top && p2->y < rect->top) ||
        (p1->y > rect->bottom && p2->y > rect->bottom)) {
        return TK_NO;
    }
    if ((p1->x <= rect->right && p1->x >= rect->left && p1->y <= rect->bottom && p1->y >= rect->top) ||
        (p2->x <= rect->right && p2->x >= rect->left && p2->y <= rect->bottom && p2->y >= rect->top)) {
        return TK_YES;
    }
    int clock0 = 0, clock1 = 0, clock2 = 0, clock3 = 0;
    rect_point.x = rect->left;
    rect_point.y = rect->bottom;
    clock0 = tk_vector_cross(p1, &rect_point, p2);
    if (clock0 == 0) {
        return TK_YES;
    }
    rect_point.x = rect->left;
    rect_point.y = rect->top;
    clock1 = tk_vector_cross(p1, &rect_point, p2);
    if (clock1 == 0 || (clock0 ^ clock1) < 0) {
        return TK_YES;
    }
    rect_point.x = rect->right;
    rect_point.y = rect->top;
    clock2 = tk_vector_cross(p1, &rect_point, p2);
    if (clock2 == 0 || (clock1 ^ clock2) < 0) {
        return TK_YES;
    }
    rect_point.x = rect->right;
    rect_point.y = rect->bottom;
    clock3 = tk_vector_cross(p1, &rect_point, p2);
    
    if (clock3 == 0 || ((clock2 ^ clock3) < 0) ||
        ((clock3 ^ clock0) < 0) ) {
        return TK_YES;
    }
    else {
        return TK_NO;
    }
}

int tk_morton_equal(int morton_tile, int morton_file, int morton_level) {
    //例如在合并次数为1时， 编号为1，2，3，4的tile是相等的。合并次数为2时，编号为1～16的tile是相等的。
    if (((morton_tile - morton_file) >= 0)
        && ((morton_tile - morton_file) >> (morton_level * 2) == 0))
        return 1;
    else
        return 0;
}

int tk_get_morton_code(int x, int y) {
    int j, newX, newY;
    if (y == 0) {
        if (x == 0)
            return 0;
        if (x == 1)
            return 1;
    }
    if (y == 1) {
        if (x == 0)
            return 2;
        if (x == 1)
            return 3;
    }
    if (x == 0) {
        newX = -1;
    } else {
        j = x;
        newX = 0;
        while (j > 1) {
            j = j >> 1;
            newX++;
        }
    }
    j = y;
    newY = 0;
    while (j > 1) {
        j = j >> 1;
        newY++;
    }
    
    if (newX > newY) {
        return ((1 << (newX << 1)) + tk_get_morton_code(x - (1 << newX), y));
    } else if (newY > newX) {
        return ((2 << (newY << 1)) + tk_get_morton_code(x, y - (1 << newY)));
    } else {
        return ((3 << (newY << 1)) + tk_get_morton_code(x - (1 << newX), y - (1 << newY)));
    }
}

//Liang-Barsky Line Clipping  Test
int tk_lb_clip_test(float p, float q, float *u1, float *u2) {
    float r;
    int retval = 1;
    
    if (p < 0.0) {//out->in
        r = q / p;
        if (r > *u2)
            retval = 0;
        else if (r > *u1)
            *u1 = r;
    } else if (p > 0.0) {//in->out
        r = q / p;
        if (r < * u1)
            retval = 0;
        else if (r < *u2)
            *u2 = r;
    } else {
        /* p = 0, so line is parallel to this clipping edge */
        if (q < 0.0)
        /* Line is outside clipping edge */
            retval = 0;
    }
    return retval;
}

/*  判断两个矩形之间的位置关系
 *  Retuern value: 0无交集，1有交集，2、3包含关系
 *  */
tk_rect_relation_t tk_geo_get_rect_relation(tk_envelope_t rect1, tk_envelope_t rect2) {
    if ((rect1.right <= rect2.left) || (rect1.bottom <= rect2.top) ||
        (rect1.left >= rect2.right) || (rect1.top >= rect2.bottom)) {
        return TK_RECT_DISJOINT;
    } else if ((rect1.left < rect2.left) && (rect1.right > rect2.right)
               && (rect1.bottom > rect2.bottom) && (rect1.top < rect2.top)) {
        return TK_RECT_COVER;		// rect1包含rect2
    } else if ((rect1.left > rect2.left) && (rect1.right < rect2.right)
               && (rect1.bottom < rect2.bottom) && (rect1.top > rect2.top)) {
        return TK_RECT_EMBODY;		// rect2包含rect1
    } else {
        return TK_RECT_INTERSECT;		// rect1和rect2交叉
    }
}


// tile的mercator包围盒, 用于寻找相关region
//void tk_get_mercator_tile_box(int tile_x, int tile_y, int zoom, tk_envelope_t *tile_mercator_box) {
//    tile_mercator_box->left = tile_x << (24 - zoom);
//    tile_mercator_box->right = (tile_x + 1) << (24 - zoom);
//    tile_mercator_box->top = tile_y << (24 - zoom);
//    tile_mercator_box->bottom = (tile_y + 1) << (24 - zoom);
//}
void tk_get_mercator_tile_box(int tile_size_bit, int tile_x, int tile_y, int zoom, int diff, tk_envelope_t *tile_mercator_box) {
    int d = tile_size_bit - 8;
    if(diff > 0) {
        tile_mercator_box->left = tile_x << (24 - zoom + d);
        tile_mercator_box->right = (tile_x + 1) << (24 - zoom + d);
        tile_mercator_box->top = tile_y << (24 - zoom + d);
        tile_mercator_box->bottom = (tile_y + 1) << (24 - zoom + d);
    }
    else {
        tile_mercator_box->left = (tile_x >> (-diff)) << (24 - zoom + d - diff);
        tile_mercator_box->right = ((tile_x >> (-diff)) + 1) << (24 - zoom + d - diff);
        tile_mercator_box->top = (tile_y >> (-diff)) << (24 - zoom + d - diff);
        tile_mercator_box->bottom = ((tile_y >> (-diff)) + 1) << (24 - zoom + d - diff);
    }
}

tk_bool_t tk_is_tile_adjacent(tk_base_tile_data_t *tile1, tk_base_tile_data_t *tile2) {
    unsigned int tile1_right = tile1->merged_tile_x + (1 << tile1->merged_level);
    unsigned int tile1_bottom = tile1->merged_tile_y + (1 << tile1->merged_level);
    unsigned int tile2_right = tile2->merged_tile_x + (1 << tile2->merged_level);
    unsigned int tile2_bottom = tile2->merged_tile_y + (1 << tile2->merged_level);
    if (!(tile1_right < tile2->merged_tile_x || tile2_right < tile1->merged_tile_x) &&
        !(tile1_bottom < tile2->merged_tile_y || tile2_bottom < tile1->merged_tile_y)) {
        return TK_YES;
    }
    else {
        return TK_NO;
    }
}


/* map scale to the meters, use the zoom level as the index to get the value */
static const int scale2meter[20] = {0, 0, 0, 0, 0, 200000, 100000, 50000, 25000,
    10000, 5000, 2000, 1000, 500, 200, 100, 50, 25, 10, 5};

double tk_meters_per_pixel(double lat, short int z) {
	return (cos(lat * MATH_PI / 180)) * MATH_PI * 2 * EARTH_RADIUS / ((1 << z) * tk_global_info.tile_size_pix);
}

int tk_get_pix_count_of_scale(double lat, short int z) {
    double meters_per_pixel;
	double scale_in_meters;
	int scale_in_pixels;
	if (z < LEVEL_MIN || z > LEVEL_MAX) {
        return -1;
    }
    meters_per_pixel = tk_meters_per_pixel(lat, z);
    scale_in_meters = scale2meter[z];
    scale_in_pixels = (int) roundf(scale_in_meters / meters_per_pixel);
    return scale_in_pixels;
}

int tk_get_meters_of_scale(short int z) {
	return scale2meter[z];
}

static double tk_round(double num) {
    double floor_num = floor(num);
    if (num < floor_num + 0.5) {
        return floor_num;
    }
    return floor_num + 1;
}

float FastSqrt (float x)
{
	float xhalf = 0.5f*x;
	int i = *(int*)&x;
	i = 0x5f3759df - (i>>1);
	x = *(float*)&i;
	x = x*(1.5f - xhalf*x*x);
	return 1/x;
}

float InvSqrt (float x)
{
	float xhalf = 0.5f*x;
	int i = *(int*)&x;
	i = 0x5f3759df - (i>>1);
	x = *(float*)&i;
	x = x*(1.5f - xhalf*x*x);
	return x;
}

float _sin(float x)
{
    while (x < -3.14159265)
        x += 6.28318531;
    while (x > 3.14159265)
        x -= 6.28318531;
    
    //compute sine
    if (x < 0)
        return (1.27323954 + 0.405284735 * x) * x;
    else
        return (1.27323954 - 0.405284735 * x) * x;
}

float _cos(float x)
{
    //compute cosine: sin(x + PI/2) = cos(x)
    x += 1.57079632;
    while (x < -3.14159265)
        x += 6.28318531;
    while (x >  3.14159265)
        x -= 6.28318531;
    
    if (x< 0)
        return (1.27323954 + 0.405284735 * x) * x;
    else
        return (1.27323954 - 0.405284735 * x) * x;
}

#define _acos(x) (FastSqrt(1-(x))*(1.5707963267948966192313216916398f + (x)*(-0.213300989f + (x)*(0.077980478f + (x)*-0.02164095f))))


//High precision sine/cosine (~8x faster)
//always wrap input angle to -PI..PI
float _hp_sin(float x)
{
    float sin;
    while (x < -3.14159265)
        x += 6.28318531;
    while (x > 3.14159265)
        x -= 6.28318531;
    
    //compute sine
    if (x< 0) {
        sin= (1.27323954 + 0.405284735 * x) * x;
        
        if (sin< 0)
            sin= .225 * (sin*-sin- sin) + sin;
        else
            sin= .225 * (sin* sin- sin) + sin;
    }
    else {
        sin = (1.27323954 - 0.405284735 * x) * x;
        
        if (sin< 0)
            sin= .225 * (sin*-sin- sin) + sin;
        else
            sin= .225 * (sin* sin- sin) + sin;
    }
    return sin;
}
//compute cosine: sin(x + PI/2) = cos(x)
float _hp_cos(float x)
{
    float cos;
    while (x < -3.14159265)
        x += 6.28318531;
    while (x > 3.14159265)
        x -= 6.28318531;
    
    if (x< 0) {
        cos= (1.27323954 + 0.405284735 * x) * x;
        
        if (cos< 0)
            cos= .225 * (cos*-cos- cos) + cos;
        else
            cos= .225 * (cos* cos- cos) + cos;
    }
    else {
        cos= (1.27323954 - 0.405284735 * x) * x;
        
        if (cos< 0)
            cos= .225 * (cos*-cos - cos) + cos;
        else
            cos= .225 * (cos* cos - cos) + cos;
    }
    return cos;
}

