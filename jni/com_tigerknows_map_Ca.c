
/**
 * @author chenming
 *
 */
#include <string.h>
#include <zlib.h>
#include <jni.h>
#include <stdlib.h>

//#include "tkm_map.h"
#include "tks_suggest.h"
#include "tk_log.h"
#include "tk_map_api.h"

//#define DEBUG
#ifdef DEBUG
#include <android/log.h>
char message[1000];
#endif

jclass lost_data_cls, str_cls, label_cls, single_label_cls, multi_label_cls, xy_cls;

jmethodID str_cid_ca;
jmethodID str_cid_ba;
jmethodID lost_data_cid;
jmethodID single_label_cid, multi_label_cid, xy_cid;
/*
 * Class:     com_tigerknows_map_Ca
 * Method:    initNativeIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_cai
  (JNIEnv *env, jclass this) {
	jclass lost_data_cls_local = (*env)->FindClass(env, "com/tigerknows/map/TileDownload");
	if(lost_data_cls_local != NULL) {
		lost_data_cls = (*env)->NewGlobalRef(env, lost_data_cls_local);
//		(*env)->DeleteLocalRef(env, lost_data_cls_local);
	}
	else {

	}

	jclass str_cls_local = (*env)->FindClass(env, "java/lang/String");
	if(str_cls_local != NULL) {
		str_cls = (*env)->NewGlobalRef(env, str_cls_local);
//		(*env)->DeleteLocalRef(env, str_cls_local);
	}
	else {

	}

	str_cid_ba = (*env)->GetMethodID(env, str_cls, "<init>",
			"([BLjava/lang/String;)V");
	if(str_cid_ba == NULL) {

	}


	lost_data_cid = (*env)->GetMethodID(env, lost_data_cls, "<init>", "(IIILjava/lang/String;)V");
	if(lost_data_cid == NULL) {

	}

	str_cid_ca = (*env)->GetMethodID(env, str_cls, "<init>", "([C)V");
	if(str_cid_ca == NULL) {

	}

	jclass label_cls_local = (*env)->FindClass(env, "com/tigerknows/map/label/Label");
	if(label_cls_local != NULL) {
		label_cls = (*env)->NewGlobalRef(env, label_cls_local);
//		(*env)->DeleteLocalRef(env, label_cls_local);
	}
	else {

	}

	jclass single_label_cls_local = (*env)->FindClass(env, "com/tigerknows/map/label/SingleRectLabel");
	if(single_label_cls_local != NULL) {
		single_label_cls = (*env)->NewGlobalRef(env, single_label_cls_local);
//		(*env)->DeleteLocalRef(env, single_label_cls_local);
	}
	else {

	}

	jclass multi_label_cls_local = (*env)->FindClass(env, "com/tigerknows/map/label/MultiRectLabel");
	if(multi_label_cls_local != NULL) {
		multi_label_cls = (*env)->NewGlobalRef(env, multi_label_cls_local);
//		(*env)->DeleteLocalRef(env, multi_label_cls_local);
	}
	else {

	}

	single_label_cid = (*env)->GetMethodID(env, single_label_cls, "<init>",
				"(Ljava/lang/String;IIIIIILcom/decarta/android/util/XYInteger;III)V");
	if(single_label_cid == NULL) {

	}

	multi_label_cid = (*env)->GetMethodID(env, multi_label_cls, "<init>",
			"(Ljava/lang/String;IIIIII[Lcom/decarta/android/util/XYInteger;III)V");
	if(multi_label_cid == NULL) {

	}

	jclass xy_cls_local = (*env)->FindClass(env, "com/decarta/android/util/XYInteger");
	if(xy_cls_local != NULL) {
		xy_cls = (*env)->NewGlobalRef(env, xy_cls_local);
//		(*env)->DeleteLocalRef(env, xy_cls_local);
	}
	else {

	}

	xy_cid = (*env)->GetMethodID(env, xy_cls, "<init>", "(II)V");
	if(xy_cid == NULL) {

	}

}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    a
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_a
  (JNIEnv *env, jclass this, jstring jresdir, jstring jmapdir, jint tile_size) {
	const char *resdir = (*env)->GetStringUTFChars(env, jresdir, 0);
	const char *mapdir = (*env)->GetStringUTFChars(env, jmapdir, 0);

	int ret = tk_init_engine_config(resdir, mapdir, tile_size);

	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_a...tk_init_engine_config end....");
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

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_destroy_engine_config
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_b
  (JNIEnv *env, jclass this) {
	#ifdef DEBUG
	sprintf(message, "................Java_com_tigerknows_map_Ca_b...tk_destroy_engine_config...");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif

	tk_destroy_engine_config();

	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_b...tk_destroy_engine_config...end....");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    ba
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_ba
  (JNIEnv *env, jclass this, jstring jmapdir) {
	const char *mapdir = (*env)->GetStringUTFChars(env, jmapdir, 0);
	tk_engine_reset_map_dir(mapdir);
	(*env)->ReleaseStringUTFChars(env, jmapdir, mapdir);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    ca
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_ca
  (JNIEnv *env, jclass this, jint tile_size) {
	tk_set_global_tile_size(tile_size);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    bb
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_tigerknows_map_Ca_bb
  (JNIEnv *env, jclass this) {
	return tk_is_engine_initialized();
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_init_context
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_c
  (JNIEnv *env, jclass this, jintArray jbmpbuf, jint tile_size_bit, jint mode) {
	unsigned int *bmpbuf = NULL;
	if(jbmpbuf != NULL && mode > 0) {
		bmpbuf = (*env)->GetIntArrayElements(env, jbmpbuf, NULL);
	}
	#ifdef DEBUG
	sprintf(message, "................Java_com_tigerknows_map_Ca_tk_1init_1context...tk_init_context...");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif

	int ret = tk_init_context((unsigned char *)bmpbuf, tile_size_bit, mode);

	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_tk_1init_1context...tk_init_context...end...ret:%i.", ret);
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif

	return ret;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_fini_context
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_d
  (JNIEnv *env, jclass proxyclass) {
	#ifdef DEBUG
	sprintf(message, "................Java_com_tigerknows_map_Ca_tk_1fini_1context...tk_fini_context...");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif

	tk_fini_context();

	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_tk_1fini_1context...tk_fini_context...end...");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_matrix_size
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_e
  (JNIEnv *env, jclass this, jint tile_size) {
	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_tk_1get_1matrix_1size...tk_get_matrix_size...tile_size:%d", tile_size);
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
	int ret = tk_get_matrix_size(tile_size);
	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_tk_1get_1matrix_1size...end....ret:%d", ret);
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
	return ret;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_lost_tile_info
 * Signature: ()[Lcom/tigerknows/map/TileDownload;
 */
