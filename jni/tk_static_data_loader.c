//
//  tk_static_data_loader.c
//  tigermap
//
//  Created by Chen Ming on 13-6-13.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include "tk_static_data_loader.h"
#include "tk_map_api.h"
#include "tk_global_info.h"
#include "tk_file_util.h"
#include "tk_style_config.h"
#include "tk_feature_data.h"
#include "tk_util.h"
#include "tk_log.h"

static int _tk_get_int(unsigned char *content, unsigned int *off) {
    int i;
    char str[64];
    int cnt = 0, flag = 0;
    
    do {
        i = *off;
        while (content[i] != '\n') {
            if (content[i] == '=') {
                flag = 1;
                i++;
                continue;
            }
            if (content[i] == '@') {
                flag = 0;
            }
            if (flag) {
                str[cnt++] = content[i];
            }
            i++;
        }
        *off = i + 1;
    } while (cnt == 0);
    str[cnt] = '\0';
    
    return atoi(str);
}

static void tk_destroy_style(tk_style_t *style) {
    if (style->pen_width) {
        free(style->pen_width);
        style->pen_width = NULL;
    }
    if (style->label_prioritys) {
        free(style->label_prioritys);
        style->label_prioritys = NULL;
    }
}

static void tk_destroy_style_buffer(tk_styles_buffer_t *buff) {
    if (buff->styles) {
        for (int i = 0 ; i < buff->layer_num; ++i) {
            tk_style_t *style = buff->styles + i;
            tk_destroy_style(style);
        }
        free(buff->styles);
    }
    if (buff->obj_type) {
        free(buff->obj_type);
    }
    if (buff->draw_order) {
        free(buff->draw_order);
    }
}

//return layer_num
static int _load_style_cfg(const char *fname, tk_styles_buffer_t *buff) {
    unsigned char *str;
    unsigned int offset, fsize;
    int i, k, priority;
    str = tk_read_file_content(fname, &fsize);
    if (!str) {
    	LOG_DBG("load file: %s failed", fname);
        return 0;
    }
    offset = 0;
    buff->layer_num = _tk_get_int(str, &offset);
    buff->obj_type = malloc(sizeof(short) * buff->layer_num);
    buff->draw_order = malloc(sizeof(short) * buff->layer_num);
    buff->styles = malloc(sizeof(tk_style_t) * buff->layer_num);
    if (buff->obj_type == NULL|| buff->styles == NULL) {
        return 0;
    }
    
    for (i = 0; i < buff->layer_num; i++) {
        char pwnum = 0;
        unsigned char priority_num = 0;
        int setting_num = _tk_get_int(str, &offset);
        buff->obj_type[i] = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].fontcolor = _tk_get_int(str, &offset);
        --setting_num;
        _tk_get_int(str, &offset);
        --setting_num;
        _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].zoom_min = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].zoom_max = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].fill_color = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].border_color = _tk_get_int(str, &offset);
        --setting_num;
        pwnum = _tk_get_int(str, &offset);
        --setting_num;
        if (pwnum) {
            buff->styles[i].pen_width = malloc(sizeof(char) * pwnum);
            for (k = 0; k < pwnum; k++) {
                buff->styles[i].pen_width[k] = _tk_get_int(str, &offset);
                --setting_num;
            }
        } else {
            buff->styles[i].pen_width = malloc(sizeof(char));
            buff->styles[i].pen_width[0] = 0;
        }
        buff->styles[i].line_type = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].icon_id = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].label_style = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].label_min = _tk_get_int(str, &offset);
        --setting_num;
        buff->styles[i].label_max = _tk_get_int(str, &offset);
        --setting_num;
        priority_num = buff->styles[i].label_max - buff->styles[i].label_min + 1;
        buff->styles[i].label_prioritys = malloc(sizeof(unsigned char) * priority_num);
        for (k = 0; k < priority_num; ++k) {
            buff->styles[i].label_prioritys[k] = _tk_get_int(str, &offset);
            --setting_num;
            if (buff->styles[i].label_prioritys[k] > tk_global_info.max_label_priority) {
                tk_global_info.max_label_priority = buff->styles[i].label_prioritys[k];
            }
        }
        buff->styles[i].font_size = _tk_get_int(str, &offset);
        if (buff->styles[i].font_size > tk_global_info.max_font_size) {
            tk_global_info.max_font_size = buff->styles[i].font_size;
        }
        else if (buff->styles[i].font_size < tk_global_info.min_font_size) {
            tk_global_info.min_font_size = buff->styles[i].font_size;
        }
        --setting_num;
        buff->styles[i].font_alter = _tk_get_int(str, &offset);
        --setting_num;
        priority = _tk_get_int(str, &offset);
        --setting_num;
        if (priority < buff->layer_num) {
            buff->draw_order[priority] = i;
        }
        if (setting_num > 0) {
            int cir = 0;
            int lim = setting_num;
            for (cir = 0; cir < lim; cir++)
                _tk_get_int(str, &offset);
        }
    }
    free(str);
    return buff->layer_num;
}

