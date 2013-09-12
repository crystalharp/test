/*
 * =====================================================================================
 *
 *       Filename:  tkm_comm.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  04/22/2011 03:38:44 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  YOUR NAME (), 
 *        Company:  
 *
 * =====================================================================================
 */

#ifndef __TKM_COMM_H
#define __TKM_COMM_H

#include "./cairo/cairo.h"

/* ==========================
 * variables to up level apps.
 * ========================== */
#define TK_LOST_DATA_PIECE 100 
#define TK_MAX_POINT_IN_BOUND 100
#define TK_MAX_REGION_CACHE_SIZE 7
#define TK_MAX_REGION_INTERSECT_COUNT 12

struct tk_map_lostdata {
    int rid;
    int offset;
    int length;
};

struct tk_label
{
    char *name;                //标注名字
    int font_color;      //颜色
    int font_size;             //大小
    float slope;                 //角度
    int outline_color;          //描边颜色
    int x;
    int y;     //显示位置
    int icon_id;
    int icon_x;
    int icon_y;

};


struct tk_latlon {
    double lon;
    double lat;
};

// the specific point for map engine 
// use this struct to save the memory
struct tk_point {
    int x;
    int y;
	int levelCode;
};

struct tk_point_slope {
    struct tk_point pt; 
    float slope;
};

#endif
