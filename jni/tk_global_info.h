//
//  tk_global_info.h
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_global_info_h
#define tigermap_tk_global_info_h

#include "tk_types.h"
#include "tk_file_util.h"
#include "tk_style_config.h"

/* =========================================================
 * the global variables and defines
 * ========================================================= */
#define F_CSTYLE "render.cfg"
#define F_NSTYLE "render_n.cfg"
#define F_RLIST "regionlist"
#define F_CLIST "citylist"
#define F_IMG "images"
#define F_ENV "env.dat"
#define F_SPI "SPI.dat"

/* exceptional region id */
#define TK_REGION_ID_OUT_BOUND -1
#define TK_REGION_ID_ERROR -2
#define TK_REGION_ID_NATIONAL -3

#define TK_MAX_SUBWAY_NUM_PER_CITY 30
#define TK_MAX_SUBWAY_CITY_NUM 50
#define TK_MAX_SUBWAY_NAME_LENGTH 32
#define TK_MAX_NAME_LENGTH 64
#define TK_MAX_PATH_LENGTH 256
#define TK_MAX_LINE_LENGTH 1024     /* the max length of each line in files */

#define TK_REGION_VERNO_LENGTH 12

#define TK_NATIONAL_LEVEL_A 8
#define TK_NATIONAL_LEVEL_B 6
#define TK_NATIONAL_LEVEL_C 3

#define TK_BASE_LEVEL_A 16
#define TK_BASE_LEVEL_B 13
#define TK_BASE_LEVEL_C 11
#define TK_BASE_LEVEL_NUM 3

struct _tk_prov_unit
{
    char *name;
    char *ename;
    int city_idx;
};

struct _tk_city_unit
{
    int cid;
    char *name;
    char *ename;
    tk_latlon_t center;
    int reg_idx;
    int reg_count;
    int next;
    int prov_idx;
    short default_zoom_level; //initial zoom level
};

struct _tk_reg_unit
{
    int rid;
    char *name;
    char *ename;
    int file_size;
    int next;
    int city_idx;
};

struct _tk_nat_unit
{
    int rid;
    int cid;
    char *pname;
    char *pename;
    char *cname;
    char *cename;
    char *rname;
    char *rename;
    int default_zoom_level;
    int file_size;
    tk_latlon_t center;
};

struct _tk_reg_rects {
    tk_envelope_t env;
    int offset;
};

struct _tk_subway_color_info {
    char subway_name[TK_MAX_SUBWAY_NAME_LENGTH];
    tk_color_t color;
};

struct _tk_subway_city_info {
    struct _tk_subway_color_info color_info[TK_MAX_SUBWAY_NUM_PER_CITY];
    int    cityid;
    int color_num;
};

struct _tk_subway_infos {
    struct _tk_subway_city_info city_infos[TK_MAX_SUBWAY_CITY_NUM];
    int    city_num;
};

/*
 * 存放引擎静态数据，初始化完毕之后就不再改变
 */
struct _tk_engine_global_info {
    char respath[TK_MAX_PATH_LENGTH];
    char datapath[TK_MAX_PATH_LENGTH];
    int tile_size_pix;
    tk_style_t *pstyle;
    int style_count;
    tk_styles_buffer_t national_styles;
    tk_styles_buffer_t city_styles;
    int style_buffer_count;
    tk_prov_unit_t *prov_list;
    tk_city_unit_t *city_list;
    tk_reg_unit_t  *reg_list;
    tk_nat_unit_t nat;
    int prov_num;
    int city_num;
    int reg_num;
    tk_reg_rects_t *reg_bounds;
    unsigned char *region_polygon_buf;
    unsigned int region_polygon_buf_size;
    char *city_name_pool;
    int city_name_pool_size;
    int city_name_mem_pos;
    int *layer_ctl;
    int max_layer_num;
    tk_subway_infos_t subway_infos;
    int max_label_priority;
    int max_font_size;
    int min_font_size;
    char is_initialized;
};

extern tk_engine_global_info_t tk_global_info;


static inline int _tk_get_base_level(int zoom) {
    if (zoom > TK_BASE_LEVEL_B) {
        return TK_BASE_LEVEL_A;
    } else if (zoom > TK_BASE_LEVEL_C) {
        return TK_BASE_LEVEL_B;
    } else if (zoom > TK_NATIONAL_LEVEL_A) {
        return TK_BASE_LEVEL_C;
    } else if (zoom > TK_NATIONAL_LEVEL_B) {
        return TK_NATIONAL_LEVEL_A;
    } else if (zoom > TK_NATIONAL_LEVEL_C) {
        return TK_NATIONAL_LEVEL_B;
    } else {
        return TK_NATIONAL_LEVEL_C;
    }
}

#endif
