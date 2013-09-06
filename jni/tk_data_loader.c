//
//  tk_data_loader.c
//  tigermap
//
//  Created by Chen Ming on 13-6-19.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include "tk_data_loader.h"
#include "tk_global_info.h"
#include "tk_context.h"
#include "tk_geo_util.h"
#include "tk_tile_data.h"
#include "tk_list.h"
#include "tk_list_inline.h"
#include "tk_region.h"
#include "tk_util.h"
#include "tk_log.h"

tk_status_t tk_load_tile(tk_context_t *context, int tile_x, int tile_y, int zoom) {
    tk_tile_data_t *last_tile = context->tile_data[0];
    if (last_tile) {
        tk_tile_key_t *last_key = &last_tile->tile_key;
        if (last_key->tile_x == tile_x && last_key->tile_y == tile_y && last_key->zoom == zoom) {
            return TK_STATUS_SUCCESS;
        }
    }
    return tk_load_tile_data(context, tile_x, tile_y, zoom);
}

static tk_bool_t _tk_is_adjacent(tk_base_tile_data_t *tile1, tk_base_tile_data_t *tile2) {
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

static void _tk_insert_feature(tk_feature_t *cur_feature, tk_feature_t *feature_to_insert) {
    tk_point_t *start, *end;
    tk_feature_data_t *cur_feature_data = cur_feature->feature, *feature_data_to_insert = feature_to_insert->feature;
    if (cur_feature->next == NULL && feature_to_insert->prev == NULL) {
        start = cur_feature_data->points + (cur_feature_data->points_num - 1);
        end = feature_data_to_insert->points;
        if (start->level_code == 1 && end->level_code == 1 && start->x == end->x && start->y == end->y){
            cur_feature->next = feature_to_insert;
            feature_to_insert->prev = cur_feature;
            return;
        }
    }
    if (cur_feature->prev == NULL && feature_to_insert->next == NULL) {
        start = feature_data_to_insert->points + (feature_data_to_insert->points_num - 1);
        end = cur_feature_data->points;
        if (start->level_code == 1 && end->level_code == 1 && start->x == end->x && start->y == end->y) {
            feature_to_insert->next = cur_feature;
            cur_feature->prev = feature_to_insert;
            return;
        }
    }
}

static void _tk_connect_tile_features(tk_tile_t *tiles_head, tk_tile_t *tile) {
    tk_feature_t* cur_feature;
    tk_feature_t* feature_to_insert;
    tk_feature_data_t* cur_feature_data;
    tk_feature_data_t* feature_data_to_insert;
    tk_tile_t *current_tile = tiles_head;
    tk_base_tile_data_t *base_tile_to_insert = tile->tile_data;
    //下面通过循环遍历所有该tile中的可以进行连接的features，以及与该tile相邻接的tile进行连接
    while(current_tile) {
        if (_tk_is_adjacent(base_tile_to_insert, current_tile->tile_data)) {
            if (!tile->header_feature) {//没有可连接的，也就不需要连接
                break;
            }
            cur_feature = current_tile->header_feature;
            while (cur_feature) {
                cur_feature_data = cur_feature->feature;
                feature_to_insert = tile->header_feature;
                while (feature_to_insert) {
                    feature_data_to_insert = feature_to_insert->feature;
                    if (cur_feature_data->type == feature_data_to_insert->type && cur_feature_data != feature_data_to_insert) {
                        if (!cur_feature_data->has_name && !feature_data_to_insert->has_name ) {
                            _tk_insert_feature(cur_feature, feature_to_insert);
                        }
                        else if ((cur_feature_data->name_length == feature_data_to_insert->name_length) &&
                                 (feature_data_to_insert->name_length > 0) &&
                                 (0 == strncmp(cur_feature_data->name, feature_data_to_insert->name, feature_data_to_insert->name_length))) {
                            _tk_insert_feature(cur_feature, feature_to_insert);
                        }
                    }
                    feature_to_insert = feature_to_insert->header_next;
                }
                cur_feature = cur_feature->header_next;
            }
        }
        current_tile = current_tile->next;
    }
}

static void _tk_link_tile(tk_tile_t **ptiles_head, tk_base_tile_data_t *base_tile, tk_bool_t is_need_name_only) {
    int i = 0, j = 0;
    tk_tile_t *tile = malloc(sizeof(tk_tile_t));
    memset(tile, 0, sizeof(tk_tile_t));
    tile->tile_data = base_tile;
    tile->features = malloc(sizeof(tk_feature_t) * base_tile->feature_num);
    memset(tile->features, 0, sizeof(tk_feature_t) * base_tile->feature_num);
    for (i = 0, j = 0; i < base_tile->feature_num; ++ i) {
        if (is_need_name_only && !base_tile->features[i].has_name) {
            continue;
        }
        tile->features[j].tile = tile;
        tile->features[j].feature = base_tile->features + i;
        if (base_tile->features[i].can_be_linked) {
            if (tile->header_feature) {
                tile->tail_feature->header_next = tile->features + j;
                tile->tail_feature = tile->features + j;
            }
            else {
                tile->header_feature = tile->features + j;
                tile->tail_feature = tile->header_feature;
            }
        }
        ++ j;
    }
    tile->feature_num = j;
    _tk_connect_tile_features(*ptiles_head, tile);
    if (!(*ptiles_head)) {
        *ptiles_head = tile;
    }
    else {
        tile->next = (*ptiles_head);
        (*ptiles_head) = tile;
    }
}

static tk_status_t _tk_link_tiles(tk_context_t *context, tk_bool_t is_need_name_only) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_tile_data_t *tile_data = context->tile_data[0];
    tk_base_tile_data_node_t *base_tile_node = tile_data->base_tile_head;
    while (base_tile_node) {
        tk_base_tile_data_t *base_tile = base_tile_node->data;
        _tk_link_tile(&context->tiles_head, base_tile, is_need_name_only);
        base_tile_node = base_tile_node->next;
    }
    return result;
}

