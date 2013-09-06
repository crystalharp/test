/*
 * =====================================================================================
 *
 *       Filename:  tklog.c
 *
 *    Description:  The log system implementation.
 *
 *        Version:  1.0
 *        Created:  12/29/2010 02:52:20 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Huiqiang Yang 
 *        Company:  TigerKnows
 *
 * =====================================================================================
 */
#include <stdarg.h>
#include <time.h>
#include "tk_log.h"
#include "tk_config.h"

FILE *log_fp;

#ifdef LINUX_PLAT

#define MES_BUF_LEN 512 
char *log_tag[] = {"[DEBUG]", "[INFO]", "[ERROR]"};

void log_print(const char *fname, int line, int level, const char * format, ...)
{
    va_list ap;
    time_t tm;
    int n;
    char message[MES_BUF_LEN];

    va_start(ap, format);

    time(&tm);
    n = 0;
#ifdef LOG_TIME
    n = sprintf(message, "[%s", ctime(&tm));
#endif
#ifdef LOG_FNAME
    n = sprintf(message, "[%s : %d", fname, line);
#endif
    n += sprintf(message + n - 1, "] %s: ", log_tag[level]);
    vsnprintf(message + n - 1, MES_BUF_LEN - n + 1, format, ap);
    fputs(message, log_fp);
    fflush(log_fp);

    va_end(ap);
}
#endif

#ifdef WINDOWS_PLAT

#define MES_BUF_LEN 1024
char *log_tag[] = {"[DEBUG]", "[INFO ]", "[ERROR]"};

void log_print(const char *fname, int line, int level, const char * format, ...)
{
    va_list ap;
    time_t tm;
    int n;
    char message[MES_BUF_LEN];

    va_start(ap, format);

    time(&tm);
    n = 0;
#ifdef LOG_TIME
    n = sprintf(message, "[%s", ctime(&tm));
#endif
#ifdef LOG_FNAME
    n = sprintf(message, "[%s : %d", fname, line);
#endif
    n += sprintf(message + n - 1, "] %s: ", log_tag[level]);
    _vsnprintf(message + n - 1, MES_BUF_LEN - n + 1, format, ap);
    fputs(message, log_fp);
    fflush(log_fp);

    va_end(ap);
}
#endif

#ifdef IPHONE_PLAT
#include "TKLog.h"
#define MES_BUF_LEN 1024
char *log_tag[] = {"[DEBUG]", "[INFO ]", "[ERROR]"};

void log_print(const char *fname, int line, int level, const char * format, ...)
{
    va_list ap;
    time_t tm;
    int n;
    char message[MES_BUF_LEN];

    va_start(ap, format);

    time(&tm);
    n = 1;
#ifdef LOG_TIME
    n = sprintf(message + n - 1, "[%s", ctime(&tm));
#endif
#ifdef LOG_FNAME
    n = sprintf(message + n - 1, "[%s : %d", fname, line);
#endif
#ifdef LOG_TAG
    n += sprintf(message + n - 1, "] %s: ", log_tag[level]);
#endif
    vsnprintf(message + n - 1, MES_BUF_LEN - n + 1, format, ap);
#ifdef TK_LOG_DEFAULT_METHOD
    fputs(message, log_fp);
    fflush(log_fp);
#else
    printGBString(message);
#endif
    va_end(ap);
}
#endif

#ifdef SYMBIAN_PLAT

#define MES_BUF_LEN 1024
char *log_tag[] = {"[DEBUG]", "[INFO ]", "[ERROR]"};

void log_print(const char *fname, int line, int level, const char * format, ...)
{
    va_list ap;
    time_t tm;
    int n;
    char message[MES_BUF_LEN];

    va_start(ap, format);

    time(&tm);
    n = 0;
#ifdef LOG_TIME
    n = sprintf(message, "[%s", ctime(&tm));
#endif
#ifdef LOG_FNAME
    n = sprintf(message, "[%s : %d", fname, line);
#endif
    n += sprintf(message + n - 1, "] %s: ", log_tag[level]);
    vsnprintf(message + n - 1, MES_BUF_LEN - n + 1, format, ap);
    fputs(message, log_fp);
    fflush(log_fp);

    va_end(ap);
}
#endif

#ifdef ANDROID_PLAT
#include <android/log.h>

#define MES_BUF_LEN 1024

void log_print(const char *fname, int line, int level, const char * format, ...)
{
    va_list ap;
    time_t tm;
    int n, len;
    char message[MES_BUF_LEN];
    char *buf = message;
    va_start(ap, format);

    time(&tm);
    n = 0;
    len = 0;
#ifdef LOG_TIME
    len = sprintf(buf, "[%s", ctime(&tm));
    buf += len;
#endif
#ifdef LOG_FNAME
    len = sprintf(buf, "[%s : %d", fname, line);
    buf += len;
#endif
    len = sprintf(buf, "] ");
    buf += len;
    vsnprintf(buf, MES_BUF_LEN - (buf - message), format, ap);
    if (level == TK_DEBUG) {
        __android_log_write(ANDROID_LOG_DEBUG, "TKMapEngine", message);
    } else if (level == TK_INFO) {
        __android_log_write(ANDROID_LOG_INFO, "TKMapEngine", message);
    } else {
        __android_log_write(ANDROID_LOG_ERROR, "TKMapEngine", message);
    }

    va_end(ap);
}
#endif

#ifdef BADA_PLAT
#include <FBaseSys.h>
#define MES_BUF_LEN 1024
void log_print(const char *fname, int line, int level, const char * format, ...)
{
    va_list ap;
    time_t tm;
    int n;
    char message[MES_BUF_LEN];

    va_start(ap, format);

    time(&tm);
    n = 0;
#ifdef LOG_TIME
    n = sprintf(message, "[%s", ctime(&tm));
#endif
#ifdef LOG_FNAME
    n = sprintf(message, "[%s : %d", fname, line);
#endif
    n += sprintf(message + n - 1, "] ");
    vsnprintf(message + n - 1, MES_BUF_LEN - n + 1, format, ap);
    if (level == DEBUG) {
        AppLogDebug("TKMapEngine %s", message);
    } else if (level == INFO) {
        AppLog("TKMapEngine %s", message);
    } else {
        AppLogException("TKMapEngine %s", message);
    }

    va_end(ap);
}
#endif 
