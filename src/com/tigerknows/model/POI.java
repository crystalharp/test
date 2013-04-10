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
import com.decarta.android.location.Position;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.SqliteWrapper;
import com.tigerknows.view.user.User;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * present a poi record.
 * 
 * @author xuehu
 * @version
 * @see Change log:
 */
public class POI extends BaseData {
    
    static final String TAG = "POI";
    
    // 0x01 x_string    uuid,全局唯一 
    public static final byte FIELD_UUID = 0x01;
    
    // 0x02 x_int poi类型（若无则写入类型0，即全部类型），可能的值见这里
    public static final byte FIELD_TYPE = 0x02;

    // 0x03 x_int poi所在经度, 是普通的度数 × 10万
    public static final byte FIELD_LONGITUDE = 0x03;

    // 0x04 x_int poi所在纬度, 是普通的度数 × 10万
    public static final byte FIELD_LATITUDE = 0x04;

    // 0x05 x_string poi名称
    public static final byte FIELD_NAME = 0x05;

    // 0x06 x_map poi 描述 ， 描述的结构见下面的desc（x_map)
    public static final byte FIELD_DESCRIPTION = 0x06;
    
    public static class Description {
        public static List<Byte> KEY_LIST = Arrays.asList(Description.FIELD_GRADE, Description.FIELD_BOX_SIZE, Description.FIELD_BOX_NUMBER,
                Description.FIELD_PRODUCT, Description.FIELD_COOKING_STYLE, Description.FIELD_HOUSING_PRICE,
                Description.FIELD_SERVICE, Description.FIELD_ENVIRONMENT, Description.FIELD_SYNOPSIS,
                Description.FIELD_GUEST_CAPACITY, Description.FIELD_TASTE, Description.FIELD_CATEGORY,
                Description.FIELD_PER_CAPITA, Description.FIELD_MOODS, Description.FIELD_FEATURE,
                Description.FIELD_RECOMMEND, Description.FIELD_RECOMMEND_COOK, Description.FIELD_STAR,
                Description.FIELD_LINE, Description.FIELD_BUSINESS_AREA, Description.FIELD_HAVE_PARK,
                Description.FIELD_DISCOUNT, Description.FIELD_TRAFFIC_DESCRIPTION, Description.FIELD_FILM_REVIEW,
                Description.FIELD_MEDICAL_TREATMENT_LEVEL, Description.FIELD_SERVICE_ATTITUDE, Description.FIELD_PRICE_LEVEL,
                Description.FIELD_HOSPITAL_KIND, Description.FIELD_HOSPITAL_LEVEL, Description.FIELD_HOSPITAL_TYPE,
                Description.FIELD_DEPARTMENT_NUMBER, Description.FIELD_MEDICAL_NUMBER, Description.FIELD_WHETHER_MEDICAL_INSURANCE,
                Description.FIELD_ADVANCE_EQUIPMENT, Description.FIELD_HONOR_REWARDED, Description.FIELD_ATTRACTIONS_COMMENT,
                Description.FIELD_NATIONAL_CERTIFICATION, Description.FIELD_TOUR_DATE, Description.FIELD_TOUR_LIKE,
                Description.FIELD_HUMAN_LANDSCAPE, Description.FIELD_SCENERY, Description.FIELD_OFFICIAL_WEBSITE,
                Description.FIELD_DETAIL, Description.FIELD_BUSINESS_HOURS, Description.FIELD_CINEMA_FEATURE,
                Description.FIELD_PREFERENCES_INFO, Description.FIELD_MEMBER_POLICY, Description.FIELD_FILM_EFFECT,
                Description.FIELD_SERVICE_QUALITY, Description.FIELD_RECOMMEND_SCENERY, Description.FIELD_POPEDOM_SCENERY,
                Description.FIELD_NEARBY_INFO, Description.FIELD_COMPANY_WEB, Description.FIELD_COMPANY_TYPE,
                Description.FIELD_COMPANY_SCOPE, Description.FIELD_INDUSTRY_INFO, Description.FIELD_FEATURE_SPECIALTY,
                Description.FIELD_PRODUCT_ATTITUDE);
                
        public static String[] Name_List = null;
        
        // 0x01 x_int 评分，取值范围0-10
        public static final byte FIELD_GRADE = 0x01;

        // 0x02 x_string 包间大小
        public static final byte FIELD_BOX_SIZE = 0x02;

        // 0x03 x_string 包间数
        public static final byte FIELD_BOX_NUMBER = 0x03;

        // 0x04 x_string 产品
        public static final byte FIELD_PRODUCT = 0x04;

        // 0x05 x_array<x_string> 菜系,每项是一个x_string
        public static final byte FIELD_COOKING_STYLE = 0x05;

        // 0x06 x_string 房价
        public static final byte FIELD_HOUSING_PRICE = 0x06;

        // 0x07 x_string 服务
        public static final byte FIELD_SERVICE = 0x07;

        // 0x08 x_string 环境
        public static final byte FIELD_ENVIRONMENT = 0x08;

        // 0x09 x_string 简介
        public static final byte FIELD_SYNOPSIS = 0x09;

        // 0x0a x_string 客容量
        public static final byte FIELD_GUEST_CAPACITY = 0x0a;

        // 0x0b x_string 口味
        public static final byte FIELD_TASTE = 0x0b;

        // 0x0c x_array<x_string> 类别, 每项是一个0x0c
        public static final byte FIELD_CATEGORY = 0x0c;

        // 0x0d x_int 人均
        public static final byte FIELD_PER_CAPITA = 0x0d;

        // 0x0e x_string 人气
        public static final byte FIELD_MOODS = 0x0e;

