/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

public class Tuangou extends BaseData {
    
    public static final String NEED_FILELD = "00010405140b0c0d0e10191f20222350565758595b5c";
    
    // 0x00 x_string uid uid
    public static final byte FIELD_UID = 0x00;
    
    // 0x01 x_string 团购名称 name
    public static final byte FIELD_NAME = 0x01;
    
    // 0x02 x_string 团购类型 tag
    public static final byte FIELD_TAG = 0x02;
    
    // 0x03 x_string 城市ID cityId
    public static final byte FIELD_CITY_ID = 0x03;
    
    // 0x04 x_string 团购图片信息 pictures
    public static final byte FIELD_PICTURES = 0x04;
    
    // 0x14 x_map   详情页团购图片信息   pictures 
    public static final byte FIELD_PICTURES_DETAIL = 0x14;
    
    // 0x05 x_string 团购详情 description
    public static final byte FIELD_DESCRIPTION = 0x05;
    
    // 0x09 x_string 开始时间 start_time
    public static final byte FIELD_START_TIME = 0x09;
    
    // 0x0a x_string 结束时间 end_time
    public static final byte FIELD_END_TIME = 0x0a;
    
    // 0x0b x_double 当前价格 price
    public static final byte FIELD_PRICE = 0x0b;
    
    // 0x0c x_double 原价 org_price
    public static final byte FIELD_ORG_PRICE = 0x0c;
    
    // 0x0d x_double 折扣 discount
    public static final byte FIELD_DISCOUNT = 0x0d;
    
    // 0x0f x_string 购买须知 noticed
    public static final byte FIELD_NOTICED = 0x0f;
    
    // 0x10 x_int 购买人数 buyer_num
    public static final byte FIELD_BUYER_NUM = 0x10;
    
    // 0x19 x_string 退款详情 refund
    public static final byte FIELD_REFUND = 0x19;
    
    // 0x1d x_string 团购内容，nf需同时提交0x1d和0x1e，结果优先取0x1d content_text
    public static final byte FIELD_CONTENT_TEXT = 0x1d;
    
    // 0x1e x_string 团购内容图片 content_pic
    public static final byte FIELD_CONTENT_PIC = 0x1e;
    
    // 0x1f x_int 数据来源 source
    public static final byte FIELD_SOURCE = 0x1f;
    
    // 0x20 x_string 团购简要介绍 short_desc
    public static final byte FIELD_SHORT_DESC = 0x20;
    
    // 0x21 x_string 团购截止时间 deadline
    public static final byte FIELD_DEADLINE = 0x21;
    
    // 0x22 x_string 团购方提供的商品id goods_id
    public static final byte FIELD_GOODS_ID = 0x22;
    
    // 0x23 x_int 命中筛选项的分店数量 实时生成
    public static final byte FIELD_BRANCH_NUM = 0x23;

    private String uid; // 0x00 x_string uid uid
    private String name; // 0x01 x_string 团购名称 name
    private String tag; // 0x02 x_string 团购类型 tag
    private String cityId; // 0x03 x_string 城市ID cityId
    private TKDrawable pictures; // 0x04 x_string 团购图片信息 pictures
    private TKDrawable picturesDetail; // 0x14    x_map   详情页团购图片信息   pictures 
    private String description; // 0x05 x_string 团购详情 description
    private String startTime;// 0x09 x_string 开始时间 start_time
    private String endTime;// 0x0a x_string 结束时间 end_time
    private String price;// 0x0b x_double 当前价格 price
    private String orgPrice;// 0x0c x_double 原价 org_price
    private String discount;// 0x0d x_double 折扣 discount
    private String noticed;// 0x0f x_string 购买须知 noticed
    private long buyerNum;// 0x10 x_int 购买人数 buyer_num
    private String refund;// 0x19 x_string 退款详情 refund
    private String contentText;// 0x1d x_string 团购内容，nf需同时提交0x1d和0x1e，结果优先取0x1d content_text
    private TKDrawable contentPic;// 0x1e x_string 团购内容图片 content_pic
    private long source;// 0x1f x_string 数据来源 source
    private String shortDesc;// 0x20 x_string 团购简要介绍 short_desc
    private String deadline;// 0x21 x_string 团购截止时间 deadline
    private String goodsId;// 0x22 x_string 团购方提供的商品id goods_id
    private long branchNum;// 0x23 x_int 命中筛选项的分店数量 实时生成
    
    private Fendian fendian;
    private DataQuery fendianQuery;
    private String filterArea;
    private int orderNumber;
    private POI poi;
    
    public Tuangou() {
    }

    public Tuangou (XMap data) throws APIException {
        super(data);
        init(data);
    }
    
