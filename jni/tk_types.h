//
//  tk_types.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_types_h
#define tigermap_tk_types_h

#include "tk_list.h"
#include "tkm_comm.h"
#include "tk_map_api.h"

typedef unsigned int tk_pixel_4b_t;
typedef unsigned short tk_pixel_2b_t;

typedef enum _tk_bool {
    TK_NO,
    TK_YES,
} tk_bool_t;

typedef struct _tk_latlon {
    double lon;
    double lat;
} tk_latlon_t;

typedef struct _tk_envelope {
    int left;
	int top;
	int right;
	int bottom;
} tk_envelope_t;

typedef struct _tk_context tk_context_t;

typedef struct _tk_reg_rects tk_reg_rects_t;
typedef struct _tk_subway_color_info tk_subway_color_info_t;
typedef struct _tk_subway_city_info tk_subway_city_info_t;
typedef struct _tk_subway_infos tk_subway_infos_t;

typedef struct _tk_hash_entry tk_hash_entry_t;
typedef struct _tk_hash_table tk_hash_table_t;
typedef struct _tk_lru_cache tk_lru_cache_t;
typedef struct _tk_cache_entry tk_cache_entry_t;

typedef struct _tk_prov_unit tk_prov_unit_t;
typedef struct _tk_city_unit tk_city_unit_t;
typedef struct _tk_reg_unit tk_reg_unit_t;
typedef struct _tk_nat_unit tk_nat_unit_t;
typedef struct _tk_engine_global_info tk_engine_global_info_t;

typedef struct _tk_styles_buffer tk_styles_buffer_t;
typedef struct _tk_style tk_style_t;

typedef struct _tk_region tk_region_t;
typedef struct _tk_feature_data tk_feature_data_t;
typedef struct _tk_layer tk_layer_t;
typedef struct _tk_base_tile_data tk_base_tile_data_t;

/**
 * tk_hash_entry_t:
 *
 * A #tk_hash_entry_t contains both a key and a value for
 * #tk_hash_table_t. User-derived types for #tk_hash_entry_t must
 * be type-compatible with this structure (eg. they must have an
 * unsigned long as the first parameter. The easiest way to get this
 * is to use:
 *
 * 	typedef _my_entry {
 *	    tk_hash_entry_t base;
 *	    ... Remainder of key and value fields here ..
 *	} my_entry_t;
 *
 * which then allows a pointer to my_entry_t to be passed to any of
 * the #tk_hash_table_t functions as follows without requiring a cast:
 *
 *	_tk_hash_table_insert (hash_table, &my_entry->base);
 *
 * IMPORTANT: The caller is responsible for initializing
 * my_entry->base.hash with a hash code derived from the key. The
 * essential property of the hash code is that keys_equal must never
 * return %TRUE for two keys that have different hashes. The best hash
 * code will reduce the frequency of two keys with the same code for
 * which keys_equal returns %FALSE.
 *
 * Which parts of the entry make up the "key" and which part make up
 * the value are entirely up to the caller, (as determined by the
 * computation going into base.hash as well as the keys_equal
 * function). A few of the #tk_hash_table_t functions accept an entry
 * which will be used exclusively as a "key", (indicated by a
 * parameter name of key). In these cases, the value-related fields of
 * the entry need not be initialized if so desired.
 **/
struct _tk_hash_entry {
    unsigned long hash;
};

#endif
