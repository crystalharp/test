/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;
import android.text.TextUtils;

import java.util.Comparator;
import java.util.Hashtable;

public class Comment extends BaseData {
    
    // 0x00    x_string    uid 点评uid   否
    public static final byte FIELD_UID = 0x00;
    
    // 0x01    x_string    content 点评内容    是
    public static final byte FIELD_CONTENT = 0x01;
    
    // 0x02    x_string    user    点评人 否
    public static final byte FIELD_USER = 0x02;
    
    // 0x03    x_string    time    点评日期    否
    public static final byte FIELD_TIME = 0x03;
    
    // 0x04    x_int   grade   点评星级    是
    public static final byte FIELD_GRADE = 0x04;
    
    // 0x06    x_string    puid    对应poi的uid   是
    public static final byte FIELD_PUID = 0x06;
    
    // 0x07    x_int   avg 人均价格    否
    public static final byte FIELD_AVG = 0x07;
    
    // 0x08    x_int   taste   口味  否
    public static final byte FIELD_TASTE = 0x08;
    
    // 0x09    x_int   qos 服务  否
    public static final byte FIELD_QOS = 0x09;
    
    // 0x0a    x_int   environment 环境  否
    public static final byte FIELD_ENVIRONMENT = 0x0a;
    
    // 0x0b    x_string    recommend   推荐菜（逗号分隔）   否
    public static final byte FIELD_RECOMMEND = 0x0b;
    
    // 0x0c    x_int   level   医疗水平    否
    public static final byte FIELD_LEVEL = 0x0c;
    
    // 0x0d    x_int   effect  观影效果    否
    public static final byte FIELD_EFFECT = 0x0d;
    
    // 0x0e    x_string   restair 餐厅氛围  
    public static final byte FIELD_RESTAIR = 0x0e;
    
    // 0x0f    x_int   poistatus   点评关联的poi是否有效    否 
    public static final byte FIELD_POI_STATUS = 0x0f;
    
    // 0x10 x_int   pattern 点评模式    否  
    public static final byte FIELD_PATTERN = 0x10;
    
    // 0x11 x_string    poiname 点评关联poi的name    否 
    public static final byte FIELD_POI_NAME = 0x11;
    
    // 0x12 x_int userId 不变的用户标识 否
    public static final byte FIELD_USER_ID = 0x12;
    
    // 0x13 x_string    citySeqId   点评相关poi的cityId（短）   否 
    public static final byte FIELD_POI_CITY_ID = 0x13;

    // 0x14 x_string url 新版客户端需要的点评url 否
    public static final byte FIELD_URL = 0x14;

    // 0x15 x_string client_uid 发表点评的客户端软件的uid  否 
    public static final byte FIELD_CLIENT_UID = 0x15;

    // 0x1f    x_string    source  点评来源    是，详细定义见点评数据表中source字段 
    public static final byte FIELD_SOURCE = 0x1f;

    protected static final String NEED_FILELD = "0001020304060708090a0b0c0d0e0f1011121314151f";

    private String uid; // 0x00    x_string    uid 点评uid   否
    private String content; // 0x01    x_string    content 点评内容    是
    private String user; //0x02    x_string    user    点评人 否
    private String time; // 0x03    x_string    time    点评日期    否
    private long grade = 6; // 0x04    x_int   grade   点评星级    是
    private String puid; // 0x06    x_string    puid    对应poi的uid   是
    private long avg = -1; // 0x07    x_int   avg 人均价格    否
    private long taste = 3; // 0x08    x_int   taste   口味  否
    private long qos = 3; // 0x09    x_int   qos 服务  否
    private long environment = 3; // 0x0a    x_int   environment 环境  否
    private String recommend; // 0x0b    x_string    recommend   推荐菜（逗号分隔）   否
    private long level = 3; // 0x0c    x_int   level   医疗水平    否
    private long effect = 3; // 0x0d    x_int   effect  观影效果    否
    private String restair; // 0x0e    x_string    restair  餐厅氛围 
    private long poiStatus; // 0x0f    x_int   poistatus   点评关联的poi是否有效    否
    private long pattern; // 0x10 x_int   pattern 点评模式    否 
    private String poiName; // 0x11 x_string    poiname 点评关联poi的name    否 
    private long userId = -1; // 0x12 x_int userId 不变的用户标识 否
    private String poiCityId; // 0x13 x_string    citySeqId   点评相关poi的cityId（短）   否 
    private String url; // 0x14 x_string url 新版客户端需要的点评url 否
    private String clientUid; // 0x15 x_string client_uid 发表点评的客户端软件的uid  否 
    private String source; // 0x1f    x_string    source  点评来源   是，详细定义见点评数据表中source字段 
    
