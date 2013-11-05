//
//  tk_tile_data.c
//  tigermap
//
//  Created by Chen Ming on 13-6-10.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include "tk_lru_cache.h"
#include "tk_tile_data.h"
#include "tk_global_info.h"
#include "tk_geo_util.h"
#include "tk_util.h"
#include "tk_region.h"
#include "tk_context.h"
#include "tk_log.h"

#define TILE_POOL_MAX_SIZE 16
#define BASE_TILE_POOL_MAX_SIZE 256

static tk_lru_cache_t _tk_tile_data_pool;
static tk_lru_cache_t _tk_base_tile_data_pool;

static void _tk_release_tile_mem_pool(tk_tile_mem_pool_t *pool) {
    if (pool != NULL) {
        if (pool->names_buf != NULL) {
            free(pool->names_buf);
            pool->names_buf = NULL;
            pool->cur_name_index = 0;
        }
        if (pool->points_buf != NULL) {
            free(pool->points_buf);
            pool->points_buf = NULL;
            pool->cur_point = 0;
        }
    }
    return;
}

static void _tk_destroy_tile_data(void *tile_key, void *tile_data) {
    //没有key的所有权，不处理key
    if (tile_data) {
        tk_base_tile_data_node_t *base_tile_node = ((tk_tile_data_t *)tile_data)->base_tile_head;
        while (base_tile_node) {
            if (base_tile_node->data) {
                tk_lru_cache_return(&_tk_base_tile_data_pool, base_tile_node->data);
            }
            tk_base_tile_data_node_t *base_tile_to_free = base_tile_node;
            base_tile_node = base_tile_node->next;
            free(base_tile_to_free);
        }
        free(tile_data);
    }
}

static void _tk_destroy_base_tile_data(void *tile_key, void *base_tile_data) {
    //没有key的所有权，不处理key
    if (base_tile_data) {
        tk_base_tile_data_t *base_tile = base_tile_data;
        if (base_tile->features) {
            free(base_tile->features);
            base_tile->features = NULL;
            base_tile->feature_num = 0;
        }
        _tk_release_tile_mem_pool(&base_tile->mem_pool);
        free(base_tile_data);
    }
}

static unsigned long _tk_base_tile_hash(const void *base_tile_key) {//borrow from decarta, 测试命中率？
    const tk_base_tile_key_t *key = base_tile_key;
    unsigned long h = 3;
	h = 29 * h + key->merged_tile_x;
	h = 29 * h + key->merged_tile_y;
	h = 29 * h + key->zoom;
    return h;
}

static tk_bool_t _tk_base_tile_key_equal(const void *tile_key1, const void *tile_key2) {
    const tk_base_tile_key_t *key1 = tile_key1;
    const tk_base_tile_key_t *key2 = tile_key2;
    return (key1->merged_tile_x == key2->merged_tile_x &&
            key1->merged_tile_y == key2->merged_tile_y &&
            key1->zoom == key2->zoom &&
            key1->region_id == key2->region_id);
}

static unsigned long _tk_tile_hash(const void *tile_key) {//borrow from decarta, 测试命中率？
    const tk_tile_key_t *key = tile_key;
    unsigned long h = 3;
	h = 29 * h + key->tile_x;
	h = 29 * h + key->tile_y;
	h = 29 * h + key->zoom;
    return h;
}

static tk_bool_t _tk_tile_key_equal(const void *tile_key1, const void *tile_key2) {
    const tk_tile_key_t *key1 = tile_key1;
    const tk_tile_key_t *key2 = tile_key2;
    return key1->tile_x == key2->tile_x && key1->tile_y == key2->tile_y && key1->zoom == key2->zoom;
}

