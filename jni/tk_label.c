//
//  tk_label.c
//  tigermap
//
//  Created by Chen Ming on 13-7-10.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "tk_label.h"
#include "tk_util.h"
#include "tk_geo_util.h"
#include "tk_context.h"
#include "tk_log.h"
#include "tk_global_info.h"
#include "tk_data_loader.h"

void tk_str_pool_init(tk_str_pool_t *str_buf) {
    assert(str_buf != NULL);
    str_buf->length = 0;
    str_buf->size = TK_ARRAY_LENGTH(str_buf->buf);
    str_buf->next = NULL;
}

char *tk_str_pool_add_string(tk_str_pool_t *str_buf, const char *string, int len) {
    assert(str_buf != NULL && string != NULL && len >= 0);
    if (str_buf->length + len < str_buf->size) {
        strncpy(str_buf->buf + str_buf->length, string, len);
        str_buf->length += (len + 1);
        str_buf->buf[str_buf->length - 1] = '\0';
        return str_buf->buf + str_buf->length - (len + 1);
    }
    else {
        if (str_buf->next) {
            return tk_str_pool_add_string(str_buf->next, string, len);
        }
        else {
            str_buf->next = malloc(sizeof(tk_str_pool_t));
            if (!str_buf->next) {
                tk_set_result(TK_STATUS_NO_MEMORY);
                return NULL;
            }
            else {
                memset(str_buf->next, 0, sizeof(tk_str_pool_t));
                tk_str_pool_init(str_buf->next);
                return tk_str_pool_add_string(str_buf->next, string, len);
            }
        }
    }
}

static void _tk_free_str_buf_recursively(tk_str_pool_t *str_buf) {
    if (str_buf) {
        _tk_free_str_buf_recursively(str_buf->next);
        free(str_buf);
    }
}

void tk_str_pool_clean(tk_str_pool_t *str_buf) {
    _tk_free_str_buf_recursively(str_buf->next);
    str_buf->length = 0;
    str_buf->next = NULL;
}

void tk_label_buf_clean(tk_label_buf_t *label_buf) {
    label_buf->size = TK_ARRAY_LENGTH(label_buf->labels_embedded);
    label_buf->label_num = 0;
    if (label_buf->labels != label_buf->labels_embedded) {
        if (label_buf->labels) {
            free(label_buf->labels);
        }
        label_buf->labels = label_buf->labels_embedded;
        //        memset(context->embedded_labels, 0, sizeof(context->embedded_labels));//可不必清0
    }
}

static tk_status_t _tk_label_buf_grow(tk_label_buf_t *buf) {
    tk_label_t *new_labels;
    int old_size = buf->size;
    int new_size = old_size < TK_LABEL_BUF_MAX_INCREASE_NUM ? (old_size + old_size) : (old_size + TK_LABEL_BUF_MAX_INCREASE_NUM);
    
    if (buf->labels == buf->labels_embedded) {
        new_labels = calloc(new_size, sizeof (tk_label_t));
        if (new_labels != NULL)
            memcpy (new_labels, buf->labels, old_size * sizeof (tk_label_t));
    } else {
        new_labels = realloc(buf->labels, new_size * sizeof (tk_label_t));
    }
    
    if (new_labels == NULL) {
        return TK_STATUS_NO_MEMORY;
    }
    buf->labels = new_labels;
    buf->size = new_size;
    return TK_STATUS_SUCCESS;
}

tk_label_t *tk_label_buf_get_free_label(tk_label_buf_t *label_buf) {
    tk_label_t *label = NULL;
    if (label_buf->label_num == label_buf->size) {
        if(_tk_label_buf_grow(label_buf) == TK_STATUS_NO_MEMORY) {
            tk_set_result(TK_STATUS_NO_MEMORY);
            return NULL;
        }
    }
    label = label_buf->labels + label_buf->label_num;
    ++ label_buf->label_num;
    return label;
}

