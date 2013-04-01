/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.decarta.CONFIG;
import com.decarta.Profile;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.map.TilesView.TileResponse;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.service.SuggestLexiconService;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.SuggestWord;

public class MapEngine {

    public static boolean BMP2PNG = false;
    
    /**
     * 删除或更新地图数据时，通知MapView清除缓存的Tile信息
     */
    public static final String ACTION_REMOVE_CITY_MAP_DATA = "action.com.tigerknows.remove.city.map.data";
    
    /**
     * 城市Id
     */
    public static final String EXTRA_CITY_ID = "extra_city_id";
    
    public static int CITY_ID_INVALID = -1;
    public static int CITY_ID_QUANGUO = -3;
    public static int CITY_ID_BEIJING = 1;
    public static int SW_ID_QUANGUO = 9999;
    
    private int matrixSize;
    private byte[] bitmapBuffer;
    private byte[] pngBuffer;
    private boolean isClosed = true;
    
    private int suggestCityId = CITY_ID_INVALID;  // 联想词引擎所属城市Id
    private List<Integer> suggestDownloaded = new ArrayList<Integer>(); // 记录被下载更新过的城市Id
    
    private String mapPath = null;
    private boolean isExternalStorage = false;
    public boolean isExternalStorage() {
        return isExternalStorage;
    }
    
    public static final int MAX_REGION_TOTAL_IN_ROM = 30;
    private int lastCityId;
    private List<Integer> lastRegionIdList = new ArrayList<Integer>();
    private Context context;
    
