//
//  tk_region.c
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>
#include <pthread.h>
#include <sys/stat.h>
#include "tk_types.h"
#include "tk_region.h"
#include "tk_util.h"
#include "tk_file_util.h"
#include "tk_global_info.h"
#include "tk_error.h"
#include "tk_log.h"
#include "tk_context.h"

#define TK_HUFF_CONFIG_UNIT_SIZE 4
#define TK_MAX_POINT_IN_BOUND 100
int tk_get_region_stat_from_file(int rid, int *ptotal_size, int *pdownloaded_size);
tk_lru_cache_t tk_region_data_pool;

static pthread_mutex_t _tk_region_file_mutex = PTHREAD_MUTEX_INITIALIZER;

static void destroy_region(void *key, void *data) {
    tk_region_t *region = (tk_region_t *)data;
    if (region->region_data) {
        free(region->region_data);
        region->region_data = NULL;
    }
    if (region->verifycode) {
        free(region->verifycode);
        region->verifycode = NULL;
    }
//    if (region->cached_polygon) {
//        free(region->cached_polygon);
//        region->cached_polygon = NULL;
//    }
    pthread_mutex_destroy(&region->region_lock);
    free(data);
}

tk_status_t tk_init_region_cache() {
    tk_status_t status = tk_lru_cache_init (&tk_region_data_pool,
                                            NULL,
                                            NULL,
                                            destroy_region,
                                            TK_MAX_REGION_CACHE_SIZE,
                                            TK_CACHE_FULL_AUTO_EXPANTION);
    return status;
}

void tk_fini_region_cache() {
    tk_lru_cache_fini(&tk_region_data_pool);
    pthread_mutex_destroy(&_tk_region_file_mutex);
}

void tk_return_region(int rid) {
    unsigned long key = rid;
    tk_lru_cache_lock(&tk_region_data_pool);
    tk_lru_cache_return(&tk_region_data_pool, &key);
    tk_lru_cache_unlock(&tk_region_data_pool);
}

tk_status_t tk_region_get_path(char *path, int rid) {
    assert(path != NULL);
    int prov_idx;
    if ((rid < 0 && rid != -3) || rid >= tk_global_info.reg_num) {
        return TK_STATUS_INVALID_ARGS;
    }
    
    if (rid == -3) {
        sprintf(path, "%s/%s/%s.dat", tk_global_info.datapath, tk_global_info.nat.pename, tk_global_info.nat.rename);
    } else {
        prov_idx = tk_global_info.city_list[tk_global_info.reg_list[rid].city_idx].prov_idx;
        sprintf(path, "%s/%s/%s.dat", tk_global_info.datapath, tk_global_info.prov_list[prov_idx].ename, tk_global_info.reg_list[rid].ename);
    }
    return TK_STATUS_SUCCESS;
}

static tk_status_t _tk_get_verify_code(tk_region_t *region, const char *chk_path) {
    FILE *chk_fp = NULL;
    unsigned char buff[8] = {0};
    int byte_num = 0;
    
    if (access(chk_path, 0) == 0) {
        chk_fp = fopen(chk_path, "rb");
        fread(buff, 1, 8, chk_fp);
        if (memcmp(region->version, buff, 6) != 0) {
            region->verifycode = NULL;//todo:
            fclose(chk_fp);
            return TK_STATUS_DATA_VERSION_NOT_MATCH;
        }
        else {
            byte_num = (buff[7] << 8) | (buff[6]);
            region->verifycode = malloc(byte_num);
            fread(region->verifycode, 1, byte_num, chk_fp);
            fclose(chk_fp);
        }
    } else {
        region->verifycode = NULL;
    }
    return TK_STATUS_SUCCESS;
}

//static tk_status_t _tk_get_polygon_bound(tk_region_t *region) {
//    tk_status_t result = TK_STATUS_SUCCESS;
//    unsigned char *buf = tk_global_info.region_polygon_buf + tk_global_info.reg_bounds[region->rid].offset;
//    int i = 0;
//    
//    region->point_num = *(buf++);
//    if (region->point_num >= TK_MAX_POINT_IN_BOUND) {
//        result = TK_STATUS_TOO_MANY_POINTS;
//        goto CATCH;
//    }
//    
//    if ((region->cached_polygon = (tk_point_t *)malloc(sizeof(tk_point_t) * region->point_num)) == NULL) {
//        result = TK_STATUS_NO_MEMORY;
//        goto CATCH;
//    }
//    for (i = 0; i < region->point_num; i++) {
//        region->cached_polygon[i].level_code = (buf[0] >> 7) & 0x01;
//        region->cached_polygon[i].x = ((buf[0] & 0x7f) << 24) + (buf[1] << 16) + (buf[2] << 8) + buf[3];
//        region->cached_polygon[i].y = (buf[4] << 24) + (buf[5] << 16) + (buf[6] << 8) + buf[7];
//        buf += 8;
//    }
//CATCH:
//    return result;
//}

