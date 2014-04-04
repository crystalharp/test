/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */
package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;

import android.content.Context;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Peng Wenyue
 *
 */
public class BootstrapModel extends XMapData {
    // 0x01 x_map 软件升级结果   
    public static final byte FIELD_SOFTWARE_UPDATE = 0x01;

    // 0x02 x_map 域名信息 
    public static final byte FIELD_DOMAIN_NAME = 0x02;

    // 0x03 x_map 推广结果  
    public static final byte FIELD_RECOMMEND = 0x03;

    // 0x04 String 是否上传日志，值为on或off   
    public static final byte FIELD_UPLOAD_LOG = 0x04;
    
    // 0x05 String 是否更改web支付为客户端支付，值为on或off
    public static final byte FIELD_GO_ALIPAY = 0x05;
    
    // 0x06  x_map  参见启动展示内容  
    public static final byte FIELD_STARTUP_DISPLAY = 0x06;
    
    // 0x05 String 是否推送应用，值为on或off
    public static final byte FIELD_APP_PUSH = 0x07;
    
    // 0x05 String 是否采集网络信息，值为on或off
    public static final byte FIELD_COLLECT_NETWORK_INFO = 0x08;
    
    // 0x05 String 是否附着第三方应用采集GPS信息，值为on或off
    public static final byte FIELD_COLLECT_GPS_INFO = 0x09;
    
    // 0x01     XArray<XMap>    推广信息列表，参见单个推广信息
    public static final byte FIELD_STARTUP_DISPLAY_LIST = 0x01;
    
    private SoftwareUpdate softwareUpdate;
    private DomainName domainName;
    private Recommend recommend;
    private String uploadLog;
    private String goAlipay;
    private List<StartupDisplay> startupDisplayList;
    private String appPush;
    private String collectNetworkInfo;
    private String collectGPSInfo;

    public SoftwareUpdate getSoftwareUpdate() {
        return softwareUpdate;
    }
    
    public void setSoftwareUpdate(SoftwareUpdate softwareUpdate) {
        this.softwareUpdate = softwareUpdate;
    }
    
    public void setRecommend(Recommend recommend) {
        this.recommend = recommend;
    }

    public DomainName getDomainName() {
        return domainName;
    }

    public Recommend getRecommend() {
        return recommend;
    }

    public String getUploadLog() {
        return uploadLog;
    }
    
    public String getGoAlipay() {
    	return goAlipay;
    }
    
    public List<StartupDisplay> getStartupDisplayList() {
        return startupDisplayList;
    }

    public String getAppPush() {
        return appPush;
    }

    public String getCollectNetworkInfo() {
        return collectNetworkInfo;
    }

    public String getCollectGPSInfo() {
        return collectGPSInfo;
    }

