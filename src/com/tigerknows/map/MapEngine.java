/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;

import com.decarta.CONFIG;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.model.TKWord;
import com.tigerknows.service.SuggestLexiconService;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

public class MapEngine {

    /**
     * 删除或更新地图数据时，通知MapView清除缓存的Tile信息
     */
    public static final String ACTION_REMOVE_CITY_MAP_DATA = "action.com.tigerknows.remove.city.map.data";
    
    /**
     * 城市Id
     */
    public static final String EXTRA_CITY_ID = "extra_city_id";
    public static final int DEFAULT_CITY_LEVEL = 15;

    public static int SW_ID_QUANGUO = 9999;
    public static int TILE_SIZE_BIT = 8;
    
    private int suggestCityId = CityInfo.CITY_ID_INVALID;  // 联想词引擎所属城市Id
    private List<Integer> suggestDownloaded = new ArrayList<Integer>(); // 记录被下载更新过的城市Id
    
    private String mapPath = null;
    private boolean isExternalStorage = false;
    public boolean isExternalStorage() {
        return isExternalStorage;
    }
    
    public static final int MAX_REGION_TOTAL_IN_ROM = 30;
    private Context context;
    private void initEngine(Context context, String mapPath) {
		synchronized (this) {
		    isExternalStorage = false;
    		this.mapPath = mapPath;
    		String externalStorageState = Environment
    				.getExternalStorageState();
    		if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
    			File externalStorageDirectory = Environment
    					.getExternalStorageDirectory();
    			String externalStoragePath = externalStorageDirectory
    					.getAbsolutePath();
    			if (this.mapPath.startsWith(externalStoragePath)) {
    				isExternalStorage = true;
    			}
    		}
            if(Ca.tk_is_engine_initialized()) {//地图引擎已被初始化过，静态数据已加载
            	Ca.tk_engine_reset_map_dir(this.mapPath);
            }
            else {
    			int status = Ca.tk_init_engine_config(
    					TKConfig.getDataPath(false), this.mapPath,
    					CONFIG.TILE_SIZE);
    			if (status == 0) {
    				this.context = context;
    				Ca.tk_init_context(null, TILE_SIZE_BIT, 0);
    			} else {
    				destroyEngine();
    			}
    			LogWrapper.d(TAG, "initEngine() status=" + status);
    		}
		}
    }

    public void setTileSize(int tile_size) {
    	Ca.tk_set_global_tile_size(tile_size);
    }
    
    public void destroyEngine() {
    	suggestDownloaded.clear();
	    suggestwordDestroy();
	    Ca.tk_destroy_engine_config();
	    mapPath = null;
	    LogWrapper.d(TAG, "destroyEngine()");
    }

    public static int getCityId(Position position) {
        int cityId = CityInfo.CITY_ID_INVALID;
        if (position == null) {
            return cityId;
        }
        cityId = Ca.tk_get_city_id(position.getLat(), position.getLon());
        return cityId;
    }

    /**
     * 解压联想词文件
     * @param cityId
     * @param filepath
     * @return
     */
    public int decompress(int cityId, String filepath) {
        synchronized (this) {
        	return Ca.tk_decompress(cityId, filepath);
        }
    }
    
    //根据城市联想词filepath和全国公共联想词filepath_common进行加载，建立索引，城市id改变时调用
    public void suggestwordInit(int cityId) {
        synchronized (this) {
        suggestwordDestroy();
        if (cityId <= 0) {
            return;
        }
        if (cityId == SW_ID_QUANGUO || cityId == CityInfo.CITY_ID_QUANGUO) {
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
        if (suggestCityId == CityInfo.CITY_ID_INVALID) {
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
        if (suggestCityId != CityInfo.CITY_ID_INVALID) {
            suggestCityId = CityInfo.CITY_ID_INVALID;
            Ca.tk_suggestword_destroy();
        }
        }
    }

    //返回当前城市的联想词版本
    public int getrevision(int cityId) {
        synchronized (this) {
        if (suggestCityId == CityInfo.CITY_ID_INVALID) {
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
        Ca.tk_remove_region_data(rid); 
        int cityId = Ca.tk_get_cid_by_rid(rid);
        sendBroadcastForRemoveMapData(cityId);
    }
    
    void sendBroadcastForRemoveMapData(int cityId) {
        synchronized (this) {
	        if (context != null) {
	            Intent intent = new Intent(ACTION_REMOVE_CITY_MAP_DATA);
	            intent.putExtra(EXTRA_CITY_ID, cityId);
	            context.sendBroadcast(intent);
	        }
        }
    }

    public boolean initRegion(int rid) {
        String path = mapPath + String.format(TKConfig.MAP_REGION_METAFILE, rid);
        int ret = Ca.tk_init_region(path, rid);
        
        File metaFile = new File(path);
        if (metaFile.exists()) {
//        	String tmpPath = path + ".rmtmp";
//        	File tmpFile = new File(tmpPath);
//        	metaFile.renameTo(tmpFile);
            metaFile.delete();
        }
        LogWrapper.d("MapEngine", "init region status: " + ret);
        return ret == 0 ? true : false;
    }
    
    public static int writeRegion(int rid, int offset, byte[] buf, String version) {
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
    
    public static LocalRegionDataInfo getLocalRegionDataInfo(int regionId) {
        int[] data = Ca.tk_get_region_stat(regionId);
        if (data == null || data.length < 1) {
            return null;
        }
        LogWrapper.i(TAG, "getLocalRegionDataInfo()regionId: " + regionId + " totalSize: " + data[0] + "downloadedSize: " + data[1] + "lostCount: " + data[2]);
        if(data[0] == 0 && data[1] == 0)
        	return null;
        LocalRegionDataInfo regionMapInfo = new LocalRegionDataInfo();
        regionMapInfo.setTotalSize(data[0]);
        regionMapInfo.setDownloadSize(data[1]);
        regionMapInfo.setLostDataNum(data[2]);
        return regionMapInfo;
    }
    
    public String getMapPath() {
        return mapPath;
    }

    public static int getMaxLabelPriority() {
    	return Ca.tk_get_max_label_priority();
    }
    
    public static int getRegionId(String regionName) {
        return Ca.tk_get_region_id(regionName);
    }

    public static List<String> getCitylist(String provinceName) {
        ArrayList<String> cityList = new ArrayList<String>();
        if (TextUtils.isEmpty(provinceName)) {
            return cityList;
        }
        String[] cities = Ca.get_citylist(provinceName.trim()); 
        if(cities == null)
        	return cityList;
        for (String city : cities) {
            cityList.add(city);
        }
        return cityList;
    }
    
    public static int getCityid(String cityName) {
        if (TextUtils.isEmpty(cityName)) {
            return -1;
        }
        return Ca.tk_get_cityid(cityName.trim());
    }
    
    public static RegionInfo getRegionInfo(int regionId) {
        String regionInfoStr = Ca.tk_get_region_info(regionId);
        if(regionInfoStr == null || regionInfoStr.length() == 0) {
        	return null;
        }
        RegionInfo regionInfo = new RegionInfo();
        String[] strs = regionInfoStr.split(",");
        regionInfo.setCName(strs[0]);
        regionInfo.setEName(strs[1]);
        regionInfo.setFileSize(Integer.parseInt(strs[2]));
        regionInfo.setCCityName(strs[3]);
        return regionInfo;
    }

    public static CityInfo getCityInfo(int cityId) {
        CityInfo cityInfo = new CityInfo();
    	if(cityId == CityInfo.CITY_ID_INVALID) {
    		LogWrapper.e("MapEngine", "get city info of invalid city id");
    		return cityInfo;
    	}
        String cityInfoStr = Ca.tk_get_city_info(cityId);
        if(cityInfoStr == null || cityInfoStr.length() == 0) {
        	return cityInfo;
        }
        String[] strs = cityInfoStr.split(",");
        cityInfo.setId(cityId);
        cityInfo.setCName(strs[0]);
        cityInfo.setEName(strs[1]);
        cityInfo.setPosition(new Position(Double.parseDouble(strs[2]), Double.parseDouble(strs[3])));
//        cityInfo.setLevel(Integer.parseInt(strs[4]));
        cityInfo.setLevel(DEFAULT_CITY_LEVEL);
        cityInfo.setCProvinceName(strs[5]);
        cityInfo.setEProvinceName(strs[6]);
        return cityInfo;
    }

    // 得到所有省的中英文名字，去掉香港、澳门
    private static HashMap<String, String> getEprovincelist(Context context) {
        HashMap<String, String> infoMap = new HashMap<String, String>();
        String allProvincesInfoStr = Ca.tk_get_eprovincelist();
        if(allProvincesInfoStr == null || allProvincesInfoStr.length() == 0)
        	return infoMap;
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
    
    public void removeCityData(String cityName) {
        if (TextUtils.isEmpty(cityName)) {
            return;
        }
        Ca.tk_remove_city_data(cityName.trim());
        sendBroadcastForRemoveMapData(getCityid(cityName));
    }

    public static RegionMetaVersion getRegionMetaVersion(int regionId) {
        byte[] data = Ca.tk_get_region_version(regionId);
        if (data == null || data.length < 1) {
            return null;
        }
        RegionMetaVersion ridDataVersion = new RegionMetaVersion(data);
        return ridDataVersion;
    }
    
    /**
     * 坐标转换
     * 这个是遵守国家规定，所有定位的坐标都要经过此转换后，在地图上显示才是正确位置
     * @param position
     * @return
     */
    public Position latlonTransform(Position position) {
        synchronized (this) {
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
     * @param position 经纬度
     * @return
     */
    public String getPositionName(Position position) {
        synchronized (this) {
            String poiName = null;
            if (position == null) {
                return poiName;
            }
            poiName = Ca.tk_get_poi_name(position.getLat(), position.getLon(), 16);
            LogWrapper.i("MapEngine", "getPositionName: " + poiName + "of lat:" + position.getLat() + " lon: " + position.getLon());
            if (poiName == null || poiName.equals("null")) {
            	return null;
            }
            return poiName;
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
                initEngine(context, appPath);
                new File(appPath, "try.txt").createNewFile();
                new File(appPath, "try.txt").delete();
                LogWrapper.i(TAG, "setupDataPath() app path:"+ appPath + " map path:"+ mapPath + ",exist:" + new File(appPath).exists());
            } catch (Exception e) {
                mapPath = null;
                e.printStackTrace();
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
        File file = new File(TKConfig.getDataPath(false)+"citylist");
        if (TextUtils.isEmpty(versionName)   // 第一次安装后使用
                || !TKConfig.getClientSoftVersion().equals(versionName)   // 更新安装后使用
                || file.exists() == false) {
            String mapPath = TKConfig.getDataPath(true);
            AssetManager am = context.getAssets();
            
            Utility.deleteAllFile(TKConfig.getDataPath(false));
            
            Utility.unZipFile(am, "tigerres.zip", TKConfig.getDataPath(false));
            Utility.unZipFile(am, "tigermap.zip", mapPath);
            
            TKConfig.setPref(context, TKConfig.PREFS_VERSION_NAME, TKConfig.getClientSoftVersion());
            LogWrapper.d(TAG, "setup()");
        }
    }
    
    public static TileDownload[] getLostData() {
    	return Ca.tk_get_lost_tile_info();
    }

    public static List<CityInfo> getAllProvinceCityList(Context context) {
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
                if(cityInfo.isAvailably() == false)
                	continue;
                if (i == cityNameList.size() - 1) {
                    province.setId(cityInfo.getId());
                }
                province.getCityList().add(cityInfo);
            }
            allCityInfoList.add(province);
        }
        
        return allCityInfoList;
    }

    //得到一个城市的所有region id
    public static ArrayList<Integer> getRegionIdList(String cityName) {
        int cityId = getCityid(cityName);
        ArrayList<Integer> regionIdList = new ArrayList<Integer>();
        if(cityId == CityInfo.CITY_ID_INVALID) {
        	LogWrapper.e("MapEngine", "got invalid city id, cityName: " + cityName);
        	return regionIdList;
        }
        int[] rids = Ca.tk_get_regionid_list(cityId);
        for (int rid : rids) {
            regionIdList.add(rid);
        }
        return regionIdList;
    }

    public void suggestwordCheck(Sphinx sphinx, int cityId) {
        if (cityId < CityInfo.CITY_ID_BEIJING) {
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
            if (cityId == suggestCityId || suggestCityId == CityInfo.CITY_ID_INVALID) {
                suggestwordInit(cityId);
            }
        }
    }
    
    //根据用户输入的词searchword及type进行联想，返回前x()个联想词，联想词在源文件TKSuggestWords.c全局变量Suggest_Word suggestwords[10]中；type为poi或者位置--对应不同搜索框
    public List<String> getwordslistString(String searchword, int type) {
        if (suggestCityId == CityInfo.CITY_ID_INVALID) {
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
        if (suggestCityId == CityInfo.CITY_ID_INVALID) {
            return null;
        }
        byte[] data = getwordslist(searchword, type);
        if (data == null || data.length < 1) {
            return null;
        }
        ArrayList<TKWord> suggestWords = ByteUtil.byte2LonLatSuggestWords(data, Ca.get_SWCOUNT());
        for (TKWord tkwork : suggestWords) {
            if (tkwork.word.equals(searchword)) {
                point = tkwork.position;
            }
        }
        return point;
    }
    
    public boolean isSuggestWordsInitSuccess() {
        return suggestCityId != CityInfo.CITY_ID_INVALID;
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
    
    /**
     * 找到cityId地图、联想词等数据所在的文件夹；如果不存在则创建该文件夹
     * @param cityId
     * @return 文件夹路径。
     */
    public static String cityId2Floder(int cityId) {
        //联想词全国id以MapEngine.QUANGUO_SW_ID表示，在此做一次转换
        if (cityId == MapEngine.SW_ID_QUANGUO) {
            cityId = CityInfo.CITY_ID_QUANGUO;
        }
        StringBuilder filePath = new StringBuilder();
        MapEngine mapEngine = MapEngine.getInstance();
        String mapPath = mapEngine.getMapPath();
        if (mapPath == null) {
            return null;
        }
        filePath.append(mapPath);
        CityInfo cityInfo = MapEngine.getCityInfo(cityId);
        if (cityInfo.isAvailably()) {
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

    /**
     * 获取地铁图数据文件路径，数据不可用则返回null
     * @param cityId
     * @return
     */
    public static String getSubwayDataPath(Context context, int cityId) {
        String result = null;
        
        CityInfo cityInfo = MapEngine.getCityInfo(cityId);
        if (cityInfo.isAvailably() == false) {
            return result;
        }
        
        if (checkSubwayMapValidity(context, cityId)) {
            String path = getSubwayMapPath(cityId);
            result = path + "index.html";
        }
        
        return result;
    }
    
    /**
     * 获取某城市地铁图的路径.
     * 只做字符串拼接用途,使用时请检查目录内是否有文件.
     * @param cityId
     * @return
     */
    public final static String getSubwayMapPath(int cityId) {
        CityInfo cityInfo = MapEngine.getCityInfo(cityId);
        if (cityInfo.isAvailably() == false) {
            return null;
        }
        return cityId2Floder(cityId)+"sw_"+cityInfo.getEName()+"/";
    }
    
    /**
     * 获取某城市地铁数据的大小
     * 无数据返回0
     * @param cityId
     * @return
     */
    public static long calcSubwayDataSize(int cityId) {
        String path = getSubwayMapPath(cityId);
        if (path == null) {
            return 0;
        }
        long size = 0;
        File subwayDir = new File(path);
        if (subwayDir.exists() && subwayDir.isDirectory()) {
            File[] files = subwayDir.listFiles();
            if (files.length != 0) {
                for (File file : files) {
                    size += file.length();
                }
            }
        }
        
        return size;
    }
    
    /**
     * 检测地铁图数据的完整性
     */
    public static boolean checkSubwayMapValidity(Context context, int cityId) {
        if (TKConfig.CloseSubwayDataCheck) {
            return true;
        }
        long size = calcSubwayDataSize(cityId);
        String recordSize = TKConfig.getPref(context, TKConfig.getSubwayMapSizePrefs(cityId), null);
        if (size != 0 && String.valueOf(size).equals(recordSize)) {
            return true;
        }
        return false;
    }
    
    public static boolean hasMunicipality(int cityId) {
        if (cityId == -3 || cityId == 1 || cityId == 2 || cityId == 6 || cityId == 8) {
            return true;
        }
        return false;
    }
    
    /**
     * 获取城市的电话区号
     * @param cityId
     * @return
     */
    public static String getAreaCodeByCityId(int cityId) {
        String result = null;
        String token = String.valueOf(cityId);
        String path = TKConfig.getDataPath(false);
        if (TextUtils.isEmpty(path)) {
            return result;
        }
        File file = new File(path+"areacode");
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                String text = Utility.readFile(fis);
                fis.close();
                String[] lines = text.split("\n");
                for(int i = lines.length-1; i >= 0; i--) {
                    String[] line = lines[i].split(" ");
                    if (line[0].equals(token)) {
                        result = line[1].trim();
                    }
                }
            } catch (Exception e) {
                
            }
        }
        
        return result;
    }
    
    public static void cleanEngineCache() {
    	Ca.tk_clean_engine_cache();
    	Ca.tk_clean_engine_label();
    	subwayMap.clear();
    }
    
    
    private static HashMap<Integer, String> subwayMap = new HashMap<Integer, String>();
    
    public static HashMap<Integer, String> getSubwayMap() {
        return subwayMap;
    }
    
    /**
     * 检查指定城市是否支持查看地铁图功能
     * @param cityId
     * @return
     */
    public static boolean checkSupportSubway(int cityId) {
        boolean result = false;
        if (subwayMap.size() == 0) {
            File file = new File(TKConfig.getDataPath(false)+"sw_city.txt");
            String line = null;
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                while((line = br.readLine()) != null) {
                    String[] strArr = line.split(",");
                    subwayMap.put(Integer.parseInt(strArr[1]), strArr[0]);
                }
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        result = subwayMap.containsKey(cityId);
        
        return result;
    }
}
