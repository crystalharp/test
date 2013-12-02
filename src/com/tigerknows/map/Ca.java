package com.tigerknows.map;

import com.tigerknows.android.app.TKApplication;
import com.tigerknows.map.label.Label;

public class Ca {
    static {
        //System.load("tkengine");
        System.load(TKApplication.getInstance().getFilesDir().getParent() + "/lib/libtkengine.so");
        cai();
    }

    private static native void cai();
    
    /**初始化/销毁引擎配置、初始化/销毁context、计算bmpBuf大小*/
    private static native int a(String resdir, String mapdir, int tile_size);
    protected static int tk_init_engine_config(String resdir, String mapdir, int tile_size) {
    	return a(resdir, mapdir, tile_size);
    }
    
    private static native void b();
    protected static void tk_destroy_engine_config() {
    	b();
    }
    
    private static native void ba(String mapdir);
    
    protected static void tk_engine_reset_map_dir(String mapdir) {
    	ba(mapdir);
    }
    
    private static native boolean bb();
    
    protected static boolean tk_is_engine_initialized() {
    	return bb();
    }
    
    private static native void ca(int tile_size);
    protected static void tk_set_global_tile_size(int tile_size) {
    	ca(tile_size);
    }
    
    private static native int c(int[] img_buf, int tile_size_bit, int mod);
    public static int tk_init_context(int[] img_buf, int tile_size_bit, int mod) {
    	return c(img_buf, tile_size_bit, mod);
    }
    
    private static native void d();
    public static void tk_fini_context() {
    	d();
    }
    
    private static native int e(int tile_size);
    public static int tk_get_matrix_size(int tile_size) {
    	return e(tile_size);
    }

    /**获取当前屏幕图片及缺失的数据*/
    private static native TileDownload[] f();
    public static TileDownload[] tk_get_lost_tile_info() {
    	return f();
    }
    
    /**渲染当前TILE图片并获取文字*/
    private static native Label[] g(int x, int y, int z);
    public static Label[] tk_render_tile(int x, int y, int z) {
    	return g(x, y, z);
    }

    /**根据经纬度计算cityId、获得当前屏幕中心点cityId*/
    private static native int h(double lat, double lon);
    protected static int tk_get_city_id(double lat, double lon) {
    	return h(lat, lon);
    }

    /**计算当前纬度、缩放级别的比例尺需要用几个像素表示,例如200km需要20个象素*/
    private static native int i(double lat, int zoom);
    protected static int tk_get_pix_count_of_scale(double lat, int zoom) {
    	return i(lat, zoom);
    }
    /**得到zoom对应的比例（以m为单位）*/
    private static native int j(short z);
    protected static int tk_get_meters_of_scale(short z) {
    	return j(z);
    }
    
    /**生成.dat .chk文件、填充缺失数据*/
    private static native int k(String path, int rid);
    protected static int tk_init_region(String path, int rid) {
    	return k(path, rid);
    }
    
    private static native int l(int rid, int offset, int len, byte[] buf);
    protected static int tk_write_region(int rid, int offset, int len, byte[] buf) {
    	return l(rid, offset, len, buf);
    }
    
    private static native int[] m(int rid);
    protected static int[] tk_get_region_stat(int rid) {
    	return m(rid);
    }
    
    /**元数据信息：省市列表等*/
    private static native int n(String regionName);
    protected static int tk_get_region_id(String regionName) {
    	return n(regionName);
    }
    
    private static native String[] o(String provinceName);
    protected static String[] get_citylist(String provinceName) {
    	return o(provinceName);
    }
    
    private static native int aa(String cityName);
    protected static int tk_get_cityid(String cityName) {
    	return aa(cityName);
    }
    //char *tk_get_region_info(int rid); 例如："中文名字, English name, file size, city id". 
    
    private static native String ab(int regionId);
    protected static String tk_get_region_info(int regionId) {
    	return ab(regionId);
    }
    // "中文名字, English name, latitude, longitude, level, 省中文名字, 省英文名字" 
    //例如："北京,beijing,39.90415599,116.397772995,11, 北京,beijing"
    //char *tk_get_city_info(int cityid);
    
    private static native String ac(int cityId);
    protected static String tk_get_city_info(int cityId) {
    	return ac(cityId);
    }
    
    private static native int[] ad(int cityId);
    protected static int[] tk_get_regionid_list(int cityId) {
    	return ad(cityId);
    }
    
    private static native String ae();
    protected static String tk_get_eprovincelist() {
    	return ae();
    }
    
    private static native void af(String cityName);
    protected static void tk_remove_city_data(String cityName) {
    	af(cityName);
    }
    
    private static native byte[] ag(int regionId);
    protected static byte[] tk_get_region_version(int regionId) {
    	return ag(regionId);
    }
    
    private static native void ah(int regionId);
    protected static void tk_remove_region_data(int regionId) {
    	ah(regionId);
    }
    
    
    protected static native byte[] ai(double lat, double lon);
    protected static byte[] tk_latlon_transform(double lat, double lon) {
    	return ai(lat, lon);
    }
    
    //获取离屏幕上某坐标点（x,y)最近的POI名称；
    private static native String aj(double lat, double lon, int zoom);
    protected static String tk_get_poi_name(double lat, double lon, int zoom) {
    	return aj(lat, lon, zoom);
    }
    
    private static native int ak(int rid);
    
    protected static int tk_get_cid_by_rid(int rid) {
    	return ak(rid);
    }

    private static native int al();
    protected static int tk_get_max_label_priority() {
    	return al();
    }
    private static native void am();
    protected static void tk_clean_engine_cache() {
    	am();
    }
    
    private static native void an();
    protected static void tk_clean_engine_label() {
    	an();
    }
    
    /**联系词解压、销毁、获取联想词、联想词版本*/
    private static native int p(int citycode, String filepath);
    protected static int tk_decompress(int citycode, String filepath) { return p(citycode, filepath); }
    private static native int q(String filepath, String filepathcommon, int citycode);
    protected static int tk_suggestword_init(String filepath, String filepathcommon, int citycode) { return q(filepath, filepathcommon, citycode); }
    private static native int r();
    protected static int get_SWCOUNT() { return r(); }
    private static native byte[] s(byte[] searchword, int type);
    protected static byte[] tk_getwordslist(byte[] searchword, int type) { return s(searchword, type); }
    private static native void t();
    protected static void tk_suggestword_destroy() { t(); }
    private static native int u(String path, int cityCode);
    protected static int tk_getrevision(String path, int cityCode) { return u(path, cityCode); }
}
