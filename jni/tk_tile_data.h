//
//  tk_tile_data.h
//  tigermap
//
//  Created by Chen Ming on 13-6-10.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_tile_data_h
#define tigermap_tk_tile_data_h

#include "tk_error.h"
#include "tk_feature_data.h"

//todo: 考虑在数据中描述
#define TK_MAX_MERGE_LEVEL 3

typedef struct _tk_tile_mem_pool {
    char *names_buf;
    int name_buf_size;
    int cur_name_index;

    tk_point_t *points_buf;
    int point_buf_size;
    int cur_point;

} tk_tile_mem_pool_t;

struct _tk_tile_key {
    int tile_x;
    int tile_y;
    int zoom;
};

typedef struct _tk_tile_key tk_tile_key_t;

struct _tk_base_tile_key {
    unsigned int merged_tile_x;
    unsigned int merged_tile_y;
    int zoom;
    int region_id;
};

typedef struct _tk_base_tile_key tk_base_tile_key_t;

struct _tk_base_tile_data {
    
    //用于查找的坐标，region_id相同，merged_tile_x,y相同则数据相同，merge_level为1时merged_tile_x,y等于base_tile_x,y
    unsigned int merged_tile_x;
    unsigned int merged_tile_y;
    int zoom;//base_tile还是跟zoom相关的，对于同一基本级别的数据还要根据base_level_diff来抽取feature的点
    int region_id;
    
    int merged_level;
    // 数据
    int length;         //the tile's length in dat file
    tk_feature_data_t *features; //坐标必须加上参考坐标
    int feature_num;
    tk_tile_mem_pool_t mem_pool;
    int overall_name_len;
    int overall_point_num;
};

struct _tk_base_tile_data_node {
    tk_base_tile_data_t *data;
    struct _tk_base_tile_data_node *next;
};

typedef struct _tk_base_tile_data_node tk_base_tile_data_node_t;

struct _tk_tile_data {
    //tile坐标
    tk_tile_key_t tile_key;
    tk_base_tile_data_node_t *base_tile_head;
    tk_base_tile_data_node_t *base_tile_tail;
};

typedef struct _tk_tile_data tk_tile_data_t;

tk_status_t tk_init_tile_data_pool(void);

void tk_fini_tile_data_pool(void);

//tk_tile_data_t *tk_get_tile(tk_tile_key_t *key);

//tk_status_t tk_put_tile(tk_tile_data_t *tile_data);

void tk_return_tile(tk_tile_key_t *key);

tk_status_t tk_load_tile_data(tk_context_t *context, int tile_x, int tile_y, int zoom);

void tk_tile_data_clean(void);

#endif

