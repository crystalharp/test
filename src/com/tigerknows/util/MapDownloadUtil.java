package com.tigerknows.util;

import java.util.Random;

public class MapDownloadUtil {
    private static final String TAG = "MapDownloadUtil";

    public static int sHttpCreateTime = 3600; //ms
    public static Random random = new Random(47);
    
    public static int sNetType;
    
    private MapDownloadUtil() {}
    
    /**
     * @return time percent of creating http connection。
     */
    public static int getHttpCreatePercent() {
        return random.nextInt()%5 + 25;
    }

    public static int getMetaDownloadTimePercent() {
        return random.nextInt()%5 + 25;
    }
    
}