static tk_status_t _tk_load_styles() {
    int i;
    int n_num, c_num;
    char file_path[TK_MAX_PATH_LENGTH];
    
    tk_global_info.max_label_priority = 0;
    tk_global_info.max_font_size = 0;
    tk_global_info.min_font_size = 1000;
    sprintf(file_path, "%s/"F_CSTYLE, tk_global_info.respath);
    c_num = _load_style_cfg(file_path, &(tk_global_info.city_styles));
    sprintf(file_path, "%s/"F_NSTYLE, tk_global_info.respath);
    n_num = _load_style_cfg(file_path, &(tk_global_info.national_styles));
    
    tk_global_info.max_layer_num = TK_MAX(c_num, n_num);
    tk_global_info.layer_ctl = malloc(sizeof(int) * tk_global_info.max_layer_num);
    if (!tk_global_info.layer_ctl) {
        return TK_STATUS_NO_MEMORY;
    }
    for (i = 0; i < tk_global_info.max_layer_num; i++) {
        tk_global_info.layer_ctl[i] = 1;
    }
    return TK_STATUS_SUCCESS;
}

static char* _tk_sgets(char *line, int n, const char *str, unsigned int *str_pos) {
    register char *p;
    int c;
    
    if (n <= 0) {
        return NULL;
    }
    
    p = line;
    str = str + (*str_pos);
    
    while (--n) {
        if ((c = *str) == '\0') {
            break;
        }
        if ((*p++ = c) == '\n') {
            (*str_pos)++;
            str++;
            break;
        }
        (*str_pos)++;
        str++;
    }
    
    if (p > line) {
        *p = '\0';
        return line;
    }
    else {
        return NULL;
    }
}

static char* _tk_add_name_to_pool(const char *str) {
    char *name;
    int name_len = strlen(str);
    if (tk_global_info.city_name_mem_pos + 1 + name_len > tk_global_info.city_name_pool_size) {
        return NULL;
    }
    name = tk_global_info.city_name_pool + tk_global_info.city_name_mem_pos;
    strcpy(name, str);
    tk_global_info.city_name_mem_pos = tk_global_info.city_name_mem_pos + name_len + 1;
    return name;
}

