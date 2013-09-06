//
//  tk_lru_cache.c
//  tigermap
//
//  Created by Chen Ming on 13-6-9.
//  Copyright (c) 2013年 TigerKnows. All rights reserved.
//

#include <stdio.h>
#include <assert.h>
#include <pthread.h>
#include "tk_lru_cache.h"
#include "tk_error.h"
#include "tk_util.h"
#include "tk_list_inline.h"
#include "tk_log.h"

static tk_bool_t
_tk_cahce_key_equal(const void *key1, const void *key2) {
    const tk_cache_key_t *cache_key1 = (const tk_cache_key_t *)key1;
    const tk_cache_key_t *cache_key2 = (const tk_cache_key_t *)key2;
    if ((cache_key1 == cache_key2) || (cache_key1->keys_equal == cache_key2->keys_equal &&
                                         cache_key1->keys_equal(cache_key1->cache_key, cache_key2->cache_key))) {
        return TK_YES;
    }
    else {
        return TK_NO;
    }
}

static unsigned long default_hash(const void *key) {
    return *(unsigned long *)key;
}

tk_status_t
tk_lru_cache_init (tk_lru_cache_t		*lru_cache,
                   tk_cache_keys_equal_func_t keys_equal,
                   tk_hash_func_t key_hash,
                   tk_destroy_func_t		 entry_destroy,
                   unsigned long		 max_size,
                   tk_cache_full_process_t full_process_type) {
    if (keys_equal == NULL) {
        lru_cache->hash_table = _tk_hash_table_create (NULL);
    }
    else {
        lru_cache->hash_table = _tk_hash_table_create (_tk_cahce_key_equal);
    }
    if (lru_cache->hash_table == NULL)
        return TK_STATUS_NO_MEMORY;
    lru_cache->hash_func = key_hash;
    lru_cache->equal_func = keys_equal;
    lru_cache->entry_destroy = entry_destroy;
    lru_cache->max_size = max_size;
    lru_cache->capacity = max_size;
    lru_cache->size = 0;
    lru_cache->head.next = &(lru_cache->head);
    lru_cache->head.prev = &(lru_cache->head);
    lru_cache->unused_head.next = &lru_cache->unused_head;
    lru_cache->unused_head.prev = &lru_cache->unused_head;
    lru_cache->full_process_t = full_process_type;
    pthread_mutex_init(&lru_cache->mutex, NULL);;
    return TK_STATUS_SUCCESS;
}

/*
 * 手动删除key对应的数据，只在内部使用，外部应该很少用到，或者不让外部使用
 */
static void
_tk_lru_cache_remove (tk_lru_cache_t *lru_cache,
                     tk_cache_entry_t *key) {
    tk_cache_node_t *cache_node = tk_container_of(key, tk_cache_node_t, key);
    assert(cache_node->use_count == 0);
    tk_list_del(&cache_node->link);
    -- lru_cache->size;
    lru_cache->entry_destroy(cache_node->key.cache_key, cache_node->value);
    free(cache_node);
}

static void
_tk_cache_pluck (void *entry, void *closure) {
    _tk_lru_cache_remove (closure, entry);
}

//需要加锁吗？
void
tk_lru_cache_fini (tk_lru_cache_t *lru_cache) {
    assert(lru_cache->head.next == &lru_cache->head);//保证没有在使用中的数据
//    pthread_mutex_lock(&lru_cache->mutex);
    _tk_hash_table_foreach (lru_cache->hash_table,
                            _tk_cache_pluck,
                            lru_cache);
//    pthread_mutex_unlock(&lru_cache->mutex);
    assert (lru_cache->size == 0);
    _tk_hash_table_destroy (lru_cache->hash_table);
    pthread_mutex_destroy (&lru_cache->mutex);
}

void
tk_lru_cache_lock(tk_lru_cache_t *lru_cache) {
    pthread_mutex_lock(&lru_cache->mutex);
}

void
tk_lru_cache_unlock(tk_lru_cache_t *lru_cache) {
    pthread_mutex_unlock(&lru_cache->mutex);
}

static tk_cache_node_t *
_tk_cache_lookup_cache_node(tk_lru_cache_t *lru_cache, void *key) {
    tk_cache_key_t hash_key, *real_key;
    hash_key.cache_key = key;
    if (lru_cache->hash_func) {
        hash_key.base.hash = lru_cache->hash_func(key);
    }
    else {
        hash_key.base.hash = default_hash(key);
    }
    hash_key.keys_equal = lru_cache->equal_func;
    real_key = _tk_hash_table_lookup(lru_cache->hash_table, &hash_key.base);
    if (!real_key) {
        return NULL;
    } else {
        return tk_container_of(real_key, tk_cache_node_t, key);
    }
}

/*
 * 寻找key对应的数据
 * 若找到，返回TK_YES,否则返回TK_NO
 */
tk_bool_t
tk_lru_cache_key_exist (tk_lru_cache_t *lru_cache,
                        void *key) {
    assert(key != NULL);
    tk_cache_node_t *cache_node = _tk_cache_lookup_cache_node(lru_cache, key);
    if (cache_node) {
        return TK_YES;
    }
    else {
        return TK_NO;
    }
}