    public void init(XMap data) throws APIException {
        super.init(data);
        if (this.data.containsKey(FIELD_UID)) {
            this.uid = this.data.getString(FIELD_UID);
        }
        if (this.data.containsKey(FIELD_NAME)) {
            this.name = this.data.getString(FIELD_NAME);
        }
        if (this.data.containsKey(FIELD_TAG)) {
            this.tag = this.data.getString(FIELD_TAG);
        }
        if (this.data.containsKey(FIELD_CITY_ID)) {
            this.cityId = this.data.getString(FIELD_CITY_ID);
        }
        if (this.data.containsKey(FIELD_PICTURES)) {
            this.pictures = new TKDrawable(this.data.getXMap(FIELD_PICTURES));
        }
        if (this.data.containsKey(FIELD_PICTURES_DETAIL)) {
            this.picturesDetail = new TKDrawable(this.data.getXMap(FIELD_PICTURES_DETAIL));
        }
        if (this.data.containsKey(FIELD_DESCRIPTION)) {
            this.description = this.data.getString(FIELD_DESCRIPTION);
        }
        if (this.data.containsKey(FIELD_START_TIME)) {
            this.startTime = this.data.getString(FIELD_START_TIME);
        }
        if (this.data.containsKey(FIELD_END_TIME)) {
            this.endTime = this.data.getString(FIELD_END_TIME);
        }
        if (this.data.containsKey(FIELD_PRICE)) {
            this.price = this.data.getString(FIELD_PRICE);
        }
        if (this.data.containsKey(FIELD_ORG_PRICE)) {
            this.orgPrice = this.data.getString(FIELD_ORG_PRICE);
        }
        if (this.data.containsKey(FIELD_DISCOUNT)) {
            this.discount = this.data.getString(FIELD_DISCOUNT);
        }
        if (this.data.containsKey(FIELD_NOTICED)) {
            this.noticed = this.data.getString(FIELD_NOTICED);
        }
        if (this.data.containsKey(FIELD_BUYER_NUM)) {
            this.buyerNum = this.data.getInt(FIELD_BUYER_NUM);
        }
        if (this.data.containsKey(FIELD_REFUND)) {
            this.refund = this.data.getString(FIELD_REFUND).replace("[", "").replace("]", "").replace(",", "\n").replace("\"", "").replace(" ", "").trim();
        }
        if (this.data.containsKey(FIELD_CONTENT_TEXT)) {
            this.contentText = this.data.getString(FIELD_CONTENT_TEXT);
        }
        if (this.data.containsKey(FIELD_CONTENT_PIC)) {
            this.contentPic = new TKDrawable(this.data.getXMap(FIELD_CONTENT_PIC));
        }
        if (this.data.containsKey(FIELD_SOURCE)) {
            this.source = this.data.getInt(FIELD_SOURCE);
        }
        if (this.data.containsKey(FIELD_SHORT_DESC)) {
            this.shortDesc = this.data.getString(FIELD_SHORT_DESC);
        }
        if (this.data.containsKey(FIELD_DEADLINE)) {
            this.deadline = this.data.getString(FIELD_DEADLINE);
        }
        if (this.data.containsKey(FIELD_GOODS_ID)) {
            this.goodsId = this.data.getString(FIELD_GOODS_ID);
        }
        if (this.data.containsKey(FIELD_BRANCH_NUM)) {
            this.branchNum = this.data.getInt(FIELD_BRANCH_NUM);
        }
        if (fendian == null) {
            fendian = new Fendian(this.data);
        } else {
            fendian.init(this.data);
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Tuangou) {
            Tuangou other = (Tuangou) object;
            if((null != other.uid && !other.uid.equals(this.uid)) || (null == other.uid && other.uid != this.uid)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    public POI getPOI() {
        if (poi == null) {
            poi = new POI();
            poi.setName(name);
            poi.setPosition(fendian.getPosition());
//            poi.setOrderNumber(orderNumber);
            poi.setSourceType(POI.SOURCE_TYPE_TUANGOU);
            poi.setAssociatedObject(this);
        }
        return poi;
    }
    
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public TKDrawable getPictures() {
        return pictures;
    }

    public TKDrawable getPicturesDetail() {
        return picturesDetail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getOrgPrice() {
        return orgPrice;
    }

    public void setOrgPrice(String orgPrice) {
        this.orgPrice = orgPrice;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    public String getNoticed() {
        return noticed;
    }

    public void setNoticed(String noticed) {
        this.noticed = noticed;
    }

    public long getBuyerNum() {
        return buyerNum;
    }

    public void setBuyerNum(long buyerNum) {
        this.buyerNum = buyerNum;
    }

    public String getRefund() {
        return refund;
    }

    public void setRefund(String refund) {
        this.refund = refund;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public TKDrawable getContentPic() {
        return contentPic;
    }

    public long getSource() {
        return source;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }

    public long getBranchNum() {
        return branchNum;
    }

    public void setBranchNum(long branchNum) {
        this.branchNum = branchNum;
    }

    public void setFendian(Fendian fendian) {
        this.fendian = fendian;
    }

    public Fendian getFendian() {
        return fendian;
    }
    
    public DataQuery getFendianQuery() {
        return fendianQuery;
    }

    public void setFendianQuery(DataQuery fendianQuery) {
        this.fendianQuery = fendianQuery;
    }

    public String getFilterArea() {
        return filterArea;
    }

    public void setFilterArea(String filterArea) {
        this.filterArea = filterArea;
    }
    
    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }
    
}
