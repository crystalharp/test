//
//  tk_lru_cache.h
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#ifndef tigermaptk_lru_cache_h
#define tigermaptk_lru_cache_h

#include <pthread.h>
#include "tk_types.h"
#include "tk_hash.h"

typedef enum {
    TK_CACHE_FULL_AUTO_EXPANTION,
    TK_CACHE_FULL_THROW_EXCEPTION,
} tk_cache_full_process_t;

typedef tk_bool_t
(*tk_cache_keys_equal_func_t) (const void *key_a, const void *key_b);

typedef void
(*tk_destroy_func_t) (void *key, void *value);

typedef unsigned long
(*tk_hash_func_t)(const void *key);

typedef struct _tk_cache_key {
    tk_hash_entry_t base;
    void *cache_key;
    tk_cache_keys_equal_func_t keys_equal;
} tk_cache_key_t;

typedef struct _tk_cache_node {
    tk_cache_key_t key;
    void *value;
    int use_count;
    tk_list_t link;
} tk_cache_node_t;

struct _tk_lru_cache {
    tk_list_t head;
    tk_list_t unused_head;
    tk_hash_table_t *hash_table;
    tk_destroy_func_t entry_destroy;
    tk_cache_keys_equal_func_t equal_func;
    tk_hash_func_t hash_func;
    int max_size;
    int capacity;
    int size;
    pthread_mutex_t mutex;
    tk_cache_full_process_t full_process_t;
};

/*
 * 初始化cache结构
 */
tk_status_t
tk_lru_cache_init (tk_lru_cache_t *lru_cache,
                   tk_cache_keys_equal_func_t keys_equal,
                   tk_hash_func_t key_hash,
                   tk_destroy_func_t		 entry_destroy,
                   unsigned long		 max_size,
                   tk_cache_full_process_t full_process_type);
/*
 * 将cache结构清空
 */
void
tk_lru_cache_fini (tk_lru_cache_t *lru_cache);

/*
 * 寻找key对应的数据
 * 若找到，返回TK_YES,否则返回TK_NO
 */
tk_bool_t
tk_lru_cache_key_exist (tk_lru_cache_t *lru_cache,
                  void *key);

/*
 * 寻找key对应的数据
 * 若找到，增加key所对应的节点的引用计数，返回数据
 * 否则返回NULL
 */
void *
tk_lru_cache_fetch (tk_lru_cache_t  *lru_cache,
                       void  *key);
/*
 * 减少key所对应的节点的引用计数
 */
void
tk_lru_cache_return (tk_lru_cache_t  *lru_cache,
                    void  *key);

/*
 * 插入key-value，初始计数为1，认为已在使用中
 * 若cache已满，根据初始化的策略，选择自动扩容，或者返回cache已满状态让使用者自行处理
 */
tk_status_t
tk_lru_cache_insert (tk_lru_cache_t *lru_cache,
                     void *key,
                     void *value);

/*
 * 插入key-value，初始计数为0，认为不在使用中
 * 若cache已满，根据初始化的策略，选择自动扩容，或者返回cache已满状态让使用者自行处理
 */
tk_status_t
tk_lru_cache_insert_without_use (tk_lru_cache_t *lru_cache,
                     void *key,
                     void *value);

void
tk_lru_cache_remove (tk_lru_cache_t *lru_cache,
                     void *key);

void
tk_lru_cache_clean (tk_lru_cache_t *lru_cache);

void
tk_lru_cache_lock(tk_lru_cache_t *lru_cache);

void
tk_lru_cache_unlock(tk_lru_cache_t *lru_cache);

#endif