static void _tk_add_tile_to_layer_context(tk_context_t *context) {
    tk_layer_t *cur_layer;
    tk_feature_t *cur_feature;
    tk_tile_t* current_tile = context->tiles_head;
    int i, type, feature_num = 0;
    memset(context->layer_list, 0, sizeof(tk_layer_t) * tk_global_info.max_layer_num);
    while(current_tile) {
        feature_num = current_tile->feature_num;
        for (i = 0; i < feature_num; ++i) {
            cur_feature = current_tile->features + i;
            type = cur_feature->feature->type;
            //需要添加到layer的feature仅包括所有前端无连接的feature，因为后续的feature都会在绘制时通过next指针找到
            if ((cur_feature->prev != NULL) || (type >= context->cur_style_buf->layer_num)) {//防止type越界
//                || ((context->cur_style_buf->obj_type[type] != 0)
//                    && (context->cur_style_buf->obj_type[type] != 4) && !cur_feature->feature->can_be_linked)) {
                continue;
            }
            cur_layer = context->layer_list + type;
            if (cur_layer->features == NULL) {
                cur_layer->ftail = cur_feature;
                cur_layer->features = cur_feature;
                cur_feature->layer_next = NULL;
            } else {
                cur_layer->ftail->layer_next = cur_feature;
                cur_layer->ftail = cur_feature;
                cur_feature->layer_next = NULL;
            }
        }
        current_tile = current_tile->next;
    }
}

tk_status_t tk_load_tile_data_default (int tile_x, int tile_y, int zoom, tk_bool_t is_need_name_only) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    
    tk_context_set_xyz(context, tile_x, tile_y, zoom);

    result = tk_load_tile(context, tile_x, tile_y, zoom);
    if (result != TK_STATUS_SUCCESS) {
        LOG_INFO("_tk_get_tiles failed");
        goto CATCH;
    }
    result = _tk_link_tiles(context, is_need_name_only);
    if (result != TK_STATUS_SUCCESS) {
        LOG_INFO("_tk_link_tiles failed");
        goto CATCH;
    }
    _tk_add_tile_to_layer_context(context);
//    LOG_INFO("tk_load_tile_data_default sucess");
    return TK_STATUS_SUCCESS;
CATCH:
    return result;
}