    private POI poi;
    
    public Comment() {
    }

    public Comment (XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.uid = getStringFromData(FIELD_UID, reset ? null : this.uid);
        this.content = getStringFromData(FIELD_CONTENT, reset ? null : this.content);
        this.user = getStringFromData(FIELD_USER, reset ? null : this.user);
        this.time = getStringFromData(FIELD_TIME, reset ? null : this.time);
        this.grade = getLongFromData(FIELD_GRADE, reset ? 6 : this.grade);
        this.puid = getStringFromData(FIELD_PUID, reset ? null : this.puid);
        this.avg = getLongFromData(FIELD_AVG, reset ? -1 : this.avg);
        this.taste = getLongFromData(FIELD_TASTE, reset ? 3 : this.taste);
        this.qos = getLongFromData(FIELD_QOS, reset ? 3 : this.qos);
        this.environment = getLongFromData(FIELD_ENVIRONMENT, reset ? 3 : this.environment);
        this.recommend = getStringFromData(FIELD_RECOMMEND, reset ? null : this.recommend);
        this.level = getLongFromData(FIELD_LEVEL, reset ? 3 : this.level);
        this.effect = getLongFromData(FIELD_EFFECT, reset ? 3 : this.effect);
        this.restair = getStringFromData(FIELD_RESTAIR, reset ? null : this.restair);
        this.poiStatus = getLongFromData(FIELD_POI_STATUS, reset ? 0 : this.poiStatus);
        this.pattern = getLongFromData(FIELD_PATTERN, reset ? 0 : this.pattern);
        this.poiName = getStringFromData(FIELD_POI_NAME, reset ? null : this.poiName);
        this.userId = getLongFromData(FIELD_USER_ID, reset ? -1 : this.userId);
        this.poiCityId = getStringFromData(FIELD_POI_CITY_ID, reset ? null : this.poiCityId);
        this.url = getStringFromData(FIELD_URL, reset ? null : this.url);
        this.clientUid = getStringFromData(FIELD_CLIENT_UID, reset ? null : this.clientUid);
        this.source = getStringFromData(FIELD_SOURCE, reset ? null : this.source);
    }
    