JNIEXPORT jobjectArray JNICALL Java_com_tigerknows_map_Ca_f
  (JNIEnv *env, jclass this) {
	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_tk_1get_1lost_1tile_1info...tk_get_lost_data");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
	jobjectArray result;
	int lost_count = 0, i = 0;
	int year;
	char version[32] = {0};
	unsigned char buf[6] = {0};
	tk_lost_data_t *lost_datas = tk_get_lost_data(&lost_count);
	// todo: move jclass to buffer
//	jclass lost_data_cls = (*env)->FindClass(env, "com/tigerknows/map/TileDownload");
//	jclass str_cls = (*env)->FindClass(env, "java/lang/String");
	if (lost_data_cls == NULL) {
		return NULL;//todo: exception thrown
	}
	result = (*env)->NewObjectArray(env, lost_count, lost_data_cls, NULL);
	if (result == NULL) {
		return NULL;//todo: out of memory error thrown
	}
	if (lost_data_cid == NULL) {
		return NULL;//todo: exception thrown
	}
	if (str_cid_ca == NULL) {
		return NULL;
	}
	for (i = 0; i < lost_count; ++i) {
		jobject lost_data;
		jstring version_obj;
		tk_get_region_version_by_id(lost_datas[i].rid, buf);
		year = (buf[2] << 8) | buf[3];
		sprintf(version, "%d.%d.%d.%d.%d", buf[0], buf[1], year, buf[4], buf[5]);
		version_obj = (*env)->NewStringUTF(env, version);
		if (version_obj == NULL) {
			return NULL;//exception
		}
		lost_data = (*env)->NewObject(env, lost_data_cls, lost_data_cid,
				lost_datas[i].rid, lost_datas[i].offset, lost_datas[i].length, version_obj);
		if (lost_data == NULL) {
			return NULL;//exception
		}
		(*env)->SetObjectArrayElement(env, result, i, lost_data);
		(*env)->DeleteLocalRef(env, lost_data);
		(*env)->DeleteLocalRef(env, version_obj);
	}
	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_tk_1get_1lost_1tile_1info...tk_get_lost_data...end");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
	return result;
}

#define   CURRENT_ENCODE   "GBK"