        // 0x0f x_array<x_string> 氛围    fenwei 
        public static final byte FIELD_FEATURE = 0x0f;

        // 0x10 x_array<x_string> 推荐, 每项是一个x_string
        public static final byte FIELD_RECOMMEND = 0x10;

        // 0x11 x_array<x_string> 推荐菜, 每项是一个x_string
        public static final byte FIELD_RECOMMEND_COOK = 0x11;

        // 0x12 x_string 星级
        public static final byte FIELD_STAR = 0x12;

        // 0x13 x_array<x_string> 线路，每项是一个x_string
        public static final byte FIELD_LINE = 0x13;

        // 0x14 x_string 营业面积
        public static final byte FIELD_BUSINESS_AREA = 0x14;

        // 0x15 x_string 有停车场
        public static final byte FIELD_HAVE_PARK = 0x15;

        // 0x16 x_string 折扣
        public static final byte FIELD_DISCOUNT = 0x16;

        // 0x17 x_string 交通描述
        public static final byte FIELD_TRAFFIC_DESCRIPTION = 0x17;

        // 0x18 x_string 观影评价
        public static final byte FIELD_FILM_REVIEW = 0x18;

        // 0x19 x_string 医疗水平
        public static final byte FIELD_MEDICAL_TREATMENT_LEVEL = 0x19;

        // 0x1a x_string 服务评价
        public static final byte FIELD_SERVICE_ATTITUDE = 0x1a;

        // 0x1b x_string 价格评价
        public static final byte FIELD_PRICE_LEVEL = 0x1b;

        // 0x1c x_string 医院性质
        public static final byte FIELD_HOSPITAL_KIND = 0x1c;

        // 0x1d x_string 医院等级
        public static final byte FIELD_HOSPITAL_LEVEL = 0x1d;

        // 0x1e x_string 医院类型
        public static final byte FIELD_HOSPITAL_TYPE = 0x1e;

        // 0x1f x_string 科室数量
        public static final byte FIELD_DEPARTMENT_NUMBER = 0x1f;

        // 0x20 x_string 医护人数
        public static final byte FIELD_MEDICAL_NUMBER = 0x20;

        // 0x21 x_string 是否医保
        public static final byte FIELD_WHETHER_MEDICAL_INSURANCE = 0x21;

        // 0x22 x_string 先进设备
        public static final byte FIELD_ADVANCE_EQUIPMENT = 0x22;

        // 0x23 x_string 所获荣誉
        public static final byte FIELD_HONOR_REWARDED = 0x23;

        // 0x24 x_string 景点点评
        public static final byte FIELD_ATTRACTIONS_COMMENT = 0x24;

        // 0x25 x_string 国家认证
        public static final byte FIELD_NATIONAL_CERTIFICATION = 0x25;

        // 0x26 x_string 最佳旅游时间
        public static final byte FIELD_TOUR_DATE = 0x26;

        // 0x27 x_string 旅游喜好
        public static final byte FIELD_TOUR_LIKE = 0x27;

        // 0x28 x_string 人文景观
        public static final byte FIELD_HUMAN_LANDSCAPE = 0x28;

        // 0x29 x_string 自然风光
        public static final byte FIELD_SCENERY = 0x29;

        // 0x2a x_string 官方网站
        public static final byte FIELD_OFFICIAL_WEBSITE = 0x2a;

        // 0x2b x_string 详情
        public static final byte FIELD_DETAIL = 0x2b;

        // 0x2c x_string    营业时间 
        public static final byte FIELD_BUSINESS_HOURS = 0x2c;

        // 0x2d x_string    影院特色 
        public static final byte FIELD_CINEMA_FEATURE = 0x2d;

        // 0x2e x_string    优惠信息 
        public static final byte FIELD_PREFERENCES_INFO = 0x2e;

        // 0x2f x_string    会员政策 
        public static final byte FIELD_MEMBER_POLICY = 0x2f;

        // 0x30 x_string    观影效果 
        public static final byte FIELD_FILM_EFFECT = 0x30;

        // 0x31 x_string    服务品质 
        public static final byte FIELD_SERVICE_QUALITY = 0x31;

        // 0x32 x_string    推荐景点 
        public static final byte FIELD_RECOMMEND_SCENERY = 0x32;

        // 0x33 x_string    辖内景点 
        public static final byte FIELD_POPEDOM_SCENERY = 0x33;

        // 0x34 x_string    周边信息 
        public static final byte FIELD_NEARBY_INFO = 0x34;

        // 0x35 x_string    公司主页 
        public static final byte FIELD_COMPANY_WEB = 0x35;

        // 0x36 x_string    公司类型 
        public static final byte FIELD_COMPANY_TYPE = 0x36;

        // 0x37 x_string    公司规模 
        public static final byte FIELD_COMPANY_SCOPE = 0x37;

        // 0x38 x_string    产业简介 
        public static final byte FIELD_INDUSTRY_INFO = 0x38;

        // 0x39 x_string    特色专科 
        public static final byte FIELD_FEATURE_SPECIALTY = 0x39;

        // 0x3a x_string    产品评价
        public static final byte FIELD_PRODUCT_ATTITUDE = 0x3a;
    }

    // 0x07 x_string 电话
    public static final byte FIELD_TELEPHONE = 0x07;

    // 0x08 x_string 预定电话
    public static final byte FIELD_RESERVE_TEL = 0x08;

    // 0x09 x_string 地址
    public static final byte FIELD_ADDRESS = 0x09;

    // 0x0a x_string 链接地址
    public static final byte FIELD_URL = 0x0a;
    
    // 0x0b x_int 距中心点距离（单位为米）
    public static final byte FIELD_TO_CENTER_DISTANCE = 0x0b;