static void _tk_get_tile_index_bound(tk_region_t *region) {
    int i = 0;
    int cur_index_count = 0;
    unsigned char *cur_pointer = NULL;
    
    for (i = 0; i < TK_BASE_LEVEL_NUM && cur_index_count < region->tile_index_count; i++) {
        region->tile_index_bound[i].low = cur_index_count + 2;
        cur_pointer = region->tile_index + cur_index_count * 6;
        cur_index_count += ((cur_pointer[9] << 16) | (cur_pointer[10] << 8) | (cur_pointer[11])) + 2;
        region->tile_index_bound[i].high = cur_index_count - 1;
    }
}

tk_bool_t tk_is_region_loaded(int rid) {
    return tk_lru_cache_key_exist(&tk_region_data_pool, &rid);
}

tk_region_t *tk_get_region(int rid) {
    int hfcoder_length = 0;
    int index_length = 0;
    unsigned char *hfcode_block;
    char map_data_path[TK_MAX_PATH_LENGTH];
    tk_status_t result = TK_STATUS_SUCCESS;
    unsigned long key = rid;
    tk_lru_cache_lock(&tk_region_data_pool);
    tk_region_t *region = tk_lru_cache_fetch(&tk_region_data_pool, &key);
    if (region) {/* already been loaded */
        tk_set_result(TK_STATUS_SUCCESS);
        tk_lru_cache_unlock(&tk_region_data_pool);
        return region;
    }
    
    if ((rid < 0 && rid != -3) || rid > tk_global_info.reg_num) {
        result = TK_STATUS_INVALID_REGION_NUM;
        goto CATCH;
    }
    
    if((result = tk_region_get_path(map_data_path, rid)) != TK_STATUS_SUCCESS) {
       goto CATCH;
    }
    
    if (access(map_data_path, 0) != 0) {
        result = TK_STATUS_FILE_NOT_EXIST;
        goto CATCH;
    }
    
    region = malloc(sizeof(tk_region_t));
    if (!region) {
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    memset(region, 0, sizeof(tk_region_t));
    pthread_mutex_init(&region->region_lock, NULL);
    
//    region_fp = fopen(map_data_path, "r+b");
//    if (region_fp == NULL) {
//        result = TK_STATUS_FILE_OPEN_FAILED;
//        goto CATCH;
//    }
    region->rid = rid;
    region->region_data = tk_read_file_content(map_data_path, &region->region_data_length);
    if (region->region_data == NULL) {
        result = tk_get_last_result();
        goto CATCH;
    }
    
    hfcoder_length = GETNUM3B(region->region_data);
    index_length = GETNUM3B(region->region_data + 3);
    
    region->tile_data = region->region_data + hfcoder_length + index_length + 6;
    region->tile_data_bias = hfcoder_length + index_length + 6;
    hfcode_block = region->region_data + 6;
    
    /* get the version number */
    memcpy(region->version, hfcode_block, 6);
    region->tile_index = region->region_data + hfcoder_length + 6;
    region->tile_index_count = index_length / 6;
    region->hf_index_count = GETNUM2B(hfcode_block + TK_REGION_VERNO_LENGTH);
    region->hf_config_num = hfcode_block[TK_REGION_VERNO_LENGTH + 2];
    region->hf_configs = hfcode_block + TK_REGION_VERNO_LENGTH + 3;
    region->hf_indexes = hfcode_block + TK_REGION_VERNO_LENGTH + 3 + region->hf_config_num * TK_HUFF_CONFIG_UNIT_SIZE;
    
    /* read the checksum data */
    strcat(map_data_path, ".chk");
    if((result = _tk_get_verify_code(region, map_data_path)) != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    
    /* read the region polygon bound */
//    if((result = _tk_get_polygon_bound(region)) != TK_STATUS_SUCCESS) {
//        goto CATCH;
//    }
    
    /* get upperbound and lowerbound of each level's tile index */
    _tk_get_tile_index_bound(region);
    
    if((result = tk_lru_cache_insert(&tk_region_data_pool, &key, region)) != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
CATCH:
    tk_lru_cache_unlock(&tk_region_data_pool);
    tk_set_result(result);
    if (result == TK_STATUS_SUCCESS) {
        return region;
    }
    else {
        if (region) {
            destroy_region(NULL, region);
            region = NULL;
        }
        return NULL;
    }
}

static int _tk_write_to_region_file(int rid, int off, int len, const char* buf) {
    char region_path[TK_MAX_PATH_LENGTH];
    FILE *fpdata;
    tk_status_t result = tk_region_get_path(region_path, rid);
    if (result != TK_STATUS_SUCCESS) {
        return result;
    }
    if ((fpdata = fopen(region_path, "r+b")) == NULL) {
        perror("error");
        return TK_STATUS_FILE_OPEN_FAILED;
    }
    if (fseek(fpdata, off, SEEK_SET) < 0) {
        perror("error");
        return TK_STATUS_FILE_SEEK_ERROR;
    }
    if (fwrite(buf, 1, len, fpdata) < len) {
        perror("error");
        return TK_STATUS_FILE_WRITE_ERROR;
    }
    fflush(fpdata);
    fclose(fpdata);
    return TK_STATUS_SUCCESS;
}

tk_status_t tk_region_write_file(int rid, int off, int len, const char* buf) {
    unsigned long key = rid;
    FILE *region_fp = NULL;
    tk_lru_cache_lock(&tk_region_data_pool);
    tk_region_t *region = tk_lru_cache_fetch(&tk_region_data_pool, &key);
    if (region) {/* already been loaded */
        tk_lru_cache_unlock(&tk_region_data_pool);
    	char map_data_path[TK_MAX_PATH_LENGTH];
    	tk_status_t result = TK_STATUS_SUCCESS;
        if((result = tk_region_get_path(map_data_path, rid)) != TK_STATUS_SUCCESS) {
           return result;
        }
        region_fp = fopen(map_data_path, "r+b");
        if (region_fp == NULL) {
        	return TK_STATUS_FILE_OPEN_FAILED;
        }
        pthread_mutex_lock(&region->region_lock);//可考虑去掉
        memcpy(region->region_data + off, buf, len);
        if (fseek(region_fp, off, SEEK_SET) < 0) {
            perror("error");
            pthread_mutex_unlock(&region->region_lock);
            tk_return_region(rid);
            return TK_STATUS_FILE_SEEK_ERROR;
        }
        if (fwrite(buf, 1, len, region_fp) < len) {
            perror("error");
            pthread_mutex_unlock(&region->region_lock);
            tk_return_region(rid);
            return TK_STATUS_FILE_WRITE_ERROR;
        }
        pthread_mutex_unlock(&region->region_lock);
        fflush(region_fp);
        fclose(region_fp);
        tk_return_region(rid);
        return TK_STATUS_SUCCESS;
    }
    else {
        tk_status_t result =  _tk_write_to_region_file(rid, off, len, buf);
        tk_lru_cache_unlock(&tk_region_data_pool);
        return result;
    }
}

/* ============================================
 * download mapdata files' interfaces
 * ============================================ */
#define DATA_BUFF_LEN  4096//4k
static int _tk_get_tile_num(unsigned char *tk_buffer_tile_index,
                 int *pA_num, int *pB_num, int *pC_num, int index_count)
{
    unsigned char *cur_pointer = tk_buffer_tile_index;
    int res = 0;
    *pA_num = GETNUM3B(cur_pointer + 9);
    res = *pA_num + 2;
    cur_pointer = tk_buffer_tile_index + res * 6;
    *pB_num = GETNUM3B(cur_pointer + 9);
    res += *pB_num + 2;
    if (res < index_count) { /* if C_level tiles exist */
        cur_pointer = tk_buffer_tile_index + res * 6;
        *pC_num = GETNUM3B(cur_pointer + 9);
    }
	return 0;
}

static int _tk_get_region_state_from_mem(tk_region_t *region, int *ptotal_size, int *pdownloaded_size) {
    int filesize, total_size, downloaded_size, offset, length, pos, map_data_bias, pre_pos, tile_size;
    int i, is_in_block, num[3];
    char j = 0, k = 0;
    unsigned char* verifycode;
    unsigned char tail, *cur_pointer = NULL;
    tk_context_t *context = tk_get_context();
    context->lost_data_count = 0;
    map_data_bias = region->tile_data_bias;
    filesize = region->region_data_length;
    if (region->verifycode == NULL) {
 /* if checksum file doesn't exist and the data file exist */
        *ptotal_size = filesize;
        *pdownloaded_size = filesize;
        return TK_STATUS_SUCCESS;
    }
    verifycode = region->verifycode;
    unsigned char *data_buffer = region->tile_data;
    cur_pointer = region->tile_index + 3;
    _tk_get_tile_num(region->tile_index, num, num+1, num+2, region->tile_index_count);
    offset = 0;
    length = 0;
    downloaded_size = 0;
    total_size = 0;
    is_in_block = 0;
    int is_reach_tail = 0;
    int count = 0;
    for (int n = 0; n < 3; ++n) {
        cur_pointer += 6 * 2;
        if (n == 0) {
            pre_pos = GETNUM3B(cur_pointer);
        }
        count += 2;
        for (i = 0; i < num[n]; ++i) {
            int idx = count;
            if (n == 0 && i == 0) {
                ++ count;
                continue;
            }
            else {
                if (n > 0 && i == 0) {
                    idx -= 2;
                }
            }
            cur_pointer += 6;
            if (count < region->tile_index_count) {
                pos = GETNUM3B(cur_pointer);
                tail = data_buffer[pos - 1];
            }
            else {
                pos = filesize - map_data_bias;
                tail = region->region_data[filesize - 1];
                is_reach_tail = 1;
            }
            tile_size = pos - pre_pos;
            total_size += tile_size;
            j = verifycode[(idx - 1)/8]; /* the (i+2-1)th tile */
            k = (idx - 1) % 8;
            if (((0x01L)&(tail ^ (j >> k))) != 0) {
                downloaded_size += tile_size;
                if (is_in_block) {//连续的需要下载的块，遇到一个不需要下载的块
                    tk_context_add_lost_data(tk_get_context(), region->rid, offset, length, TK_LOST_TYPE_DATA_LOST);
                    is_in_block = 0;
                }
            } else {
                if (is_in_block) {
                    length += tile_size;
                } else {
                    offset = pre_pos + map_data_bias;
                    length = tile_size;
                    is_in_block = 1;
                }
            }
            if (is_reach_tail) {
                break;
            }
            ++ count;
            pre_pos = pos;
        }
        if (is_reach_tail) {
            if (is_in_block == 1) {
                is_in_block = 0;
                tk_context_add_lost_data(context, region->rid, offset, length, 0);
            }
            break;
        }
        if (n == 2) {
            tail = region->region_data[filesize - 1];
            is_reach_tail = 1;
            pos = filesize - region->tile_data_bias;
            tile_size = pos - pre_pos;
            total_size += tile_size;
            j = verifycode[(count - 1)/8]; /* the (i+2-1)th tile */
            k = (count - 1) % 8;
            if (((0x01L)&(tail ^ (j >> k))) != 0) {
                downloaded_size += tile_size;
                if (is_in_block) {//连续的需要下载的块，遇到一个不需要下载的块
                    tk_context_add_lost_data(context, region->rid, offset, length, TK_LOST_TYPE_DATA_LOST);
                    is_in_block = 0;
                }
            } else {
                if (is_in_block) {
                    length += tile_size;
                } else {
                    offset = pre_pos + map_data_bias;
                    length = tile_size;
                    is_in_block = 1;
                }
            }
            if (is_in_block == 1) {
                is_in_block = 0;
                tk_context_add_lost_data(context, region->rid, offset, length, 0);
            }
        }
    }
    *ptotal_size = total_size + map_data_bias;
    *pdownloaded_size = map_data_bias + downloaded_size;
    return TK_STATUS_SUCCESS;
}

static int _tk_get_tile_info(FILE *map_data, unsigned char *cur_pointer, int map_data_bias,
                             int pre_pos, int *ppos, unsigned char *ptail,
                             int *ptile_size, int *ptotal_size, unsigned char *data_buffer, int *data_buffer_offset)
{
    int offset;
    *ppos = GETNUM3B(cur_pointer);
    /* bit_jump_to(*ppos + map_data_bias - 1); */
    //    fseek(map_data, *ppos + map_data_bias - 1, SEEK_SET);
    /* change */
    offset = *ppos - 1;
    while (*data_buffer_offset + DATA_BUFF_LEN <= offset) {
        fread(data_buffer, 1, DATA_BUFF_LEN, map_data);
        *data_buffer_offset += DATA_BUFF_LEN;
    }
    *ptail = data_buffer[offset % DATA_BUFF_LEN];
    *ptile_size = *ppos - pre_pos;
    *ptotal_size += *ptile_size;
	return 0;
}

static int _tk_add_lostdata(tk_context_t *context, int rid, unsigned char tail, unsigned char code,
                            unsigned char shift_num, unsigned int map_data_bias, int pre_pos,
                            int tile_size, int *pdownloaded_size, int *poffset,
                            int *plength, int *pis_in_block)
{
    if (((0x01L)&(tail ^ (code >> shift_num))) != 0) {
		*pdownloaded_size += tile_size;
        if (*pis_in_block) {/* in a block, now we must begin a new block */
            tk_context_add_lost_data(context, rid, *poffset, *plength, 0);
            *pis_in_block = 0;/* not in a block now */
        }
    } else {
        if (*pis_in_block) {/* in a block, just increase the length */
            *plength += tile_size;
        } else {/* not in a block, now begin a new block */
            *poffset = pre_pos + map_data_bias;
            *plength = tile_size;/* attention now is assignment operator */
            *pis_in_block = 1;
        }
    }
	return 0;
}

static int _tk_get_region_state_from_file(int rid, int *ptotal_size, int *pdownloaded_size)
{
    unsigned char* buff;
    unsigned short int byte_num;
    int filesize;
    unsigned char* verifycode;
    int total_size;
    int downloaded_size;
    char map_data_path[TK_MAX_PATH];
    char chk_data_path[TK_MAX_PATH];
    
    unsigned char *cur_pointer;
    int i;
    char j = 0;
    char k = 0;
    unsigned char *coder_code;
    unsigned char *tk_buffer_tile_index;
    int coder_length = 0;
    int index_length = 0;
    int offset;
    int length;
    int is_in_block;
    int map_data_bias, pos, A_num, B_num, C_num;
    int tile_num;
    int pre_pos, tile_size;
    unsigned char tail;
    FILE *map_data;
    FILE *chk_fp;
    unsigned char chk_ver[8];
    unsigned char data_ver[6];
    int data_buffer_offset = 0;
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_context_t *context = tk_get_context();
    if((result = tk_region_get_path(map_data_path, rid)) != TK_STATUS_SUCCESS) {
        return result;
    }
//    pthread_mutex_lock(&_tk_region_file_mutex);
    //check
    if (access(map_data_path, 0) != 0) {
        tk_context_add_lost_data(context, rid, 0, 0, TK_LOST_TYPE_DATA_LOST);
        result = TK_STATUS_SUCCESS;
        goto CATCH;
    }
    filesize = tk_get_file_size(map_data_path);
    sprintf(chk_data_path, "%s%s", map_data_path, ".chk");
    if (access(chk_data_path, 0) == 0) {
        chk_fp = fopen(chk_data_path, "rb");
        if (chk_fp == NULL) {
            result = TK_STATUS_FILE_OPEN_FAILED;
            goto CATCH;
        }
        else {
            fread(chk_ver, 1, 8, chk_fp);
            byte_num = (chk_ver[7] << 8) | chk_ver[6]; /* the byte order is changed */
        }
    }
    else { /* if checksum file doesn't exist and the data file exist */
        *ptotal_size = filesize;
        *pdownloaded_size = filesize;
        result = TK_STATUS_SUCCESS;
        goto CATCH;
    }
    map_data = fopen(map_data_path, "rb");
    if (map_data == NULL) {
        result = TK_STATUS_FILE_OPEN_FAILED;
        fclose(chk_fp);
        goto CATCH;
    }
    
    buff = malloc(6);
    if (buff == NULL) {
        fclose(map_data);
        fclose(chk_fp);
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    fread(buff, 1, 6, map_data);
    
    coder_length = GETNUM3B(buff);
    index_length = GETNUM3B(buff+3);
    map_data_bias = coder_length + index_length + 6;
    
    coder_code = malloc(coder_length);    /* free in the end of function */
    if (coder_code == NULL) {
        fclose(map_data);
        fclose(chk_fp);
        free(buff);
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    fread(coder_code, 1, coder_length, map_data);
    /* get the data's version number */
    memcpy(data_ver, coder_code, 6);
    
    if (memcmp(data_ver, chk_ver, 6) != 0) {
        LOG_DBG("The chk file and data file 's version don't match\n");
        *ptotal_size = filesize;
        *pdownloaded_size = filesize;
        fclose(map_data);
        fclose(chk_fp);
        free(buff);
        free(coder_code);
        result = TK_STATUS_DATA_VERSION_NOT_MATCH;
        goto CATCH;
    }
    
    verifycode = malloc(byte_num);
    if (verifycode == NULL) {
    	fclose(map_data);
        fclose(chk_fp);
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    fread(verifycode, 1, byte_num, chk_fp);
    fclose(chk_fp);
    
    tk_buffer_tile_index = malloc(index_length);    /* free in the end of function */
    if (tk_buffer_tile_index == NULL) {
        fclose(map_data);
        free(buff);
        free(coder_code);
        result = TK_STATUS_NO_MEMORY;
        goto CATCH;
    }
    fread(tk_buffer_tile_index, 1, index_length, map_data);
    if (!context->region_data_buf) {
        context->region_data_buf = malloc(DATA_BUFF_LEN);
    }
    unsigned char *data_buffer = context->region_data_buf;
    fread(data_buffer, 1, DATA_BUFF_LEN, map_data);
    
    cur_pointer = tk_buffer_tile_index;
    _tk_get_tile_num(tk_buffer_tile_index, &A_num, &B_num, &C_num, index_length);
    tile_num = A_num + B_num + C_num;   /* tiles' total num */
    
    offset = 0;
    length = 0;
    downloaded_size = 0;
    total_size = 0;
    is_in_block = 0;
    cur_pointer = tk_buffer_tile_index + 6 * 2 + 3;
    pos = GETNUM3B(cur_pointer);
    pre_pos = pos;
    assert(pre_pos == 0);
    /* need 2 redundant bits in LEVEL_A's tile */
    for (i = 1; i < A_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (2 + i) + 3;
        _tk_get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos,
                          &pos, &tail, &tile_size, &total_size, data_buffer, &data_buffer_offset);
        j = verifycode[(i+2-1)/8]; /* the (i+2-1)th tile */
        k = (i+2-1)% 8;
        _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                         tile_size, &downloaded_size, &offset, &length, &is_in_block);
        pre_pos = pos;
    }
    /* read LEVEL_B's first tile */
    cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
    _tk_get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos,
                      &pos, &tail, &tile_size, &total_size, data_buffer, &data_buffer_offset);
    j = verifycode[(i+2-1)/8]; /* the (i+2-1)th tile */
    k = (i+2-1)% 8;
    _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                     tile_size, &downloaded_size, &offset, &length, &is_in_block);
    pre_pos = pos;
    i++;
    /* need 4 redundant bits in LEVEL_B's tile */
    /* process the tiles in LEVEL_B */
    for ( ; i < A_num + B_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
        _tk_get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos,
                          &pos, &tail, &tile_size, &total_size, data_buffer, &data_buffer_offset);
        j = verifycode[(i+4-1)/8]; /* the (i+4-1)th tile */
        k = (i+4-1)% 8;
        _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                         tile_size, &downloaded_size, &offset, &length, &is_in_block);
        pre_pos = pos;
    }
    if (i + 4 < index_length/6) {
        /* read LEVEL_C's first tile */
        cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
        _tk_get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos,
                          &pos, &tail, &tile_size, &total_size, data_buffer, &data_buffer_offset);
        j = verifycode[(i+4-1)/8]; /* the (i+4-1)th tile */
        k = (i+4-1)% 8;
        _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                         tile_size, &downloaded_size, &offset, &length, &is_in_block);
        pre_pos = pos;
        i++;
        /* need 6 redundant bits in LEVEL_C's tile */
        /*process the tiles in LEVEL_C */
        for ( ; i < A_num + B_num + C_num ; i++) {
            cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
            _tk_get_tile_info(map_data, cur_pointer, map_data_bias, pre_pos,
                              &pos, &tail, &tile_size, &total_size, data_buffer, &data_buffer_offset);
            j = verifycode[(i+6-1)/8]; /* the (i+6-1)th tile */
            k = (i+6-1)% 8;
            _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                             tile_size, &downloaded_size, &offset, &length, &is_in_block);
            pre_pos = pos;
        }
        fseek(map_data, filesize-1, SEEK_SET);
        
        fread(&tail, 1, 1, map_data);
        
        total_size += filesize - pre_pos - map_data_bias;
        tile_size = filesize - pre_pos - map_data_bias; /* fixed bug */
        j = verifycode[(i+6-1)/8]; /* the (i+6-1)th tile */
        k = (i+6-1)% 8;
        _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                         tile_size, &downloaded_size, &offset, &length, &is_in_block);
        if (is_in_block == 1) {
            is_in_block = 0;
            tk_context_add_lost_data(context, rid, offset, length, 0);
        }
    } else {
        fseek(map_data, filesize-1, SEEK_SET);
        fread(&tail, 1, 1, map_data);
        total_size += filesize - pre_pos - map_data_bias;
        tile_size = filesize - pre_pos - map_data_bias; /* fixed bug */
        j = verifycode[(i+4-1)/8]; /* the (i+4-1)th tile */
        k = (i+4-1)% 8;
        _tk_add_lostdata(context, rid, tail, j, k, map_data_bias, pre_pos,
                         tile_size, &downloaded_size, &offset, &length, &is_in_block);
        if (is_in_block == 1) {
            is_in_block = 0;
            tk_context_add_lost_data(context, rid, offset, length, 0);
        }
    }
    *ptotal_size = total_size + map_data_bias;
    *pdownloaded_size = map_data_bias + downloaded_size;
    free(buff);
    free(coder_code);
    free(tk_buffer_tile_index);
    fclose(map_data);
    free(verifycode);
    result = TK_STATUS_SUCCESS;
