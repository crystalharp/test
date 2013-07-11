/*
 * =====================================================================================
 *
 *       Filename:  tklog.h
 *
 *    Description:  the declaration of log system implementation 
 *
 *        Version:  1.0
 *        Created:  12/29/2010 03:36:21 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Huiqiang Yang 
 *        Company:  TigerKnows 
 *
 * =====================================================================================
 */
#ifndef __TKLOG_H
#define __TKLOG_H

#include <stdio.h>
#include "tk_config.h" 

extern FILE *log_fp;

enum log_tag_level {
    DEBUG, INFO, ERROR, IGNORE
};

extern void log_print(const char *fname, int line, int level, const char * format, ...);

#ifdef LOG_LEVEL_DEBUG 
    #define LOG_DBG(...)  log_print(__func__, __LINE__, DEBUG, __VA_ARGS__)
    #define LOG_INFO(...)   log_print(__func__, __LINE__, INFO, __VA_ARGS__)
    #define LOG_ERR(...)  log_print(__func__, __LINE__, ERROR, __VA_ARGS__)
#endif

#ifdef LOG_LEVEL_INFO 
    #define LOG_DBG(...)  
    #define LOG_INFO(...) log_print(__func__, __LINE__, INFO, __VA_ARGS__)   
    #define LOG_ERR(...)  log_print(__func__, __LINE__, ERROR, __VA_ARGS__)
#endif

#ifdef LOG_LEVEL_ERROR
    #define LOG_DBG(...) 
    #define LOG_INFO(...) 
    #define LOG_ERR(...)  log_print(__func__, __LINE__, ERROR, __VA_ARGS__)   
#endif

#ifdef LOG_LEVEL_IGNORE
    #define LOG_DBG(...)
    #define LOG_INFO(...)
    #define LOG_ERR(...)
#endif

#endif
