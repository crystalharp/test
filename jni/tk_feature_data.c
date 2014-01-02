//
//  tk_feature_data.c
//  tigermap
//
//  Created by Chen Ming on 13-6-10.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include "tk_feature_data.h"
#include "tk_context.h"
#include "tk_util.h"
#include "tk_region.h"
#include "tk_log.h"

enum {
    TK_SIDE_LEFT,
    TK_SIDE_TOP,
    TK_SIDE_RIGHT,
    TK_SIDE_BOTTOM,
};

static unsigned short _tk_get_huff_code_idx(tk_buf_info_t *data_buf, unsigned char *hf_configs, int hf_config_num) {
    unsigned short i = 0;
    unsigned short huffnum = 0;
    unsigned short cur_bit;
    unsigned short pre_bit = 0;
    unsigned int cur_stop;
    unsigned int pre_stop = 0;
    unsigned int huff_idx = 0;
    unsigned int stop_buf_idx = 0;
    
    for (i = 0; i < hf_config_num; i++) {
        cur_bit = hf_configs[i * TK_HUFF_STOP_UNIT_SIZE];
        stop_buf_idx = i * TK_HUFF_STOP_UNIT_SIZE + 1;
        cur_stop = GETNUM3B(hf_configs + stop_buf_idx);
        if (huffnum < cur_stop) {
            break;
        }
        huffnum = (huffnum << cur_bit) | (unsigned short)(tk_read_data_from_buf(data_buf, (unsigned char)cur_bit));
        if (tk_get_last_result() == TK_STATUS_BUF_OVERFLOAT) {
            break;
        }
        if (i != 0) {
            huff_idx += cur_stop - (pre_stop << pre_bit);
        }
        pre_bit = cur_bit;
        pre_stop = cur_stop;
    }
    huff_idx += huffnum - (pre_stop << pre_bit);
    return (unsigned short)huff_idx;
}

static tk_status_t _tk_set_point_on_edge(tk_point_t *point, int which_edge, int up_bound, int coord) {
    switch (which_edge) {
        case TK_SIDE_LEFT:
            point->x = 0;
            point->y = coord;
            break;
        case TK_SIDE_TOP:
            point->x = coord;
            point->y = up_bound;
            break;
        case TK_SIDE_RIGHT:
            point->x = up_bound;
            point->y = coord;
            break;
        case TK_SIDE_BOTTOM:
            point->x = coord;
            point->y = 0;
            break;
        default:
            return TK_STATUS_TILE_DATA_ERROR;
    }
    return TK_STATUS_SUCCESS;
}

static tk_status_t _tk_read_point(tk_context_t *context, tk_buf_info_t *data_buf, tk_point_t *point, tk_base_tile_data_t *base_tile, int must_read) {
    unsigned int xy;
    int coordinate_bits = 8 + base_tile->merged_level;
    int i = tk_read_data_from_buf(data_buf, 2);
    int base_level_diff = _tk_get_base_level(base_tile->zoom) - base_tile->zoom;
    if (tk_get_last_result() == TK_STATUS_BUF_OVERFLOAT) {
        LOG_DBG("_tk_read_point failed: TK_STATUS_BUF_OVERFLOAT1");
        goto CATCH;
    }
    if (i == 1 || must_read) {
        point->level_code = i;
    }
    else {//当diff=0时，全部读入，diff=1时不读入i=0的点，diff=2时，不读入i=0,i=2的点
        if ((context->base_level_diff > (i - 1) && (i > 0)) || ((context->base_level_diff > 0) && (i == 0)))
            i = -1;
        else
            point->level_code = i;
    }
    
    if (i != -1) {
        if (i != 1) {
            xy = tk_read_data_from_buf(data_buf, coordinate_bits << 1);
            if (tk_get_last_result() == TK_STATUS_BUF_OVERFLOAT) {
                goto CATCH;
            }
            point->x = xy >> coordinate_bits;
            point->y = xy & ((1 << coordinate_bits) -1);
            if (i == 3) {
                xy = ((1 << coordinate_bits) -1);
                if (point->x == (int)xy && ((point->y == 0)||(point->y == (int)xy)))
                    point->x++;
                if (point->y == (int)xy && ((point->x == 0)||(point->x == (int)xy + 1)))
                    point->y++;
            }
        } else {//i == 1, 边界点
            int coord;
            int in_which_edge;
            int up_bound = (1 << coordinate_bits);
            xy = tk_read_data_from_buf(data_buf, coordinate_bits + 2);
            if (tk_get_last_result() == TK_STATUS_BUF_OVERFLOAT) {
                goto CATCH;
            }
            in_which_edge = xy & 0x3;
            coord = xy >> 2;
            if(_tk_set_point_on_edge(point, in_which_edge, up_bound, coord) == TK_STATUS_TILE_DATA_ERROR) {
                LOG_DBG("_tk_read_point failed: TK_STATUS_TILE_DATA_ERROR2");
                return TK_STATUS_TILE_DATA_ERROR;
            }
        }
    } else {
        if (tk_skip_buf_bits(data_buf, coordinate_bits << 1) == TK_STATUS_BUF_OVERFLOAT) {
            LOG_DBG("_tk_read_point failed: TK_STATUS_BUF_OVERFLOAT");
            goto CATCH;
        }
        return TK_STATUS_POINT_SKIPED;
    }
    int abs_dif = TK_ABS(base_level_diff);
    point->x = ((point->x + (base_tile->merged_tile_x << 8)) << abs_dif) >> (abs_dif + base_level_diff);//基本像素坐标=>tile像素坐标
    point->y = ((point->y + (base_tile->merged_tile_y << 8)) << abs_dif) >> (abs_dif + base_level_diff);
    return TK_STATUS_SUCCESS;
CATCH:
    return TK_STATUS_TILE_DATA_ERROR;
}