tk_status_t tk_add_poi_labels(tk_context_t *context, tk_layer_t *poi_layer) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_gdi_t *gdi = &context->gdi;
    tk_feature_t *poi_feature = poi_layer->features;
    tk_feature_data_t *poi_data = NULL;
    tk_point_t *poi_point = NULL;
    tk_label_t *label = NULL;
    tk_envelope_t *rect = &context->polygon_feature_filter_rect;
    int ref_x = context->center_tile_pix_box.left;
    int ref_y = context->center_tile_pix_box.top;
    while (poi_feature) {
        poi_data = poi_feature->feature;
        if (!poi_data) {
            goto NEXT;
        }
        poi_point = poi_data->points;
        if (poi_point->x < rect->left || poi_point->x >= rect->right ||
            poi_point->y < rect->top || poi_point->y >= rect->bottom) {
            goto NEXT;
        }
        if (poi_data->has_name) {
            label = tk_label_buf_get_free_label(&context->label_buf);
            if (label) {
                label->point_start_idx = context->label_point_pool.point_num;
                result = tk_point_buf_add_one(&context->label_point_pool, (poi_point->x - ref_x) * context->scale, (poi_point->y - ref_y) * context->scale, poi_point->level_code);
                if (result != TK_STATUS_SUCCESS) {
                    goto CATCH;
                }
                label->point_num = 1;
                label->name = tk_str_pool_add_string(&context->label_name_pool, poi_data->name, poi_data->name_length);
                if (!label->name) {
                    result = tk_get_last_result();
                    goto CATCH;
                }
                label->priority = context->gdi.label_priority;
                label->text_color = gdi->text_color;
                label->bg_color = context->gdi.background_color;
                label->font_size = context->gdi.font_size;
                label->icon_id = context->gdi.icon_id;
                label->type = poi_data->type;
            }
        }
    NEXT:
        poi_feature = poi_feature->layer_next;
    }
CATCH:
    return result;
}

/* add labels */
tk_status_t tk_add_line_feature_to_labels(tk_context_t *context, tk_feature_data_t *feature_data) {
    tk_label_t *label = NULL;
    if (feature_data && feature_data->has_name) {
        label = tk_label_buf_get_free_label(&context->label_buf);
        if (label) {
            label->point_start_idx = context->label_point_pool.point_num;
            label->text_color = context->gdi.text_color;
            label->bg_color = context->gdi.background_color;
            label->font_size = context->gdi.font_size;
            label->icon_id = context->gdi.icon_id;
            label->name = tk_str_pool_add_string(&context->label_name_pool, feature_data->name, feature_data->name_length);
            label->point_start_idx = context->label_point_pool.point_num;
            label->point_num = context->feature_point_buf.point_num;
            label->priority = context->gdi.label_priority;
            label->type = feature_data->type;
            return tk_point_buf_add_points(&context->label_point_pool, context->feature_point_buf.points, label->point_num);
        }
    }
    return TK_STATUS_SUCCESS;
}

tk_status_t tk_add_polygon_feature_to_labels(tk_context_t *context, tk_feature_data_t *poly_data) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_envelope_t *label_rect = &context->polygon_feature_filter_rect;
    tk_label_t *label = NULL;
    tk_point_t center_point;
    int ref_x = context->center_tile_pix_box.left;
    int ref_y = context->center_tile_pix_box.top;
    center_point.x = (poly_data->left_top.x + poly_data->right_bottom.x) >> 1;
    center_point.y = (poly_data->left_top.y + poly_data->right_bottom.y) >> 1;
    if (center_point.x >= label_rect->left && center_point.x < label_rect->right &&
        center_point.y >= label_rect->top && center_point.y < label_rect->bottom) {
        label = tk_label_buf_get_free_label(&context->label_buf);
        if (label) {
            label->point_start_idx = context->label_point_pool.point_num;
            label->point_num = 0;
            result = tk_point_buf_add_one(&context->label_point_pool,
                                          (center_point.x - ref_x) * context->scale,
                                          (center_point.y - ref_y) * context->scale,
                                          center_point.level_code);
            if (result != TK_STATUS_SUCCESS) {
                goto CATCH;
            }
            ++ label->point_num;
            label->name = tk_str_pool_add_string(&context->label_name_pool, poly_data->name, poly_data->name_length);
            if (!label->name) {
                result = tk_get_last_result();
                goto CATCH;
            }
            label->text_color = context->gdi.text_color;
            label->bg_color = context->gdi.background_color;
            label->font_size = context->gdi.font_size;
            label->icon_id = context->gdi.icon_id;
            label->priority = context->gdi.label_priority;
            label->type = poly_data->type;
        }
    }
CATCH:
    return result;
}

static tk_status_t _tk_check_layer_and_set_gdi(tk_context_t *context, tk_layer_t *layer, tk_bool_t need_pen_width) {
    tk_gdi_t *gdi = &context->gdi;
    int pen_width = 0;
    tk_style_t *style = context->style;
    int level = context->zoom;
    if (need_pen_width) {
        pen_width = style->pen_width[level - style->zoom_min];
        if (pen_width == 0)
            return TK_STATUS_NEED_NOT_DRAW;
    }
    else {
        pen_width = 1;
    }
    
    if (level < style->label_min || level > style->label_max)
        return TK_STATUS_NEED_NOT_DRAW;
    gdi->label_priority = style->label_prioritys[level - style->label_min];
    if (gdi->label_priority == 0) {
        return TK_STATUS_NEED_NOT_DRAW;
    }
    gdi->color = style->fill_color;
    gdi->background_color = style->border_color;
    gdi->pen_size = pen_width;
    gdi->line_style = style->line_type;
    gdi->text_style = style->label_style;
    gdi->text_color = style->fontcolor;
    gdi->font_size = style->font_size;
    gdi->icon_id = style->icon_id;
    return TK_STATUS_SUCCESS;
};

