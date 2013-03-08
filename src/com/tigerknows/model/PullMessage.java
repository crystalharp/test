package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

import android.os.Parcel;
import android.os.Parcelable;

public class PullMessage extends XMapData {
    
    public static int RecordMessageUpperLimit = Integer.MAX_VALUE;
    
    // 0x00  x_int  普通消息的个数。目前只能为0（本次请求没有消息推送）或1（本次请求有一个消息推送）     
    private static final byte FIELD_MESSAGE_TOTAL = 0x00;

    // 0x01  x_string  下次请求日期，形如"2013-01-15" 
    private static final byte FIELD_NEXT_REQUEST_DATE = 0x01;

    // 0x02  x_int  响应状态码，具体待定  
    private static final byte FIELD_RESPONSE_CODE = 0x02;

    // 0x03  x_int  客户端记录的msgId上限个数   
    private static final byte FIELD_RECORD_MESSAGE_UPPER_LIMIT = 0x03;

    // 0x10  x_array<x_map>  #消息格式   
    private static final byte FIELD_MESSAGE = 0x10;
    
    public static class Message extends XMapData implements Parcelable {

        // 10 产品升级推送
        public static final int TYPE_PRODUCT_UPGRADE = 10;
        // 20 重要产品信息推送
        public static final int TYPE_PRODUCT_INFOMATION = 20;
        // 30 节假日推送
        public static final int TYPE_HOLIDAY = 30;
        // 40 新片推送
        public static final int TYPE_FILM = 40;
        // 50 短间隔推送
        public static final int TYPE_INTERVAL = 50;
        
        // 0x00  x_int  消息id      
        private static final byte FIELD_MESSAGE_ID = 0x00;

        // 0x01  x_int  #适推优先级。 10，20为产品类消息；30，40，50为动态poi类消息 
        private static final byte FIELD_TYPE = 0x01;

        // 0x02  x_string  cityId  
        private static final byte FIELD_CITY_ID = 0x02;

        // 0x03  x_string  有效期的截止时间  
        private static final byte FIELD_EXPIRY_DATE = 0x03;

        // 0x10  x_string  动态poi消息时有效，表示动态poi的uid。  
        private static final byte FIELD_POI_UID = 0x10;

        // 0x11  x_string  动态poi消息时有效，表示动态poi的名称。  
        private static final byte FIELD_POI_NAME = 0x11;

        // 0x12  x_string  动态poi消息时有效，表示动态poi的时间。  
        private static final byte FIELD_POI_DATETIME = 0x12;

        // 0x13  x_string  动态poi消息时有效，表示动态poi的地点。  
        private static final byte FIELD_POI_ADDRESS= 0x13;

        // 0x30  x_string  产品升级推送消息时有效，表示下载链接。  
        private static final byte FIELD_PRODUCT_DOWNLOAD = 0x30;

        // 0x31  x_string  产品类消息时有效，表示适推模板/消息描述。    
        private static final byte FIELD_PRODUCT_DESCRIPTION = 0x31;     

        private long id;
        private long type;
        private int cityId;
        private String expiryDate;
        private String poiUid;
        private String poiName;
        private String poiDateTime;
        private String poiAddress;
        private String productDownload;
        private String productDescription;
        
        public Message() {
        }