#define str(x) # x
#define xstr(x) str(x)
static tk_status_t _tk_load_city_list() {
    int i;
    char city_path[TK_MAX_PATH_LENGTH];
    char line[TK_MAX_LINE_LENGTH];
    char cname[TK_MAX_NAME_LENGTH], cename[TK_MAX_NAME_LENGTH], pname[TK_MAX_NAME_LENGTH], pename[TK_MAX_NAME_LENGTH];
    int city_id, zoom_level, p_priority, c_priority;
    unsigned int fsize;
    int data_pos;
    unsigned int str_pos = 0;
    tk_latlon_t center;
    char *str_buff;
    int *cur_city;
    char *format_str = "%" xstr(TK_MAX_NAME_LENGTH) "s%" xstr(TK_MAX_NAME_LENGTH) "s%" xstr(TK_MAX_NAME_LENGTH) "s%" xstr(TK_MAX_NAME_LENGTH) "s%lf%lf%d%d%d%d\r\n";
    
    sprintf(city_path, "%s/citylist", tk_global_info.respath);
    str_buff = tk_read_text_file_content(city_path, &fsize);
    if (!str_buff) {
        return tk_get_last_result();
    }
    _tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos);
    sscanf(line, "%d", &tk_global_info.city_name_pool_size);
    if ((tk_global_info.city_name_pool = malloc(tk_global_info.city_name_pool_size)) == NULL) {
        //        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return TK_STATUS_NO_MEMORY;
    }
    /* read the header line */
    _tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos);
    /* read the national related data */
    _tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos);
    data_pos = str_pos;
    sscanf(line, format_str, cname, cename, pname, pename, &center.lon, &center.lat,
           &c_priority, &p_priority, &zoom_level, &city_id);
    tk_global_info.nat.cname = _tk_add_name_to_pool(cname);
    tk_global_info.nat.cename = _tk_add_name_to_pool(cename);
    tk_global_info.nat.pname = _tk_add_name_to_pool(pname);
    tk_global_info.nat.pename = _tk_add_name_to_pool(pename);
    tk_global_info.nat.center.lon = center.lon;
    tk_global_info.nat.center.lat = center.lat;
    tk_global_info.prov_num = tk_global_info.city_num = 0;
 	while (_tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos) != NULL) {
        sscanf(line, format_str, cname, cename, pname, pename, &center.lon, &center.lat,
               &c_priority, &p_priority, &zoom_level, &city_id);
        if (p_priority > tk_global_info.prov_num) {
            tk_global_info.prov_num = p_priority;
        }
        if (city_id > tk_global_info.city_num) {
            tk_global_info.city_num = city_id;
        }
    }
    tk_global_info.prov_list = malloc(sizeof(tk_prov_unit_t) * (tk_global_info.prov_num + 1));
    tk_global_info.city_list = malloc(sizeof(tk_city_unit_t) * (tk_global_info.city_num + 1));
    cur_city = (int *)malloc(sizeof(int) * (tk_global_info.prov_num + 1));
    if ((tk_global_info.city_list == NULL) || (tk_global_info.prov_list == NULL) || (cur_city == NULL)) {
        //        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return TK_STATUS_NO_MEMORY;
    }
    for (i = 0; i < tk_global_info.prov_num + 1; i++) {
        cur_city[i] = -1;
        tk_global_info.prov_list[i].name = NULL;
        tk_global_info.prov_list[i].ename = NULL;
        tk_global_info.prov_list[i].city_idx = -1;
    }
    for (i = 0; i < tk_global_info.city_num + 1; i++) {
        tk_global_info.city_list[i].cid = -1;
        tk_global_info.city_list[i].name = NULL;
        tk_global_info.city_list[i].ename = NULL;
        tk_global_info.city_list[i].reg_idx = -1;
        tk_global_info.city_list[i].next = -1;
        tk_global_info.city_list[i].prov_idx = -1;
        tk_global_info.city_list[i].reg_count = 0;
    }
    str_pos = data_pos;
 	while (_tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos) != NULL) {
        sscanf(line, format_str, cname, cename, pname, pename, &center.lon, &center.lat,
               &c_priority, &p_priority, &zoom_level, &city_id);
        if (tk_global_info.prov_list[p_priority].name == NULL) {/* first encount this province */
            tk_global_info.prov_list[p_priority].name = _tk_add_name_to_pool(pname);
            tk_global_info.prov_list[p_priority].ename = _tk_add_name_to_pool(pename);
            tk_global_info.prov_list[p_priority].city_idx = city_id;
            tk_global_info.city_list[city_id].cid = city_id;
            tk_global_info.city_list[city_id].name = _tk_add_name_to_pool(cname);
            tk_global_info.city_list[city_id].ename = _tk_add_name_to_pool(cename);
            tk_global_info.city_list[city_id].center.lon = center.lon;
            tk_global_info.city_list[city_id].center.lat = center.lat;
            tk_global_info.city_list[city_id].default_zoom_level = zoom_level;
            tk_global_info.city_list[city_id].prov_idx = p_priority;
            cur_city[p_priority] = city_id;
        } else {
            tk_global_info.city_list[city_id].cid = city_id;
            tk_global_info.city_list[city_id].name = _tk_add_name_to_pool(cname);
            tk_global_info.city_list[city_id].ename = _tk_add_name_to_pool(cename);
            tk_global_info.city_list[city_id].center.lon = center.lon;
            tk_global_info.city_list[city_id].center.lat = center.lat;
            tk_global_info.city_list[city_id].default_zoom_level = zoom_level;
            tk_global_info.city_list[city_id].prov_idx = p_priority;
            tk_global_info.city_list[cur_city[p_priority]].next = city_id;
            cur_city[p_priority] = city_id;
        }
    }
    
    free(cur_city);
    free(str_buff);
    return TK_STATUS_SUCCESS;
}