CATCH:
//    pthread_mutex_unlock(&_tk_region_file_mutex);
    return result;
}

tk_status_t tk_region_get_state(int rid, int *ptotal_size, int *pdownloaded_size)
{
    tk_status_t result = TK_STATUS_SUCCESS;
    unsigned long key = rid;
    tk_lru_cache_lock(&tk_region_data_pool);
    tk_region_t *region = tk_lru_cache_fetch(&tk_region_data_pool, &key);
    if (!region) {//缓存里还没加载region，很可能是在地图下载页
        result = _tk_get_region_state_from_file(rid, ptotal_size, pdownloaded_size);
        tk_lru_cache_unlock(&tk_region_data_pool);
    }
    else {//缓存里已经加载了region，很可能是在边看边下载
        tk_lru_cache_unlock(&tk_region_data_pool);
        pthread_mutex_lock(&region->region_lock);//可考虑去掉
        result = _tk_get_region_state_from_mem(region, ptotal_size, pdownloaded_size);
        pthread_mutex_unlock(&region->region_lock);
        tk_return_region(rid);
    }
    return result;
}

#define MAX_LINE_LEN 1024
/* Generate the empty map data file according to the metadata */
tk_status_t tk_region_init_file(const char *metafile, int rid)
{
    int i, size, metasize, tile_num, coder_length=0, index_length=0;
    int map_data_bias, pos, A_num, B_num, C_num, res, pre_pos;
    short int byte_num = 0;
    char buf[MAX_LINE_LEN], rname[TK_MAX_PATH], tmp_path[TK_MAX_PATH];
    const char *ename, *pname;
    unsigned char tail, *coder_code = NULL, *tk_buffer_tile_index = NULL, *cur_pointer = NULL, *data = NULL, *index = NULL;
	FILE *meta_data_fp = NULL, *region_data_fp = NULL, *chk_fp = NULL;
    unsigned char buff[6];
    tk_status_t result = TK_STATUS_SUCCESS;
    
    /* get the city name and province name according to the region id */
    ename = tk_get_region_name_by_id(rid);
    pname = tk_get_provname_by_rid(rid);
    
    sprintf(rname,"%s/%s", tk_global_info.datapath, pname);
    if (access(rname, 0) < 0) { //judge whether the directory exists
        if (tk_mkdir(rname) < 0) {
            return TK_STATUS_MKDIR_FAILED;
        }
    }
    strcat(rname, "/");
    strcat(rname, ename);
    strcat(rname, ".dat");
    LOG_DBG("data file name: %s", rname);
    sprintf(tmp_path, "%s.tmp", rname);
    
    tk_lru_cache_lock(&tk_region_data_pool);
    
    if ((region_data_fp = fopen(tmp_path, "w+b")) == NULL) {
        result = TK_STATUS_FILE_OPEN_FAILED;
        goto err;
    }
    metasize = tk_get_file_size(metafile);
    if (metafile <= 0) {
        result = TK_STATUS_FILE_OPEN_FAILED;
        goto err;
    }
    
    /* parse the temp meta data file */
    meta_data_fp = fopen(metafile, "rb");
    if (meta_data_fp == NULL){
        LOG_DBG("map_data get error");
        result =  TK_STATUS_FILE_OPEN_FAILED;
        goto err;
    }
    
    fread(buff, 1, 4, meta_data_fp);
    size = (buff[0] << 24) + (buff[1] << 16) + (buff[2] << 8) + buff[3];
    
    fseek(region_data_fp, size - 1, SEEK_SET);
    fwrite("\0", 1, 1, region_data_fp);
    rewind(region_data_fp);
    
    fread(buff, 1, 6, meta_data_fp);
    coder_length = (buff[0]<<16) + (buff[1]<<8) + (buff[2]);
    index_length = (buff[3]<<16) + (buff[4]<<8) + (buff[5]);
    
    map_data_bias = coder_length + index_length + 6;
    
    index = malloc(coder_length + index_length);    /* free in the end of function */
    if (index == NULL) {
        result = TK_STATUS_NO_MEMORY;
        goto err;
    }
    fread(index, 1, coder_length + index_length, meta_data_fp);
    
    coder_code = index;
    tk_buffer_tile_index = index + coder_length;
    
    /* write index */
    fwrite(buff, 1, 6, region_data_fp);
    fwrite(index, 1, coder_length + index_length, region_data_fp);
    
    A_num = B_num = C_num = 0;
    cur_pointer = tk_buffer_tile_index;
    A_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    res = A_num + 2;
    cur_pointer = tk_buffer_tile_index + res * 6;
    B_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    res += B_num + 2;
    if (res < index_length/6) { /* if the C_level tiles exist */
        cur_pointer = tk_buffer_tile_index + res * 6;
        C_num = (cur_pointer[9] << 16) | (cur_pointer[10] << 8) | cur_pointer[11];
    }
    
    tile_num = A_num + B_num + C_num;   /* tiles' total num */
    byte_num = (tile_num + 6 + 7)/8;    /* more 6 bit added */
    
    assert((map_data_bias + byte_num + 4) == metasize);
    
    data = malloc(byte_num); /* read the check sum data */
    if (data == NULL) {
        result = TK_STATUS_NO_MEMORY;
        goto err;
    }
    fread(data, 1, byte_num, meta_data_fp);
    
    assert(map_data_bias == ftell(region_data_fp));
    /* write tile */
    cur_pointer = tk_buffer_tile_index + 6 * 2 + 3;
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
    pre_pos = pos;
    assert(pos == 0);
    
    /* the codes below use i as index，remain 2 bits at the start*/
    for (i = 1; i < A_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (2 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+2-1)/8] & (1<<(i+2-1)%8);    //get the related bits
        tail = tail >> (i+2-1)%8;
        fseek(region_data_fp, pos + map_data_bias - 1, SEEK_SET);
        fwrite(&tail, 1, 1, region_data_fp);
    }
    cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;//4+i not 2+i
    pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
    tail = data[(i+2-1)/8] & (1<<(i+2-1)%8);    //get the related bits
    tail = tail >> (i+2-1)%8;
    fseek(region_data_fp, pos + map_data_bias - 1, SEEK_SET);
    fwrite(&tail, 1, 1, region_data_fp);
    i++;
    /* process LEVEL_B's tiles */
    for ( ; i < A_num + B_num; i++) {
        cur_pointer = tk_buffer_tile_index + 6 * (4 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        fseek(region_data_fp, pos + map_data_bias - 1, SEEK_SET);
        fwrite(&tail, 1, 1, region_data_fp);
    }
    if (i < tile_num) {
        cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
        pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        fseek(region_data_fp, pos + map_data_bias - 1, SEEK_SET);
        fwrite(&tail, 1, 1, region_data_fp);
        i++;
        /* process LEVEL_C's tiles */
        for ( ; i < A_num + B_num + C_num ; i++) {
            cur_pointer = tk_buffer_tile_index + 6 * (6 + i) + 3;
            pos = (cur_pointer[0] << 16) | (cur_pointer[1] << 8) | cur_pointer[2];
            tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
            tail = tail >> (i+6-1)%8;
            fseek(region_data_fp, pos + map_data_bias - 1, SEEK_SET);
            fwrite(&tail, 1, 1, region_data_fp);
        }
        fseek(region_data_fp, size - 1, SEEK_SET);
        tail = data[(i+6-1)/8] & (1<<(i+6-1)%8);    //get the related bits
        tail = tail >> (i+6-1)%8;
        fwrite(&tail, 1, 1, region_data_fp);
    } else {
        fseek(region_data_fp, size - 1, SEEK_SET);
        tail = data[(i+4-1)/8] & (1<<(i+4-1)%8);    //get the related bits
        tail = tail >> (i+4-1)%8;
        assert((i+4-1)/8 == byte_num-1);
        fwrite(&tail, 1, 1, region_data_fp);
    }
    
    /* write tempory check file of map data*/
    sprintf(buf, "%s.chk", rname);
    if ((chk_fp = fopen(buf, "w+b")) == NULL) {
        exit(1);
    }
    fwrite(coder_code, 1, 6, chk_fp);
    fwrite(&byte_num, 1, 2, chk_fp);
    fwrite(data, 1, byte_num, chk_fp);
    fclose(chk_fp);
    /* end of writing tempory check file */
    rename(tmp_path, rname);
err:
    if (index != NULL) {
        free(index);
    }
    if (data != NULL) {
        free(data);
    }
    if (meta_data_fp != NULL) {
        fclose(meta_data_fp);
    }
    if (region_data_fp != NULL) {
        fclose(region_data_fp);
    }
    tk_lru_cache_unlock(&tk_region_data_pool);
    return result;
}