jstring CharTojstring(JNIEnv* env, const char* str) {
	jstring rtn = 0;
	jsize len = strlen(str);

//	jclass clsstring = (*env)->FindClass(env, "java/lang/String");

	//new   encode   string   default   "GBK"
	jstring strencode = (*env)->NewStringUTF(env, CURRENT_ENCODE);
	jbyteArray barr = (*env)->NewByteArray(env, len);

	(*env)->SetByteArrayRegion(env, barr, 0, len, (jbyte*) str);

	//call   new   String(byte[]   b,String   encode)
	rtn = (jstring) (*env)->NewObject(env, str_cls, str_cid_ba, barr, strencode);
	(*env)->DeleteLocalRef(env, strencode);
	return rtn;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_render_tile
 * Signature: (III)[Lcom/tigerknows/map/label/Label;
 */
JNIEXPORT jobjectArray JNICALL Java_com_tigerknows_map_Ca_g
  (JNIEnv *env, jclass this, jint tile_x, jint tile_y, jint zoom) {
	jobjectArray result;
	tk_label_t *labels;
	int label_num = 0, i = 0;
	int halfOfTotalNumber = 1 << (zoom - 1);
	int tk_tile_x = (halfOfTotalNumber + tile_x) % (halfOfTotalNumber << 1);
	int tk_tile_y = halfOfTotalNumber - 1 - tile_y;
	labels = tk_render_tile(tk_tile_x, tk_tile_y, zoom, &label_num);
	if (labels == NULL) {
		return NULL;
	}
	if (label_cls == NULL) {
		return NULL;
	}
	if (single_label_cls == NULL) {
		return NULL;
	}
	if (multi_label_cls == NULL) {
		return NULL;
	}
	result = (*env)->NewObjectArray(env, label_num, label_cls, NULL);
	if (result == NULL) {
		return NULL;//todo: out of memory error thrown
	}
	if(single_label_cid == NULL) {
		return NULL;
	}
	if(multi_label_cid == NULL) {
		return NULL;
	}
	if(xy_cls == NULL) {
		return NULL;
	}
	if(xy_cid == NULL) {
		return NULL;
	}

	jobject label = NULL;
	jobject point = NULL;
	int point_num = 0;
	tk_point_t *points = NULL;
	jstring name = NULL;
	jobjectArray jpoints = NULL;
	for(i = 0; i < label_num; ++i) {
		point_num = labels[i].point_num;
		points = labels[i].points;
		if (point_num == 1){
			point = (*env)->NewObject(env, xy_cls, xy_cid, points[0].x, points[0].y);
			if(point == NULL) {
				return NULL;
			}
			name = CharTojstring(env, labels[i].name);
			if(name == NULL) {
				return NULL;
			}
			label = (*env)->NewObject(env, single_label_cls, single_label_cid,
								name, labels[i].text_color, labels[i].bg_color, labels[i].icon_id,
								labels[i].font_size, labels[i].type, labels[i].priority, point, tile_x, tile_y, zoom);
			if(label == NULL) {
				return NULL;
			}
			(*env)->DeleteLocalRef(env, name);
			(*env)->DeleteLocalRef(env, point);
		}
		else if (point_num > 1) {
			jpoints = (*env)->NewObjectArray(env, point_num, xy_cls, NULL);
			if(jpoints == NULL) {
				return NULL;
			}
			name = CharTojstring(env, labels[i].name);
			if(name == NULL) {
				return NULL;
			}
			int j;
			for (j = 0; j < point_num; ++j) {
				point = (*env)->NewObject(env, xy_cls, xy_cid, points[j].x, points[j].y);
				if(point == NULL) {
					return NULL;
				}
				(*env)->SetObjectArrayElement(env, jpoints, j, point);
				(*env)->DeleteLocalRef(env, point);
			}
			label = (*env)->NewObject(env, multi_label_cls, multi_label_cid,
					name, point_num, labels[i].text_color, labels[i].bg_color,
					labels[i].font_size, labels[i].type, labels[i].priority, jpoints, tile_x, tile_y, zoom);
			if(label == NULL) {
				return NULL;
			}
			(*env)->DeleteLocalRef(env, name);
			(*env)->DeleteLocalRef(env, jpoints);
		}
		else {
			return NULL;
		}
		(*env)->SetObjectArrayElement(env, result, i, label);
		(*env)->DeleteLocalRef(env, label);
	}
	return result;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_city_id
 * Signature: (DD)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_h
  (JNIEnv *env, jclass this, jdouble lat, jdouble lon) {
	return tk_get_cid_by_pos(lon, lat);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_pix_count_of_scale
 * Signature: (DI)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_i
  (JNIEnv *env, jclass this, jdouble lat, jint zoom) {
	return tk_get_pix_count_of_scale(lat, zoom);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_meters_of_scale
 * Signature: (S)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_j
  (JNIEnv *env, jclass this, jshort zoom) {
	return tk_get_meters_of_scale(zoom);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_init_region
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_k
  (JNIEnv *env, jclass this, jstring metafile, jint rid) {
	const char *path = (*env)->GetStringUTFChars(env, metafile, NULL);
	int ret = 0;
	if(path == NULL) {
		return -1;
	}
	ret = tk_init_region_file(path, rid);
	(*env)->ReleaseStringUTFChars(env, metafile, path);
	return ret;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_write_region
 * Signature: (III[B)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_l
  (JNIEnv *env, jclass this, jint rid, jint offset, jint len, jbyteArray jbuf) {
    jbyte *buf = (*env)->GetByteArrayElements(env, jbuf, NULL);
    int ret = tk_write_to_region(rid, offset, len, buf);
    (*env)->ReleaseByteArrayElements(env, jbuf, buf, 0);
    return ret;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_region_stat
 * Signature: (I)[I
 */
JNIEXPORT jintArray JNICALL Java_com_tigerknows_map_Ca_m
  (JNIEnv *env, jclass this, jint rid) {
	jintArray iarr = (*env)->NewIntArray(env, 3);
	int ret, total_size = 0, downloaded_size = 0, lost_data_count = 0;
	jint size[3];
//	int tk_get_region_state(int rid, int *ptotal_size, int *pdownloaded_size);
	ret = tk_get_region_state(rid, &total_size, &downloaded_size);
	size[0] = total_size;
	size[1] = downloaded_size;
	tk_get_lost_data(&lost_data_count);
	size[2] = lost_data_count;
	(*env)->SetIntArrayRegion(env, iarr, 0, 3, size);
#ifdef DEBUG
//	char message[256] = {0};
sprintf(message, "...........Java_com_tigerknows_map_Ca_m...tk_get_region_stat...total_size: %d, downloaded_size: %d, lost_count: %d",
		total_size, downloaded_size, lost_data_count);
__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
#endif
	return iarr;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_region_id
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_n
  (JNIEnv *env, jclass this, jstring jname) {
	int rid;
	const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
	if(name == NULL) {
		return -1;
	}
	rid = tk_get_region_id_by_name(name);
	(*env)->ReleaseStringUTFChars(env, jname, name);
	return rid;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    get_citylist
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_tigerknows_map_Ca_o
  (JNIEnv *env, jclass this, jstring jprovname) {
//	char **tk_get_city_list(char *pname, int *num_of_cities);
	jobjectArray jcitylist;
	char **city_list;
	int num_of_cities, i;
	const char *prov_name = (*env)->GetStringUTFChars(env, jprovname, NULL);
	if(prov_name == NULL) {
		return NULL;
	}
	city_list = tk_get_city_list(prov_name, &num_of_cities);
	if(num_of_cities == 0) {
		return NULL;
	}
	(*env)->ReleaseStringUTFChars(env, jprovname, prov_name);
	if(str_cls == NULL) {
		return NULL;
	}
	jcitylist = (*env)->NewObjectArray(env, num_of_cities, str_cls, NULL);
	if(jcitylist == NULL) {
		return NULL;
	}
	for(i = 0; i < num_of_cities; ++i) {
		jstring city_name = (*env)->NewStringUTF(env, city_list[i]);
		if(city_name == NULL) {
			return NULL;
		}
		free(city_list[i]);//需释放malloc分配的内存
		(*env)->SetObjectArrayElement(env, jcitylist, i, city_name);
		(*env)->DeleteLocalRef(env, city_name);
	}
	if(city_list != NULL) {
		free(city_list);
	}
	return jcitylist;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_cityid
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_aa
  (JNIEnv *env, jclass this, jstring jname) {
//	int tk_get_cityid_by_name(const char *cname);
	int city_id;
	const char *cname = (*env)->GetStringUTFChars(env, jname, NULL);
	if(cname == NULL) {
		return -1;
	}
	city_id = tk_get_cityid_by_name(cname);
	(*env)->ReleaseStringUTFChars(env, jname, cname);
	return city_id;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_region_info
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_tigerknows_map_Ca_ab
  (JNIEnv *env, jclass this, jint rid) {
//	char *tk_get_region_info_by_id(int rid);
	char *info = tk_get_region_info_by_id(rid);
	return (*env)->NewStringUTF(env, info);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_city_info
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_tigerknows_map_Ca_ac
  (JNIEnv *env, jclass this, jint cid) {
//	char *tk_get_city_info_by_id(int city_id);
	char *info = tk_get_city_info_by_id(cid);
	return (*env)->NewStringUTF(env, info);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_regionid_list
 * Signature: (I)[I
 */
JNIEXPORT jintArray JNICALL Java_com_tigerknows_map_Ca_ad
  (JNIEnv *env, jclass this, jint cid) {
//	int *tk_get_regionid_list_by_cityid(int cityid, int *region_num);
	int *region_id_list;
	int region_num;
	jintArray result;
	region_id_list = tk_get_regionid_list_by_cityid(cid, &region_num);
	result = (*env)->NewIntArray(env, region_num);
	(*env)->SetIntArrayRegion(env, result, 0, region_num, region_id_list);
	free(region_id_list);
	return result;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_eprovincelist
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_tigerknows_map_Ca_ae
  (JNIEnv *env, jclass this) {
//	char *tk_get_province_list_ename()
	jstring result;
	char *name_list = tk_get_province_list_ename();
	if(name_list == NULL)
		return NULL;
	result = (*env)->NewStringUTF(env, name_list);
	free(name_list);
	return result;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_remove_city_data
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_af
  (JNIEnv *env, jclass this, jstring jcityname) {
	const char *city_name = (*env)->GetStringUTFChars(env, jcityname, NULL);
	if(city_name == NULL) {
		return ;//todo: exception
	}
	tk_remove_city_data_by_name(city_name);
	(*env)->ReleaseStringUTFChars(env, jcityname, city_name);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_region_version
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ag
  (JNIEnv *env, jclass this, jint rid) {
//	int tk_get_region_version_by_id(int rid, unsigned char *rversion);
	jbyteArray versionByte;
    unsigned char versions[6] = {0};
    int ret = tk_get_region_version_by_id(rid, versions);
    if (ret != 0)
    	return NULL;
    versionByte = (*env)->NewByteArray(env, 6);
    if(versionByte == NULL) {
    	return NULL;
    }
    (*env)->SetByteArrayRegion(env, versionByte, 0, 6, versions);
    return versionByte;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_remove_region_data
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_ah
  (JNIEnv *env, jclass this, jint rid) {
	tk_remove_region_data_by_id(rid);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_latlon_transform
 * Signature: (DD)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_ai
  (JNIEnv *env, jclass this, jdouble lat, jdouble lon) {
//	void tk_gps_latlon_transform(double lon, double lat, int wg_heit, int wg_week, unsigned int wg_time, double* out_lon, double* out_lat);
	double position[2];
	tk_gps_latlon_transform(lon, lat, 0, 0, 0, position, position + 1);
	jbyteArray barray = (*env)->NewByteArray(env, sizeof(position));
	(*env)->SetByteArrayRegion(env, barray, 0, sizeof(position), (jbyte *)position);
	return barray;
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    tk_get_poi_name
 * Signature: (DDI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_tigerknows_map_Ca_aj
  (JNIEnv *env, jclass this, jdouble lat, jdouble lon, jint zoom) {
	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_aj...tk_get_poi_name");
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
//	const char *tk_find_nearest_poi_by_latlon(double lon, double lat, int zoom);
	const char *poi_name = tk_find_nearest_poi_by_latlon(lon, lat, zoom);
	if(poi_name == NULL)
		return NULL;
	#ifdef DEBUG
	sprintf(message, "...........Java_com_tigerknows_map_Ca_aj...end...tk_get_poi_name: %s", poi_name);
	__android_log_write(ANDROID_LOG_ERROR, "TKEngine-stat", message);
	#endif
	return CharTojstring(env, poi_name);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    ak
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_ak
  (JNIEnv *env, jclass this, jint rid) {
	return tk_get_cid_by_rid(rid);
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    al
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_al
  (JNIEnv *env, jclass this) {
	return tk_get_max_label_priority();
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    am
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_am
  (JNIEnv *env, jclass this) {
	return tk_clean_cache();
}

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    an
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_an
  (JNIEnv *env, jclass this) {
	return tk_clean_label_mem();
}


/*
 * Class:     com_tigerknows_map_Ca
 * Method:    p
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_p
  (JNIEnv *env, jclass this, jint citycode, jstring jfilepath) {
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

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    q
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_q
  (JNIEnv *env, jobject thiz, jstring jfilepath, jstring jfilepathcommon, jint citycode) {
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

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    r
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_r
  (JNIEnv *env, jobject thiz) {
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

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    s
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_tigerknows_map_Ca_s
  (JNIEnv *env, jobject thiz, jbyteArray jsearchword, jint type) {
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

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    t
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tigerknows_map_Ca_t
  (JNIEnv *env, jobject thiz) {
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

/*
 * Class:     com_tigerknows_map_Ca
 * Method:    u
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_tigerknows_map_Ca_u
  (JNIEnv *env, jobject thiz, jstring jpath, jint citycode) {
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

