//
//  tk_building.h
//  testcmd
//
//  Created by Chen Ming on 14-1-15.
//  Copyright (c) 2014年 Chen Ming. All rights reserved.
//

#ifndef testcmd_tk_building_h
#define testcmd_tk_building_h

#include "tk_types.h"
#include "tk_map_api.h"

typedef struct _tk_building_buf {
#define TK_BUILDING_BUF_MAX_INCREASE_NUM 32
    tk_building_t *buildings;
    short building_num;
    short size;
    tk_building_t buildings_embedded[32];
} tk_building_buf_t;

tk_status_t tk_add_buildings(tk_context_t *context, tk_layer_t *geo_layer);

void tk_building_buf_clean(tk_building_buf_t *buf);

#endif
