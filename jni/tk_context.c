//
//  tk_tile_render_context.c
//  tigermap
//
//  Created by Chen Ming on 13-6-14.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include "tk_tls.h"
#include "tk_context.h"
#include "tk_global_info.h"
#include "tk_map_api.h"
#include "tk_log.h"

TK_DEFINE_THREAD_LOCAL(tk_context_t, tk_default_context);

tk_context_t* tk_get_context() {
    return TK_GET_THREAD_LOCAL(tk_default_context);
};

static void _tk_set_context_rect(tk_context_t *context, int tile_size) {    
    context->draw_rect.left = -14;
    context->draw_rect.right = tile_size + 14;
    context->draw_rect.top = -14;
    context->draw_rect.bottom = tile_size + 14;
}

static void _tk_set_context_cairo(tk_context_t *context, unsigned char *img_buf, int tile_size) {
    int stride = cairo_format_stride_for_width(CAIRO_FORMAT_ARGB32, tile_size);
    context->surface = cairo_image_surface_create_for_data (img_buf, CAIRO_FORMAT_ARGB32, tile_size, tile_size, stride);    
    context->cr = cairo_create (context->surface);
//    cairo_scale(context->cr, context->scale, context->scale);
    cairo_set_antialias(context->cr, CAIRO_ANTIALIAS_FAST);
}

int tk_init_default_context(unsigned char *img_buf, int tile_size_bit, int mod) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    memset(context, 0, sizeof(tk_context_t));
    context->tile_size = 1 << tile_size_bit;
    context->tile_size_bit = tile_size_bit;
    context->scale = tk_global_info.tile_size_pix / (float)context->tile_size;
    if (context->tile_size_bit < 0) {
        return TK_STATUS_INVALID_ARGS;
    }
    if (mod) {
        context->img_buf = img_buf;
        _tk_set_context_cairo(context, img_buf, tk_global_info.tile_size_pix);
    }
    
    _tk_set_context_rect(context, tk_global_info.tile_size_pix);
    
    context->tile_data[0] = NULL;
    
    context->layer_list = malloc(sizeof(tk_layer_t) * tk_global_info.max_layer_num);
    if (!context->layer_list) {
        return TK_STATUS_NO_MEMORY;
    }
    memset(context->layer_list, 0, sizeof(tk_layer_t) * tk_global_info.max_layer_num);
    LOG_DBG("tk_init_default_context success");
    
    tk_str_pool_init(&context->label_name_pool);
    return result;
}

//归还tile数据，只需在析构context时调用
static void _tk_return_tile_datas(tk_context_t *context) {
    int i = 0;
    if (context->tile_data) {
        for (; i < context->tile_data_count; ++ i) {
            tk_return_tile((tk_tile_key_t *)context->tile_data[i]);
            context->tile_data[i] = NULL;
        }
    }
}

// todo: 处理此处内存碎片问题
static void _tk_destroy_tile(tk_tile_t *tile) {
    if (tile) {
        if (tile->features) {
            free(tile->features);
            tile->features = NULL;
        }
        free(tile);
    }
}

void tk_clean_tiles(tk_context_t *context) {
    tk_tile_t *tile_to_delete = context->tiles_head;
    tk_tile_t *next = NULL;
    while (tile_to_delete) {
        next = tile_to_delete->next;
        _tk_destroy_tile(tile_to_delete);
        tile_to_delete = next;
    }
    context->tiles_head = NULL;
    if (context->tile_data[0]) {
        tk_return_tile((tk_tile_key_t *)context->tile_data[0]);
        context->tile_data[0] = NULL;
    }
}

void tk_fini_default_context() {
    tk_context_t *context = tk_get_context();
    tk_clean_lost_data(context);
    tk_clean_labels(context);
    tk_clean_tiles(context);
    _tk_return_tile_datas(context);
    if (context->cr) {
        cairo_destroy (context->cr);
        context->cr = NULL;
    }
    if (context->surface) {
        cairo_surface_destroy(context->surface);
        context->surface = NULL;
    }
    if (context->layer_list) {
        free(context->layer_list);
        context->layer_list = NULL;
    }
    if (context->region_data_buf) {
        free(context->region_data_buf);
        context->region_data_buf = NULL;
    }
    if (context->string_buf) {
        free(context->string_buf);
        context->string_buf = NULL;
    }
    
    TK_DESTROY_THREAD_LOCAL(tk_default_context);
}

