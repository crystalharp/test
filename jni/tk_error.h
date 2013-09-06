//
//  tk_error.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_error_h
#define tigermap_tk_error_h

#include <stdlib.h>
#include "tk_tls.h"

enum _tk_status {
    TK_STATUS_SUCCESS,
    
    TK_STATUS_NO_MEMORY,
    TK_STATUS_LRU_CACHE_FULL,
    
    TK_STATUS_INVALID_REGION_NUM,
    
    TK_STATUS_FILE_NOT_EXIST,
    TK_STATUS_FILE_OPEN_FAILED,
    TK_STATUS_FILE_READ_ERROR,
    TK_STATUS_FILE_WRITE_ERROR,
    TK_STATUS_FILE_SEEK_ERROR,
    TK_STATUS_MKDIR_FAILED,
    TK_STATUS_EMPTY_FILE,
    
    TK_STATUS_INVALID_ARGS,
    TK_STATUS_TOO_MANY_POINTS,
    
    TK_STATUS_TILE_OUT_BOUND,
    TK_STATUS_TILE_DATA_LOST,
    TK_STATUS_TILE_DATA_ERROR,
    
    TK_STATUS_BUF_OVERFLOAT,
    TK_STATUS_POINT_SKIPED,
    TK_STATUS_UNKNOWN_FEATURE,
    TK_STATUS_FEATURE_NO_POINT,
    TK_STATUS_NEED_NOT_DRAW,
    
    TK_STATUS_DATA_VERSION_NOT_MATCH,
    TK_STATUS_CANNOT_FIND_NEAREST_POI,
};

typedef enum _tk_status tk_status_t;

extern int tk_get_last_result(void);

extern void tk_set_result(int result);

#endif