    private void readLastRegionIdList(Context context) {
        synchronized (this) {
        if (isExternalStorage || isClosed) {
            return;
        }
        lastCityId = CITY_ID_INVALID;
        lastRegionIdList.clear();
        String str = TKConfig.getPref(context, TKConfig.PREFS_LAST_REGION_ID_LIST, null);
        if (str == null || TextUtils.isEmpty(str)) {
            return;
        }
        String strArr[] = str.split(",");

        for(int i = 0, length = strArr.length; i < length; i++) {
            try {
                int rid = Integer.parseInt(strArr[i]);
                addLastRegionId(rid, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        }
    }
    
    public void writeLastRegionIdList(Context context) {
        synchronized (this) {
        if (isExternalStorage || isClosed) {
            return;
        }
        StringBuilder s = new StringBuilder();
        for(int i = 0, length = lastRegionIdList.size(); i < length; i++) {
            if (i > 0) {
                s.append(",");
            }
            s.append(lastRegionIdList.get(i));
        }
        TKConfig.setPref(context, TKConfig.PREFS_LAST_REGION_ID_LIST, s.toString());
        }
    }
    
    public void setLastCityId(int cityId) {
        synchronized (this) {
        if (isExternalStorage || isClosed) {
            return;
        }
        if (cityId == CITY_ID_INVALID || cityId == CITY_ID_QUANGUO || lastCityId == cityId) {
            return;
        }
        lastCityId = cityId;
        String[] regionIdStrArr = getRegionlist(lastCityId);
        if (regionIdStrArr != null && regionIdStrArr.length > 0) {
            for(int i = 0, length = regionIdStrArr.length; i < length; i++) {
                try {
                    addLastRegionId(getRegionId(regionIdStrArr[i]), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        }
    }
    
    public void addLastRegionId(int rid, boolean isBackground) {
        synchronized (this) {
        if (isExternalStorage || isClosed) {
            return;
        }
        if (rid == CITY_ID_INVALID || rid == CITY_ID_QUANGUO) {
            return;
        }
        Integer regionId = new Integer(rid);
        if (isBackground == false) {
            if (lastRegionIdList.contains(regionId)) {
                lastRegionIdList.remove(regionId);
            }
            lastRegionIdList.add(regionId);
            if (lastRegionIdList.size() > MAX_REGION_TOTAL_IN_ROM) {
                removeRegion(lastRegionIdList.remove(0));
            }
        } else {
            if (lastRegionIdList.contains(regionId) == false) {
                if (lastRegionIdList.size() >= MAX_REGION_TOTAL_IN_ROM) {
                    removeRegion(lastRegionIdList.remove(0));
                }
                lastRegionIdList.add(0, regionId);
            }
        }
        }
    }
    
    public void removeLastRegionId(int rid) {
        synchronized (this) {
        if (isExternalStorage || isClosed) {
            return;
        }
        if (rid == CITY_ID_INVALID || rid == CITY_ID_QUANGUO) {
            return;
        }
        Integer regionId = new Integer(rid);
        if (lastRegionIdList.contains(regionId)) {
            lastRegionIdList.remove(regionId);
        }
        }
    }
    
    private void initEngine(Context context, String mapPath) {
        synchronized (this) {
        if (this.isClosed) {
        if (bitmapBuffer == null) {
        matrixSize = Ca.tk_get_matrix_size(CONFIG.TILE_SIZE, CONFIG.TILE_SIZE);
        bitmapBuffer = new byte[matrixSize];
        if (BMP2PNG) {
            pngBuffer = new byte[matrixSize];
        }
        }
        isExternalStorage = false;
        this.mapPath = mapPath;
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            String externalStoragePath = externalStorageDirectory.getAbsolutePath();
            if (this.mapPath.startsWith(externalStoragePath)) {
                isExternalStorage = true;
            }
        }
        readLastRegionIdList(context);
        int status = Ca.tk_init_engine(TKConfig.getDataPath(false), this.mapPath, CONFIG.TILE_SIZE, CONFIG.TILE_SIZE, bitmapBuffer, 1);
        if (status == 0) {
            int icon_num = MapWord.Icon.RESOURCE_ID.length;
            Ca.tk_init_icon_num(icon_num);
            for(int i=0; i < icon_num; i++) {
                Bitmap bm = MapWord.Icon.getBitmap(i);
                if (bm != null) {
                    Ca.tk_set_icon(i, bm.getWidth(), bm.getHeight());
                }
            }
            this.isClosed = false;
            this.context = context;
        } else {
            destroyEngine();
        }
        LogWrapper.d(TAG, "initEngine() status="+status);
        }
        }
    }

    public void destroyEngine() {
        synchronized (this) {
        if (this.isClosed == false) {
        suggestDownloaded.clear();
        suggestwordDestroy();
        Ca.tk_destroy_engine();
        isClosed = true;
        mapPath = null;
        LogWrapper.d(TAG, "destroyEngine()");
        }
        }
    }

    public int getCityId(Position position) {
        synchronized (this) {
        int cityId = CITY_ID_INVALID;
        if (isClosed || position == null) {
            return cityId;
        }
        cityId = Ca.tk_get_city_id(position.getLat(), position.getLon());
        return cityId;
        }
    }

    public int getRegionId(Position position) {
        synchronized (this) {
        int rid = CITY_ID_INVALID;
        if (isClosed || position == null) {
            return rid;
        }
        rid = Ca.tk_get_rid_by_point(position.getLat(), position.getLon());
        return rid;
        }
    }

    /**
     * 解压联想词文件
     * @param cityId
     * @param filepath
     * @return
     */
    public int decompress(int cityId, String filepath) {
        synchronized (this) {
        if (isClosed) {
            return -1;
        }
        return Ca.tk_decompress(cityId, filepath);
        }
    }
    
    //根据城市联想词filepath和全国公共联想词filepath_common进行加载，建立索引，城市id改变时调用
    public void suggestwordInit(int cityId) {
        synchronized (this) {
        if (isClosed) {
            return;
        }
        suggestwordDestroy();
        if (cityId <= 0) {
            return;
        }
        if (cityId == SW_ID_QUANGUO || cityId == CITY_ID_QUANGUO) {
            return;
        } 
        if (hasCorrectSW(cityId)) {
            String cityFolderPath = cityId2Floder(cityId);
            //城市切换先销毁前一个城市的联想词引擎
            if (TextUtils.isEmpty(cityFolderPath)) {
                return;
            }
            // 初始化引擎
            if(Ca.tk_suggestword_init(cityFolderPath, cityFolderPath, cityId) == 1) {
                suggestCityId = cityId;
            }
        }
        }
    }
    
    private byte[] getwordslist(String searchword, int type) {
        synchronized (this) {
        byte[] data = null;
        if (isClosed || suggestCityId == CITY_ID_INVALID) {
            return data;
        }
        try {
            byte[] byteSearchWord = searchword.getBytes("GBK");
            byte[] fullByteSearchWord = new byte[searchword.length()*2 + 2];
            for (int i=0; i<byteSearchWord.length; i++) {
                fullByteSearchWord[i] = byteSearchWord[i];
            }
            //解决输入两个汉字时jni中c中数组后数据没有清0的问题
            fullByteSearchWord[searchword.length()*2] = 0;
            fullByteSearchWord[searchword.length()*2 + 1] = 0;
            data = Ca.tk_getwordslist(fullByteSearchWord, type);
        } catch (Exception e) {
        }
        return data;
        }
    }
    
    public void suggestwordDestroy() {
        synchronized (this) {
        if (isClosed) {
            return;
        }
        if (suggestCityId != CITY_ID_INVALID) {
            suggestCityId = CITY_ID_INVALID;
            Ca.tk_suggestword_destroy();
        }
        }
    }

    //返回当前城市的联想词版本
    public int getrevision(int cityId) {
        synchronized (this) {
        if (isClosed || suggestCityId == CITY_ID_INVALID) {
            return -1;
        }
        String path = cityId2Floder(cityId);
        if (TextUtils.isEmpty(path)) {
            return -1;
        }
        return Ca.tk_getrevision(path,cityId);
        }
    }

    public void removeRegion(int rid) {
        synchronized (this) {
        if (isClosed) {
            return;
        }
        int cityId = MapEngine.CITY_ID_INVALID;
        RegionInfo regionInfo = getRegionInfo(rid);
        if (regionInfo != null) {
            cityId = getCityid(regionInfo.getCCityName());
        }
        Ca.tk_remove_region_data(rid); 
        sendBroadcastForRemoveMapData(cityId);
        }
    }
    
    void sendBroadcastForRemoveMapData(int cityId) {
        synchronized (this) {
        if (isClosed) {
            return;
        }
        if (context != null) {
            Intent intent = new Intent(ACTION_REMOVE_CITY_MAP_DATA);
            intent.putExtra(EXTRA_CITY_ID, cityId);
            context.sendBroadcast(intent);
        }
        }
    }

    public boolean initRegion(int rid) {
        synchronized (this) {
        if (isClosed) {
            return false;
        }

        String path = mapPath + String.format(TKConfig.MAP_REGION_METAFILE, rid);
        int ret = Ca.tk_init_region(path, rid);
        
        File metaFile = new File(path);
        if (metaFile.exists()) {
            metaFile.delete();
        }
        addLastRegionId(rid, false);
        return ret == 0 ? true : false;
        }
    }
    
    public int writeRegion(int rid, int offset, byte[] buf, String version) {
        synchronized (this) {
        if (isClosed) {
            return -1;
        }
        if (buf == null || TextUtils.isEmpty(version)) {
            return -1;
        }
        RegionMetaVersion regionMetaVersion = getRegionMetaVersion(rid);
        if (regionMetaVersion != null) {
            if (version.equals(regionMetaVersion.toString()) == false) {
                return -1;
            }
        } else {
            return -1;
        }
        return Ca.tk_write_region(rid, offset, buf.length, buf);
        }
    }
    
    public LocalRegionDataInfo getLocalRegionDataInfo(int regionId) {
        synchronized (this) {
        if (isClosed) {
            return null;
        }
        byte[] data = Ca.tk_get_region_stat(regionId);
        if (data == null || data.length < 1) {
            return null;
        }
        return ByteUtil.parseRegionMapInfo(data);
        }
    }
    
    public String getMapPath() {
        return mapPath;
    }

    public int getRegionId(String regionName) {
        synchronized (this) {
        if (isClosed) {
            return -1;
        }
        return Ca.tk_get_region_id(regionName);
        }
    }

    public List<String> getCitylist(String provinceName) {
        synchronized (this) {
        ArrayList<String> cityList = new ArrayList<String>();
        if (isClosed) {
            return cityList;
        }
        if (TextUtils.isEmpty(provinceName)) {
            return cityList;
        }
        byte[] cityCNamesBytes = Ca.get_citylist(provinceName.trim());
        if (cityCNamesBytes == null) {
            return cityList;
        }
        String[] cities = new String(cityCNamesBytes).split(",");
        for (String city : cities) {
            cityList.add(city);
        }
        return cityList;
        }
    }
    
    public int getCityid(String cityName) {
        synchronized (this) {
        if (isClosed) {
            return -1;
        }
        if (TextUtils.isEmpty(cityName)) {
            return -1;
        }
        return Ca.tk_get_cityid(cityName.trim());
        }
    }
    
    public RegionInfo getRegionInfo(int regionId) {

        synchronized (this) {
        if (isClosed) {
            return null;
        }
        byte[] data = Ca.tk_get_region_info(regionId);
        if (data == null || data.length < 1) {
            return null;
        }
        RegionInfo regionInfo = new RegionInfo();
        String[] strs = new String(data, 0, data.length).split(",");
        regionInfo.setCName(strs[0]);
        regionInfo.setEName(strs[1]);
        regionInfo.setFileSize(Integer.parseInt(strs[2]));
        regionInfo.setCCityName(strs[3]);
//        regionInfo.setCityId(Integer.parseInt(strs[3]));
        return regionInfo;
        }
    }

    public CityInfo getCityInfo(int cityId) {
        synchronized (this) {
        CityInfo cityInfo = new CityInfo();
        if (isClosed) {
            return cityInfo;
        }
        byte[] data = Ca.tk_get_city_info(cityId);
        if (data == null || data.length < 1) {
            return cityInfo;
        }
        String[] strs = new String(data, 0, data.length).split(",");
        cityInfo.setId(cityId);
        cityInfo.setCName(strs[0]);
        cityInfo.setEName(strs[1]);
        cityInfo.setPosition(new Position(Double.parseDouble(strs[2]), Double.parseDouble(strs[3])));
        cityInfo.setLevel(Integer.parseInt(strs[4]));
        cityInfo.setCProvinceName(strs[5]);
        cityInfo.setEProvinceName(strs[6]);
        return cityInfo;
        }
    }
    
    public String[] getRegionlist(int cityId) {
        synchronized (this) {
        if (isClosed) {
            return new String[0];
        }
        byte[] data = Ca.tk_get_regionlist(cityId);
        if (data == null || data.length < 1) {
            return new String[0];
        }
        return new String(data).split(",");
        }
    }

    // 得到所有省的中英文名字，去掉香港、澳门
    public HashMap<String, String> getEprovincelist(Context context) {
        synchronized (this) {
        HashMap<String, String> infoMap = new HashMap<String, String>();
        if (isClosed) {
            return infoMap;
        }
        byte[] data = Ca.tk_get_eprovincelist();
        if (data == null || data.length < 1) {
            return infoMap;
        }
        String allProvincesInfoStr = new String(data);
        String[] provices = allProvincesInfoStr.split(",");
        String hongkong = context.getString(R.string.hongkong);
        String macao = context.getString(R.string.macao);
        for (int i=0; i<provices.length; i++) {
            if (!provices[i].equals(hongkong) && 
                    !provices[i].equals(macao)) {
                infoMap.put(provices[i], provices[++i]);
            } else {
                ++i;
            }
        }
        return infoMap;
        }
    }
    
    public void removeCityData(String cityName) {
        synchronized (this) {
        if (isClosed) {
            return;
        }
        if (TextUtils.isEmpty(cityName)) {
            return;
        }
        Ca.tk_remove_city_data(cityName.trim());
        sendBroadcastForRemoveMapData(getCityid(cityName));
        }
    }

    public RegionMetaVersion getRegionMetaVersion(int regionId) {
        synchronized (this) {
        if (isClosed) {
            return null;
        }
        byte[] data = Ca.tk_get_region_version(regionId);
        if (data == null || data.length < 1) {
            return null;
        }
        RegionMetaVersion ridDataVersion = new RegionMetaVersion(data);
        return ridDataVersion;
        }
    }
    
    /**
     * 坐标转换
     * 这个是遵守国家规定，所有定位的坐标都要经过此转换后，在地图上显示才是正确位置
     * @param position
     * @return
     */
    public Position latlonTransform(Position position) {
        synchronized (this) {
        if (isClosed) {
            return null;
        }
        if (position == null) {
           return null;
        } 
        byte[] data = Ca.tk_latlon_transform(position.getLat(), position.getLon());
        if (data == null || data.length < 1) {
           return null;
        } 
        return new Position(ByteUtil.arr2double(data, 8), ByteUtil.arr2double(data, 0));
        }
    }
    
    /**
     * 地图页获取点击位置
     * @param position
     * @param zoomLevel
     * @return
     */
    public String getPositionName(Position position, int zoomLevel) {
        String poiName = "";
        if (isClosed) {
            return poiName;
        }
        if (position == null) {
        	return poiName;
        }
        poiName = getPOIName(position, zoomLevel, 1);
        if (!TextUtils.isEmpty(poiName)) {
            poiName = poiName.substring(1);
        }
        return poiName;
    }
    
    /**
     * 搜索首页获取当前位置
     * @param position
     * @return
     */
    public String getPositionName(Position position) {
    	String poiName = "";
        if (isClosed) {
            return poiName;
        }
        if (position == null) {
        	return poiName;
        }
        
        float accurary = position.getAccuracy();
        int flag = (accurary > 500 && accurary != 0) ? 0 : 1;
        
        poiName = getPOIName(position, 16, flag);
        if ("G".equals(poiName)) {
            int cityId = getCityId(position);
            CityInfo cityInfo = getCityInfo(cityId);
            if (cityInfo != null) {
            	poiName = "U" + cityInfo.getCName();
            }
        }
        
        return poiName;
    }
    
    /**
     * @param position POI经纬度
     * @param zoomLevel 期望获取该经纬度名字的缩放级别
     * @param flag 1表示获取点的名字, 0表示获取线的名字
     * @return
     */
    private String getPOIName(Position position, int zoomLevel, int flag) {
    	synchronized (this) {
            String poiName = null;
            if (isClosed) {
                return poiName;
            }
            if (position == null) {
                return poiName;
            }
            byte[] data = null;	

            data = Ca.tk_get_poi_namel(position.getLat(), position.getLon(), zoomLevel, flag);
            if (data == null || data.length < 1) {
                return poiName;
            }
            try {
                poiName = new String(data, 0, data.length, "GBK");
                if (poiName.equals("null")) {
                    poiName = null;
                }
                String encode = poiName.substring(0, 1);
                if (encode.equals("G")) {
                    poiName = "G"+poiName.substring(1);
                } else if (encode.equals("U")){
                    poiName = new String(data, 0, data.length, "UTF-8");
                    poiName = "U"+poiName.substring(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return poiName;
        }
    }
    
    public void resetFontSize(float offset) {
        synchronized (this) {
        Ca.tk_reset_font_size(offset);
        }
    }
    
    public void resetIconSize(int offset) {
        synchronized (this) {
        Ca.tk_reset_icon_size(offset);
        }
    }
    
    public int bmp2Png(byte[] bmpBuf, byte[] pngBuf) {
        synchronized (this) {
        return Ca.bmp2Png(bmpBuf, pngBuf);
        }
    }

    private static String TAG = "MapEngine";

    private static MapEngine instance;
    
    /**
     * This method can be used only after "getInstance(Context context, MapView mv, ImageView ivMap, int width, int height)"
     * was invoked.
     * 
     * ===This kind of restriction is bad, Need to deal with !!!===
     * 
     * @return
     */
    public static MapEngine getInstance() {
        
        if(instance == null) {
            instance = new MapEngine();
        }
        return instance;
    }

    /**
     * 设置地图引擎数据文件夹路径，仅在初始化或扩展存储卡插拔时才设置并重启地图引擎
     */
    public void initMapDataPath(Context context) throws APIException{
        String appPath = TKConfig.getDataPath(true);
        if (!appPath.equals(mapPath)) {
            try {
                File appFile = new File(appPath);
                if (!appFile.exists() || !appFile.isDirectory()) {
                    appFile.mkdirs();
                }
                new File(appPath, "try.txt").createNewFile();
                new File(appPath, "try.txt").delete();
                destroyEngine();
                initEngine(context, appPath);
                LogWrapper.i(TAG, "setupDataPath() app path:"+ appPath + " map path:"+ mapPath + ",exist:" + new File(appPath).exists());
            } catch (Exception e) {
                isClosed = true;
                mapPath = null;
                throw new APIException("Can't write/read cache. Please Grand permission");
            }
            
        }
    }
    
    /**
     * 解压地图引擎资源文件及部分地图数据文件，通过保存版本名称来确保不出现重复解压的情况
     * @param context
     * @throws Exception
     */
    public void setup(Context context) throws Exception {
        String versionName = TKConfig.getPref(context, TKConfig.PREFS_VERSION_NAME, null);
        if (TextUtils.isEmpty(versionName)   // 第一次安装后使用
                || !TKConfig.getClientSoftVersion().equals(versionName)) {   // 更新安装后使用
            String mapPath = TKConfig.getDataPath(true);
            AssetManager am = context.getAssets();
            
            CommonUtils.deleteAllFile(TKConfig.getDataPath(false));
            
            CommonUtils.unZipFile(am, "tigerres.zip", TKConfig.getDataPath(false));
            CommonUtils.unZipFile(am, "tigermap.zip", mapPath);
            
            TKConfig.setPref(context, TKConfig.PREFS_VERSION_NAME, TKConfig.getClientSoftVersion());
        }
    }
    
    public MapWord[] getScreenLabel(Position position, int width, int height, int zoomLevel) {
        synchronized (this) {
            LogWrapper.d(TAG, "getScreenLabel() position="+position+", zoomLevel="+zoomLevel);
            MapWord[] mapWords = null;
            int ret = Ca.tk_get_screen_label(position.getLon(), position.getLat(), width, height, zoomLevel);
            if (ret == 0) {
                byte[] data = Ca.get_map_text_info();
                mapWords = ByteUtil.parseMapText(data);
            }
            return mapWords;
        }
    }

    public TileResponse getTileBuffer(int x, int y, int z) {
        synchronized (this) {
            TileResponse tileResponse = new TileResponse(null, 0);
            if (isClosed) {
                return tileResponse;
            }
            try {
                long start = System.nanoTime();
                int ret = Ca.tk_get_tile_buffer(x, y, z);
                long loadTime=System.nanoTime()-start;    
                Profile.getTileBufferInc(loadTime);
                byte[] data;
                if (ret == 0) {
                    start=System.nanoTime();
                    Bitmap bm;
                    if (BMP2PNG) {
                        int size = bmp2Png(bitmapBuffer, pngBuffer);
                        bm= BitmapFactory.decodeByteArray(pngBuffer, 0, size);
                    } else {
                        bm= BitmapFactory.decodeByteArray(bitmapBuffer, 0, matrixSize);
                    }
                    loadTime=System.nanoTime()-start;    
                    Profile.decodeByteArrayInc(loadTime);

                    tileResponse.bitmap = bm;
                } else {
                    if (ret < 0) {
                        data = Ca.tk_get_lost_tile_info();
                        if (data != null) {
                            tileResponse.lostTileInfos = ByteUtil.parseTileInfo(data);
                        }
                    }
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                LogWrapper.e(TAG, "getTileBuffer() outOfMemoryError,heap size:"+android.os.Debug.getNativeHeapAllocatedSize());
            }
            return tileResponse;
        }
    }

    public List<CityInfo> getAllProvinceCityList(Context context) {
        synchronized (this) {
        List<CityInfo> allCityInfoList = new ArrayList<CityInfo>();
        HashMap<String,String> allProvincesInfo = getEprovincelist(context);
        Iterator<String> iterator = allProvincesInfo.keySet().iterator();
        while(iterator.hasNext()) {
            String provinceCName = iterator.next();
            String provinceEName = allProvincesInfo.get(provinceCName);
            
            CityInfo province = new CityInfo();
            province.setCName(provinceCName);
            province.setEName(provinceEName);
            province.setType(CityInfo.TYPE_PROVINCE);
            
            List<String> cityNameList = getCitylist(provinceCName);
            for (int i = cityNameList.size()-1; i >= 0; i--) {
                String cityName = cityNameList.get(i);
                CityInfo cityInfo = getCityInfo(getCityid(cityName));
                if (i == cityNameList.size()-1) {
                    province.setId(cityInfo.getId());
                }
                province.getCityList().add(cityInfo);
            }
            allCityInfoList.add(province);
        }
        
        return allCityInfoList;
        }
    }

    //得到一个城市的所有region id
    public ArrayList<Integer> getRegionIdList(String cityName) {
        synchronized (this) {
        int cityId = getCityid(cityName);
        ArrayList<Integer> regionIdList = new ArrayList<Integer>();
        String[] regionList = getRegionlist(cityId);
        for (String regionName : regionList) {
            int regionId = getRegionId(regionName);
            regionIdList.add(regionId);
        }
        return regionIdList;
        }
    }

    public void suggestwordCheck(Sphinx sphinx, int cityId) {
        if (cityId < CITY_ID_BEIJING) {
            return;
        }
        // 在新线程中停止下载的联想词，开始下载当前城市的联想词
        if (!suggestDownloaded.contains(cityId)) {
            suggestDownloaded.add(cityId);
            suggestDownloadStart(sphinx, cityId);
        }
        // 联想词引擎非当前城市，初始化当前城市引擎
        if (suggestCityId != cityId) {
            suggestwordInit(cityId);
        }
    }
    
    //在新线程中下载一个城市的联想词。
    private void suggestDownloadStart(final Sphinx sphinx, final int cityId) {
        //延迟下载城市联想词
        sphinx.getHandler().postDelayed(new Runnable() {
                
                @Override
                public void run() {
                    
                    if (!sphinx.isFinishing()) {
                        Intent service = new Intent(sphinx, SuggestLexiconService.class);
                        service.putExtra(SuggestLexiconService.CITY_ID, cityId);
                        sphinx.startService(service);
                    }
                    
                }
            }, 1*30*1000);
    }
    
    public void suggestDownloadEnd(int cityId) {
        synchronized (this) {
            if (cityId == suggestCityId || suggestCityId == CITY_ID_INVALID) {
                suggestwordInit(cityId);
            }
        }
    }
    
    //根据用户输入的词searchword及type进行联想，返回前x()个联想词，联想词在源文件TKSuggestWords.c全局变量Suggest_Word suggestwords[10]中；type为poi或者位置--对应不同搜索框
    public List<String> getwordslistString(String searchword, int type) {
        if (isClosed || suggestCityId == CITY_ID_INVALID) {
            return new ArrayList<String>();
        }
        if (TextUtils.isEmpty(searchword)) {
            return new ArrayList<String>();
        }
        byte[] data = getwordslist(searchword, type);
        if (data == null || data.length < 1) {
            return new ArrayList<String>();
        }
        return ByteUtil.byte2SuggestWords(data, Ca.get_SWCOUNT());
    }
    
    public Position getwordslistStringWithPosition(String searchword, int type) {
        Position point = null;
        if (isClosed || suggestCityId == CITY_ID_INVALID) {
            return null;
        }
        byte[] data = getwordslist(searchword, type);
        if (data == null || data.length < 1) {
            return null;
        }
        ArrayList<SuggestWord> suggestWords = ByteUtil.byte2LonLatSuggestWords(data, Ca.get_SWCOUNT());
        for (SuggestWord suggestWord : suggestWords) {
            if (suggestWord.getWord().equals(searchword)) {
                point = new Position(suggestWord.getLat(), suggestWord.getLon());
            }
        }
        return point;
    }
    
    public boolean isSuggestWordsInitSuccess() {
        return suggestCityId != CITY_ID_INVALID;
    }
    
    public static class RegionMetaVersion {
        
        /*
         * 当没有地图数据（*.dat）时的版本号默认为"0.0.0.0.0"
         */
        public static final String NONE = "0.0.0.0.0";
        
        //主版本号 次版本号    年   月   日
        //uint1   uint1   uint2   uint1   uint1 
        private int mainVersion;
        private int secondVersion;
        private int year;
        private int month;
        private int day;
        
        public RegionMetaVersion() {
        }
        
        public RegionMetaVersion(int mainVersion, int secondVersion, int year, int month, int day) {
            super();
            this.mainVersion = mainVersion;
            this.secondVersion = secondVersion;
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public RegionMetaVersion(byte[] versionBytes) {
            mainVersion = ByteUtil.byteToInt(versionBytes, 0, 1);
            secondVersion = ByteUtil.byteToInt(versionBytes, 1, 1);
            year = ByteUtil.byteToInt(versionBytes, 2, 2);
            month = ByteUtil.byteToInt(versionBytes, 4, 1);
            day = ByteUtil.byteToInt(versionBytes, 5, 1);
        }

        public int getMainVersion() {
            return mainVersion;
        }

        public void setMainVersion(int mainVersion) {
            this.mainVersion = mainVersion;
        }

        public int getSecondVersion() {
            return secondVersion;
        }

        public void setSecondVersion(int secondVersion) {
            this.secondVersion = secondVersion;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }
        
        @Override
        public String toString() {
            return mainVersion + "." + secondVersion + "." + year
                    +  "." + month + "." + day;
        }
        
        public static String INVALD_VERSION = "0.0.0.0.0";
    }

    public static class RegionInfo {
        // 中文名字, English name, file size, city id
        private String cName;
        private String eName;
        private int fileSize;
        private String cCityName;
        private int cityId;
        
        public String getCName() {
            return cName;
        }
        public void setCName(String cName) {
            this.cName = cName;
        }
        public String getEName() {
            return eName;
        }
        public void setEName(String eName) {
            this.eName = eName;
        }
        public int getFileSize() {
            return fileSize;
        }
        public void setFileSize(int fileSize) {
            this.fileSize = fileSize;
        }
        public void setCCityName(String cCityName) {
            this.cCityName = cCityName;
        }
        public String getCCityName() {
            return cCityName;
        }
        
        public int getCityId() {
            return cityId;
        }
        public void setCityId(int cityId) {
            this.cityId = cityId;
        }
        
        @Override
        public String toString() {
            return "cName: " + cName + "\t eName: " + eName + "\t fileSize: " + fileSize
                    +  "\t cCityName" + cCityName + "\t cityId: " + cityId;
        }
    }

    public static class CityInfo implements Parcelable {
        // "城市中文名字, City english name, latitude, longitude, level, 省份中文名字, city
        // Province english name" such as
        // "北京,beijing,39.90415599,116.397772995,11, 北京,beijing"
        public static int TYPE_CITY = 0;
        public static int TYPE_PROVINCE = 1;
        
        private int id = CITY_ID_INVALID;
        private int type = TYPE_CITY;
        private List<CityInfo> cityInfoList = new ArrayList<CityInfo>();
        
        private String cName;
        private String eName;
        private Position position;
        private int level;
        private String cProvinceName;
        private String eProvinceName;
        public int order;
        
        public CityInfo() {
        }
        
        public int getType() {
            return type;
        }
        public void setType(int type) {
            this.type = type;
        }
        
        public List<CityInfo> getCityList() {
            return cityInfoList;
        }
        
        public String getCName() {
            return cName;
        }
        public void setCName(String cName) {
            this.cName = cName;
        }
        public String getEName() {
            return eName;
        }
        public void setEName(String eName) {
            this.eName = eName;
        }
        public int getLevel() {
            return level;
        }
        public void setLevel(int level) {
            this.level = level;
        }
        public String getCProvinceName() {
            return cProvinceName;
        }
        public void setCProvinceName(String cProvinceName) {
            this.cProvinceName = cProvinceName;
        }
        public String getEProvinceName() {
            return eProvinceName;
        }
        public void setEProvinceName(String eProvinceName) {
            this.eProvinceName = eProvinceName;
        }
        
        public void setPosition(Position position) {
            this.position = position;
        }
        
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        
        public Position getPosition() {
            return position;
        }
        
        @Override
        public String toString() {
            return "CityInfo[id:=" + id +", cName: " + cName + ", eName: " + eName + ", level: " + level + ", cProvinceName: "
                    + cProvinceName + ", eProvinceName: " + eProvinceName  + ", position: " + position +"]";
        }
        
        public CityInfo clone() {
            CityInfo cityInfo = new CityInfo();
            cityInfo.id = id;
            cityInfo.cName = cName;
            cityInfo.cProvinceName = cProvinceName;
            cityInfo.eName = eName;
            cityInfo.eProvinceName = eProvinceName;
            cityInfo.type = type;
            if (position != null) {
                cityInfo.position = position.clone();
            } else {
                cityInfo.position = null;
            }
            cityInfo.level = level;
            List<CityInfo> cityInfoList1 = new ArrayList<CityInfo>();
            for(CityInfo cityInfo1 : cityInfoList) {
                cityInfoList1.add(cityInfo1.clone());
            }
            cityInfo.cityInfoList = cityInfoList1;
            return cityInfo;
        }
        
        public boolean isAvailably() {
            return id != CITY_ID_INVALID && !TextUtils.isEmpty(cName);
        }
        
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof CityInfo) {
                CityInfo other = (CityInfo) object;
                if (this.id != other.id) {
                    return false;
                } else if((null != this.cName && !this.cName.equals(other.cName)) || (null == this.cName && this.cName != other.cName)) {
                    return false;
//                } else if((null != this.cProvinceName && !this.cProvinceName.equals(other.cProvinceName)) || (null == this.cProvinceName && this.cProvinceName != other.cProvinceName)) {
//                    return false;
//                } else if((null != this.eName && !this.eName.equals(other)) || (null == this.eName && this.eName != other.eName)) {
//                    return false;
//                } else if((null != this.eProvinceName && !this.eProvinceName.equals(other.eProvinceName)) || (null == this.eProvinceName && this.eProvinceName != other.eProvinceName)) {
//                    return false;
//                } else if (this.type != other.type) {
//                    return false;
//                } else if((null != this.position && !this.position.equals(other.position)) || (null == this.position && this.position != other.position)) {
//                    return false;
//                } else if (this.level != other.level) {
//                    return false;
//                } else {
//                    if (cityInfoList.size() != other.cityInfoList.size()) {
//                        return false;
//                    } else {
//                        int i = 0;
//                        for(CityInfo cityInfo : cityInfoList) {
//                            if (!cityInfo.equals(other.cityInfoList.get(i++))) {
//                                return false;
//                            }
//                        }
//                    }
                }
            } else {
                return false;
            }
            
            return true;
        }
        
        private volatile int hashCode = 0;
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int hash = 29 * id;
                if (cName != null) {
                    hash += cName.hashCode();
                }
//                if (cProvinceName != null) {
//                    hashCode += cProvinceName.hashCode();
//                }
//                if (eName != null) {
//                    hashCode += eName.hashCode();
//                }
//                if (eProvinceName != null) {
//                    hashCode += eProvinceName.hashCode();
//                }
//                hashCode += type * id;
//                hashCode += level * id;
//                hashCode += position.hashCode();
//                for(int i = cityInfoList.size()-1; i >= 0; i--) {
//                    CityInfo cityInfo = cityInfoList.get(i);
//                    hashCode += cityInfo.hashCode();
//                }
                hashCode = hash;
            }
            return hashCode;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int arg1) {
            parcel.writeInt(id);
            parcel.writeInt(type);
            parcel.writeString(cName);
            parcel.writeString(eName);
            parcel.writeInt(level);
            parcel.writeString(cProvinceName);
            parcel.writeString(eProvinceName);
            parcel.writeInt(order);            
        }

        public static final Parcelable.Creator<CityInfo> CREATOR
                = new Parcelable.Creator<CityInfo>() {
            public CityInfo createFromParcel(Parcel in) {
                return new CityInfo(in);
            }

            public CityInfo[] newArray(int size) {
                return new CityInfo[size];
            }
        };
        
        private CityInfo(Parcel in) {
            id = in.readInt();
            type = in.readInt();
            cName = in.readString();
            eName = in.readString();
            level = in.readInt();
            cProvinceName = in.readString();
            eProvinceName = in.readString();
            order = in.readInt();
        }
    }
    
    /**
     * 找到cityId地图、联想词等数据所在的文件夹；如果不存在则创建该文件夹
     * @param cityId
     * @return 文件夹路径。
     */
    public static String cityId2Floder(int cityId) {
        //联想词全国id以MapEngine.QUANGUO_SW_ID表示，在此做一次转换
        if (cityId == MapEngine.SW_ID_QUANGUO) {
            cityId = MapEngine.CITY_ID_QUANGUO;
        }
        StringBuilder filePath = new StringBuilder();
        MapEngine mapEngine = MapEngine.getInstance();
        String mapPath = mapEngine.getMapPath();
        if (mapPath == null) {
            return null;
        }
        filePath.append(mapPath);
        CityInfo cityInfo = mapEngine.getCityInfo(cityId);
        if (null != cityInfo) {
            filePath.append(cityInfo.getEProvinceName());
            // 创建文件夹
            File mapFile = new File(filePath.toString());
            if (!mapFile.exists()) {
                if (!mapFile.mkdirs()) {
                    LogWrapper.e(TAG, "Unable to create new folder: " + filePath.toString());
                    return null;
                }
            }
            filePath.append("/");
        } else {
            return null;
        }
        return filePath.toString();
    }
    
    /**
     * 判断一个城市是否有正确的联想词文件。
     * @param cityId
     * @return
     */
    public static boolean hasCorrectSW(int cityId) {
        boolean hasCorrect = true;
        String cityFolder = cityId2Floder(cityId);
        if (TextUtils.isEmpty(cityFolder)) {
            return false;
        }
        File indexFile = new File(cityFolder + "/sw2_" + cityId + "_index");
        if (!indexFile.exists()) {
            hasCorrect = false;
        }
        File lswFile = new File(cityFolder + "/sw2_" + cityId + "_l");
        if (!lswFile.exists()) {
            hasCorrect = false;
        }
        File sswFile = new File(cityFolder + "/sw2_" + cityId + "_s"); 
        if (!sswFile.exists()) {
            hasCorrect = false;
        }
        return hasCorrect;
    }
    
    public static boolean hasMunicipality(int cityId) {
        if (cityId == -3 || cityId == 1 || cityId == 2 || cityId == 6 || cityId == 8) {
            return true;
        }
        return false;
    }
}