    // 0x12 x_int   点评模式 
    public static final byte FIELD_COMMENT_PATTERN = 0x12;
    
    // 0x13 x_int   poi属性, 32bits代表不同状态，bits[0]代表该登录用户点评过本poi，此时bits[0]==1，否则为0.bits[1]代表该客户端匿名点评过本poi，取值规则同bits[0].其余bits为预留
    public static final byte FIELD_ATTRIBUTE = 0x13;
    
    // 0x14 x_int   status，> 0 代表poi有效，< 0 代表poi当前已失效， == 0 保留 
    public static final byte FIELD_STATUS = 0x14;
    
    // 0x15 x_array<x_map>  动态poi摘要，最多给10条 
    public static final byte FIELD_DYNAMIC_POI = 0x15;
    
    // 0x16 x_map  最近的一条点评 
    public static final byte FIELD_LAST_COMMENT = 0x16;
    
    public static class DynamicPOI extends XMapData {
        // 0x01 x_int 动态poi的类型
        public static final byte FIELD_TYPE = 0x01;
        
        // 0x02 x_string    主动态poi的uid，masterUid
        public static final byte FIELD_MASTER_UID = 0x02;
        
        // 0x03 x_string 动态poi的摘要信息，ps：需进一步定义
        public static final byte FIELD_SUMMARY = 0x03;

        // 0x04 x_string 从动态poi的uid，slaveUid
        public static final byte FIELD_SLAVE_UID = 0x04;

        private long type;
        private String masterUid;
        private String summary;
        private String slaveUid;
        
        public DynamicPOI(XMap data) throws APIException {
            super(data);
            
            if (data.containsKey(FIELD_TYPE)) {
                type = data.getInt(FIELD_TYPE);
            }
            
            if (data.containsKey(FIELD_MASTER_UID)) {
                masterUid = data.getString(FIELD_MASTER_UID);
            }
            
            if (data.containsKey(FIELD_SUMMARY)) {
                summary = data.getString(FIELD_SUMMARY);
            }
            
            if (data.containsKey(FIELD_SLAVE_UID)) {
                slaveUid = data.getString(FIELD_SLAVE_UID);
            }
        }

        public String getType() {
            return String.valueOf(type);
        }

        public String getMasterUid() {
            return masterUid;
        }

        public String getSummary() {
            return summary;
        }

        public String getSlaveUid() {
            return slaveUid;
        }
        
    }
    
    // 0x14 x_int   status，> 0 代表poi有效，< 0 代表poi当前已失效， == 0 保留
    public static final int STATUS_NONE = 0;
    public static final int STATUS_VALID = 1;
    public static final int STATUS_INVALID = -1;
    
    // 0x13 x_int   poi属性, 32bits代表不同状态，bits[0]代表该登录用户点评过本poi，此时bits[0]==1，否则为0.bits[1]代表该客户端匿名点评过本poi，取值规则同bits[0].其余bits为预留
    public static final int ATTRIBUTE_COMMENT_USER = 0x1;
    public static final int ATTRIBUTE_COMMENT_ANONYMOUS = 0x2;

    // 0x12 x_int   点评模式 
    public static final int COMMENT_PATTERN_FOOD = 1;
    public static final int COMMENT_PATTERN_HOTEL = 2;
    public static final int COMMENT_PATTERN_CINEMA = 3;
    public static final int COMMENT_PATTERN_HOSPITAL = 4;
    public static final int COMMENT_PATTERN_BUY = 5;
    public static final int COMMENT_PATTERN_OTHER = 6;

    public static final int SOURCE_TYPE_NORMAL_POI = 0;

    public static final int SOURCE_TYPE_MY_LOCATION = 1;

    public static final int SOURCE_TYPE_MAP_CENTER = 2;

    public static final int SOURCE_TYPE_SELECT_POINT = 3;

    public static final int SOURCE_TYPE_CITY_CENTER = 4;

    public static final int SOURCE_TYPE_OTHER = 5;

    public static final int SOURCE_TYPE_TUANGOU = 6;

    public static final int SOURCE_TYPE_FENDIAN = 7;

    public static final int SOURCE_TYPE_DIANYING = 8;

    public static final int SOURCE_TYPE_YINGXUN = 9;

    public static final int SOURCE_TYPE_YANCHU = 10;

    public static final int SOURCE_TYPE_ZHANLAN = 11;

    public static final int SOURCE_TYPE_CLICK_SELECT_POINT = 12;

    public static final int SOURCE_TYPE_LONG_CLICK_SELECT_POINT = 13;

    public static final int FROM_ONLINE = 0;

    public static final int FROM_LOCAL = 1;
    
    public static final String NEED_FILELD = "0102030405060708090a0b1213141516";
    
    private String uuid;
    
    private int type;
    
    private Position position = null;

    private String name;
    
    private String alise;

    private XMap description;

    private String telephone;

    private String reserveTel;

    private String address = null;

    private String url = null;
    
    private String toCenterDistance;
    
    private long commentPattern;
    
    private long attribute;
    
    private long status = STATUS_NONE;
    
    private List<DynamicPOI> dynamicPOIList = new ArrayList<DynamicPOI>();
    
    private DataQuery commentQuery = null;
    
    private int resultType = 0;
    
    private int sourceType = 0;
    
    private int grade;
    
    private int from = FROM_ONLINE;
    
    // 菜系
    private String cookingStyle;
    
    private int perCapity = -1;
    
    private String recommendCook;
    
    private String feature;
    
    // 口味 FIELD_TASTE
    private String taste;
    
    // 服务 FIELD_SERVICE|FIELD_SERVICE_ATTITUDE|FIELD_SERVICE_QUALITY
    private String service;
    