tk_status_t tk_region_get_version(int rid, unsigned char *rversion)
{
    unsigned long key = rid;
    tk_lru_cache_lock(&tk_region_data_pool);
    tk_region_t *region = tk_lru_cache_fetch(&tk_region_data_pool, &key);
    tk_lru_cache_unlock(&tk_region_data_pool);
    if (region != NULL) {
        pthread_mutex_lock(&region->region_lock);//可考虑去掉
        memcpy(rversion, region->region_data + 6, 6);
        pthread_mutex_unlock(&region->region_lock);
        tk_return_region(rid);
        return TK_STATUS_SUCCESS;
    }
    else {
        char rname[TK_MAX_PATH_LENGTH];
        FILE *fp = NULL;
        tk_status_t result = tk_region_get_path(rname, rid);
        if (result != TK_STATUS_SUCCESS) {
            return result;
        }
        if ((fp = fopen(rname, "r")) == NULL) {
            return TK_STATUS_FILE_OPEN_FAILED;
        }
        fseek(fp, 6, SEEK_CUR);
        fread(rversion, 1, 6, fp);
        fclose(fp);
        return TK_STATUS_SUCCESS;
    }
}

void tk_region_remove(int rid)//需要在外层加写锁，1. 删文件，2.删缓存
{
    char path[TK_MAX_PATH];
    unsigned long key = rid;
    tk_lru_cache_remove(&tk_region_data_pool, &key);
    tk_region_get_path(path, rid);
    remove(path);
    strcat(path, ".chk");
    remove(path);
    tk_tile_data_clean();
}

void tk_region_clean_unuse_cache(void) {
    tk_lru_cache_clean(&tk_region_data_pool);
}
