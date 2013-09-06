//
//  tk_label.h
//  tigermap
//
//  Created by Chen Ming on 13-7-10.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_label_h
#define tigermap_tk_label_h

#include "tk_error.h"
#include "tk_types.h"
#include "tk_map_api.h"

#define TK_STRING_POOL_NODE_BUF_LENGTH 1024
#define TK_POINT_POOL_NODE_NUM 128

typedef struct _tk_label_buf {
#define TK_LABEL_BUF_MAX_INCREASE_NUM 128
    tk_label_t *labels;
    short label_num;
    short size;
    tk_label_t labels_embedded[64];
} tk_label_buf_t;

typedef struct _tk_str_pool {
    char buf[TK_STRING_POOL_NODE_BUF_LENGTH];
    short length;
    short size;
    struct _tk_str_pool *next;
} tk_str_pool_t;

struct _tk_point_pool {
    tk_point_t buf[TK_POINT_POOL_NODE_NUM];
    int num;
    int size;
} tk_point_pool_t;

void tk_str_pool_init(tk_str_pool_t *str_buf);

char *tk_str_pool_add_string(tk_str_pool_t *str_buf, const char *string, int len);

void tk_str_pool_clean(tk_str_pool_t *str_buf);

void tk_label_buf_clean(tk_label_buf_t *label_buf);

tk_label_t *tk_label_buf_get_free_label(tk_label_buf_t *label_buf);

tk_status_t tk_get_tile_labels (int tile_x, int tile_y, int zoom);
tk_status_t tk_add_poi_labels(tk_context_t *context, tk_layer_t *poi_layer);
tk_status_t tk_add_line_feature_to_labels(tk_context_t *context, tk_feature_data_t *feature_data);
tk_status_t tk_add_polygon_feature_to_labels(tk_context_t *context, tk_feature_data_t *poly_data);

#endif