static tk_rect_relation_t _tk_get_relation_of_feature_and_filter(tk_context_t *context, tk_feature_t *pfeature, tk_bool_t is_line) {
    tk_envelope_t temp_env, filter_rect;
    tk_feature_t *cur_ft = pfeature;
    int relation = TK_RECT_DISJOINT, cur_relation = TK_RECT_DISJOINT;
    if (is_line) {
        filter_rect = context->line_feature_filter_rect;
    }
    else {
        filter_rect = context->polygon_feature_filter_rect;
    }
    while (cur_ft != NULL) {
        temp_env.left = cur_ft->feature->left_top.x;
        temp_env.top = cur_ft->feature->left_top.y;
        temp_env.right = cur_ft->feature->right_bottom.x;
        temp_env.bottom = cur_ft->feature->right_bottom.y;
        cur_relation = tk_geo_get_rect_relation(temp_env, filter_rect);
        if (cur_relation == TK_RECT_INTERSECT || cur_relation == TK_RECT_COVER)
            return TK_RECT_INTERSECT;
        if ((relation == TK_RECT_COVER && cur_relation == TK_RECT_DISJOINT) ||
            (relation == TK_RECT_DISJOINT && cur_relation == TK_RECT_COVER)) {
            return TK_RECT_INTERSECT;
        }
        relation = cur_relation;
        cur_ft = cur_ft ->next;
    }
    return relation;
}

static tk_status_t _tk_get_points_from_line_feature(tk_context_t *context, tk_feature_t *pfeature) {
    tk_feature_t *cur_feature = pfeature;
    int i, points_num;
    tk_point_buf_t *point_buf = &context->feature_point_buf;
    tk_point_t *point = NULL;
    int ref_x = context->center_tile_pix_box.left;
    int ref_y = context->center_tile_pix_box.top;
    while (cur_feature) {
        tk_feature_data_t *feature_data = cur_feature->feature;
        if (!feature_data) {
            goto NEXT;
        }
        if (cur_feature == pfeature) {//第一个才有
            if (!feature_data->points_num) {
                LOG_DBG("NULL feature points");
                goto NEXT;
            }
            point = feature_data->points + 0;//feature中存的点，已经是tile像素坐标，只需要减去左上角坐标（y轴向下）
            if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
                return TK_STATUS_NO_MEMORY;
            }
        }
        points_num = feature_data->points_num;
        for (i = 1; i < points_num - 1; i++) {//中间的feature的两个边界点不要
            point = cur_feature->feature->points + i;
            if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
                return TK_STATUS_NO_MEMORY;
            }
        }
        if (cur_feature->next == NULL) {//最后一个
            point = feature_data->points + points_num - 1;
            if(tk_point_buf_add_one(point_buf, (point->x - ref_x) * context->scale, (point->y - ref_y) * context->scale, 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
                return TK_STATUS_NO_MEMORY;
            }
        }
    NEXT:
        cur_feature = cur_feature->next;
    }
    context->point_num += context->feature_point_buf.point_num;
    return TK_STATUS_SUCCESS;
}

static int _tk_find_index_swcity(int cid) {
    if(cid == -1)
        return -1;
    int i = 0;
    for(i = 0; i < tk_global_info.subway_infos.city_num; ++i) {
        if(tk_global_info.subway_infos.city_infos[i].cityid == cid){
            return  i;
        }
    }
    return -1;
}

static void _tk_set_subway_gdi(tk_gdi_t *gdi, int rid, tk_feature_t *subway_feature) {
    int color_index = -1;
    int current_city_id = 0;
    current_city_id = tk_get_cid_by_rid(rid);
    color_index = _tk_find_index_swcity(current_city_id);
    if(color_index != -1) {
        int i = 0, is_found = 0, num = tk_global_info.subway_infos.city_infos[color_index].color_num;
        for(;i < num; i++) {
            tk_subway_color_info_t *color_info = tk_global_info.subway_infos.city_infos[color_index].color_info + i;
            char *name = color_info->subway_name;
            if(strlen(name) == subway_feature->feature->name_length){
                if(strncmp(name, subway_feature->feature->name, subway_feature->feature->name_length) == 0){
                    gdi->color = color_info->color;
                    gdi->text_color = color_info->color;
                    is_found = 1;
                    break;
                }
            }
        }
        if (!is_found) {
//            char a[256] = {0};
//            strncpy(a, subway_feature->feature->name, subway_feature->feature->name_length);
//            LOG_INFO(a);
            gdi->color = 0x4ABAA5;
            gdi->text_color = 0x4ABAA5;
        }
    } else {
        gdi->color = 0x4ABAA5;
        gdi->text_color = 0x4ABAA5;
    }
}