static tk_status_t _tk_load_region_list()
{
    int i;
    int *cur_reg;
    unsigned int file_size;
    int data_pos;
    unsigned int str_pos = 0;
    int city_id, region_id;
    char cname[TK_MAX_NAME], ename[TK_MAX_NAME];
    char reg_path[TK_MAX_PATH];
    char line[TK_MAX_LINE_LENGTH];
    char *str_buff;
    char *format_str = "%d%d" "%" xstr(TK_MAX_NAME) "s%" xstr(TK_MAX_NAME) "s%d\r\n";
    
    sprintf(reg_path, "%s/"F_RLIST, tk_global_info.respath);
    file_size = get_fsize(reg_path);
    str_buff = tk_read_text_file_content(reg_path, &file_size);
    if (!str_buff) {
        return tk_get_last_result();
    }
    /* read header */
    _tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos);
    /* read national related data */
    _tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos);
    data_pos = str_pos;
    sscanf(line, format_str, &region_id, &city_id, ename, cname, &file_size);
    tk_global_info.nat.rid = region_id;
    tk_global_info.nat.rname = _tk_add_name_to_pool(cname);
    tk_global_info.nat.rename = _tk_add_name_to_pool(ename);
    tk_global_info.nat.file_size = file_size;
    
    while (_tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos) != NULL) {//可在头部或者某文件里注明region个数
        tk_global_info.reg_num++;
    }
    tk_global_info.reg_list = malloc(sizeof(tk_reg_unit_t) * tk_global_info.reg_num);
    cur_reg = (int *)malloc(sizeof(int) * tk_global_info.reg_num);
    if (tk_global_info.reg_list == NULL || cur_reg == NULL) {
        //        LOG_INFO("Memory allocate fail in %s, %d.\n", __FILE__, __LINE__);
        return TK_STATUS_NO_MEMORY;
    }
    for (i = 0; i < tk_global_info.reg_num; i++) {
        tk_global_info.reg_list[i].rid = -1;
        tk_global_info.reg_list[i].name = NULL;
        tk_global_info.reg_list[i].ename = NULL;
        tk_global_info.reg_list[i].city_idx = -1;
        tk_global_info.reg_list[i].next = -1;
        cur_reg[i] = -1;
    }
    str_pos = data_pos;
    while (_tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos) != NULL) {
        sscanf(line, format_str, &region_id, &city_id, ename, cname, &file_size);
        tk_global_info.reg_list[region_id].rid = region_id;
        tk_global_info.reg_list[region_id].name = _tk_add_name_to_pool(cname);
        tk_global_info.reg_list[region_id].ename = _tk_add_name_to_pool(ename);
        tk_global_info.reg_list[region_id].file_size = file_size;
        tk_global_info.reg_list[region_id].city_idx = city_id;
        if (cur_reg[city_id] == -1) {
            tk_global_info.city_list[city_id].reg_idx = region_id;
        } else {
            tk_global_info.reg_list[cur_reg[city_id]].next = region_id;
        }
        ++tk_global_info.city_list[city_id].reg_count;
        cur_reg[city_id] = region_id;
    }
    free(str_buff);
    free(cur_reg);
    return 0;
}

