/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Peng Wenyue
 *
 */
public class UserLogonModel extends XMapData {
    // 0x01 x_map 软件升级结果   
    private static final byte FIELD_SOFTWARE_UPDATE = 0x01;

    // 0x02 x_map 域名信息 
    private static final byte FIELD_DOMAIN_NAME = 0x02;

    // 0x03 x_map 推广结果  
    private static final byte FIELD_RECOMMEND = 0x03;

    // 0x04 String 是否上传日志，值为on或off   
    private static final byte FIELD_UPLOAD_LOG = 0x04;

    private SoftwareUpdate softwareUpdate;
    private DomainName domainName;
    private Recommend recommend;
    private String uploadLog;

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

    public UserLogonModel(XMap data) throws APIException {
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
        
        if (this.data.containsKey(FIELD_UPLOAD_LOG)) {
            this.uploadLog = this.data.getString(FIELD_UPLOAD_LOG);
        }
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
            if (this.data.containsKey(FIELD_ID)) {
                id = this.data.getInt(FIELD_ID);
            }
            if (this.data.containsKey(FIELD_VERSION)) {
                version = this.data.getString(FIELD_VERSION);
            }
            if (this.data.containsKey(FIELD_URL)) {
                url = this.data.getString(FIELD_URL);
            }
            if (this.data.containsKey(FIELD_TEXT)) {
                text = this.data.getString(FIELD_TEXT);
            }
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
            if (this.data.containsKey(FIELD_DOWNLOAD)) {
                download = this.data.getString(FIELD_DOWNLOAD);
            }
            if (this.data.containsKey(FIELD_QUERY)) {
                query = this.data.getString(FIELD_QUERY);
            }
            if (this.data.containsKey(FIELD_LOCATION)) {
                location = this.data.getString(FIELD_LOCATION);
            }
            if (this.data.containsKey(FIELD_NEW_QUERY)) {
                newQuery = this.data.getString(FIELD_NEW_QUERY);
            }
            if (this.data.containsKey(FIELD_ACCOUNT_MANAGE)) {
                accountManage = this.data.getString(FIELD_ACCOUNT_MANAGE);
            }
        }
    }
    
    public static class Recommend extends XMapData {
        // 0x01 XArray<XMap>  返回结果数组 
        private static final byte FIELD_RECOMMEND_APP = 0x01;
        
        private List<RecommendApp> recommendAppList;
        
        public List<RecommendApp> getRecommendAppList() {
            return recommendAppList;
        }
        
        @SuppressWarnings("unchecked")        
        public Recommend(XMap data) throws APIException {
            super(data);
            if (this.data.containsKey(FIELD_RECOMMEND_APP)) {
                recommendAppList = new ArrayList<RecommendApp>();
                XArray<XMap> list = (XArray<XMap>)this.data.getXArray(FIELD_RECOMMEND_APP);
                int length = list.size();
                for(int i = 0; i < length; i++) {
                    recommendAppList.add(new RecommendApp(list.get(i)));
                }
            }
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
//            private static final byte FIELD_ICON = 0x05;
            
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
                if (this.data.containsKey(FIELD_NAME)) {
                    name = this.data.getString(FIELD_NAME);
                }
                if (this.data.containsKey(FIELD_BODY)) {
                    body = this.data.getString(FIELD_BODY);
                }
                if (this.data.containsKey(FIELD_URL)) {
                    url = this.data.getString(FIELD_URL);
                }
                if (this.data.containsKey(FIELD_ID)) {
                    id = this.data.getString(FIELD_ID);
                }
//                if (this.data.containsKey(FIELD_ICON)) {
//                    icon = new TKDrawable(this.data.getXMap(FIELD_ICON));
//                }
            }
        }
    }
}