    public XMap getData() {
        if (this.data == null) {
            this.data = new XMap();            
            if (!TextUtils.isEmpty(this.uid)) {
                this.data.put(FIELD_UID, this.uid);
            }
            if (!TextUtils.isEmpty(this.content)) {
                this.data.put(FIELD_CONTENT, this.content);
            }
            if (!TextUtils.isEmpty(this.user)) {
                this.data.put(FIELD_USER, this.user);
            }
            if (!TextUtils.isEmpty(this.time)) {
                this.data.put(FIELD_TIME, this.time);
            }
            this.data.put(FIELD_GRADE, this.grade);
            if (!TextUtils.isEmpty(this.puid)) {
                this.data.put(FIELD_PUID, this.puid);
            }
            this.data.put(FIELD_AVG, this.avg);
            this.data.put(FIELD_TASTE, this.taste);
            this.data.put(FIELD_QOS, this.qos);
            this.data.put(FIELD_ENVIRONMENT, this.environment);
            if (!TextUtils.isEmpty(this.recommend)) {
                this.data.put(FIELD_RECOMMEND, this.recommend);
            }
            this.data.put(FIELD_LEVEL, this.level);
            this.data.put(FIELD_EFFECT, this.effect);
            if (!TextUtils.isEmpty(this.restair)) {
                this.data.put(FIELD_RESTAIR, this.restair);
            }
            this.data.put(FIELD_POI_STATUS, this.poiStatus);
            this.data.put(FIELD_PATTERN, this.pattern);
            if (!TextUtils.isEmpty(this.poiName)) {
                this.data.put(FIELD_POI_NAME, this.poiName);
            }
            this.data.put(FIELD_USER_ID, this.userId);
            if (!TextUtils.isEmpty(this.poiCityId)) {
                this.data.put(FIELD_POI_CITY_ID, this.poiCityId);
            }
            if (!TextUtils.isEmpty(this.url)) {
                this.data.put(FIELD_URL, this.url);
            }
            if (!TextUtils.isEmpty(this.clientUid)) {
                this.data.put(FIELD_CLIENT_UID, this.clientUid);
            }
            if (!TextUtils.isEmpty(this.source)) {
                this.data.put(FIELD_SOURCE, this.source);
            }
        }
        return this.data;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getGrade() {
        return grade;
    }

    public void setGrade(long grade) {
        this.grade = grade;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPuid() {
        return puid;
    }

    public void setPuid(String puid) {
        this.puid = puid;
    }

    public long getAvg() {
        return avg;
    }

    public void setAvg(long avg) {
        this.avg = avg;
    }

    public long getTaste() {
        return taste;
    }

    public void setTaste(long taste) {
        this.taste = taste;
    }

    public long getQos() {
        return qos;
    }

    public void setQos(long qos) {
        this.qos = qos;
    }

    public long getEnvironment() {
        return environment;
    }

    public void setEnvironment(long environment) {
        this.environment = environment;
    }

    public String getRecommend() {
        return recommend;
    }

    public void setRecommend(String recommend) {
        this.recommend = recommend;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    public long getEffect() {
        return effect;
    }

    public void setEffect(long effect) {
        this.effect = effect;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRestair() {
        return restair;
    }

    public void setRestair(String restair) {
        this.restair = restair;
    }

    public long getPOIStatus() {
        return poiStatus;
    }

    public void setPOIStatus(long poiStatus) {
        this.poiStatus = poiStatus;
    }

    public long getPattern() {
        return pattern;
    }

    public void setPattern(long pattern) {
        this.pattern = pattern;
    }

    public String getPOIName() {
        return poiName;
    }

    public void setPOIName(String poiName) {
        this.poiName = poiName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getPoiCityId() {
        return poiCityId;
    }

    public void setPoiCityId(String poiCityId) {
        this.poiCityId = poiCityId;
    }

    public String getClientUid() {
        return clientUid;
    }

    public void setClientUid(String clientUid) {
        this.clientUid = clientUid;
    }

    public POI getPOI() {
        if (poi == null) {
            poi = new POI();
            if (!TextUtils.isEmpty(puid)) {
                poi.setUUID(puid);
            }
            if (!TextUtils.isEmpty(poiName)) {
                poi.setName(poiName);
            }
            poi.setStatus(poiStatus);
            poi.setCommentPattern(pattern);
            poi.setMyComment(this);
            if (!TextUtils.isEmpty(poiCityId)) {
                try {
                    poi.ciytId = Integer.parseInt(poiCityId);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
        
        return poi;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Comment) {
            Comment other = (Comment) object;
            if((null != other.uid && !other.uid.equals(this.uid)) || (null == other.uid && other.uid != this.uid)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public static Comparator COMPARATOR = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            Comment comment1 = (Comment) object1;
            Comment comment2 = (Comment) object2;

            if (isAuthorMe(comment1) > 0) {
                return -1;
            } else if (isAuthorMe(comment2) > 0) {
                return 1;
            }
            return comment2.getTime().compareTo(comment1.getTime());
        };
    };
    
    @SuppressWarnings("unchecked")
    public static Comparator COMPARATOR_ONLY_TIME = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            Comment comment1 = (Comment) object1;
            Comment comment2 = (Comment) object2;
            return comment2.getTime().compareTo(comment1.getTime());
        };
    };
    
    /**
     * 判断评论的作者是否为自己
     * @param comment
     * @return
     */
    public static long isAuthorMe(Comment comment) {
        long attr = 0;
        if (comment == null) {
            return attr; 
        }
        String clientUID = Globals.g_ClientUID;
        User user = Globals.g_User;
        long userId = -1;
        if (user != null) {
            userId = user.getUserId();
        }
        
        // 金截
        if (userId != -1 && comment.getUserId() == userId) {
            attr = POI.ATTRIBUTE_COMMENT_USER;
            
        // 银截
        } else if (userId == -1
                && comment.getUserId() == -1
                && clientUID != null
                && clientUID.equals(comment.getClientUid())) {
            attr = POI.ATTRIBUTE_COMMENT_ANONYMOUS;
        }
        
        return attr;
    }
    
    @SuppressWarnings("unchecked")
    public static Comparator COMPARATOR_DATETIME = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            Comment comment1 = (Comment) object1;
            Comment comment2 = (Comment) object2;
            return comment2.getTime().compareTo(comment1.getTime());
        };
    };
    
    public static DataQuery createPOICommentQuery(Context context, POI poi, int sourceViewId, int targerViewId) {

        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
        criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
        criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
        DataQuery commentQuery = new DataQuery(context);
        commentQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), sourceViewId, targerViewId, null, false, false, poi);
        return commentQuery;
    }
    
    public static XMapInitializer<Comment> Initializer = new XMapInitializer<Comment>() {

        @Override
        public Comment init(XMap data) throws APIException {
            return new Comment(data);
        }
    };
}
