//
//  tk_tile_render_context.h
//  tigermap
//
//  Created by Chen Ming on 13-6-14.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_tile_render_context_h
#define tigermap_tk_tile_render_context_h

#include "cairo/cairo.h"
#include "tk_error.h"
#include "tk_map_api.h"
#include "tk_tile_data.h"
#include "tk_types.h"
#include "tk_label.h"
#include "tk_building.h"

#define TK_LOST_DATA_MAX_COUNT 100
#define TK_TILE_BOUND_SIZE 1
#define TK_MAX_REGION_CACHE_SIZE 32


typedef struct _tk_tile tk_tile_t;

struct _tk_feature {
    tk_tile_t *tile;
    tk_feature_data_t *feature;
    struct _tk_feature *next;
    struct _tk_feature *prev;
    struct _tk_feature *header_next;
    struct _tk_feature *layer_next;
};

typedef struct _tk_feature tk_feature_t;

struct _tk_tile {
    tk_tile_key_t tile_key;
    tk_base_tile_data_t *tile_data;
    tk_feature_t *features;
    int feature_num;
    tk_feature_t *header_feature;
    tk_feature_t *tail_feature;
    tk_tile_t *next;
};

struct _tk_layer {
    /* the head pointer of this layer's feature list */
	tk_feature_t *features;
    /* the tail pointer of this layer's feature list */
    tk_feature_t *ftail;
	short int fnum;
    struct _tk_layer *next_layer;
};

struct _tk_geo_layer {
    int geo_type;
    tk_layer_t *sub_layer_list;
    int sub_layer_num;
};

//定义gdi相关信息
struct _tk_gdi {
	tk_color_t		    background_color;
    tk_color_t          color;
    tk_color_t          text_color;
    int                 font_size;
    int                 icon_id;
    short               label_priority;
	unsigned char       label_style;
	unsigned char		line_style;
	unsigned char		pen_size;
	unsigned char		pen_shape;
	unsigned char		pixel_size;
	unsigned char		text_style;
};

typedef struct _tk_gdi tk_gdi_t;

struct _tk_context {
    /* thread static variables: */
    float scale;
    int tile_size;
    int tile_size_bit;
    cairo_t *cr;
    cairo_surface_t *surface;
    unsigned char *img_buf;
    // render box
    tk_envelope_t draw_rect;
    tk_envelope_t center_tile_pix_box;
    
    /* thread dynamic variables: */
    int zoom; //current zoom level
    tk_envelope_t line_feature_filter_rect;
    tk_envelope_t polygon_feature_filter_rect;
    int related_region_num;
    int related_region[TK_MAX_REGION_CACHE_SIZE];
    
    int base_level;
    // base level index : A ==> 0, B ==> 1, C ==> 2
    int base_level_idx;
    int base_level_diff;
    
    /* map_buffer (tile buffer)*/
    tk_tile_data_t *tile_data[1];
    int tile_data_count;
    tk_tile_t *tiles_head;
    
    /* an array of layer */
    tk_layer_t *layer_list;
    tk_styles_buffer_t *cur_style_buf;
    tk_style_t *style;
    tk_gdi_t gdi;
    tk_geo_layer_t *geo_layer_list;
    
    tk_lost_data_t lost_data[TK_LOST_DATA_MAX_COUNT];
    int lost_data_count;
    
    tk_point_buf_t feature_point_buf;
    tk_point_buf_t clipped_point_buf;
    
    tk_label_buf_t label_buf;
    tk_str_pool_t label_name_pool;
    tk_point_buf_t label_point_pool;
    tk_building_buf_t building_buf;
    
    int point_num;
    unsigned char *region_data_buf;
    char *string_buf;
};

//typedef struct _tk_context tk_context_t;

tk_context_t* tk_get_context(void);

int tk_init_default_context(unsigned char *img_buf, int tile_size_bit, int mod);

void tk_fini_default_context(void);

void tk_clean_lost_data(tk_context_t *context);

void tk_clean_labels(tk_context_t *context);

void tk_clean_tiles(tk_context_t *context);

void tk_context_add_lost_data(tk_context_t *context, int rid, int offset, int length, tk_lost_type_t force);

void tk_context_set_xyz(tk_context_t *context, int tile_x, int tile_y, int zoom);

void tk_context_set_center_tile(tk_context_t *context, int tile_x, int tile_y);

void tk_contxt_clear_point_buf(tk_context_t *context);

void tk_context_clear_building_buf(tk_context_t *context);

#endif