static tk_status_t _tk_add_line_labels(tk_context_t *context, tk_layer_t *line_layer) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_feature_t *line_feature = line_layer->features;
    int relation;
    while (line_feature) {
        relation = _tk_get_relation_of_feature_and_filter(context, line_feature, TK_YES);
        if(relation == TK_RECT_DISJOINT) {
            goto NEXT;
        }
        //清空顶点缓存
        tk_contxt_clear_point_buf(context);
        
        //添加feature顶点
        if(_tk_get_points_from_line_feature(context, line_feature) == TK_STATUS_NO_MEMORY) {
            return TK_STATUS_NO_MEMORY;
        }
        if (line_feature->feature->type == 9) {//地铁配色另取
            _tk_set_subway_gdi(&context->gdi, line_feature->feature->tile->region_id, line_feature);
        }
        // add label
        result = tk_add_line_feature_to_labels(context, line_feature->feature);
        if (result != TK_STATUS_SUCCESS) {
            return result;
        }
    NEXT:
        line_feature = line_feature->layer_next;
    }
    return TK_STATUS_SUCCESS;
}

//static tk_status_t _tk_get_points_from_poly_feature(tk_context_t *context, tk_feature_t* pfeature) {
//    tk_feature_t *cur_feature = pfeature;
//    int i, points_num;
//    tk_point_buf_t *point_buf = &context->feature_point_buf;
//    tk_point_t *point = NULL;
//    int ref_x = context->center_tile_pix_box.left;
//    int ref_y = context->center_tile_pix_box.top;
//    points_num = cur_feature->feature->points_num;
//    for (i = 0; i < points_num; i++) {
//        point = cur_feature->feature->points + i;
//        if(tk_point_buf_add_one(point_buf, (point->x - ref_x), (point->y - ref_y), 0) == TK_STATUS_NO_MEMORY) {//不需要feature的level_code
//            return TK_STATUS_NO_MEMORY;
//        }
//    }
//    context->point_num += context->feature_point_buf.point_num;
//    return TK_STATUS_SUCCESS;
//}

static tk_status_t _tk_add_polygon_labels(tk_context_t *context, tk_layer_t *layer) {
    tk_feature_t *poly_feature = layer->features;
    tk_rect_relation_t relation;
    
    while (poly_feature) {
        relation = _tk_get_relation_of_feature_and_filter(context, poly_feature, TK_NO);
        if(relation == TK_RECT_DISJOINT) {
            goto NEXT;
        }
        if (!poly_feature->feature) {
            goto NEXT;
        }
        //清空顶点缓存
//        tk_contxt_clear_point_buf(context);
//        
//        //添加feature顶点
//        if(_tk_get_points_from_poly_feature(context, poly_feature) == TK_STATUS_NO_MEMORY) {
//            return TK_STATUS_NO_MEMORY;
//        }
        
        if (poly_feature->feature->has_name) {//添加label
            tk_add_polygon_feature_to_labels(context, poly_feature->feature);
        }
    NEXT:
        poly_feature = poly_feature->layer_next;
    }
    return TK_STATUS_SUCCESS;
}


tk_status_t tk_get_tile_labels (int tile_x, int tile_y, int zoom) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    int i = 0, layer_idx = 0;
    tk_layer_t *layer;
    int obj_type = 0;
    context->point_num = 0;
    for(i = 0; i < context->cur_style_buf->layer_num; ++ i){
        layer_idx = context->cur_style_buf->draw_order[i];
        if (layer_idx < 0) {
            continue;
        }
        layer = context->layer_list + layer_idx;
        context->style = context->cur_style_buf->styles + layer_idx;
        if (layer == NULL || layer->features == NULL)
            continue;
        obj_type = context->cur_style_buf->obj_type[layer_idx];
        
        if (tk_global_info.layer_ctl[layer_idx] == 1) {
            switch (obj_type) {
                case TKGEO_ENMFTYPE_POINT:
                    if (_tk_check_layer_and_set_gdi(context, layer, TK_NO) == TK_STATUS_SUCCESS) {
                        tk_add_poi_labels(context, layer);
                    }
                    break;
                case TKGEO_ENMFTYPE_LINE:
                case TKGEO_ENMFTYPE_RAIL:
                case TKGEO_ENMFTYPE_ROAD:
                    if (_tk_check_layer_and_set_gdi(context, layer, TK_YES) == TK_STATUS_SUCCESS) {
                        _tk_add_line_labels(context, layer);
                    }
                    break;
                case TKGEO_ENMFTYPE_POLY:
                    if (_tk_check_layer_and_set_gdi(context, layer, TK_NO) == TK_STATUS_SUCCESS) {
                        _tk_add_polygon_labels(context, layer);
                    }
                    break;
                default:
                    break;
            }
        }
    }
    return result;
}