    // 环境 FIELD_ENVIRONMENT
    private String envrionment;
    
    // 产品 FIELD_PRODUCT|FIELD_PRODUCT_ATTITUDE
    private String product;
    
    private boolean onlyAPOI = false;
    
    private Comment myComment = null;
    
    private Comment lastComment;
    
    public int ciytId = 0;
    
    public void updateData(Context context, XMap data) {
        try {
            BaseData baseData = checkStore(context, storeType, -1, false);
            init(data);
            if (baseData != null) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(Tigerknows.POI.DATA, ByteUtil.xobjectToByte(data));
                    this.dateTime = System.currentTimeMillis();
                    values.put(Tigerknows.POI.DATETIME, this.dateTime);
                    SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, baseData.id), values, null, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                writeToDatabases(context, -1, com.tigerknows.provider.Tigerknows.STORE_TYPE_HISTORY);
            }
        } catch (APIException e1) {
            e1.printStackTrace();
        }
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public Comment getMyComment() {
        return myComment;
    }

    public void setMyComment(Comment myComment) {
        this.myComment = myComment;
        this.attribute = Comment.isAuthorMe(myComment);
        XMap data = getData();
        if (data != null) {
            data.put(FIELD_ATTRIBUTE, this.attribute);
        }
    }

    public Comment getLastComment() {
        return this.lastComment;
    }
    
    public void setLastComment(Comment lastComment) {
        this.lastComment = lastComment;
    }
    
    public boolean isOnlyAPOI() {
        return onlyAPOI;
    }

    public void setOnlyAPOI(boolean onlyAPOI) {
        this.onlyAPOI = onlyAPOI;
    }

    public void setSourceType(int sourceType) {
        this.sourceType = sourceType;
    }

    public int getSourceType() {
        return sourceType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public int getResultType() {
        return resultType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    public void setCommentQuery(DataQuery commentQuery) {
        this.commentQuery = commentQuery;
    }
    
    public DataQuery getCommentQuery() {
        return this.commentQuery;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlise() {
        return alise;
    }

    public void setAlise(String alise) {
        this.alise = alise;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getTelephone() {
        return telephone;
    }
    
    public XMap getXDescription() {
        return description;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public int getType() {
        return type;
    }
    
    public String getToCenterDistance() {
        return toCenterDistance;
    }
    
    public int getGrade() {
        return grade;
    }
    
    public String getCookingStyle() {
        return cookingStyle;
    }
    
    public int getPerCapity() {
        return perCapity;
    }
    
    public String getRecommendCook() {
        return recommendCook;
    }
    
    public String getFeature() {
        return feature;
    }
    
    public String getTaste() {
    	return taste;
    }
    
    public String getService() {
    	return service;
    }

    public String getEnvironment() {
    	return envrionment;
    }
    
    public String getProduct() {
    	return product;
    }
    /**
     * 设置POI类型.如果类型不合法，设为全部类型.
     * 
     * @param type
     */
    public void setType(int type) {
        if (type < 0 || type > 19)
            type = 0;

        this.type = type;
    }
    
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
    
    public String getUUID() {
        return uuid;
    }

    public long getCommentPattern() {
        return commentPattern;
    }

    public void setCommentPattern(long commentPattern) {
        this.commentPattern = commentPattern;
    }

    public long getAttribute() {
        return attribute;
    }

    public long getStatus() {
        return status;
    }

    public void setAttribute(long attribute) {
        this.attribute = attribute;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public List<DynamicPOI> getDynamicPOIList() {
        return dynamicPOIList;
    }

    public POI() {
    }

    public POI (XMap data) throws APIException {
        super(data);
        init(data);
    }
    
    @SuppressWarnings("unchecked")
    public void init(XMap data) throws APIException {
        super.init(data);

        if (this.data.containsKey(FIELD_UUID)) {
            this.uuid = this.data.getString(FIELD_UUID);
        }
        if (this.data.containsKey(FIELD_TYPE)) {
            int type = (int)this.data.getInt(FIELD_TYPE);
            if (type < 0) {
                type = 0;
            } else if (type > 10) {
                type = 10;
            }
            this.type = type;
        }
        if (this.data.containsKey(FIELD_LATITUDE) && this.data.containsKey(FIELD_LONGITUDE)) {
            this.position = new Position(((double)this.data.getInt(FIELD_LATITUDE))/TKConfig.LON_LAT_DIVISOR, ((double)this.data.getInt(FIELD_LONGITUDE))/TKConfig.LON_LAT_DIVISOR);
        }
        if (this.data.containsKey(FIELD_NAME)) {
            this.name = this.data.getString(FIELD_NAME);
        }
        if (this.data.containsKey(FIELD_DESCRIPTION)) {
            this.description = this.data.getXMap(FIELD_DESCRIPTION);
            if (this.description != null) {
                if (this.description.containsKey(Description.FIELD_GRADE)) {
                    this.grade = (int)this.description.getInt(Description.FIELD_GRADE);
                }
                if (this.description.containsKey(Description.FIELD_COOKING_STYLE)) {
                    List<String> strs = this.description.getXArray(Description.FIELD_COOKING_STYLE).toStringList();
                    StringBuilder s = new StringBuilder();
                    for(String str : strs) {
                        s.append(",");
                        s.append(str);
                    }
                    if (s.length() > 0) {
                        this.cookingStyle = s.substring(1);
                    }
                }
                if (this.description.containsKey(Description.FIELD_PER_CAPITA)) {
                    this.perCapity = (int) this.description.getInt(Description.FIELD_PER_CAPITA);
                }
                if (this.description.containsKey(Description.FIELD_RECOMMEND_COOK)) {
                    List<String> strs = this.description.getXArray(Description.FIELD_RECOMMEND_COOK).toStringList();
                    StringBuilder s = new StringBuilder();
                    for(String str : strs) {
                        s.append(",");
                        s.append(str);
                    }
                    if (s.length() > 0) {
                        this.recommendCook = s.substring(1);
                    }
                }
                if (this.description.containsKey(Description.FIELD_FEATURE)) {
                    List<String> strs = this.description.getXArray(Description.FIELD_FEATURE).toStringList();
                    StringBuilder s = new StringBuilder();
                    for(String str : strs) {
                        s.append(",");
                        s.append(str);
                    }
                    if (s.length() > 0) {
                        this.feature = s.substring(1);
                    }
                }
                
                if (this.description.containsKey(Description.FIELD_TASTE)) {
                	this.taste = this.description.getString(Description.FIELD_TASTE);
                }
                
                if (this.description.containsKey(Description.FIELD_SERVICE_ATTITUDE)) {
                	this.service = this.description.getString(Description.FIELD_SERVICE_ATTITUDE);
                } else if (this.description.containsKey(Description.FIELD_SERVICE_QUALITY)) {
                    this.service = this.description.getString(Description.FIELD_SERVICE_QUALITY);
                }
                
                if (this.description.containsKey(Description.FIELD_ENVIRONMENT)) {
                	this.envrionment = this.description.getString(Description.FIELD_ENVIRONMENT);
                }
                
                //购物POI中的产品信息，4.30 ALPHA3中暂未添加
                //目前暂无其他代码调用此段信息，仅作为预留
                if (this.description.containsKey(Description.FIELD_PRODUCT)){
                	this.product = this.description.getString(Description.FIELD_PRODUCT);
                } else if(this.description.containsKey(Description.FIELD_PRODUCT_ATTITUDE)){
                	this.product = this.description.getString(Description.FIELD_PRODUCT_ATTITUDE);
                }
            }
        } else {
            this.description = null;
        }
        if (this.data.containsKey(FIELD_TELEPHONE)) {
            this.telephone = this.data.getString(FIELD_TELEPHONE);
        }
        if (this.data.containsKey(FIELD_RESERVE_TEL)) {
            this.reserveTel = this.data.getString(FIELD_RESERVE_TEL);
        }
        if (this.data.containsKey(FIELD_ADDRESS)) {
            this.address = this.data.getString(FIELD_ADDRESS);
        }
        if (this.data.containsKey(FIELD_URL)) {
            this.url = this.data.getString(FIELD_URL);
        }
        if (this.data.containsKey(FIELD_TO_CENTER_DISTANCE)) {
            this.toCenterDistance = this.data.getString(FIELD_TO_CENTER_DISTANCE);
        }
        if (this.data.containsKey(FIELD_COMMENT_PATTERN)) {
            this.commentPattern = this.data.getInt(FIELD_COMMENT_PATTERN);
        }
        if (this.data.containsKey(FIELD_ATTRIBUTE)) {
            this.attribute = this.data.getInt(FIELD_ATTRIBUTE);
        }
        if (this.data.containsKey(FIELD_STATUS)) {
            this.status = this.data.getInt(FIELD_STATUS);
        }
        dynamicPOIList.clear();
        if (this.data.containsKey(FIELD_DYNAMIC_POI)) {
            XArray<XMap> xarray = data.getXArray(FIELD_DYNAMIC_POI);
            if (xarray != null) {
                for(int i = 0; i < xarray.size(); i++) {
                    dynamicPOIList.add(new DynamicPOI(xarray.get(i)));
                }
            }
        }
        if (this.data.containsKey(FIELD_LAST_COMMENT)) {
            this.lastComment = new Comment(this.data.getXMap(FIELD_LAST_COMMENT));
        } else {
            this.lastComment = null;
        }
    }
    
    public XMap getData() {
        if (this.data == null) {
            this.data = new XMap();

            if (this.uuid != null) {
                this.data.put(FIELD_UUID, this.uuid);
            }
            
            this.data.put(FIELD_TYPE, this.type);
            
            if (this.position != null) {
                this.data.put(FIELD_LONGITUDE, (int)(this.position.getLon()*TKConfig.LON_LAT_DIVISOR));
                this.data.put(FIELD_LATITUDE, (int)(this.position.getLat()*TKConfig.LON_LAT_DIVISOR));
            }
            
            if (!TextUtils.isEmpty(this.name)) {
                this.data.put(FIELD_NAME, this.name);
            }
            
            if (description != null) {
                this.data.put(FIELD_DESCRIPTION, description);
            }
            if (!TextUtils.isEmpty(this.telephone)) {
                this.data.put(FIELD_TELEPHONE, this.telephone);
            }
            if (!TextUtils.isEmpty(this.reserveTel)) {
                this.data.put(FIELD_RESERVE_TEL, this.reserveTel);
            }
            if (!TextUtils.isEmpty(this.address)) {
                this.data.put(FIELD_ADDRESS, this.address);
            }
            if (!TextUtils.isEmpty(this.url)) {
                this.data.put(FIELD_URL, this.url);
            }
            this.data.put(FIELD_TO_CENTER_DISTANCE, this.toCenterDistance);
            this.data.put(FIELD_COMMENT_PATTERN, this.commentPattern);
            this.data.put(FIELD_ATTRIBUTE, this.attribute);
            this.data.put(FIELD_STATUS, this.status);
            if (lastComment != null) {
                this.data.put(FIELD_LAST_COMMENT, lastComment.getData());
            }
            if (dynamicPOIList.size() > 0) {
                XArray<XMap> xarray = new XArray<XMap>();
                for(int i = 0, size = dynamicPOIList.size(); i < size; i++) {
                    xarray.add(dynamicPOIList.get(i).getData());
                }
                this.data.put(FIELD_DYNAMIC_POI, xarray);
            }
        }
        return this.data;
    }

    public String getSMSString(Context context) {
        StringBuilder body = new StringBuilder();

        if (!TextUtils.isEmpty(name)) {
            body.append(name);
            body.append("\n");
        }
        if (!TextUtils.isEmpty(address)) {
            body.append(address);
            body.append("\n");
        }
        if (!TextUtils.isEmpty(telephone)) {
            body.append(telephone);
            body.append("\n");
        }
        body.append(getDescription(context));
        return body.toString();
    }
    
    /**
     * 获取POI描述的某一字段名称
     * 
     * @param context
     * @param showKey
     * @return
     */
    public String getDescriptionName(Context context, byte showKey) {    	
        if (Description.Name_List == null) {
            Description.Name_List = context.getResources().getStringArray(R.array.poi_xdescription_name);
        }
    	
    	String showName = Description.Name_List[Description.KEY_LIST.indexOf(showKey)];
    	return showName+" ";
    }
    
    /**
     * 获取POI描述的某一字段返回值
     * 
     * @param context
     * @param showKey
     * @return
     */
    public String getDescriptionValue(byte showKey) {
    	String str = "";
        if (description != null) {
            return xdescriptionToString(showKey);
        }
        return str;
    }
    /**
     * 获取POI描述的某一字段13版返回值
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    private String xdescriptionToString(byte key) {
        if (description == null) {
            return null;
        }

        StringBuilder s = new StringBuilder();

        if (description.containsKey(key)) {
            if (description.getXObject(key) instanceof XArray) {
                List<String> strs = description.getXArray(key).toStringList();
                for(String str : strs) {
                    if (s.length() > 0) {
                        s.append(' ');
                    }
                    s.append(str);
                }
            } else {
                s.append(description.getXObject(key));
            }
        }
        
        return s.toString();
    }
    
    public String getCategory() {
        
        StringBuilder s = new StringBuilder();
        
        byte[] showKeys = {Description.FIELD_COOKING_STYLE, Description.FIELD_STAR, Description.FIELD_CATEGORY, Description.FIELD_HOSPITAL_LEVEL, Description.FIELD_NATIONAL_CERTIFICATION};
        
        for(int i = 0; i < showKeys.length; i++) {
            
            byte key = showKeys[i];
            String value = getDescriptionValue(key);
            
            if(!TextUtils.isEmpty(value)) {
                
                if (!TextUtils.isEmpty(s)) {
                    s.append(' ');
                }
                
                s.append(value);
                
            }
        }
        
        return s.toString();
    }
    
    @SuppressWarnings("unchecked")
    public String getDescription(Context context) {
        StringBuilder s = new StringBuilder();
        if (description == null) {
            return s.toString();
        }
        
        byte[] filedKey = {
                Description.FIELD_GRADE, Description.FIELD_BOX_SIZE, Description.FIELD_BOX_NUMBER,
                Description.FIELD_PRODUCT, Description.FIELD_COOKING_STYLE, Description.FIELD_HOUSING_PRICE,
                Description.FIELD_SERVICE, Description.FIELD_ENVIRONMENT, Description.FIELD_SYNOPSIS,
                Description.FIELD_GUEST_CAPACITY, Description.FIELD_TASTE, Description.FIELD_CATEGORY,
                Description.FIELD_PER_CAPITA, Description.FIELD_MOODS, Description.FIELD_FEATURE,
                Description.FIELD_RECOMMEND, Description.FIELD_RECOMMEND_COOK, Description.FIELD_STAR,
                Description.FIELD_LINE, Description.FIELD_BUSINESS_AREA, Description.FIELD_HAVE_PARK,
                Description.FIELD_DISCOUNT, Description.FIELD_TRAFFIC_DESCRIPTION, Description.FIELD_FILM_REVIEW,
                Description.FIELD_MEDICAL_TREATMENT_LEVEL, Description.FIELD_SERVICE_ATTITUDE, Description.FIELD_PRICE_LEVEL,
                Description.FIELD_HOSPITAL_KIND, Description.FIELD_HOSPITAL_LEVEL, Description.FIELD_HOSPITAL_TYPE,
                Description.FIELD_DEPARTMENT_NUMBER, Description.FIELD_MEDICAL_NUMBER, Description.FIELD_WHETHER_MEDICAL_INSURANCE,
                Description.FIELD_ADVANCE_EQUIPMENT, Description.FIELD_HONOR_REWARDED, Description.FIELD_ATTRACTIONS_COMMENT,
                Description.FIELD_NATIONAL_CERTIFICATION, Description.FIELD_TOUR_DATE, Description.FIELD_TOUR_LIKE,
                Description.FIELD_HUMAN_LANDSCAPE, Description.FIELD_SCENERY, Description.FIELD_OFFICIAL_WEBSITE,
                Description.FIELD_DETAIL
        };
        
        String[] filedName = context.getResources().getStringArray(R.array.poi_xdescription_name);
        byte key;
        for(int i = 0, size = filedKey.length; i < size; i++) {
            key = filedKey[i];
            if (description.containsKey(key)) {
                s.append(filedName[i]);
                if (description.getXObject(key) instanceof XArray) {
                    List<String> strs = description.getXArray(key).toStringList();
                    for(String str : strs) {
                        s.append(str);
                        s.append(",");
                    }
                } else {
                    s.append(description.getXObject(key));
                }
                s.append("\n");
            }
        }
        
        return s.toString();
    }
    
    /**
     * 将POI信息写入数据库
     * 
     * @param favoriteType
     */
    public Uri writeToDatabases(Context context, long parentId, int storeType) {
        this.storeType = storeType;
        this.parentId = parentId;
        ContentValues values = new ContentValues();
        boolean availably = initContetValues(this, values);

        Uri uri = null;
        if (availably) {
            uri = SqliteWrapper.insert(context, context.getContentResolver(),
                    Tigerknows.POI.CONTENT_URI, values);
            id = Long.parseLong(uri.getPathSegments().get(1));
        }
        return uri;
    }
    
    private static boolean initContetValues(POI poi, ContentValues values) {
        if (poi == null || values == null) {
            return false;
        }
        values.put(Tigerknows.POI.STORE_TYPE, poi.storeType);
        values.put(Tigerknows.POI.PARENT_ID, poi.parentId);
        if (!TextUtils.isEmpty(poi.name)) {
            values.put(Tigerknows.POI.POI_NAME, poi.name);
        } else {
            return false;
        }
        if (TextUtils.isEmpty(poi.uuid) && (poi.storeType == Tigerknows.STORE_TYPE_FAVORITE || poi.storeType == Tigerknows.STORE_TYPE_HISTORY)) {
            return false;
        }
        if (!TextUtils.isEmpty(poi.alise)) {
            values.put(Tigerknows.POI.ALIAS, poi.alise);
        }
        
        if (poi.position != null) {
            values.put(Tigerknows.POI.POI_X, poi.position.getLon());
            values.put(Tigerknows.POI.POI_Y, poi.position.getLat());
        } else {
            return false;
        }
        XMap xmap = poi.getData();
        if (xmap != null) {
            try {
                values.put(Tigerknows.POI.DATA, ByteUtil.xobjectToByte(xmap));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        values.put(Tigerknows.POI.DATETIME, System.currentTimeMillis());
        values.put(Tigerknows.POI.POI_VERSION, DataQuery.VERSION);
        return true;
    }
    
    /**
     * 从数据库中读取POI
     * 
     * @param id
     */
    public static POI readFormDatabases(Context context, long id) {
        POI poi = null;
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), ContentUris
                .withAppendedId(Tigerknows.POI.CONTENT_URI, id), null, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                poi = readFormCursor(context, c);
            }
            c.close();
        }
        return poi;
    }

    public static List<POI> readFormDatabases(Context context, long parentId, int storeType,
            String sortOrder) {
        List<POI> list = new ArrayList<POI>();
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                Tigerknows.POI.CONTENT_URI, null, "(" + Tigerknows.POI.PARENT_ID + "=" + parentId
                        + ") AND (" + Tigerknows.POI.STORE_TYPE + "=" + storeType + ")", null,
                sortOrder);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                for (int i = 0; i < c.getCount(); i++) {
                    list.add(readFormCursor(context, c));
                    c.moveToNext();
                }
            }
            c.close();
        }
        return list;
    }

    public static POI readFormCursor(Context context, Cursor cursor) {
        POI poi = new POI();
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                poi.id = cursor.getLong(cursor.getColumnIndex(Tigerknows.POI._ID));
                poi.storeType = cursor.getInt(cursor.getColumnIndex(Tigerknows.POI.STORE_TYPE));
                poi.parentId = cursor.getLong(cursor.getColumnIndex(Tigerknows.POI.PARENT_ID));
                String str = cursor.getString(cursor.getColumnIndex(Tigerknows.POI.POI_NAME));
                if (!TextUtils.isEmpty(str)) {
                    poi.setName(str);
                }
                str = cursor.getString(cursor.getColumnIndex(Tigerknows.POI.ALIAS));
                if (!TextUtils.isEmpty(str)) {
                    poi.setAlise(str);
                }
                poi.setPosition(new Position(cursor.getDouble(cursor
                        .getColumnIndex(Tigerknows.POI.POI_Y)), cursor.getDouble(cursor
                        .getColumnIndex(Tigerknows.POI.POI_X))));
                
                byte[] dataBytes = null;
                int index = cursor.getColumnIndex(Tigerknows.POI.DATA); 
                if (index > 0) {
                    dataBytes = cursor.getBlob(index);
                }

                int version = cursor.getInt(cursor.getColumnIndex(Tigerknows.POI.POI_VERSION));
                if (version == 13) {
                    if (dataBytes != null && dataBytes.length > 0) {
                        try {
                            poi.init((XMap) ByteUtil.byteToXObject(dataBytes));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                poi.dateTime = cursor.getLong(cursor.getColumnIndex(Tigerknows.POI.DATETIME));
                poi.from = FROM_LOCAL;
                poi.dynamicPOIList.clear();
                poi.toCenterDistance = null;
            }
        }

        return poi;
    }

    @Override
    public int deleteHistory(Context context) {
        int count = 0;
        BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_HISTORY, -1, false);
        if (baseData != null) {
            count = SqliteWrapper.delete(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, baseData.id), null, null);
        }
        return count;
    }

    @Override
    public int deleteFavorite(Context context) {
        int count = 0;
        BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_FAVORITE, -1, false);
        if (baseData != null) {
            count = SqliteWrapper.delete(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, baseData.id), null, null);
        }
        return count;
    }

    @Override
    public boolean checkHistory(Context context) {
        return checkStore(context, Tigerknows.STORE_TYPE_HISTORY, -1, false) != null;
    }

    @Override
    public boolean checkFavorite(Context context) {
        return checkStore(context, Tigerknows.STORE_TYPE_FAVORITE, -1, false) != null;
    }
    
    public int updateHistory(Context context) {
        int count = 0;
        BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_HISTORY, -1, false);
        if (baseData != null) {
            ContentValues values = new ContentValues();
            this.dateTime = System.currentTimeMillis();
            values.put(Tigerknows.POI.DATETIME, this.dateTime);
            count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, baseData.id), values, null, null);
        } else {
            try {
                init(getData());
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            writeToDatabases(context, -1, Tigerknows.STORE_TYPE_HISTORY);
        }
        return count;
    }
    
    public int update(Context context, int storyType) {
        int count = -1;
        BaseData baseData = checkStore(context, storyType, -1, false);
        if (baseData != null) {
            ContentValues values = new ContentValues();
            boolean availably = initContetValues((POI) baseData, values);
            if (availably) {
                count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, baseData.id), values, null, null);
            }
        } else {
            writeToDatabases(context, -1, storyType);
        }
        return count;
    }
    
    public int updateAlias(Context context) {
        int count = 0;
        ContentValues values = new ContentValues();
        values.put(Tigerknows.POI.ALIAS, this.alise);
        count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, id), values, null, null);
        return count;
    }

    public BaseData checkStore(Context context, int store_Type, long parentId, boolean onlyCheckId) {
        BaseData baseData = null;
        StringBuilder s = new StringBuilder();
        if (id > 0 && onlyCheckId) {
            s.append("(");
            s.append(com.tigerknows.provider.Tigerknows.POI._ID);
            s.append("=");
            s.append(id);
            s.append(")");
        } else {
            s.append("(");
            s.append(Tigerknows.POI.STORE_TYPE);
            s.append("=");
            s.append(store_Type);
            s.append(")");
            s.append(" AND (");
            s.append(com.tigerknows.provider.Tigerknows.POI.POI_NAME);
            s.append("='");
            s.append(name.replace("'", "''"));
            s.append("')");
            // sqlite 比较real型数据类型会失误连连
            /*
             * s.append(" AND (");
             * s.append(com.tigerknows.provider.Tigerknows.POI.POI_X);
             * s.append("=="); 
             * s.append(poi.x); 
             * s.append(")");
             * s.append(" AND (");
             * s.append(com.tigerknows.provider.Tigerknows.POI.POI_Y);
             * s.append("=="); 
             * s.append(poi.y); 
             * s.append(")");
             */
            if (parentId > 0) {
                s.append(" AND (");
                s.append(com.tigerknows.provider.Tigerknows.POI.PARENT_ID);
                s.append("=");
                s.append(parentId);
                s.append(")");
            }
        }
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                Tigerknows.POI.CONTENT_URI, null, s.toString(), null, null);
        int count;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                c.moveToFirst();
                for(int i = 0; i < count; i++) {
                    POI other = readFormCursor(context, c);
                    if((null != other && !other.equals(this)) || (null == other && other != this)) {
                    } else {
                        baseData = other;
                        break;
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }
        return baseData;
    }
    
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof POI) {
            POI other = (POI) object;
//            XMap otherData = other.data;
//            if((null != otherData && !otherData.equals(data)) || (null == otherData && otherData != data)) {
//                return false;
            if((null != other.name && !other.name.equals(this.name)) || (null == other.name && other.name != this.name)) {
                return false;
            } else if((null != other.position && !other.position.equals(this.position)) || (null == other.position && other.position != this.position)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }

    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 29 * hash + (this.position != null ? this.position.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        // return name+"\n"+position.toString()+"\n"+phoneNumber;
        return (name == null ? "" : name) + "\n" + (position == null ? "" : position.toString())
                + "\n" + (telephone == null ? "" : telephone);
    }

    public Bundle toBundle(String packageName) {
        Bundle bundle = new Bundle();
        bundle.putString(packageName + ".name", name);
        bundle.putString(packageName + ".phoneNumber", telephone);
        bundle.putString(packageName + ".address", address);
        bundle.putString(packageName + ".position", position.toString());
        return bundle;

    }
    
    /*
     * 比较两个POI点名字是否相同
     */
    public static boolean isNameEqual(POI aPOI, POI bPOI) {
    	
		if (aPOI == null || bPOI == null)
			return false;
		
		if ((aPOI.getName().equals(bPOI.getName()))) {
			return true;
		} 
		
		return false;
	}
    
    /*
     * 比较两个POI点经纬度是否相同
     */
    public static boolean isPositionEqual(POI aPOI, POI bPOI) {
    	
		if (aPOI == null || bPOI == null)
			return false;
		
		if (aPOI.getPosition()!=null && aPOI.getPosition().equals(bPOI.getPosition())){
			return true;
		}
		
		return false;
	}
    
    /**
     * 克隆一个POI对象。
     * Notice: 当前仅克隆了名字及经纬度
     */
    public POI clone() {
    	POI newPOI = new POI();
    	newPOI.setName(this.name);
    	newPOI.setPosition(this.position);
    	
    	return newPOI;
    }
    
    /**
     * 根据当前用户登录状态，刷新金/银戳状态
     * @return
     */
    long refreshAttribute() {
        User user = Globals.g_User;
        if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0 && user != null) {
            attribute = POI.ATTRIBUTE_COMMENT_USER;
        } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0 && user == null) {
            attribute = POI.ATTRIBUTE_COMMENT_ANONYMOUS;
        }
        return attribute;
    }
    
    /**
     * 检查是否为金戳，注册用户点评过此POI
     * @return
     */
    public boolean isGoldStamp() {
        boolean result = false;
        refreshAttribute();
        if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0) {
            result = true;
        }
        
        return result;
    }
    
    /**
     * 检查是否为银戳，非注册用户点评过此POI
     * @return
     */
    public boolean isSilverStamp() {
        boolean result = false;
        refreshAttribute();
        if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
            result = true;
        }
        
        return result;
    }
}
