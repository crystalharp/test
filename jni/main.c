#include <string.h>
#include <zlib.h>
#include <jni.h>

#include "tkm_map.h"
#include "tks_suggest.h"
#include "tk_log.h"

//#define DEBUG
#ifdef DEBUG
#include <android/log.h>
char message[1000];
#endif

//init regine
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_a(JNIEnv *env, jobject thiz,
        jstring jresdir, jstring jmapdir, jint w, jint h, jbyteArray jbmpbuf, jint need_opt) {
    char *bmpbuf = (*env)->GetByteArrayElements(env, jbmpbuf, NULL);
    const char *resdir = (*env)->GetStringUTFChars(env, jresdir, 0);
    const char *mapdir = (*env)->GetStringUTFChars(env, jmapdir, 0);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_a...tk_init_render_engine...resdir:%s...mapdir:%s...w:%d...h:%d...need_opt:%d", jresdir, jmapdir, w, h, need_opt);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    int ret = tk_init_engine(resdir, mapdir, w, h, bmpbuf, need_opt);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_a...tk_init_render_engine end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    (*env)->ReleaseStringUTFChars(env, jmapdir, mapdir);
    (*env)->ReleaseStringUTFChars(env, jresdir, resdir);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_a...end....ret%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif 
    return ret;
}

//reset engine
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_b(JNIEnv *env, jobject thiz,
        jint w, jint h, jbyteArray jbmpbuf) {
    const char *bmpbuf = (*env)->GetByteArrayElements(env, jbmpbuf, NULL);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_b...tk_reset_matrix_size...w:%d...h:%d", w, h);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    tk_reset_matrix_size(w, h, bmpbuf);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_b...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}

//destory engine
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_c(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "................Java_com_tigerknows_map_Ca_c...tk_destroy_engine...");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    tk_destroy_engine();
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_c...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}

//get bmp buffer size
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_d(JNIEnv *env, jobject thiz, jint w, jint h) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_d...tk_get_matrix_size...w:%d...h:%d", w, h);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_get_matrix_size(w, h);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_d...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//refresh bmp buffer, get lost data info
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_e(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message,
      "...........Java_com_tigerknows_map_Ca_e...tk_get_lost_tile...");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    jbyteArray lost_tile_info = NULL;
    // tk_refresh_map_buffer();
    int lost_data_piece = (int) TK_LOST_DATA_PIECE;

    if (lostdata_idx > lost_data_piece) {
         lost_tile_info = (*env)->NewByteArray(env, 2*sizeof(int) + (lost_data_piece)*sizeof(struct tk_map_lostdata));
         (*env)->SetByteArrayRegion(env, lost_tile_info, 0, sizeof(int), &lostdata_idx);
         (*env)->SetByteArrayRegion(env, lost_tile_info, sizeof(int), sizeof(int), &lost_data_piece);
         (*env)->SetByteArrayRegion(env, lost_tile_info, 2*sizeof(int), (lost_data_piece)*sizeof(struct tk_map_lostdata), lostdata);
    } else {
         lost_tile_info = (*env)->NewByteArray(env, 2*sizeof(int) + (lostdata_idx)*sizeof(struct tk_map_lostdata));
         (*env)->SetByteArrayRegion(env, lost_tile_info, 0, sizeof(int), &lostdata_idx);
         (*env)->SetByteArrayRegion(env, lost_tile_info, sizeof(int), sizeof(int), &lostdata_idx);
         (*env)->SetByteArrayRegion(env, lost_tile_info, 2*sizeof(int), (lostdata_idx)*sizeof(struct tk_map_lostdata), lostdata);
    }
    
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_e...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return lost_tile_info;
}

//get tile data
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_ae(JNIEnv *env, jobject thiz, jint x, jint y, jint z) {
    #ifdef DEBUG
    sprintf(message,
      "...........Java_com_tigerknows_map_Ca_ae... tk_get_tile_buffer...%d....%d....%d....", x, y, z);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    jint tile_data = tk_get_tile_buffer(x,y,z);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ae...end....%d...", tile_data);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return tile_data;
}