tk_status_t tk_init_tile_data_pool(void) {
    tk_status_t result = TK_STATUS_SUCCESS;
    result = tk_lru_cache_init(&_tk_base_tile_data_pool, _tk_base_tile_key_equal, _tk_base_tile_hash, _tk_destroy_base_tile_data, BASE_TILE_POOL_MAX_SIZE, TK_CACHE_FULL_AUTO_EXPANTION);
    if (result != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    result = tk_lru_cache_init(&_tk_tile_data_pool, _tk_tile_key_equal, _tk_tile_hash, _tk_destroy_tile_data, TILE_POOL_MAX_SIZE, TK_CACHE_FULL_AUTO_EXPANTION);
    if (result != TK_STATUS_SUCCESS) {
        tk_lru_cache_fini(&_tk_base_tile_data_pool);
    }
CATCH:
    return result;
}

void tk_fini_tile_data_pool(void) {
    tk_lru_cache_fini(&_tk_tile_data_pool);
}

/* convert 10 12 14 15 level to 11 13 16 level tile_bound*/
// 获取在基本级别下的tile包围盒, 因为数据只存
static void _tk_get_base_level_tile_bound(int x, int y, int bl_dif, tk_envelope_t *box) {
    if (bl_dif >= 0) {
        box->left = x << bl_dif;
        box->top = y << bl_dif;
        box->right = box->left + (1 << bl_dif) - 1;
        box->bottom = box->top + (1 << bl_dif) - 1;
    } else {
        box->left = x >> (-bl_dif);
        box->top = y >> (-bl_dif);
        box->right = box->left;
        box->bottom = box->top;
    }
}

//tk_tile_data_t *tk_get_tile(tk_tile_key_t *key) {
//    return tk_lru_cache_fetch(&_tk_tile_data_pool, key);
//}

//tile_data本身第一个元素就是tile_key因此自己就可以做为key
//tk_status_t tk_put_tile(tk_tile_data_t *tile_data) {
//    return tk_lru_cache_insert(&_tk_tile_data_pool, tile_data, tile_data);
//}

void tk_return_tile(tk_tile_key_t *key) {
    tk_lru_cache_lock(&_tk_tile_data_pool);
    tk_lru_cache_return(&_tk_tile_data_pool, key);
    tk_lru_cache_unlock(&_tk_tile_data_pool);
}
//
//static void _tk_get_base_tile_box(int tile_x, int tile_y, int bl_dif, tk_envelope_t *box) {
//    /* convert 10 12 14 15 level to 11 13 16 level tile_bound*/
//    // 获取在基本级别下的tile包围盒, 因为数据只存基本级别坐标
//    if (bl_dif >= 0) {
//        box->left = tile_x << bl_dif;
//        box->top = tile_y << bl_dif;
//        box->right = box->left + (1 << bl_dif) - 1;
//        box->bottom = box->top + (1 << bl_dif) - 1;
//    } else {
//        box->left = tile_x >> (-bl_dif);
//        box->top = tile_y >> (-bl_dif);
//        box->right = box->left;
//        box->bottom = box->top;
//    }
//}

static void _tk_get_base_tile_box(int tile_size_bit, int tile_x, int tile_y, int bl_dif, tk_envelope_t *box) {
    /* convert 10 12 14 15 level to 11 13 16 level tile_bound*/
    // 获取在基本级别下的tile包围盒, 因为数据只存基本级别坐标
    int bit = bl_dif + (tile_size_bit - 8);
    if (bit >= 0) {
        box->left = tile_x << bit;
        box->top = tile_y << bit;
        box->right = box->left + (1 << bit) - 1;
        box->bottom = box->top + (1 << bit) - 1;
    } else {
        box->left = tile_x >> (-bit);
        box->top = tile_y >> (-bit);
        box->right = box->left;
        box->bottom = box->top;
    }
}

//获取城市左上角参考坐标(y轴向下)
static void _tk_get_tile_ref(tk_region_t *region, int base_level, int *ref_x, int *ref_y) {
    int cursor = 0;
    unsigned char *cur_pointer;
    unsigned char *index = region->tile_index;
    
    cur_pointer = index + cursor * 6;
    if (base_level == TK_BASE_LEVEL_A || base_level == TK_NATIONAL_LEVEL_A) {
        *ref_x = GETNUM3B(cur_pointer);
        *ref_y = GETNUM3B(cur_pointer + 3);
        return;
    }
    cursor += GETNUM3B(cur_pointer + 9);
    cursor += 2;
    cur_pointer = index + cursor * 6;
    if (base_level == TK_BASE_LEVEL_B || base_level == TK_NATIONAL_LEVEL_B) {
        *ref_x = GETNUM3B(cur_pointer);
        *ref_y = GETNUM3B(cur_pointer + 3);
        return;
    }
    cursor += GETNUM3B(cur_pointer + 9);
    cursor += 2;
    cur_pointer = index + cursor * 6;
    if (base_level == TK_BASE_LEVEL_C || base_level == TK_NATIONAL_LEVEL_C) {
        *ref_x = GETNUM3B(cur_pointer);
        *ref_y = GETNUM3B(cur_pointer + 3);
        return;
    }
}

//通过二分查找morton码来查找tile索引号
static int _tk_search_tile_by_morton_code(tk_region_t *region, int base_level_idx, int morton_x, int morton_y, int *merged_level) {
    int upbound;
    int lowbound;
    int morton_code, cur_code, cur_level, cur_tile_number;
    unsigned char *region_index, *pointer;
    
    region_index = region->tile_index;
    upbound = region->tile_index_bound[base_level_idx].high;
    lowbound = region->tile_index_bound[base_level_idx].low;
    cur_tile_number = (upbound + lowbound) / 2;
    morton_code = tk_get_morton_code(morton_x, morton_y);
    
    while (1) {
        if (upbound < lowbound) {
            tk_set_result(TK_STATUS_TILE_OUT_BOUND);
            return -1;
        }
        /*cur_level = tile levelcode  tile 合并次数最大为3(参考数据生成程序) */
        pointer = region_index  + 6 * cur_tile_number;
        cur_level = (int)pointer[0] >> 4;
        cur_code = (int)(((pointer[0] & 0xf) << 16) | (pointer[1] << 8) | (pointer[2]));
        if (tk_morton_equal(morton_code, cur_code, cur_level)) {
            break;
        } else {
            if (morton_code < cur_code) {
                upbound = cur_tile_number - 1;
                cur_tile_number = (lowbound + upbound) / 2;
            } else {
                lowbound = cur_tile_number + 1;
                cur_tile_number = (lowbound + upbound) / 2;
            }
        }
    }
    *merged_level = cur_level;
    return cur_tile_number;
}

////todo: 参数有点多，考虑用一个结构体包装，如tk_tile_buf_info
static unsigned char *_tk_get_tile_buf(tk_region_t *region, tk_context_t *context, int cur_tile_number,
                                       unsigned int *buf_offset, unsigned int *buf_length) {
    int pos = 0, next_tile_pos = 0;
    int index_count;
    unsigned char *tile_buf = NULL;
    unsigned char *cur_pointer = NULL, *region_index = NULL;
    unsigned char *verifycode = NULL;
    unsigned char i, j, k;
    
    int map_data_bias = region->tile_data - region->region_data;
    region_index = region->tile_index;
    index_count = region->tile_index_count;
    verifycode = region->verifycode;
    
    cur_pointer = region_index + 6 * cur_tile_number + 3;
    pos = map_data_bias + GETNUM3B(cur_pointer);
    
    //获取下一个tile的偏移量，如果没有下一个tile则取region数据长度
    if (cur_tile_number < index_count - 1) {
        //如果是当前级别最后一个tile，则下一个tile为下一级别第一个tile，需要额外加上下一级别两个特殊tile索引占位(12字节)
        if (cur_tile_number == region->tile_index_bound[context->base_level_idx].high) {
            cur_pointer += 18;
        } else {
            cur_pointer += 6;
        }
        next_tile_pos = map_data_bias + GETNUM3B(cur_pointer);
    } else {
        next_tile_pos = region->region_data_length;
    }
    *buf_offset = pos;
    *buf_length = next_tile_pos - *buf_offset;
    //校验，看tile是否已下载
    if (NULL != verifycode) {
        i = region->region_data[next_tile_pos - 1];
        j = verifycode[cur_tile_number/8];
        k = cur_tile_number % 8;
        if (((0x01L)&(i ^ (j >> k))) == 0) {
            tk_set_result(TK_STATUS_TILE_DATA_LOST);
            tk_context_add_lost_data(context, region->rid, *buf_offset, *buf_length, TK_LOST_TYPE_DATA_LOST);
            LOG_INFO("_tk_get_tile_buf failed: lost data");
            goto CATCH;
        }
    }
    tile_buf = region->region_data + *buf_offset;
CATCH:
    return tile_buf;
}

static void _tk_destruct_base_tile(tk_base_tile_data_t **base_tile_data) {
    if (base_tile_data && *base_tile_data) {
        tk_base_tile_data_t *base_tile = *base_tile_data;
        if (base_tile->features) {
            free(base_tile->features);
            base_tile->features = NULL;
            base_tile->feature_num = 0;
        }
        _tk_release_tile_mem_pool(&base_tile->mem_pool);
        free(base_tile);
        *base_tile_data = NULL;
    }
}

static tk_base_tile_data_t *_tk_create_base_tile(int feature_num, int point_num, int name_length, int max_length) {
    tk_base_tile_data_t *base_tile = NULL;
    tk_status_t result = TK_STATUS_SUCCESS;
    if ((feature_num <= 0 || feature_num > max_length / 2) ||
        (point_num < 0 || point_num > max_length / 2) ||
        (name_length < 0)){
        tk_set_result(TK_STATUS_TILE_DATA_ERROR);
        return NULL;
    }

    if (name_length > max_length) {
    	name_length = max_length;
    }

    base_tile = malloc(sizeof(tk_base_tile_data_t));
    if (!base_tile) {
        tk_set_result(TK_STATUS_NO_MEMORY);
        return NULL;
    }
    memset(base_tile, 0, sizeof(tk_base_tile_data_t));
    base_tile->features = malloc((feature_num + 1) * sizeof(tk_feature_data_t));
    if (!base_tile->features) {
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    memset(base_tile->features, 0, (feature_num + 1) * sizeof(tk_feature_data_t));
    base_tile->mem_pool.names_buf = malloc(sizeof(char) * (name_length + 1));
    if (!base_tile->mem_pool.names_buf) {
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    memset(base_tile->mem_pool.names_buf, 0, sizeof(char) * (name_length + 1));
    base_tile->mem_pool.points_buf = malloc(sizeof(tk_point_t) * (point_num + 1));
    if (!base_tile->mem_pool.points_buf) {
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    memset(base_tile->mem_pool.points_buf, 0, sizeof(tk_point_t) * (point_num + 1));
    base_tile->mem_pool.cur_name_index = 0;
    base_tile->mem_pool.cur_point = 0;
    base_tile->feature_num = 0;
CATCH:
    if (result != TK_STATUS_SUCCESS) {
        tk_set_result(result);
        _tk_destruct_base_tile(&base_tile);
    }
    return base_tile;
}

static int _tk_seek_tile(tk_context_t *context, tk_region_t *region, unsigned int base_tile_x, unsigned int base_tile_y,
                         int *ref_x, int *ref_y, int *merged_level) {
    _tk_get_tile_ref(region, context->base_level, ref_x, ref_y);
    if (base_tile_x < (*ref_x) || base_tile_y < (*ref_y)) {
        tk_set_result(TK_STATUS_TILE_OUT_BOUND);
        goto CATCH;
    }
    return _tk_search_tile_by_morton_code(region, context->base_level_idx, base_tile_x - (*ref_x), base_tile_y - (*ref_y), merged_level);
CATCH:
    return -1;
}

static inline int _tk_get_merged_coordinate(int coord, int ref_coord, int merged_level) {
    return coord - ((coord - ref_coord) % (1 << merged_level));
}

static tk_base_tile_data_t *_tk_load_tile_from_region(tk_region_t *region, tk_context_t *context, int tile_number,
                                                      int merged_x, int merged_y, int merged_level) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_base_tile_data_t *base_tile = NULL;
    tk_buf_info_t tile_data_buf;
    unsigned int tile_offset, buf_pos;
    int feature_num, name_length, point_num;
    
    tile_data_buf.buf_pos = 0;
    tile_data_buf.remain_bits = 0;
    tile_data_buf.remain_value = 0;
    tile_data_buf.buf = _tk_get_tile_buf(region, context, tile_number, &tile_offset, &tile_data_buf.buf_length);
    if (!tile_data_buf.buf) {//result可能是out_bound也可能是lost_data
        goto CATCH;
    }
    switch (context->base_level_diff) {
        case 2:
            buf_pos = 0;
            break;
        case 1:
            buf_pos = 5;
            break;
        default:
            buf_pos = 10;
            break;
    }
    feature_num = (tile_data_buf.buf[buf_pos] << 8) + tile_data_buf.buf[buf_pos + 1];
//    if (feature_num < 0 || (feature_num == 0 && tile_data_buf.buf_length > 15) || feature_num > tile_data_buf.buf_length) {//出错
//    	LOG_INFO("_tk_load_tile_from_region failed: TK_STATUS_TILE_DATA_ERROR, data buf length: %d", tile_data_buf.buf_length);
//        tk_set_result(TK_STATUS_TILE_DATA_ERROR);
//        tk_context_add_lost_data(context, region->rid, tile_offset, tile_data_buf.buf_length, TK_LOST_TYPE_DATA_ERROR);
//        goto CATCH;
//    }
    if (feature_num == 0) {
    	tk_set_result(TK_STATUS_SUCCESS);
    	return NULL;
    }
    name_length = (tile_data_buf.buf[buf_pos + 2] << 4) + ((tile_data_buf.buf[buf_pos + 3] >> 4) & 0x0f);
    point_num = ((tile_data_buf.buf[buf_pos + 3] & 0xf) << 8) + tile_data_buf.buf[buf_pos + 4];//这里数据是否已是每级需要读取节点个数，预先分配的空间是否正好？最终看来不是。如何无缝兼容？
    tile_data_buf.buf_pos = 15;
    base_tile = _tk_create_base_tile(feature_num, point_num, name_length, tile_data_buf.buf_length);////todo: 考虑拆分成check和create
    if (!base_tile) {
        if (tk_get_last_result() == TK_STATUS_TILE_DATA_ERROR) {
            LOG_INFO("_tk_load_tile_from_region failed: TK_STATUS_TILE_DATA_ERROR, base tile error: %d, %d, %d, %d",
            		feature_num, point_num, name_length, tile_data_buf.buf_length);
            //todo: 将tile末尾标记为该tile未下载
            tk_context_add_lost_data(context, region->rid, tile_offset, tile_data_buf.buf_length, TK_LOST_TYPE_DATA_ERROR);//重新下载该tile，而不是删除整个region
        }
        goto CATCH;
    }
    base_tile->merged_tile_x = merged_x;
    base_tile->merged_tile_y = merged_y;
    base_tile->merged_level = merged_level;
    base_tile->region_id = region->rid;
    base_tile->length = tile_data_buf.buf_length;
    base_tile->zoom = context->zoom;
    result = tk_read_features(context, region, base_tile, tile_data_buf, feature_num, point_num);
    if (result == TK_STATUS_TILE_DATA_ERROR) {
        tk_set_result(result);
        LOG_DBG("tk_read_features failed: TK_STATUS_TILE_DATA_ERROR");
        goto CATCH;
    }
    return base_tile;
CATCH:
    _tk_destruct_base_tile(&base_tile);
    return NULL;
}

static tk_base_tile_data_t *_tk_get_base_tile_from_cache(tk_context_t *context, tk_region_t *region, unsigned int merged_x, unsigned int merged_y, int zoom, int rid) {
    tk_base_tile_key_t base_tile_key;
    base_tile_key.zoom = zoom;
    base_tile_key.region_id = rid;
    base_tile_key.merged_tile_x = merged_x;
    base_tile_key.merged_tile_y = merged_y;

    return tk_lru_cache_fetch(&_tk_base_tile_data_pool, &base_tile_key);
}

static tk_status_t _tk_put_base_tile(tk_base_tile_data_t *base_tile) {
    return tk_lru_cache_insert(&_tk_base_tile_data_pool, base_tile, base_tile);
}

static tk_status_t _tk_insert_base_tile_to_tile(tk_tile_data_t *tile, tk_base_tile_data_t *base_tile) {
    tk_base_tile_data_node_t *cur_node = tile->base_tile_head;
    if (base_tile->merged_level > 0) {
        for (; cur_node != NULL; cur_node = cur_node->next) {
            if (_tk_base_tile_key_equal(cur_node->data, base_tile)) {
                tk_lru_cache_return(&_tk_base_tile_data_pool, base_tile);
                return TK_STATUS_SUCCESS;
            }
        }
    }
    tk_base_tile_data_node_t *base_tile_node = malloc(sizeof(tk_base_tile_data_node_t));
    if (!base_tile_node) {
        return TK_STATUS_NO_MEMORY;
    }
    memset(base_tile_node, 0, sizeof(tk_base_tile_data_node_t));
    base_tile_node->data = base_tile;
    //插入到尾部
    if (!tile->base_tile_head) {
        tile->base_tile_head = base_tile_node;
        tile->base_tile_tail = base_tile_node;
    }
    else {
        tile->base_tile_tail->next = base_tile_node;
        tile->base_tile_tail = base_tile_node;
    }
    return TK_STATUS_SUCCESS;
}

//依次判断多边形每条边是否和矩形相交，只判断矩形各顶点在多边形内或只判断多边形各顶点在矩形内是不全面的。
static tk_bool_t _tk_is_rect_cross_region(int rid, const tk_envelope_t *rect) {
    int i, buf_idx = 0;
    tk_point_t p1, p2;
    const unsigned char *buf = tk_global_info.region_polygon_buf + tk_global_info.reg_bounds[rid].offset;
    
    int point_num = buf[buf_idx ++];
    
    // y轴向下，x轴向右
    for (i = 0; i < point_num; i++) {
        p2.level_code = (buf[buf_idx] >> 7) & 0x1f;
        p2.x = ((buf[buf_idx] & 0x7f) << 24) + (buf[buf_idx + 1] << 16) + (buf[buf_idx + 2] << 8) + buf[buf_idx + 3];
        p2.y = (buf[buf_idx + 4] << 24) + (buf[buf_idx + 5] << 16) + (buf[buf_idx + 6] << 8) + buf[buf_idx + 7];
        buf_idx += 8;
        if (p2.level_code) {
            p1 = p2;
            continue;
        }
        if (tk_vector_rect_cross(&p1, &p2, rect)) {//线段和矩形相交
            return TK_YES;
        }
        p1 = p2;
    }
    return TK_NO;
}

static int _tk_find_regions_in_bbox(const tk_envelope_t *tile_box, int zoom, int *reg_in_bound, int max_region_num, int *regnum_in_bound) {
    int center_region_id = TK_REGION_ID_OUT_BOUND;
    if (zoom > TK_NATIONAL_LEVEL_A) {
        int i;
        tk_point_t center_p;
        int is_intersect = 0;
        *regnum_in_bound = 0;
		center_p.x = (tile_box->left + tile_box->right) >> 1;
        center_p.y = (tile_box->top + tile_box->bottom) >> 1;
        for (i = 0; i < tk_global_info.reg_num; i++) {
            if (TK_ENV_NO_OVERLAP((*tile_box), tk_global_info.reg_bounds[i].env) == 0) {
                int is_inpoly = tk_is_point_in_region(i, &center_p);
                
                if (is_inpoly) { //中心点被region包含
                    if (*regnum_in_bound < max_region_num){
                        reg_in_bound[*regnum_in_bound] = i;
                        ++(*regnum_in_bound);
                    } else {
                        // log_debug, 是否存在这样的区域，该区域同时与max_region_num以上的region相交？
                        reg_in_bound[*regnum_in_bound-1] = i;
                    }
                    if (center_region_id == TK_REGION_ID_OUT_BOUND) { //若中心点落在region边界上，则有可能被多个region包含
                        center_region_id = i;
                    }
                    continue;
                }
                is_intersect = _tk_is_rect_cross_region(i, tile_box);
                if (is_intersect) { // 中心点不被包含，但包围盒相交
                    if (*regnum_in_bound < max_region_num){
                        reg_in_bound[*regnum_in_bound] = i;
                        ++(*regnum_in_bound);
                    }
                    continue;
                }
            }
        }
    } else {
        *regnum_in_bound = 1;
        reg_in_bound[0] = TK_REGION_ID_NATIONAL;//National
        center_region_id = TK_REGION_ID_NATIONAL;
    }
    return center_region_id;
}

static tk_status_t _tk_get_related_regions(tk_context_t *context, const tk_envelope_t *tile_mercator_box) {
    int center_id = _tk_find_regions_in_bbox(tile_mercator_box, context->zoom, context->related_region, TK_MAX_REGION_CACHE_SIZE, &context->related_region_num);
    if (center_id == -1) {
        return TK_STATUS_TILE_OUT_BOUND;
    }
    return TK_STATUS_SUCCESS;
}

tk_status_t tk_load_tile_data(tk_context_t *context, int tile_x, int tile_y, int zoom) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_tile_data_t *tile_data = NULL;
    tk_base_tile_data_t *base_tile_data = NULL;
    tk_envelope_t base_tile_box;
    unsigned int base_x, base_y;
    int region_idx, rid;
    int region_num;
    tk_region_t *region = NULL;
    tk_envelope_t tile_mercator_box;
    
    //先去缓存看看有没有
    tk_tile_key_t tile_key;
    tile_key.tile_x = tile_x;
    tile_key.tile_y = tile_y;
    tile_key.zoom = zoom;
    tk_lru_cache_lock(&_tk_tile_data_pool);
    tile_data = tk_lru_cache_fetch(&_tk_tile_data_pool, &tile_key);
    if (tile_data) {
        tk_lru_cache_unlock(&_tk_tile_data_pool);
        goto SUCCESS;
    }
    // 缓存中没有，要去region中加载
    // 获取tile的mercator包围盒, 用于寻找相关region
    tk_get_mercator_tile_box(context->tile_size_bit, tile_x, tile_y, zoom, context->base_level_diff, &tile_mercator_box);
    result = _tk_get_related_regions(context, &tile_mercator_box);
    if (result != TK_STATUS_SUCCESS) {
        tk_lru_cache_unlock(&_tk_tile_data_pool);
        LOG_INFO("_tk_get_related_regions failed:x:%i, y:%i, z:%i", tile_x, tile_y, zoom);
        return result;
    }
    region_num = context->related_region_num;
    
    _tk_get_base_tile_box(context->tile_size_bit, tile_x, tile_y, context->base_level_diff, &base_tile_box);
    tile_data = malloc(sizeof(tk_tile_data_t));
    memset(tile_data, 0, sizeof(tk_tile_data_t));
    tile_data->tile_key.tile_x = tile_x;
    tile_data->tile_key.tile_y = tile_y;
    tile_data->tile_key.zoom = context->zoom;
    // 从每个可能相关的region中寻找相关的数据
    for (region_idx = 0; region_idx < region_num; ++ region_idx) {
        tk_bool_t is_lost_data = TK_NO;
        base_tile_data = NULL;
        rid = context->related_region[region_idx];
        region = tk_get_region(rid);
        if (!region) {
            result = tk_get_last_result();
            if (result == TK_STATUS_FILE_NOT_EXIST || result == TK_STATUS_DATA_VERSION_NOT_MATCH) {
                tk_context_add_lost_data(context, rid, 0, 0, TK_LOST_TYPE_DATA_LOST);
                LOG_INFO("tk_load_tile failed: lost data");
            }
            goto CATCH;
        }
        //遍历每个可能的基本级别(11,13,16)小方块
        for (base_x = base_tile_box.left - 1; base_x <= base_tile_box.right + 1; ++ base_x) {
            for (base_y = base_tile_box.top - 1; base_y <= base_tile_box.bottom + 1; ++ base_y) {
                int ref_x, ref_y, merged_level, merged_x, merged_y;
                //先通过motorn码查找tile是否在该region中
                int tile_number = _tk_seek_tile(context, region, base_x, base_y, &ref_x, &ref_y, &merged_level);
                if (tile_number < 0) {
                    continue;// 只可能out bound
                }
                merged_x = _tk_get_merged_coordinate(base_x, ref_x, merged_level);
                merged_y = _tk_get_merged_coordinate(base_y, ref_y, merged_level);
                base_tile_data = _tk_get_base_tile_from_cache(context, region, merged_x, merged_y, context->zoom, rid);

                if (!base_tile_data) {
                    base_tile_data = _tk_load_tile_from_region(region, context, tile_number, merged_x, merged_y, merged_level);
                    if (!base_tile_data) {
                        result = tk_get_last_result();
                        if (result == TK_STATUS_SUCCESS) {
                            continue;//若是tile不在region范围内或者没有feature数据则继续
                        }
                        else if (result == TK_STATUS_TILE_DATA_LOST){
                            is_lost_data = TK_YES;
                            continue;
                        } else {
                            goto CATCH;
                        }
                    }
                    base_tile_data->merged_tile_x = merged_x;
                    base_tile_data->merged_tile_y = merged_y;
                    if((result = _tk_put_base_tile(base_tile_data)) != TK_STATUS_SUCCESS) {
                        goto CATCH;
                    }
                }
                if((result = _tk_insert_base_tile_to_tile(tile_data, base_tile_data)) != TK_STATUS_SUCCESS) {
                    goto CATCH;
                }
                base_tile_data = NULL;
            }
        }
        if (is_lost_data) {
            result = TK_STATUS_TILE_DATA_LOST;
            goto CATCH;
        }
        tk_return_region(rid);
    }
    if((result = tk_lru_cache_insert(&_tk_tile_data_pool, tile_data, tile_data)) == TK_STATUS_SUCCESS) {
        tk_lru_cache_unlock(&_tk_tile_data_pool);
        goto SUCCESS;
    }
CATCH:
    if (region) {
        tk_return_region(rid);
    }
    if (base_tile_data) {
        tk_lru_cache_return(&_tk_base_tile_data_pool, base_tile_data);
        _tk_destruct_base_tile(&base_tile_data);
    }
    if (tile_data) {
        _tk_destroy_tile_data(NULL, tile_data);
        tile_data = NULL;
    }
    tk_lru_cache_unlock(&_tk_tile_data_pool);
    LOG_INFO("tk_load_tile failed : %i", result);
    return result;
SUCCESS:
    if (context->tile_data[0]) {
        tk_return_tile((tk_tile_key_t *)context->tile_data[0]);
    }
    context->tile_data[0] = tile_data;
    return TK_STATUS_SUCCESS;
}

void tk_tile_data_clean(void) {
    tk_lru_cache_clean(&_tk_tile_data_pool);
    tk_lru_cache_clean(&_tk_base_tile_data_pool);
}