        public Message(XMap data) throws APIException {
            super(data);

            if (this.data.containsKey(FIELD_MESSAGE_ID)) {
                this.id = this.data.getInt(FIELD_MESSAGE_ID);
            }
            
            if (this.data.containsKey(FIELD_TYPE)) {
                this.type = this.data.getInt(FIELD_TYPE);
            }
            
            if (this.data.containsKey(FIELD_CITY_ID)) {
                String cityId = this.data.getString(FIELD_CITY_ID);
                try {
                    this.cityId = Integer.parseInt(cityId);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            
            if (this.data.containsKey(FIELD_EXPIRY_DATE)) {
                expiryDate = this.data.getString(FIELD_EXPIRY_DATE);
            }
            
            if (this.data.containsKey(FIELD_POI_UID)) {
                this.poiUid = this.data.getString(FIELD_POI_UID);
            }
            
            if (this.data.containsKey(FIELD_POI_NAME)) {
                this.poiName = this.data.getString(FIELD_POI_NAME);
            }
            
            if (this.data.containsKey(FIELD_POI_DATETIME)) {
                this.poiDateTime = this.data.getString(FIELD_POI_DATETIME);
            }
            
            if (this.data.containsKey(FIELD_POI_ADDRESS)) {
                this.poiAddress = this.data.getString(FIELD_POI_ADDRESS);
            }
            
            if (this.data.containsKey(FIELD_PRODUCT_DOWNLOAD)) {
                this.productDownload = this.data.getString(FIELD_PRODUCT_DOWNLOAD);
            }
            
            if (this.data.containsKey(FIELD_PRODUCT_DESCRIPTION)) {
                this.productDescription = this.data.getString(FIELD_PRODUCT_DESCRIPTION);
            }
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getType() {
            return type;
        }

        public void setType(long type) {
            this.type = type;
        }

        public int getCityId() {
            return cityId;
        }

        public void setCityId(int cityId) {
            this.cityId = cityId;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getPoiUid() {
            return poiUid;
        }

        public void setPoiUid(String poiUid) {
            this.poiUid = poiUid;
        }

        public String getPoiName() {
            return poiName;
        }

        public void setPoiName(String poiName) {
            this.poiName = poiName;
        }

        public String getPoiDateTime() {
            return poiDateTime;
        }

        public void setPoiDateTime(String poiDateTime) {
            this.poiDateTime = poiDateTime;
        }

        public String getPoiAddress() {
            return poiAddress;
        }

        public void setPoiAddress(String poiAddress) {
            this.poiAddress = poiAddress;
        }

        public String getProductDownload() {
            return productDownload;
        }

        public void setProductDownload(String productDownload) {
            this.productDownload = productDownload;
        }

        public String getProductDescription() {
            return productDescription;
        }

        public void setProductDescription(String productDescription) {
            this.productDescription = productDescription;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(id);
            out.writeLong(type);
            out.writeInt(cityId);
            out.writeString(expiryDate);
            out.writeString(poiUid);
            out.writeString(poiName);  
            out.writeString(poiDateTime);  
            out.writeString(poiAddress);  
            out.writeString(productDownload);  
            out.writeString(productDescription);  
        }

        public static final Parcelable.Creator<Message> CREATOR
                = new Parcelable.Creator<Message>() {
            public Message createFromParcel(Parcel in) {
                return new Message(in);
            }

            public Message[] newArray(int size) {
                return new Message[size];
            }
        };
        
        private Message(Parcel in) {
            id = in.readLong();
            type = in.readLong();
            cityId = in.readInt();
            expiryDate = in.readString();
            poiUid = in.readString();
            poiName = in.readString();
            poiDateTime = in.readString();
            poiAddress = in.readString();
            productDownload = in.readString();
            productDescription = in.readString();
        }
        
    }

    private long total;
    private String nextRequsetDate;
    private long responseCode;
    private long recordMessageUpperLimit;
    private Message message;
    
    public PullMessage(XMap data) throws APIException {
        super(data);

        if (this.data.containsKey(FIELD_MESSAGE_TOTAL)) {
            this.total = this.data.getInt(FIELD_MESSAGE_TOTAL);
        }
        
        if (this.data.containsKey(FIELD_NEXT_REQUEST_DATE)) {
            nextRequsetDate = this.data.getString(FIELD_NEXT_REQUEST_DATE);
        }
        
        if (this.data.containsKey(FIELD_RESPONSE_CODE)) {
            this.responseCode = this.data.getInt(FIELD_RESPONSE_CODE);
        }
        
        if (this.data.containsKey(FIELD_RECORD_MESSAGE_UPPER_LIMIT)) {
            this.recordMessageUpperLimit = this.data.getInt(FIELD_RECORD_MESSAGE_UPPER_LIMIT);
        }
        
        if (this.data.containsKey(FIELD_MESSAGE)) {
            message = new Message(this.data.getXMap(FIELD_MESSAGE));
        }
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getNextRequsetDate() {
        return nextRequsetDate;
    }

    public void setNextRequsetDate(String nextRequsetDate) {
        this.nextRequsetDate = nextRequsetDate;
    }

    public long getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(long responseCode) {
        this.responseCode = responseCode;
    }

    public long getRecordMessageUpperLimit() {
        return recordMessageUpperLimit;
    }

    public void setRecordMessageUpperLimit(long recordMessageUpperLimit) {
        this.recordMessageUpperLimit = recordMessageUpperLimit;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
    
}
