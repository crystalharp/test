package com.tigerknows.model;

import java.util.Date;

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
        private Date expireDate;
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
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(id);
            out.writeLong(type);
            out.writeInt(cityId);
            out.writeSerializable(expireDate);
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
            expireDate = (Date) in.readSerializable();
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
    }
    
}
