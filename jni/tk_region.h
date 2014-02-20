//
//  tk_region.h
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_region_h
#define tigermap_tk_region_h

#include <pthread.h>
#include "tk_lru_cache.h"
#include "tk_global_info.h"
#include "tk_types.h"

#define TK_HUFF_CODE_UNIT_SIZE 3
#define TK_HUFF_STOP_UNIT_SIZE 4
#define TK_POINT_NUMBER_BITS 8
typedef enum {
    TK_REGION_NO_DATA,
    TK_REGION_INITIALIZED,
} tk_region_state_t;

/* the region stucture used in map engine internal */
struct _tk_region {
    tk_region_state_t state;
    pthread_mutex_t region_lock;
    
    int rid;

    unsigned char *verifycode;
    
    char version[6];
    
    //region文件内容
    FILE *region_fp;
    unsigned int region_data_length;
    unsigned char *meta_data;
    //huffman索引块
    unsigned int hf_index_count;
    unsigned char *hf_indexes;
    int hf_config_num;//huffman编码配置区域个数
    unsigned char *hf_configs;
    
    //tile数据索引块
    unsigned char *tile_index; /* index part of region data */
    unsigned int tile_index_count;
    
    //tile数据区
    int tile_data_bias;
    
    int tile_meta_length;

    struct _tile_index_bound {
        int high;
        int low;
    } tile_index_bound[TK_BASE_LEVEL_NUM];
};

tk_status_t tk_init_region_cache(void);
void tk_fini_region_cache(void);
tk_bool_t tk_is_region_loaded(int rid);
tk_region_t *tk_get_region(int rid);
void tk_return_region(int rid);

void tk_region_remove(int rid);

void tk_region_clean_unuse_cache(void);

tk_status_t tk_region_init_file(const char *metafile, int rid);
tk_status_t tk_region_write_file(int rid, int off, int len, const char* buf);
tk_status_t tk_region_get_state(int rid, int *ptotal_size, int *pdownloaded_size);
tk_status_t tk_region_get_version(int rid, unsigned char *rversion);
tk_status_t tk_region_get_path(char *path, int rid);

#endif