//refresh bmp text
//struct label
//{
//    char *name;                //标注名字
//    int font_color;      //颜色
//    int font_size;             //大小
//    float slope;                 //角度
//    int outline_color;          //描边颜色
//    int x;
//    int y;     //显示位置
//
//};
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ac(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message,
      "...........Java_com_tigerknows_map_Ca_ac...start...");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    sprintf(message, ".......map text total num:%d....", glabels_num);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    
    
    //cal array lenth
    int totalLenth = sizeof(int);
    for (int i=0; i<glabels_num; i++) {
        struct tk_label *labelPtr = (struct tk_label *) (label_buffer+i);
        
        char *name = (char *) labelPtr->name;
        do {
            totalLenth++;
        } while(*name++); 
        totalLenth += 8*sizeof(int) + sizeof(float);
    }
    
    jbyteArray map_text_info = NULL;
    map_text_info = (*env)->NewByteArray(env, totalLenth);
    (*env)->SetByteArrayRegion(env, map_text_info, 0, sizeof(int), &(glabels_num));
    //copy array
    int start = sizeof(int);
    for (int i=0; i<glabels_num; i++) {
        struct tk_label *labelPtr = (struct tk_label *) (label_buffer+i);
        int nameLen = 0;
        char *name = (char *) labelPtr->name;
        do {
            nameLen++;
        } while(*name++); 
        (*env)->SetByteArrayRegion(env, map_text_info, start, nameLen*sizeof(char), (char *) labelPtr->name);
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen, sizeof(int), &(labelPtr->font_color));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + sizeof(int), sizeof(int), &(labelPtr->font_size));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 2*sizeof(int), sizeof(float), &(labelPtr->slope));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 2*sizeof(int) + sizeof(float), sizeof(int), &(labelPtr->outline_color));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 3*sizeof(int) + sizeof(float), sizeof(int), &(labelPtr->x));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 4*sizeof(int) + sizeof(float), sizeof(int), &(labelPtr->y));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 5*sizeof(int) + sizeof(float), sizeof(int), &(labelPtr->icon_id));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 6*sizeof(int) + sizeof(float), sizeof(int), &(labelPtr->icon_x));
        (*env)->SetByteArrayRegion(env, map_text_info, start + nameLen + 7*sizeof(int) + sizeof(float), sizeof(int), &(labelPtr->icon_y));
        start = start + nameLen + 8*sizeof(int) + sizeof(float);
    }
    
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ac...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return map_text_info;
}

