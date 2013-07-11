/*
 * =====================================================================================
 *
 *       Filename:  tkm_font.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时49分35秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *        Company:  
 *
 * =====================================================================================
 */
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include "tkm_mapint.h"
#include "tkm_font.h"
#include "tk_config.h"
#include "tk_util.h"
#include "tk_log.h"

#define F_FONT "ziku.fnt"

#define NUM_OF_ASCII_CHAR   (0x7f - 0x20 + 1)
#define NUM_OF_GB2312_SEC1_CHAR  ((0xfe - 0xa1 + 1) * (0xa9 - 0xa1 + 1))
#define NUM_OF_GB2312_SEC2_CHAR  ((0xfe - 0xa1 + 1) * (0xf7 - 0xb0 + 1))

#define ASCII_CHAR_DATA_SIZE font_size
#define GB2312_CHAR_DATA_SIZE (font_size * 2)
#define TOTAL_FONT_DATA_SIZE (NUM_OF_ASCII_CHAR * ASCII_CHAR_DATA_SIZE + \
                            (NUM_OF_GB2312_SEC1_CHAR + NUM_OF_GB2312_SEC2_CHAR) * \
                            GB2312_CHAR_DATA_SIZE)

int font_size = 13;

static unsigned char *_font_data = NULL;
static struct char_data _charinfo;

int tk_font_init() {
    char fn[TK_MAX_PATH];
    unsigned int actual = 0;

    _charinfo.XSize = font_size;    
    _charinfo.XDist = font_size;

    sprintf(fn, "%s/"F_FONT, respath);
    _font_data = read_whole_file(fn, &actual);

    if(_font_data == NULL) {
    	LOG_ERR("tk_font_init failed: tk_read_binaryfile_content returns null\n");
    	return -1;
    }
    if(TOTAL_FONT_DATA_SIZE != actual){
		LOG_ERR("tk_font_init failed: TOTAL_FONT_DATA_SIZE(%d) != actual(%d)\n", TOTAL_FONT_DATA_SIZE, actual);
    	return -1;
    }
    
    return 0;
}

void tk_font_destroy() {    
    if (_font_data != NULL) {
        free(_font_data);
        _font_data = NULL;
    }
}

struct char_data* tk_x_get_char(unsigned short c)
{
    int high = (c & 0xff00) >> 8;
    int low = c & 0xff;
    if ( c >= 0x20 && c <= 0x7f) {
        _charinfo.XSize = font_size / 2 + font_size % 2;
        _charinfo.XDist = font_size / 2 + font_size % 2;
        _charinfo.BytesPerLine = 1;
        _charinfo.pData = _font_data + ((c - 0x20) * font_size);
    } else if (c >= 0xa1a1 && c <= 0xa9fe) {
        _charinfo.XSize = font_size;
        _charinfo.XDist = font_size;
        _charinfo.BytesPerLine = 2;
        _charinfo.pData = _font_data + (NUM_OF_ASCII_CHAR * font_size) 
            + ((high - 0xa1) * (0xfe - 0xa1 + 1) + (low - 0xa1)) * font_size * 2;

    } else if (c >= 0xb0a1 && c <= 0xf7fe) {
        _charinfo.XSize = font_size;
        _charinfo.XDist = font_size;
        _charinfo.BytesPerLine = 2;
        _charinfo.pData = _font_data + (NUM_OF_ASCII_CHAR * font_size) 
            + (NUM_OF_GB2312_SEC1_CHAR * font_size * 2)
            + ((high - 0xb0) * (0xfe - 0xa1 + 1) + (low - 0xa1)) * font_size * 2;

    } else {
        LOG_INFO("TKFONT.c: unknow char: %d", c);
        return NULL;
    }
    return &_charinfo;
}

void set_font_size(int size)
{
    font_size = size;
}
