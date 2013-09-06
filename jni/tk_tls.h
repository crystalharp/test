//
//  tk_tls.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermap_tk_tls_h
#define tigermap_tk_tls_h

/*
 * borrow from pixman
 * add TK_DESTROY_THREAD_LOCAL macro by ChenMing
 */

#include <pthread.h>
#include <stdlib.h>

#ifdef _MSC_VER
/* 'inline' is available only in C++ in MSVC */
#   define inline __inline
#   define force_inline __forceinline
#   define noinline __declspec(noinline)
#elif defined __GNUC__ || (defined(__SUNPRO_C) && (__SUNPRO_C >= 0x590))
#   define inline __inline__
#   define force_inline __inline__ __attribute__ ((__always_inline__))
#   define noinline __attribute__((noinline))
#else
#   ifndef force_inline
#      define force_inline inline
#   endif
#   ifndef noinline
#      define noinline
#   endif
#endif

#  define TK_DEFINE_THREAD_LOCAL(type, name)			\
static pthread_once_t tls_ ## name ## _once_control = PTHREAD_ONCE_INIT; \
static pthread_key_t tls_ ## name ## _key;				\
\
static void								\
tls_ ## name ## _destroy_value (void *value)			\
{									\
free (value);							\
}									\
\
static void								\
tls_ ## name ## _make_key (void)					\
{									\
pthread_key_create (&tls_ ## name ## _key,			\
tls_ ## name ## _destroy_value);		\
}									\
\
static type *							\
tls_ ## name ## _alloc (void)					\
{									\
type *value = calloc (1, sizeof (type));			\
if (value)							\
pthread_setspecific (tls_ ## name ## _key, value);		\
return value;							\
}									\
\
static force_inline type *						\
tls_ ## name ## _get (void)						\
{									\
type *value = NULL;						\
if (pthread_once (&tls_ ## name ## _once_control,		\
tls_ ## name ## _make_key) == 0)		\
{								\
value = pthread_getspecific (tls_ ## name ## _key);		\
if (!value)							\
value = tls_ ## name ## _alloc ();			\
}								\
return value;							\
}                                   \
\
static void \
tls_##name##_reset_value(void) \
{                                       \
    type *value = pthread_getspecific (tls_ ## name ## _key);		\
    if (!value)                 \
        tls_ ## name ## _destroy_value (value);      \
    pthread_setspecific (tls_ ## name ## _key, NULL);   \
}

#   define TK_GET_THREAD_LOCAL(name)				\
tls_ ## name ## _get ()

#   define TK_DESTROY_THREAD_LOCAL(name)   \
tls_ ## name ## _reset_value()

#endif
