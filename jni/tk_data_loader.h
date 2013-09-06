//
//  tk_data_loader.h
//  tigermap
//
//  Created by Chen Ming on 13-6-19.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_data_loader_h
#define tigermap_tk_data_loader_h

#include "tk_error.h"
#include "tk_types.h"
#include "tk_context.h"

tk_status_t tk_load_tile(tk_context_t *context, int tile_x, int tile_y, int zoom);

tk_status_t tk_load_tile_data_default (int tile_x, int tile_y, int zoom, tk_bool_t is_need_name_only);

#endif