//借用
void *
tk_lru_cache_fetch (tk_lru_cache_t  *lru_cache,
                    void  *key) {
    assert(key != NULL);
//    pthread_mutex_lock(&lru_cache->mutex);
    tk_cache_node_t *cache_node = _tk_cache_lookup_cache_node(lru_cache, key);
    if (!cache_node) {
//        pthread_mutex_unlock(&lru_cache->mutex);
        return NULL;
    }
    if (cache_node->use_count < 0) {
        LOG_INFO("----lru cache use count exception when fetch!-----");
    }
    ++ cache_node->use_count;
    tk_list_del(&cache_node->link);
    tk_list_add(&cache_node->link, &lru_cache->head);
//    pthread_mutex_unlock(&lru_cache->mutex);
    return cache_node->value;
}

//归还
void
tk_lru_cache_return (tk_lru_cache_t  *lru_cache,
                     void *key) {
    assert(key != NULL);
    
//    pthread_mutex_lock(&lru_cache->mutex);
    tk_cache_node_t *cache_node = _tk_cache_lookup_cache_node(lru_cache, key);
    if (!cache_node) {
        LOG_INFO("lru cache return failed: key not found");
//        pthread_mutex_unlock(&lru_cache->mutex);
        return;
    }
    if (--cache_node->use_count == 0) {//若当前没有使用者了，就放入不在用链表中，但还能被找到
        tk_list_del(&cache_node->link);
        tk_list_add(&cache_node->link, &lru_cache->unused_head);
    }
    if (cache_node->use_count < 0) {
        LOG_INFO("----lru cache use count exception when return!-----");
    }
//    pthread_mutex_unlock(&lru_cache->mutex);
}

tk_status_t
tk_lru_cache_insert (tk_lru_cache_t *lru_cache,
                     void *key, void *value) {
    assert(key != NULL);
    tk_cache_node_t *cache_node = malloc(sizeof(tk_cache_node_t));
    if (!cache_node) {
        return TK_STATUS_NO_MEMORY;
    }
    if (lru_cache->hash_func) {
        cache_node->key.base.hash = lru_cache->hash_func(key);
        cache_node->key.cache_key = key; //考虑设置一个深拷贝函数, 或者直接取得key的所有权
    }
    else {
        cache_node->key.base.hash = default_hash(key);
        cache_node->key.cache_key = NULL;
    }
    cache_node->key.keys_equal = lru_cache->equal_func;
    cache_node->value = value;
    cache_node->use_count = 1;
    
//    pthread_mutex_lock(&lru_cache->mutex);
    
    _tk_hash_table_insert(lru_cache->hash_table, &cache_node->key.base);//入表
    tk_list_add(&cache_node->link, &lru_cache->head);//入链
    ++ lru_cache->size;
    
    if (lru_cache->size >= lru_cache->max_size) {
        if (lru_cache->unused_head.prev == &lru_cache->unused_head) {//满了，还没的删，可以选择自动增加容量或者报异常
            if (lru_cache->full_process_t == TK_CACHE_FULL_AUTO_EXPANTION) {
                ++lru_cache->max_size;
//                pthread_mutex_unlock(&lru_cache->mutex);
//                LOG_INFO("lru cache increase size to : %i", lru_cache->max_size);
                return TK_STATUS_SUCCESS;
            }
            else {
//                pthread_mutex_unlock(&lru_cache->mutex);
                return TK_STATUS_LRU_CACHE_FULL;
            }
        }
        else {//满了，可以删，删除不在用链表的尾节点
            while (lru_cache->size >= lru_cache->capacity && lru_cache->unused_head.prev != &lru_cache->unused_head) {
                tk_cache_node_t *unused_tail_node = tk_container_of(lru_cache->unused_head.prev, tk_cache_node_t, link);
                _tk_hash_table_remove(lru_cache->hash_table, &unused_tail_node->key.base);
                tk_list_del(&unused_tail_node->link);
                //            pthread_mutex_unlock(&lru_cache->mutex);
                lru_cache->entry_destroy(unused_tail_node->key.cache_key, unused_tail_node->value);
                free(unused_tail_node);
                --lru_cache->size;
            }
            lru_cache->max_size = lru_cache->size + 1;
            return TK_STATUS_SUCCESS;
        }
    }
//    pthread_mutex_unlock(&lru_cache->mutex);
    return TK_STATUS_SUCCESS;
}

void
tk_lru_cache_clean (tk_lru_cache_t *lru_cache) {
    while (lru_cache->unused_head.prev != &lru_cache->unused_head) {
        tk_cache_node_t *unused_tail_node = tk_container_of(lru_cache->unused_head.prev, tk_cache_node_t, link);
        _tk_hash_table_remove(lru_cache->hash_table, &unused_tail_node->key.base);
        tk_list_del(&unused_tail_node->link);
        lru_cache->entry_destroy(unused_tail_node->key.cache_key, unused_tail_node->value);
        free(unused_tail_node);
        --lru_cache->size;
    }
    lru_cache->max_size = lru_cache->capacity > lru_cache->size ? lru_cache->capacity : lru_cache->size;
}

void
tk_lru_cache_remove (tk_lru_cache_t *lru_cache,
                      void *key) {
    tk_cache_key_t hash_key, *real_key;
    hash_key.cache_key = key;
    if (lru_cache->hash_func) {
        hash_key.base.hash = lru_cache->hash_func(key);
    }
    else {
        hash_key.base.hash = default_hash(key);
    }
    hash_key.keys_equal = lru_cache->equal_func;
    real_key = _tk_hash_table_lookup(lru_cache->hash_table, &hash_key.base);
    if (real_key) {
        tk_cache_node_t *cache_node = tk_container_of(real_key, tk_cache_node_t, key);
        assert(cache_node->use_count == 0);
        tk_list_del(&cache_node->link);
        _tk_hash_table_remove(lru_cache->hash_table, (tk_hash_entry_t *)real_key);
    }
}