static tk_status_t _tk_load_envelope()
{
    FILE *envelope_fp;
    char file_path[TK_MAX_PATH] = "\0";
    int i;
	unsigned char envelopebuf[20];
    
    sprintf(file_path, "%s/"F_SPI, tk_global_info.respath);
    tk_global_info.region_polygon_buf = tk_read_file_content(file_path, &tk_global_info.region_polygon_buf_size);
    if (!tk_global_info.region_polygon_buf) {
        return tk_get_last_result();
    }
    
    sprintf(file_path, "%s/"F_ENV, tk_global_info.respath);
    envelope_fp = fopen(file_path, "rb");
    // skip the version of map data and software
    fseek(envelope_fp, TK_REGION_VERNO_LENGTH, SEEK_CUR);
    
    tk_global_info.reg_bounds = malloc(sizeof(tk_reg_rects_t)* (tk_global_info.reg_num + 1));
    for (i = 0; i < tk_global_info.reg_num; i++) {
        fread(envelopebuf, sizeof(unsigned char), 20, envelope_fp);
        tk_global_info.reg_bounds[i].env.left = GETNUM4B(envelopebuf);//(envelopebuf[0] << 24) + (envelopebuf[1] << 16) + (envelopebuf[2] << 8) + envelopebuf[3];
        tk_global_info.reg_bounds[i].env.top = GETNUM4B(envelopebuf + 4);//(envelopebuf[4] << 24) + (envelopebuf[5] << 16) + (envelopebuf[6] << 8) + envelopebuf[7];
        tk_global_info.reg_bounds[i].env.right = GETNUM4B(envelopebuf + 8);//(envelopebuf[8] << 24) + (envelopebuf[9] << 16) + (envelopebuf[10] << 8) + envelopebuf[11];
        tk_global_info.reg_bounds[i].env.bottom = GETNUM4B(envelopebuf + 12);//(envelopebuf[12] << 24) + (envelopebuf[13] << 16) + (envelopebuf[14] << 8) + envelopebuf[15];
        tk_global_info.reg_bounds[i].offset = GETNUM4B(envelopebuf + 16);//(envelopebuf[16] << 24) + (envelopebuf[17] << 16) + (envelopebuf[18] << 8) + envelopebuf[19];
    }
    //    LOG_DBG("Reading Region Num: %d\n", reg_num);
    fclose(envelope_fp);
	return 0;
}

static int _tk_update_swcity(int cityid) {
    if(cityid == -1)
        return -1;
    int i = 0;
    for(i = 0; i < tk_global_info.subway_infos.city_num; i++){
        if(tk_global_info.subway_infos.city_infos[i].cityid == cityid){
            return i;
        }
    }
    tk_global_info.subway_infos.city_infos[i].cityid = cityid;
    tk_global_info.subway_infos.city_num++;
    return i;
}

static int _tk_update_swcolor(int cityid, tk_color_t color, const char* name) {
    int index = _tk_update_swcity(cityid);
    int color_num = tk_global_info.subway_infos.city_infos[index].color_num;
    if (index == -1)
        return -1;
    tk_global_info.subway_infos.city_infos[index].color_info[color_num].color = color;
    strcpy(tk_global_info.subway_infos.city_infos[index].color_info[color_num].subway_name, name);
    tk_global_info.subway_infos.city_infos[index].color_num++;
    return 0;
}