    public BootstrapModel(XMap data) throws APIException {
        super(data);
        
        if (this.data.containsKey(FIELD_SOFTWARE_UPDATE)) {
            this.softwareUpdate = new SoftwareUpdate(this.data.getXMap(FIELD_SOFTWARE_UPDATE));
        }
        
        if (this.data.containsKey(FIELD_DOMAIN_NAME)) {
            this.domainName = new DomainName(this.data.getXMap(FIELD_DOMAIN_NAME));
        }
        
        if (this.data.containsKey(FIELD_RECOMMEND)) {
            this.recommend = new Recommend(this.data.getXMap(FIELD_RECOMMEND));
        }
        
        this.uploadLog = getStringFromData(FIELD_UPLOAD_LOG);
        this.goAlipay = getStringFromData(FIELD_GO_ALIPAY);
        
        if (this.data.containsKey(FIELD_STARTUP_DISPLAY)) {
            XMap xmap = this.data.getXMap(FIELD_STARTUP_DISPLAY);
            synchronized (Globals.StartupDisplayFile) {
                try {
                    Utility.writeFile(Globals.StartupDisplayFile, ByteUtil.xobjectToByte(xmap), true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            this.startupDisplayList = getListFromData(xmap, FIELD_STARTUP_DISPLAY_LIST, StartupDisplay.Initializer, null);
        }
        this.appPush = getStringFromData(FIELD_APP_PUSH);
        this.collectNetworkInfo = getStringFromData(FIELD_COLLECT_NETWORK_INFO);
        this.collectGPSInfo = getStringFromData(FIELD_COLLECT_GPS_INFO);
     }
    
    public static class SoftwareUpdate extends XMapData {
        // 0x01 int ID 
        private static final byte FIELD_ID = 0x01;
        // 0x02 string 版本  
        private static final byte FIELD_VERSION = 0x02;
        // 0x03 string URL字符串  
        private static final byte FIELD_URL = 0x03;
        // 0x04 string 提示给用户的字符串 
        private static final byte FIELD_TEXT = 0x04;
        
        private long id;
        private String version;
        private String url;
        private String text;
        
        public long getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public String getURL() {
            return url;
        }

        public String getText() {
            return text;
        }
        
        public SoftwareUpdate(XMap data) throws APIException {
            super(data);
            id = getLongFromData(FIELD_ID);
            version = getStringFromData(FIELD_VERSION);
            url = getStringFromData(FIELD_URL);
            text = getStringFromData(FIELD_TEXT);
        }
    }
    
    public static class DomainName extends XMapData {
        // 0x01 string  下载域名信息
        private static final byte FIELD_DOWNLOAD = 0x01;
        // 0x02    string  普通搜索、交通查询的域名信息
        private static final byte FIELD_QUERY = 0x02;
        // 0x03    string  定位服务的域名信息 
        private static final byte FIELD_LOCATION = 0x03;
        // 0x04 string 新版普通搜索、交通查询的域名信息
        private static final byte FIELD_NEW_QUERY = 0x04;
        // 0x05 string 账户系统域名信息
        private static final byte FIELD_ACCOUNT_MANAGE = 0x05;
        
        private String download;
        private String query;
        private String location;
        private String newQuery;
        private String accountManage;
        
        public String getDownload() {
            return download;
        }

        public String getQuery() {
            return query;
        }

        public String getLocation() {
            return location;
        }
        
        public String getNewQuery() {
            return newQuery;
        }

        public String getAccountManage() {
            return accountManage;
        }

        public DomainName(XMap data) throws APIException {
            super(data);
            download = getStringFromData(FIELD_DOWNLOAD);
            query = getStringFromData(FIELD_QUERY);
            location = getStringFromData(FIELD_LOCATION);
            newQuery = getStringFromData(FIELD_NEW_QUERY);
            accountManage = getStringFromData(FIELD_ACCOUNT_MANAGE);
        }
    }
    
    public static class Recommend extends XMapData {
        // 0x01 XArray<XMap>  返回结果数组 
        private static final byte FIELD_RECOMMEND_APP = 0x01;
        
        private List<RecommendApp> recommendAppList;
        
        public List<RecommendApp> getRecommendAppList() {
            return recommendAppList;
        }
        
        public Recommend(XMap data) throws APIException {
            super(data);
            recommendAppList = getListFromData(FIELD_RECOMMEND_APP, RecommendApp.Initializer);
        }
        
        public static class RecommendApp extends XMapData {
            // 0x01 String  名字  
            private static final byte FIELD_NAME = 0x01;
            // 0x02 String  内容  
            private static final byte FIELD_BODY = 0x02;
            // 0x03 String  URL  
            private static final byte FIELD_URL = 0x03;
            // 0x04 String  ID，指不同平台上软件版本的唯一ID 
            private static final byte FIELD_ID = 0x04;
            // 0x05 String  icon url，图片URL 
            private static final byte FIELD_ICON = 0x05;
            
            private String name;
            private String body;
            private String url;
            private String id;
            private TKDrawable icon;
            
            public String getName() {
                return name;
            }
            
            public String getBody() {
                return body;
            }
            
            public String getUrl() {
                return url;
            }
            
            public String getId() {
                return id;
            }
            
            public TKDrawable getIcon() {
                return icon;
            }

            public RecommendApp(XMap data) throws APIException {
                super(data);
                name = getStringFromData(FIELD_NAME);
                body = getStringFromData(FIELD_BODY);
                url = getStringFromData(FIELD_URL);
                id = getStringFromData(FIELD_ID);
                String url = getStringFromData(FIELD_ICON);
                if (url != null) {
                    TKDrawable tkDrawable = new TKDrawable();
                    tkDrawable.setUrl(url);
                    icon = tkDrawable;
                }
            }

            public static XMapInitializer<RecommendApp> Initializer = new XMapInitializer<RecommendApp>() {

                @Override
                public RecommendApp init(XMap data) throws APIException {
                    return new RecommendApp(data);
                }
            };
        }
    }
    
    public static class StartupDisplay extends XMapData {
        
        public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 0x01  String  图片url  
        public static final byte FIELD_URL = 0x01;
        // 0x02  String  图片展示起始时间，格式为1970-01-01 00：00：00 
        public static final byte FIELD_BEGIN = 0x02;
        // 0x03  String  图片展示结束时间，格式为1970-01-01 00：00：00  
        public static final byte FIELD_END = 0x03;
        
        private String url;
        private Date begin;
        private Date end;

        public String getUrl() {
            return url;
        }

        public StartupDisplay(XMap data) throws APIException {
            super(data);
            url = getStringFromData(FIELD_URL);
            begin = getDateFromData(FIELD_BEGIN);
            end = getDateFromData(FIELD_END);
        }
        
        Date getDateFromData(byte key) {
            Date date = null;
            if (this.data.containsKey(key)) {
                String str = getStringFromData(key);
                try {
                    date = simpleDateFormat.parse(str);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return date;
        }
        
        public boolean isAvailably(Context context) {
            boolean result = false;
            if (url != null && begin != null && end != null) {
                long time = CalendarUtil.getExactTime(context);
                if (time >= begin.getTime() && time <= end.getTime()) {
                    result = true;
                }
            }
            return result;
        }

        public static XMapInitializer<StartupDisplay> Initializer = new XMapInitializer<StartupDisplay>() {

            @Override
            public StartupDisplay init(XMap data) throws APIException {
                return new StartupDisplay(data);
            }
        };
    }
}
