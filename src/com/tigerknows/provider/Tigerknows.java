/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * @author Peng Wenyue
 */
public class Tigerknows {

    public static final String AUTHORITY = "com.tigerknows.provider.Tigerknows";
    
    public static final int STORE_TYPE_FAVORITE = 1;
    
    public static final int STORE_TYPE_OTHER = 2;
    
    public static final int STORE_TYPE_HISTORY = 3;
    
    public static final int HISTORY_MAX_SIZE = 100;
    
    public interface History extends BaseColumns {
        
        public static final int HISTORY_POI = 1;
        
        public static final int HISTORY_BUSLINE = 2;
        
        public static final int HISTORY_DRIVE = 3;
        
        public static final int HISTORY_TRANSFER = 4;
        
        public static final int HISTORY_WALK = 5;
        
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/history");
        
        public static final Uri CONTENT_URI_COUNT = Uri.parse("content://" + AUTHORITY + "/history_count");

        public static final String DEFAULT_SORT_ORDER = "_datetime DESC";

        public static final String HISTORY_TYPE = "history_type";

        public static final String DATETIME = "_datetime";

    }
    
    public interface Favorite extends BaseColumns {
        
        public static final int FAVORITE_POI = 1;
        
        public static final int FAVORITE_BUSLINE = 2;
        
        public static final int FAVORITE_DRIVE = 3;
        
        public static final int FAVORITE_TRANSFER = 4;
        
        public static final int FAVORITE_WALK = 5;
        
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/favorite");
        
        public static final Uri CONTENT_URI_COUNT = Uri.parse("content://" + AUTHORITY + "/favorite_count");

        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String FAVORITE_TYPE = "favorite_type";
        
        public static final String ALIAS = "_alias";

        public static final String DATETIME = "_datetime";

    }

    public interface POI extends BaseColumns {
        
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/poi");
        
        public static final Uri CONTENT_URI_COUNT = Uri.parse("content://" + AUTHORITY + "/poi_count");
        
        public static final String DEFAULT_SORT_ORDER = "_id ASC";
        
        public static final String STORE_TYPE = "store_type";

        public static final String PARENT_ID = "parent_id";
        
        public static final String POI_NAME = "poi_name";
        
        public static final String ALIAS = "_alias";
        
        public static final String POI_X = "poi_x";
        
        public static final String POI_Y = "poi_y";
        
        public static final String ADDRESSS = "addresss";
        
        public static final String PHONE = "phone";

        public static final String POI_VERSION = "poi_version";

        public static final String DATA = "_data";

        public static final String COMMENT_DATA = "_comment_data";

        public static final String DATETIME = "_datetime";

    }

    public interface TransitPlan extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/transitplan");

        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String TYPE = "_type";

        public static final String TIMES = "times";
        
        public static final String TOTAL_LENGTH = "total_length";
        
        public static final String START = "start";
        
        public static final String END = "end";
        
        public static final String ROUTE = "route";
        
        public static final String STEPS = "steps";
        
        public static final String ONFOOTS = "onfoots";

        public static final String STORE_TYPE = "store_type";

        public static final String PARENT_ID = "parent_id";

        public static final String DATA = "_data";

    }

    public interface Busline extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/busline");

        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String BUSLINE_NAME = "busline_name";
        
        public static final String BUSLINE_NUM = "busline_num";
        
        public static final String TOTAL_LENGTH = "total_length";
        
        public static final String DESCRITPION = "descritpion";

        public static final String STOPS = "stops";

        public static final String LINE = "line";

        public static final String STORE_TYPE = "store_type";

        public static final String PARENT_ID = "parent_id";

        public static final String DATA = "_data";

    }

    public interface Alarm extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/alarm");

        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String NAME = "tk_name";
        
        public static final String POSITION = "tk_position";
        
        public static final String RANGE = "tk_range";
        
        public static final String RINGTONE = "tk_ringtone";
        
        public static final String RINGTONE_NAME = "tk_ringtone_name";
        
        public static final String STATUS = "tk_status";

    }
}
