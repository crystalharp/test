/*
 * =====================================================================================
 *
 *       Filename:  tkconfig.h
 *
 *    Description:  Config the platform and the loglevel.
 *
 *        Version:  1.0
 *        Created:  12/29/2010 02:36:55 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Huiqiang Yang
 *        Company: TigerKnows
 *
 * =====================================================================================
 */

#ifndef __TKCONFIG_H
#define __TKCONFIG_H


#define ANDROID_PLAT
//#define SYMBIAN_PLAT
//#define IPHONE_PLAT
//#define WINDOWS_PLAT
//#define LINUX_PLAT
//#define BADA_PLAT

//#define TK_LOG_LEVEL_DEBUG
#define TK_LOG_LEVEL_INFO
//#define TK_LOG_LEVEL_ERROR
//#define TK_LOG_LEVEL_IGNORE

//#define TK_LOG_FNAME
//#define TK_LOG_TIME
//#define TK_LOG_TAG
//#define TK_LOG_TO_FILE
//#define TK_LOG_DEFAULT_METHOD
//#define DRAW_STRING
//#define NEED_BMP_HEADER

//#define MAX_TILE_BUFFER_SIZE 102400   //for smartphone
#define MAX_TILE_BUFFER_SIZE 409600   //for tablet

#define TK_BPP_32
//#define TK_BPP_16

#endif