static tk_status_t _tk_load_subway_colorcfg()
{
    int i = 0;
    char *str_buff;
    char city_path[TK_MAX_PATH];
    char line[TK_MAX_LINE_LENGTH];
    char *format =  "%s %d %s %s\r\n";
    char city[32];
    int cityid;
    char sw_name[32];
    char color[32];
    int  old_rid = 0;
    char *end;
    unsigned int fsize;
    unsigned int str_pos = 0;
    memset(&tk_global_info.subway_infos, 0, sizeof(tk_subway_infos_t));
    sprintf(city_path, "%s/subwaycolor", tk_global_info.respath);
    str_buff = tk_read_text_file_content(city_path, &fsize);
    if (!str_buff) {
        return tk_get_last_result();
    }
    memset(city, 0, sizeof(city));
    memset(sw_name, 0, sizeof(sw_name));
    while (_tk_sgets(line, TK_MAX_LINE_LENGTH, str_buff, &str_pos) != NULL) {
        sscanf(line, format, city, &cityid, sw_name, color);
        if(old_rid != cityid){
            old_rid = cityid;
            i = 0;
        }
        _tk_update_swcolor(cityid, strtol(color, &end, 16), sw_name);
        memset(city, 0, sizeof(city));
        memset(sw_name, 0, sizeof(sw_name));
        memset(line, 0, TK_MAX_LINE_LENGTH);
        i++;
    }
    free(str_buff);
    return TK_STATUS_SUCCESS;
}

tk_status_t _tk_load_static_data() {
    tk_status_t result = TK_STATUS_SUCCESS;
        
    if ((result = _tk_load_styles()) != TK_STATUS_SUCCESS) {
        goto CATCH;
    }
    LOG_DBG("_tk_load_styles success");
    if ((result = _tk_load_city_list()) != TK_STATUS_SUCCESS) {
        //        LOG_INFO("failed to load the citylist!\n");
        goto CATCH;
    }
    LOG_DBG("_tk_load_city_list success");
    if ((result = _tk_load_region_list()) != TK_STATUS_SUCCESS) {
        //        LOG_INFO("failed to load the regionlist!\n");
        goto CATCH;
    }
    
    LOG_DBG("_tk_load_region_list success");
    if ((result = _tk_load_envelope()) != TK_STATUS_SUCCESS) {
        //        LOG_INFO("failed to load the envelope!\n");
        goto CATCH;
    }
    LOG_DBG("_tk_load_envelope success");
    result = _tk_load_subway_colorcfg();
    LOG_DBG("_tk_load_subway_colorcfg complete");
CATCH:
    return result;
}

void tk_destroy_static_data() {
    if (tk_global_info.prov_list) {
        free(tk_global_info.prov_list);
        tk_global_info.prov_list = NULL;
    }
    if (tk_global_info.city_list) {
        free(tk_global_info.city_list);
        tk_global_info.city_list = NULL;
    }
    if (tk_global_info.reg_bounds) {
        free(tk_global_info.reg_bounds);
        tk_global_info.reg_bounds = NULL;
    }
    if (tk_global_info.reg_list) {
        free(tk_global_info.reg_list);
        tk_global_info.reg_list = NULL;
    }
    if (tk_global_info.city_name_pool) {
        free(tk_global_info.city_name_pool);
        tk_global_info.city_name_pool = NULL;
    }
    if (tk_global_info.region_polygon_buf) {
        free(tk_global_info.region_polygon_buf);
        tk_global_info.region_polygon_buf = NULL;
    }
    if (tk_global_info.layer_ctl) {
        free(tk_global_info.layer_ctl);
        tk_global_info.layer_ctl = NULL;
    }
    tk_destroy_style_buffer(&tk_global_info.national_styles);
    tk_destroy_style_buffer(&tk_global_info.city_styles);
    memset(&tk_global_info, 0, sizeof(tk_engine_global_info_t));
}
