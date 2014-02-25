//
//  tk_building.c
//  testcmd
//
//  Created by Chen Ming on 14-1-15.
//  Copyright (c) 2014年 Chen Ming. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include "tk_context.h"
#include "tk_building.h"
#include "tk_geo_util.h"
#include "tk_util.h"
#include "tk_map_api.h"

void tk_building_buf_clean(tk_building_buf_t *buf) {
    if (!buf) {
        return;
    }
    buf->size = TK_ARRAY_LENGTH(buf->buildings_embedded);
    buf->building_num = 0;
    if (buf->buildings != buf->buildings_embedded) {
        if (buf->buildings) {
            free(buf->buildings);
        }
        buf->buildings = buf->buildings_embedded;
    }
}

static tk_status_t _tk_building_buf_grow(tk_building_buf_t *buf) {
    tk_building_t *new_buildings;
    int old_size = buf->size;
    int new_size = old_size < TK_BUILDING_BUF_MAX_INCREASE_NUM ? (old_size + old_size) : (old_size + TK_BUILDING_BUF_MAX_INCREASE_NUM);
    
    if (buf->buildings == buf->buildings_embedded) {
        new_buildings = calloc(new_size, sizeof (tk_building_t));
        if (new_buildings != NULL)
            memcpy (new_buildings, buf->buildings, old_size * sizeof (tk_building_t));
    } else {
        new_buildings = realloc(buf->buildings, new_size * sizeof (tk_building_t));
    }
    
    if (new_buildings == NULL) {
        return TK_STATUS_NO_MEMORY;
    }
    buf->buildings = new_buildings;
    buf->size = new_size;
    return TK_STATUS_SUCCESS;
}

static tk_building_t *tk_building_buf_get_free_building(tk_building_buf_t *buf) {
    tk_building_t *label = NULL;
    if (buf->building_num == buf->size) {
        if(_tk_building_buf_grow(buf) == TK_STATUS_NO_MEMORY) {
            tk_set_result(TK_STATUS_NO_MEMORY);
            return NULL;
        }
    }
    label = buf->buildings + buf->building_num;
    ++ buf->building_num;
    return label;
}

tk_status_t tk_add_buildings(tk_context_t *context, tk_layer_t *geo_layer) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_feature_t *building_feature = geo_layer->features;
    tk_feature_data_t *building_data = NULL;
    tk_building_t *building = NULL;
    tk_envelope_t *rect = &context->polygon_feature_filter_rect;
    int ref_x = context->center_tile_pix_box.left;
    int ref_y = context->center_tile_pix_box.top;
    while (building_feature) {
        tk_bool_t has_point_out_of_tile = TK_NO;
        tk_bool_t has_point_in_tile = TK_NO;
        building_data = building_feature->feature;
        if (!building_data) {
            goto NEXT;
        }
        for (int i = 0; i < building_data->points_num; ++i) {
            tk_point_t *p = building_data->points + i;
            if (!(p->x < rect->left || p->x > rect->right || p->y < rect->top || p->y > rect->bottom)) {
                has_point_in_tile = TK_YES;
                break;
            }
        }
        //没有任何点落在当前tile，可以略去，即使building覆盖了整个tile。只有包含至少一点的tile才会绘制building。
        if (!has_point_in_tile) {
            goto NEXT;
        }
        building = tk_building_buf_get_free_building(&(context->building_buf));
        if (building) {
            building->point_start_idx = context->label_point_pool.point_num;
            for (int i = 0; i < building_data->points_num; ++i) {
                tk_point_t *p = building_data->points + i;
                if (p->x < rect->left || p->x > rect->right || p->y < rect->top || p->y > rect->bottom) {
                    has_point_out_of_tile = TK_YES;//有至少1点落在其它tile，那么有可能被该tile先渲染，在渲染时要判断是否已被渲染。
                }
                result = tk_point_buf_add_one(&context->label_point_pool, (p->x - ref_x) * context->scale, (p->y - ref_y) * context->scale, p->level_code);
                if (result != TK_STATUS_SUCCESS) {
                    goto CATCH;
                }
            }
            building->point_num = building_data->points_num;
            building->height = building_data->height;
            building->has_point_out_of_tile = has_point_out_of_tile;
        }
    NEXT:
        building_feature = building_feature->layer_next;
    }
CATCH:
    return result;

}