//为了向前兼容
static tk_status_t _tk_read_unknown_feature(tk_context_t *context, tk_buf_info_t *tile_buf, tk_base_tile_data_t *base_tile, int tile_point_num) {
    short name_length, point_num;
    
    int coordinate_bits = 8 + base_tile->merged_level;
    //名字
    if (tk_read_data_from_buf(tile_buf, 1) == 1) {//=1表示有名字
        name_length = (short)tk_read_data_from_buf(tile_buf, 8);
        if (name_length < 0) {
            return TK_STATUS_TILE_DATA_ERROR;
        }
        if (tk_skip_buf_bytes(tile_buf, name_length) != TK_STATUS_SUCCESS) {
            return TK_STATUS_TILE_DATA_ERROR;
        }
    }
    //顶点
    point_num = tk_read_data_from_buf(tile_buf, TK_POINT_NUMBER_BITS);
    if (point_num == 0xff)
        point_num += (short int)tk_read_data_from_buf(tile_buf, 16);
    if (point_num > tile_point_num) {
        return TK_STATUS_TILE_DATA_ERROR;
    }
    if (tk_skip_buf_bits(tile_buf, (coordinate_bits << 1) * point_num) == TK_STATUS_BUF_OVERFLOAT) {
        return TK_STATUS_TILE_DATA_ERROR;
    }
    return TK_STATUS_UNKNOWN_FEATURE;
}

