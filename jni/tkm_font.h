/*
 * =====================================================================================
 *
 *       Filename:  tkm_font.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  2010年12月28日 17时51分29秒
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  YOUR NAME (), 
 *        Company:  
 *
 * =====================================================================================
 */
#ifndef _FONT_H_
#define _FONT_H_

extern int font_size;

struct char_data {
    unsigned char XSize;                  
	unsigned char XDist;             
    unsigned char BytesPerLine;      
    unsigned char *pData;            
};

struct char_data* tk_x_get_char(unsigned short c);

extern int tk_font_init();
extern void tk_font_destroy();
void set_font_size(int size);

#endif