//src point x,y to latlon
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_k(JNIEnv *env, jobject thiz, jint x, jint y) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_k...tk_scr2latlon...x:%d...y:%d", x, y);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    struct tk_point point = { x, y, -1 };
    struct tk_latlon latlon;
    tk_scr2latlon(&latlon, &point);
    jbyteArray barray = (*env)->NewByteArray(env, sizeof(struct tk_latlon));
    (*env)->SetByteArrayRegion(env, barray, 0, sizeof(struct tk_latlon), &latlon);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_k...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_yv(JNIEnv *env, jobject thiz,
		jdouble lat, jdouble lon) {
    #ifdef DEBUG
    sprintf(message, "............Java_com_tigerknows_map_Ca_yv...tk_get_rid_by_point...lat,%lf...lon:%lf", lat, lon);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    struct tk_latlon latlon = {lon, lat};
    struct tk_point point;
    tk_latlon2scr(&latlon, &point);
    int ret = tk_get_rid_by_point(&point);
    #ifdef DEBUG
    sprintf(message, "............Java_com_tigerknows_map_Ca_yv...end...ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//latlon to src
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_l(JNIEnv *env, jobject thiz,
        jdouble lat, jdouble lon) {
    #ifdef DEBUG
    //sprintf(message, "...........Java_com_tigerknows_map_Ca_l...tk_latlon2scr...lat:%lf....lon:%lf", lat, lon);
    //__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    struct tk_latlon latlon = { lon, lat };
    struct tk_point point;
    tk_latlon2scr(&latlon, &point);
    jbyteArray barray = (*env)->NewByteArray(env, sizeof(struct tk_point));
    (*env)->SetByteArrayRegion(env, barray, 0, sizeof(struct tk_point), &point);
    #ifdef DEBUG
    //sprintf(message, "...........Java_com_tigerknows_map_Ca_l...end....");
    //__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get cityId by latlon
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_m(JNIEnv *env, jobject thiz,
        jdouble lat, jdouble lon) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_m...tk_get_city_id...lat:%lf....lon:%lf", lat, lon);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    struct tk_latlon latlon = { lon, lat };
    int ret = tk_get_city_id(latlon);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_m...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//get current cityId
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_n(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_n...tk_get_current_city_id...");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_get_current_city_id();
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_n...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//在zoom、lat下一个象素代表的距离
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_o(JNIEnv *env, jobject thiz,
        jdouble lat, jint zoom) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_o...tk_scale_in_pixels...lat:%lf...zoom:%d.", lat, zoom);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_scale_in_pixels(lat, zoom);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_o...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//得到zoom对应的比例（以m为单位）
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_aa(JNIEnv *env, jobject thiz, jint zoom) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_aa...tk_scale_in_meters...zoom:%d.", zoom);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_scale_in_meters(zoom);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_aa...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//解压下载的联想词
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_p(JNIEnv *env, jobject thiz, jint citycode, jstring jfilepath) {
    const char *filepath = (*env)->GetStringUTFChars(env, jfilepath, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_p...tk_decompress...citycode:%d...filepath:%s.", citycode, filepath);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_decompress(citycode, filepath);
    (*env)->ReleaseStringUTFChars(env, jfilepath, filepath);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_p...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//init suggest word
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_q(JNIEnv *env, jobject thiz, jstring jfilepath, jstring jfilepathcommon, jint citycode) {
    const char *filepath = (*env)->GetStringUTFChars(env, jfilepath, 0);
    const char *filepathcommon = (*env)->GetStringUTFChars(env, jfilepathcommon, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_q...tk_suggestword_init...citycode:%d...filepath:%s...filepathcommon:%s", citycode, filepath, filepathcommon);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_suggestword_init(filepath, filepathcommon, citycode);
    
    (*env)->ReleaseStringUTFChars(env, jfilepath, filepath);
    (*env)->ReleaseStringUTFChars(env, jfilepathcommon, filepathcommon);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_q...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//引擎声明的默认返回的联想词个�?define SWCOUNT 15
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_r(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_r...SWCOUNT....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = SWCOUNT;
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_r...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//get suggest words list
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_s(JNIEnv *env, jobject thiz, jbyteArray jsearchword, jint type) {
    const char *searchword = (*env)->GetByteArrayElements(env, jsearchword, NULL);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_s...tk_getwordslist...type:%d...searchword:%s...", type, searchword);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int hit = 0;
    int ret = tk_getwordslist(searchword, type, &hit);

    jbyteArray barray = (*env)->NewByteArray(env, (SWCOUNT)*sizeof(Suggest_Word));
    (*env)->SetByteArrayRegion(env, barray, 0, (SWCOUNT)*sizeof(Suggest_Word), suggestwords);
    
    (*env)->ReleaseByteArrayElements(env, jsearchword, searchword, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_s...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//suggestword destroy
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_t(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_t...tk_suggestword_destroy...");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    tk_suggestword_destroy();
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_t...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}

//get suggest word version
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_u(JNIEnv *env, jobject thiz, jstring jpath, jint citycode) {
   const char *path = (*env)->GetStringUTFChars(env, jpath, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_u...tk_getwordslist...citycode:%d...path:%s...", citycode, path);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
   int ret = tk_getrevision(path, citycode);
   (*env)->ReleaseStringUTFChars(env, jpath, path);
    #ifdef DEBUG
   sprintf(message, "...........Java_com_tigerknows_map_Ca_u...end....ret:%d", ret);
   __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//解压region meta生成.dat .chk文件
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_v(JNIEnv *env, jobject thiz, jstring jfilepath, jint rid) {
    const char *filepath = (*env)->GetStringUTFChars(env, jfilepath, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_v...tk_init_region...filePath:%s...rid:%d.", filepath, rid);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_init_region(filepath, rid);
    (*env)->ReleaseStringUTFChars(env, jfilepath, filepath);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_v...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//write lost data
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_w(JNIEnv *env, jobject thiz, jint rid, jint offset, jint len, jbyteArray jbuf) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_w...tk_write_region...rid:%d...offset:%d...len:%d", rid, offset, len);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    const char *buf = (*env)->GetByteArrayElements(env, jbuf, NULL);
    int ret = tk_write_region(rid, offset, len, buf);
    (*env)->ReleaseByteArrayElements(env, jbuf, buf, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_w...end....ret:%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//get region data total size, downloadSize, lost data info.
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_x(JNIEnv *env, jobject thiz, jint rid) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_x...tk_get_region_stat...rid:%d...", rid);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    jbyteArray lost_tile_info = NULL;
    int total_size = 0;
    int downloaded_size = 0;
    int ret = tk_get_region_stat(rid, &total_size, &downloaded_size);
    if (ret == -1) {
        return NULL;
    }

   int lost_data_piece = (int) TK_LOST_DATA_PIECE;

   if (lostdata_idx > lost_data_piece) {
       lost_tile_info = (*env)->NewByteArray(env, 4*sizeof(int) + (lost_data_piece)*sizeof(struct tk_map_lostdata));
       (*env)->SetByteArrayRegion(env, lost_tile_info, 0, sizeof(int), &lostdata_idx);
       (*env)->SetByteArrayRegion(env, lost_tile_info, sizeof(int), sizeof(int), &lost_data_piece);
       (*env)->SetByteArrayRegion(env, lost_tile_info, 2*sizeof(int), sizeof(int), &total_size);
       (*env)->SetByteArrayRegion(env, lost_tile_info, 3*sizeof(int), sizeof(int), &downloaded_size);
       (*env)->SetByteArrayRegion(env, lost_tile_info, 4*sizeof(int), (lost_data_piece)*sizeof(struct tk_map_lostdata), lostdata);
   } else {
       lost_tile_info = (*env)->NewByteArray(env, 4*sizeof(int) + (lostdata_idx)*sizeof(struct tk_map_lostdata));
       (*env)->SetByteArrayRegion(env, lost_tile_info, 0, sizeof(int), &lostdata_idx);
       (*env)->SetByteArrayRegion(env, lost_tile_info, sizeof(int), sizeof(int), &lostdata_idx);
       (*env)->SetByteArrayRegion(env, lost_tile_info, 2*sizeof(int), sizeof(int), &total_size);
       (*env)->SetByteArrayRegion(env, lost_tile_info, 3*sizeof(int), sizeof(int), &downloaded_size);
       (*env)->SetByteArrayRegion(env, lost_tile_info, 4*sizeof(int), (lostdata_idx)*sizeof(struct tk_map_lostdata), lostdata);
   }
    #ifdef DEBUG
   sprintf(message, "...........Java_com_tigerknows_map_Ca_x...end....ret:%d", ret);
   __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
   return lost_tile_info;
}

//get map data root dir
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_y(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_y...tk_get_data_root....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* dataRootPtr;

    len = 0;
    dataRootPtr = NULL;

    dataRootPtr = tk_get_data_root();
    if (dataRootPtr == NULL) {
        return NULL;
    }
    while(dataRootPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, dataRootPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_y...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get region file path
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ya(JNIEnv *env, jobject thiz, jint regionId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ya...tk_get_region_path...regionId:%d", regionId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* dataRootPtr;

    len = 0;
    dataRootPtr = NULL;

    dataRootPtr = tk_get_region_path(regionId);
    if (dataRootPtr == NULL) {
        return NULL;
    }
    while(dataRootPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, dataRootPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ya...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get province name
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yb(JNIEnv *env, jobject thiz, jint regionId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yb...tk_get_provname...regionId:%d", regionId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* strPtr;

    len = 0;
    strPtr = NULL;

    strPtr = tk_get_provname(regionId);
    if (strPtr == NULL) {
        return NULL;
    }
    while(strPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, strPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yb...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get region id
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_yc(JNIEnv *env, jobject thiz, jstring jregionidname) {
   const char *regionidname = (*env)->GetStringUTFChars(env, jregionidname, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yc...tk_get_region_id...regionname:%s", regionidname);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
   int ret = tk_get_region_id(regionidname);
   (*env)->ReleaseStringUTFChars(env, jregionidname, regionidname);
    #ifdef DEBUG
   sprintf(message, "...........Java_com_tigerknows_map_Ca_yc...end....ret:%d", ret);
   __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//get region name
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yd(JNIEnv *env, jobject thiz, jint regionId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yd...tk_get_region_name...regionId:%d", regionId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* strPtr;

    len = 0;
    strPtr = NULL;

    strPtr = tk_get_region_name(regionId);
    if (strPtr == NULL) {
        return NULL;
    }
    while(strPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, strPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yd...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get province's city list
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ye(JNIEnv *env, jobject thiz, jstring jprovincename) {
    char** citylist;
    char* cityNamePtr;
    int len = 0;
    int start = 0;
    int citynum = 1;
    const char *provincename = (*env)->GetStringUTFChars(env, jprovincename, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ye...tk_get_citylist...provincenmae:%s", provincename);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    citylist = tk_get_citylist(provincename, &citynum);
    if (citylist == NULL) {
        return NULL;
    }
    for(int i=0; i<citynum; i++) {
        cityNamePtr = citylist[i];
        start = 0;
        if (cityNamePtr == NULL) {
            return NULL;
        }
        while(cityNamePtr[start] != '\0') {
            start++;
            len++;
        }
    }
    jbyteArray barray = (*env)->NewByteArray(env, len+citynum-1);
    start = 0;
    char seprator[1] = {','};
    for(int i=0; i<citynum; i++) {
        len = 0;
        cityNamePtr = citylist[i];
        if (cityNamePtr == NULL) {
            return NULL;
        }
        while(cityNamePtr[len] != '\0') {
            len++;
        }
        (*env)->SetByteArrayRegion(env, barray, start, len, cityNamePtr);
        start += len;
        if (i<citynum-1)
        {
            (*env)->SetByteArrayRegion(env, barray, start, 1, seprator);
            start++;
        }
    }

   (*env)->ReleaseStringUTFChars(env, jprovincename, provincename);
    #ifdef DEBUG
   sprintf(message, "...........Java_com_tigerknows_map_Ca_ye...end....");
   __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get city id
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_yf(JNIEnv *env, jobject thiz, jstring jcityname) {
   const char *cityname = (*env)->GetStringUTFChars(env, jcityname, 0);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yf...tk_get_cityid...jcityname:%s...", cityname);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
   int ret = tk_get_cityid(cityname);
   (*env)->ReleaseStringUTFChars(env, jcityname, cityname);
    #ifdef DEBUG
   sprintf(message, "...........Java_com_tigerknows_map_Ca_yf...end....ret:%d", ret);
   __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

//get region info  "中文名字, English name, file size, city ename"
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yg(JNIEnv *env, jobject thiz, jint regionId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yg...tk_get_region_info...regionId:%d...", regionId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* regionInfoPtr;

    len = 0;
    regionInfoPtr = NULL;

    regionInfoPtr = tk_get_region_info(regionId);
    if (regionInfoPtr == NULL) {
        return NULL;
    }
    while(regionInfoPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, regionInfoPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yg...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get city info "中文名字, English name, latitude, longitude, level, 省份中文名字, province English name"
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yh(JNIEnv *env, jobject thiz, jint cityId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yh...tk_get_city_info...cityId:%d...", cityId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* cityInfoPtr;

    len = 0;
    cityInfoPtr = NULL;

    cityInfoPtr = tk_get_city_info(cityId);
    if (cityInfoPtr == NULL) {
        return NULL;
    }
    while(cityInfoPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, cityInfoPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yh...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get city's region list
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yi(JNIEnv *env, jobject thiz, jint cityId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yi...tk_get_regionlist...cityId:%d...", cityId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* regionListPtr;

    len = 0;
    regionListPtr = NULL;

    regionListPtr = tk_get_regionlist(cityId);
    if (regionListPtr == NULL) {
        return NULL;
    }
    while(regionListPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, regionListPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yi...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get province's chinese name list
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yj(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yj...tk_get_provincelist.....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* provinceListPtr;

    len = 0;
    provinceListPtr = NULL;

    provinceListPtr = tk_get_provincelist();
    if (provinceListPtr == NULL) {
        return NULL;
    }
    while(provinceListPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, provinceListPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yj...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get province's chinese & english name list
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yk(JNIEnv *env, jobject thiz) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yk...tk_get_eprovincelist.....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* provinceListPtr;

    len = 0;
    provinceListPtr = NULL;

    provinceListPtr = tk_get_eprovincelist();
    if (provinceListPtr == NULL) {
        return NULL;
    }
    while(provinceListPtr[len] != '\0') {
        len++;
    }

    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, provinceListPtr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yk...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//remove city data�?dat文件�?
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_yl(JNIEnv *env, jobject thiz, jstring jcityname) {
   const char *cityname = (*env)->GetStringUTFChars(env, jcityname, 0);
    #ifdef DEBUG
    sprintf(message,
            "...........Java_com_tigerknows_map_Ca_yl...tk_remove_city_data...jcityname:%s..", jcityname);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    tk_remove_city_data(cityname);
   (*env)->ReleaseStringUTFChars(env, jcityname, cityname);
    #ifdef DEBUG
   sprintf(message, "...........Java_com_tigerknows_map_Ca_yl...end....");
   __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}

//get region version
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ym(JNIEnv *env, jobject thiz, jint regionId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ym...tk_get_region_version...regionId:%d", regionId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    char versions[6];
    for (int i = 0; i < 6; i++) {
        versions[i] = 0;
    }
    tk_get_region_version(regionId, versions);
    jbyteArray versionByte = (*env)->NewByteArray(env, 6);
    (*env)->SetByteArrayRegion(env, versionByte, 0, 6, versions);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ym...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return versionByte;
}

//remove region data�?dat文件 .chk文件�?
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_yn(JNIEnv *env, jobject thiz, jint regionId) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yn...tk_remove_region_data...regionId:%d..", regionId);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    tk_remove_region_data(regionId);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yn...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}

//服务器返回的经纬度解密偏移后获取正确的经纬度
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_z(JNIEnv *env, jobject thiz,
        jdouble lat, jdouble lon) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_z...tk_latlon_transform...lat:%lf....lon:%lf", lat, lon);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    struct tk_latlon latlon;
    jbyteArray barray = (*env)->NewByteArray(env, sizeof(struct tk_latlon));
    tk_latlon_transform(lon, lat, 0, 0, 0, &latlon.lon, &latlon.lat);
    (*env)->SetByteArrayRegion(env, barray, 0, sizeof(struct tk_latlon), &latlon);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_z...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get POI name by Point
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ab(JNIEnv *env, jobject thiz, jint x, jint y, jint flag) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ab...tk_get_poi_name...x:%d,y:%d,flag:%d", x, y, flag);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* featureStr;
    
    len = 0;
    featureStr = NULL;
    
    featureStr = tk_get_poi_name(x, y, flag);
    if (featureStr == NULL) {
        return NULL;
    }
    while(featureStr[len] != '\0') {
        len++;
    }
    
    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, featureStr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ab...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//get POI name by Position
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_abl(JNIEnv *env, jobject thiz, jdouble x, jdouble y, jint zoom, jint flag) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_abl...tk_get_poi_namel...x:%lf,y:%lf,zooom:%d,flag:%d", x, y, zoom, flag);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* featureStr;
    
    len = 0;
    featureStr = NULL;
    
    featureStr = tk_get_poi_namel(y, x, zoom, flag);
    if (featureStr == NULL) {
	#ifdef DEBUG
    	sprintf(message, "...........Java_com_tigerknows_map_Ca_abl...end....NULL");
    	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    	#endif
        return NULL;
    }
    while(featureStr[len] != '\0') {
        len++;
    }
    
    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, featureStr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_abl...end....%s", featureStr);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

//reset font size
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_yt(JNIEnv *env, jobject thiz, jfloat offset) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yt...tk_reset_font_size...offset:%d...", offset);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    tk_reset_font_size(offset);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yt...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return;
}

JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_zb(JNIEnv *env, jobject thiz, jint offset) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_zb...tk_reset_icon_size...offset:%d", offset);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    tk_reset_icon_size(offset);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_zb...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}

JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_zc(JNIEnv *env, jobject thiz, jbyteArray jbmpbuf, jbyteArray jpngbuf) {
    char *bmpbuf = (*env)->GetByteArrayElements(env, jbmpbuf, NULL);
    char *pngbuf = (*env)->GetByteArrayElements(env, jpngbuf, NULL);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_zc...bmp_to_png...");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    int ret = bmp_to_png(bmpbuf, pngbuf);

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_zc...bmp_to_png end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_zc...end....ret%d", ret);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif 
    return ret;
}

JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_ys(JNIEnv *env, jobject thiz, jdouble lon, jdouble lat, jint w, jint h, jint zoom) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ys...tk_get_screen_label...lon:%lf,lat:%lf,w:%d,h:%d,zoom:%d", lon, lat, w, h, zoom);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int ret = tk_get_screen_label(lon, lat, w, h, zoom);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_ys...end...lostdata_idx:%d", lostdata_idx);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return ret;
}

// char *init_icon_num( int icon_num)
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_yw(JNIEnv *env, jobject thiz, jint icon_num) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yw...tk_init_icon_num...icon_num:%d", icon_num);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    int len;
    char* resultStr;
    
    len = 0;
    resultStr = NULL;
    
    resultStr  = tk_init_icon_num(icon_num);
    if (resultStr == NULL) {
	#ifdef DEBUG
    	sprintf(message, "...........Java_com_tigerknows_map_Ca_yw...end....NULL");
    	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    	#endif
        return NULL;
    }
    while(resultStr[len] != '\0') {
        len++;
    }
    
    jbyteArray barray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, barray, 0, len, resultStr);
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yw...end....%s", resultStr);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
    return barray;
}

// void set_icon(int icon_id, int w, int h)
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_yu(JNIEnv *env, jobject thiz, jint icon_id, jint w, jint h) {
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yu...tk_set_icon...icon_id:%d...w:%d...h:%d", icon_id, w, h);
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif

    tk_set_icon(icon_id, w, h);
    
    #ifdef DEBUG
    sprintf(message, "...........Java_com_tigerknows_map_Ca_yu...end....");
    __android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
    #endif
}
