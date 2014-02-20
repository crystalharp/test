//
//  tk_feature_data.h
//  tigermap
//
//  Created by Chen Ming on 13-6-10.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_feature_data_h
#define tigermap_tk_feature_data_h

#include "tk_types.h"
#include "tk_error.h"
#include "tk_util.h"

//define the feature type
enum _tk_geo_type
{
	TKGEO_ENMFTYPE_POINT = 0,		//
	TKGEO_ENMFTYPE_POLY,			//
	TKGEO_ENMFTYPE_RAIL,			//
	TKGEO_ENMFTYPE_ROAD,			//
    TKGEO_ENMFTYPE_SUBWAY,
    TKGEO_ENMFTYPE_BUILDING,
	TKGEO_ENMFTYPE_MAX,
};

struct _tk_feature_data {
    // the tile this feature belonged to
    tk_base_tile_data_t *tile;
    tk_point_t *points;//存tile像素坐标，文件里存的是基本像素坐标因此读取时需要做个转换
    char *name;
	short points_num;
    short name_length;
    // the left top of its outside rectangle
    tk_point_t left_top;
    tk_point_t right_bottom;
    int priority;
   	unsigned short type;
    unsigned char can_be_linked;
    unsigned char has_name;

    unsigned char sub_type;
    unsigned char zlevel;
    unsigned char has_direction;
    unsigned char alias_num;
    int alias_length[2];
    int height;
    char *alias_names[2];
    unsigned char level_diff16;
};

tk_status_t tk_read_features(tk_context_t *context,
                             tk_region_t *region,
                             tk_base_tile_data_t *base_tile,
                             tk_buf_info_t *tile_data_buf,
                             tk_buf_info_t *addition_buf,
                             int feature_num,
                             int max_point_num, tk_bool_t is_new);

#endif