static tk_status_t _tk_read_feature(tk_context_t *context, tk_region_t *region, tk_buf_info_t *tile_buf, tk_base_tile_data_t *base_tile, int tile_point_num) {
    tk_status_t result = TK_STATUS_SUCCESS;
    tk_feature_data_t *reading_feature = base_tile->features + base_tile->feature_num;
    short i, point_num = 0, point_idx = 0;
    unsigned short feature_type = 0;
    
    //类型
    unsigned short huff_code_idx = _tk_get_huff_code_idx(tile_buf, region->hf_configs, region->hf_config_num);
    if (tk_get_last_result() == TK_STATUS_BUF_OVERFLOAT) {
        LOG_INFO("_tk_read_feature failed: TK_STATUS_BUF_OVERFLOAT1");
        goto CATCH;
    }
    if (context->zoom < (context->base_level - region->hf_indexes[huff_code_idx * 3 + 2])) {//读到了更高级别的feature数据直接返回错误
        LOG_INFO("_tk_read_feature failed:higher level feature data");
        goto CATCH;
    }
    feature_type = GETNUM2B(region->hf_indexes + huff_code_idx * 3);
    if (feature_type >= context->cur_style_buf->layer_num) {
        //不认识的feature，过掉该feature的数据，这里需要为点状地物另设一种地物类型，使的点状地物也要读取point_num，这样可以成功过滤任意类型的未知feature
        return _tk_read_unknown_feature(context, tile_buf, base_tile, tile_point_num);
    }
    reading_feature->has_name = TK_NO;
    reading_feature->name_length = -1;
    reading_feature->type = feature_type;
    reading_feature->priority = 0;
    if (reading_feature->type == 4 && context->zoom >= 10){
        reading_feature->type = 3;
    }
    
    //名字
    if (tk_read_data_from_buf(tile_buf, 1) == 1) {//=1表示有名字
        reading_feature->name_length = (short)tk_read_data_from_buf(tile_buf, 8);
        if (tk_get_last_result() == TK_STATUS_BUF_OVERFLOAT) {
            LOG_INFO("_tk_read_feature failed: TK_STATUS_BUF_OVERFLOAT2");
            goto CATCH;
        }
        if (reading_feature->name_length < 0) {
            LOG_INFO("_tk_read_feature failed: name_length < 0");
            goto CATCH;
        }
        if (reading_feature->name_length == 0 && base_tile->feature_num > 0) {
            tk_feature_data_t *last_feature = base_tile->features + base_tile->feature_num - 1;
            reading_feature->name = last_feature->name;
            reading_feature->name_length = last_feature->name_length;
        }
        else {
            reading_feature->name = base_tile->mem_pool.names_buf + base_tile->mem_pool.cur_name_index;
            if (tk_read_string_from_buf(tile_buf, reading_feature->name, reading_feature->name_length) == TK_STATUS_BUF_OVERFLOAT) {
                LOG_INFO("_tk_read_feature failed: TK_STATUS_BUF_OVERFLOAT3");
                goto CATCH;
            }
            base_tile->mem_pool.cur_name_index += reading_feature->name_length;
        }
        reading_feature->has_name = TK_YES;
    }
    
    //顶点
    if (context->cur_style_buf->obj_type[reading_feature->type] == 0) {
        point_num = 1;//点状feature
    }
    else {
        point_num = tk_read_data_from_buf(tile_buf, TK_POINT_NUMBER_BITS);
        if (point_num == 0xff)
            point_num += (short int)tk_read_data_from_buf(tile_buf, 16);
    }
    
    if (point_num + base_tile->mem_pool.cur_point > tile_point_num) {
        LOG_INFO("_tk_read_feature failed: cur_point > tile_point_num");
        goto CATCH;
    }
    reading_feature->points = base_tile->mem_pool.points_buf + base_tile->mem_pool.cur_point;
    for (i = 0; i < point_num; ++ i) {
        //这里数据是否已包含每级节点个数，预先分配的空间是否正好？todo: 考虑在生成数据时把同一feature的数据按不同级别分开
        if (i == 0 || i == point_num - 1) {// 首尾
            if (_tk_read_point(context, tile_buf, reading_feature->points + point_idx, base_tile, 1) == TK_STATUS_TILE_DATA_ERROR) {
                goto CATCH;
            }
            if (i == 0 ){
                reading_feature->left_top = reading_feature->points[0];
                reading_feature->right_bottom = reading_feature->points[0];
            }
            else {
                reading_feature->left_top.x = TK_MIN(reading_feature->left_top.x, reading_feature->points[point_idx].x);
                reading_feature->left_top.y = TK_MIN(reading_feature->left_top.y, reading_feature->points[point_idx].y);
                reading_feature->right_bottom.x = TK_MAX(reading_feature->right_bottom.x, reading_feature->points[point_idx].x);
                reading_feature->right_bottom.y = TK_MAX(reading_feature->right_bottom.y, reading_feature->points[point_idx].y);
            }
            ++point_idx;
        } else {
            result = _tk_read_point(context, tile_buf, reading_feature->points + point_idx, base_tile, 0);
            if ( result == TK_STATUS_SUCCESS) {
                reading_feature->left_top.x = TK_MIN(reading_feature->left_top.x, reading_feature->points[point_idx].x);
                reading_feature->left_top.y = TK_MIN(reading_feature->left_top.y, reading_feature->points[point_idx].y);
                reading_feature->right_bottom.x = TK_MAX(reading_feature->right_bottom.x, reading_feature->points[point_idx].x);
                reading_feature->right_bottom.y = TK_MAX(reading_feature->right_bottom.y, reading_feature->points[point_idx].y);
                ++point_idx;
            }
            else {
                if (result == TK_STATUS_TILE_DATA_ERROR) {
                    LOG_INFO("_tk_read_feature failed: TK_STATUS_TILE_DATA_ERROR");
                    goto CATCH;
                }//else TK_STATUS_POINT_SKIPED continue
            }
        }
    }
    if (point_idx == 0) {
        LOG_INFO("there is no point in this feature: %i", base_tile->feature_num);
        return TK_STATUS_FEATURE_NO_POINT;
    }
    reading_feature->points_num = point_idx;
    base_tile->mem_pool.cur_point += reading_feature->points_num;
    return TK_STATUS_SUCCESS;
CATCH:
    return TK_STATUS_TILE_DATA_ERROR;
}

tk_status_t tk_read_features(tk_context_t *context,
                             tk_region_t *region,
                             tk_base_tile_data_t *base_tile,
                             tk_buf_info_t tile_data_buf,
                             int feature_num,
                             int max_point_num) {
    tk_status_t result = TK_STATUS_SUCCESS;
    while (base_tile->feature_num < feature_num) {
        result = _tk_read_feature(context, region, &tile_data_buf, base_tile, max_point_num);
        if (result == TK_STATUS_UNKNOWN_FEATURE || result == TK_STATUS_FEATURE_NO_POINT) { //读到不认识的feature，或者feature没有可显示的点，过掉
            -- feature_num;
        }
        else if (result == TK_STATUS_SUCCESS) {
            tk_feature_data_t *cur_ft = base_tile->features + (base_tile->feature_num);
            cur_ft->tile = base_tile;
            if ((cur_ft->points[0].level_code == 1 || cur_ft->points[cur_ft->points_num - 1].level_code == 1)//有边界点，并且是线性地物，则可能需要链接
                && (context->cur_style_buf->obj_type[cur_ft->type] != 0)
                && (context->cur_style_buf->obj_type[cur_ft->type] != 4)){
                cur_ft->can_be_linked = 1;
            }
            else {
                cur_ft->can_be_linked = 0;
            }
            base_tile->overall_point_num += base_tile->features[base_tile->feature_num].points_num;
            if (base_tile->features[base_tile->feature_num].has_name) {
                base_tile->overall_name_len += base_tile->features[base_tile->feature_num].name_length;
            }
            ++ base_tile->feature_num;
        } else {
            goto CATCH;
        }
    }
CATCH:
    return result;
}