void tk_clean_labels(tk_context_t *context) {
    //clear labels
    tk_label_buf_clean(&context->label_buf);
    // clear names pool and points pool
    tk_str_pool_clean(&context->label_name_pool);
    tk_point_buf_clean(&context->label_point_pool);
}

void tk_clean_lost_data(tk_context_t *context) {
    assert(context != NULL);
    context->lost_data_count = 0;
    memset(context->lost_data, 0, sizeof(tk_lost_data_t) * TK_LOST_DATA_MAX_COUNT);
}

////todo: 需要考虑多线程重复下载的情况
//上层可以这样：只要有数据在下载就不取lostdata，必须等下载结束之后才可接着取lostdata
void tk_context_add_lost_data(tk_context_t *context, int rid, int offset, int length, tk_lost_type_t type) {
    tk_lost_data_t *lost_data = context->lost_data;
    int lost_count = context->lost_data_count;
    if (lost_count < TK_LOST_DATA_MAX_COUNT) {
        lost_data[lost_count].rid = rid;
        lost_data[lost_count].offset = offset;
        lost_data[lost_count].length = length;
        lost_data[lost_count].type = type;
        ++ context->lost_data_count;
    }
}

void tk_context_set_xyz(tk_context_t *context, int tile_x, int tile_y, int zoom) {
    tk_envelope_t *tile_pix_box = &context->center_tile_pix_box;
    tk_envelope_t *feature_filter_rect = NULL;//&context->line_feature_filter_rect;
    int tile_size = context->tile_size;
    
    context->zoom = zoom;
    if (zoom > TK_BASE_LEVEL_B) {
        context->base_level = TK_BASE_LEVEL_A;
        context->base_level_idx = 0;
    } else if (zoom > TK_BASE_LEVEL_C) {
        context->base_level = TK_BASE_LEVEL_B;
        context->base_level_idx = 1;
    } else if (zoom > TK_NATIONAL_LEVEL_A) {
        context->base_level = TK_BASE_LEVEL_C;
        context->base_level_idx = 2;
    } else if (zoom > TK_NATIONAL_LEVEL_B) {
        context->base_level = TK_NATIONAL_LEVEL_A;
        context->base_level_idx = 0;
    } else if (zoom > TK_NATIONAL_LEVEL_C) {
        context->base_level = TK_NATIONAL_LEVEL_B;
        context->base_level_idx = 1;
    } else {
        context->base_level = TK_NATIONAL_LEVEL_C;
        context->base_level_idx = 2;
    }
    context->base_level_diff = context->base_level - zoom;
    if (zoom > TK_NATIONAL_LEVEL_A) {
        context->cur_style_buf = &tk_global_info.city_styles;
    } else {
        context->cur_style_buf = &tk_global_info.national_styles;
    }
    
    //tile像素坐标包围盒
//    tile_pix_box->left = tile_x << 8;
//    tile_pix_box->right = (tile_x << 8) + tile_size ;
//    tile_pix_box->top = tile_y << 8;
//    tile_pix_box->bottom = (tile_y << 8) + tile_size ;

    tile_pix_box->left = tile_x << context->tile_size_bit;
    tile_pix_box->right = tile_pix_box->left+ tile_size ;
    tile_pix_box->top = tile_y << context->tile_size_bit;
    tile_pix_box->bottom = tile_pix_box->top + tile_size ;

    feature_filter_rect = &context->line_feature_filter_rect;
    feature_filter_rect->left = tile_pix_box->left - 16;
    feature_filter_rect->right = tile_pix_box->right + 16;
    feature_filter_rect->top = tile_pix_box->top - 16;
    feature_filter_rect->bottom = tile_pix_box->bottom + 16;
    
    feature_filter_rect = &context->polygon_feature_filter_rect;
    feature_filter_rect->left = tile_pix_box->left;
    feature_filter_rect->right = tile_pix_box->right;
    feature_filter_rect->top = tile_pix_box->top;
    feature_filter_rect->bottom = tile_pix_box->bottom;
}

void tk_contxt_clear_point_buf(tk_context_t *context) {
    tk_point_buf_clean(&context->feature_point_buf);
    tk_point_buf_clean(&context->clipped_point_buf);
}

void tk_context_clear_building_buf(tk_context_t *context) {
    tk_building_buf_clean(&context->building_buf);
}
