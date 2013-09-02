package com.tigerknows.model;

import java.util.List;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

import android.os.Parcel;
import android.os.Parcelable;

public class PullMessage extends Response {
    
    public static int RecordMessageUpperLimit = Integer.MAX_VALUE;

    // 0x01  x_int  下次请求间隔时间，单位为天 
    public static final byte FIELD_REQUEST_INTERVAL_DAYS = 0x02;

    // 0x02  x_int  客户端记录的msgId上限个数   
    public static final byte FIELD_RECORD_MESSAGE_UPPER_LIMIT = 0x03;

    // 0x10  x_array<x_map>  #消息格式   
    public static final byte FIELD_MESSAGE = 0x10;
    
    public static class Message extends XMapData implements Parcelable {

        // 10 产品升级推送
        public static final int TYPE_PRODUCT_UPGRADE = 10;
        // 20 重要产品信息推送
        public static final int TYPE_PRODUCT_INFOMATION = 20;
        // 25 主题活动推送
        public static final int TYPE_ACTIVITY = 25;
        // 30 节假日推送
        public static final int TYPE_HOLIDAY = 30;
        // 40 新片推送
        public static final int TYPE_FILM = 40;
        // 50 短间隔推送
        public static final int TYPE_INTERVAL = 50;
        
        // 0x00  x_int  消息id      
        public static final byte FIELD_MESSAGE_ID = 0x00;

        // 0x01  x_int  #适推优先级。 10，20为产品类消息；30，40，50为动态poi类消息 
        public static final byte FIELD_MSG_TYPE = 0x01;

        // 0x02  x_string  cityId  
        public static final byte FIELD_CITY_ID = 0x02;

        // 0x03  x_string  有效期的截止时间  
        public static final byte FIELD_EXPIRY_DATE = 0x03;
        
        // 0x10  x_string  动态poi消息时有效，表示动态poi的uid。  
        public static final byte FIELD_POI_INFO = 0x10;
        
        /**
         * Dynamic POI with a specific master type/uid and slave type/uid
         * Slava fields can be null
         * @author jiangshuai
         *
         */
        public static class PulledDynamicPOI extends XMapData implements Parcelable {

            // 0x01	 主动态POI的dty
            public static final byte FIELD_MASTER_POI_TYPE = 0x01;
            
            // 0x02	 主动态POI的uid
            public static final byte FIELD_MASTER_POI_UID = 0x02;
            
            // 0x03	 从动态POI的dty
            public static final byte FIELD_SLAVE_POI_TYPE = 0x03;

            // 0x04 从动态POI的uid
            public static final byte FIELD_SLAVE_POI_UID = 0x04;
            
            // 0x10	 x_string	 动态POI的摘要字符串
            public static final byte FIELD_POI_DESCRIPTION = 0x10;


            private long masterType;
            private String masterUID;
            private long slaveType;
            private String slaveUID;
            private String description;
            
            public PulledDynamicPOI(){
            	
            }
            
            public PulledDynamicPOI(XMap data) throws APIException{
            	super(data);
            	
            	this.masterType = getLongFromData(FIELD_MASTER_POI_TYPE);
                this.masterUID = getStringFromData(FIELD_MASTER_POI_UID);
                this.slaveType = getLongFromData(FIELD_SLAVE_POI_TYPE);
                this.slaveUID = getStringFromData(FIELD_SLAVE_POI_UID);
                this.description = getStringFromData(FIELD_POI_DESCRIPTION);
            }

            public static final Parcelable.Creator<PulledDynamicPOI> CREATOR
		            = new Parcelable.Creator<PulledDynamicPOI>() {
		        public PulledDynamicPOI createFromParcel(Parcel in) {
		            return new PulledDynamicPOI(in);
		        }
		
		        public PulledDynamicPOI[] newArray(int size) {
		            return new PulledDynamicPOI[size];
		        }
		    };
		    

			public long getMasterType() {
				return masterType;
			}

			public void setMasterType(long masterType) {
				this.masterType = masterType;
			}

			public String getMasterUID() {
				return masterUID;
			}

			public void setMasterUID(String masterUID) {
				this.masterUID = masterUID;
			}
			
			public long getSlaveType() {
				return slaveType;
			}
			
			public void setSlaveType(long slaveType) {
				this.slaveType = slaveType;
			}

			public String getSlaveUID() {
				return slaveUID;
			}

			public void setSlaveUID(String slaveUID) {
				this.slaveUID = slaveUID;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			@Override
			public int describeContents() {
				return 0;
			}

            private PulledDynamicPOI(Parcel in) {
            	masterType = in.readLong();
                masterUID = in.readString();
                slaveType = in.readLong();
                slaveUID = in.readString();
                description = in.readString();
            }
            
			@Override
			public void writeToParcel(Parcel dest, int flags) {
				dest.writeLong(masterType);
				dest.writeString(masterUID);
	            dest.writeLong(slaveType);  
	            dest.writeString(slaveUID);
	            dest.writeString(description);
			}
            
        }

        // 0x10  x_string  动态poi消息时有效，表示动态poi的uid。  
        public static final byte FIELD_PRODUCT_MESSAGE = 0x11;

        public static class PulledProductMessage extends XMapData implements Parcelable{

        	public static final byte FILED_PRODUCT_DOWNLOAD_URL = 0x01;
        	public static final byte FIELD_PRODUCT_DESCRIPTION = 0x02;
        	
        	private String downloadUrl;
        	private String description;
        	
            public static final Parcelable.Creator<PulledProductMessage> CREATOR
		            = new Parcelable.Creator<PulledProductMessage>() {
		        public PulledProductMessage createFromParcel(Parcel in) {
		            return new PulledProductMessage(in);
		        }
		
		        public PulledProductMessage[] newArray(int size) {
		            return new PulledProductMessage[size];
		        }
		    };
		    
			@Override
			public int describeContents() {
				return 0;
			}

	        private PulledProductMessage(XMap data) throws APIException {
	        	super(data);
	            this.downloadUrl = getStringFromData(FILED_PRODUCT_DOWNLOAD_URL);
	            this.description = getStringFromData(FIELD_PRODUCT_DESCRIPTION);
	        }

	        private PulledProductMessage(Parcel in) {
	        	downloadUrl = in.readString();
	        	description = in.readString();
	        }
	        
			@Override
			public void writeToParcel(Parcel dest, int flags) {
				dest.writeString(downloadUrl);
				dest.writeString(description);
			}

			public String getDownloadUrl() {
				return downloadUrl;
			}

			public void setDownloadUrl(String downloadUrl) {
				this.downloadUrl = downloadUrl;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

        }  

        private long id;
        private long type;
        private int cityId;
        private String expiryDate;
        private PulledDynamicPOI dynamicPOI;
        private PulledProductMessage productMsg;
        
		public Message() {
        }

        public Message(XMap data) throws APIException {
            super(data);

            this.id = getLongFromData(FIELD_MESSAGE_ID);
            this.type = getLongFromData(FIELD_MSG_TYPE);
            switch ((int)this.type) {
				case TYPE_HOLIDAY:
				case TYPE_FILM:
				case TYPE_INTERVAL:
					if(data.containsKey(FIELD_POI_INFO)){
						dynamicPOI = new PulledDynamicPOI(this.data.getXMap(FIELD_POI_INFO));
					}
					break;

				case TYPE_PRODUCT_UPGRADE:
                case TYPE_ACTIVITY:
				case TYPE_PRODUCT_INFOMATION:
					if(data.containsKey(FIELD_PRODUCT_MESSAGE)){
						productMsg = new PulledProductMessage(this.data.getXMap(FIELD_PRODUCT_MESSAGE));
					}
					break;

				default:
					break;
			}
                
            
            String cityId = getStringFromData(FIELD_CITY_ID);
            if (cityId != null) {
                try {
                    this.cityId = Integer.parseInt(cityId);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            expiryDate = getStringFromData(FIELD_EXPIRY_DATE);
            
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

        public PulledDynamicPOI getDynamicPOI() {
			return dynamicPOI;
		}

		public void setDynamicPOI(PulledDynamicPOI dynamicPOI) {
			this.dynamicPOI = dynamicPOI;
		}

		public PulledProductMessage getProductMsg() {
			return productMsg;
		}

		public void setProductMsg(PulledProductMessage productMsg) {
			this.productMsg = productMsg;
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
        
        public int describeContents() {
        	return 0;
        }
        
        private Message(Parcel in) {
            id = in.readLong();
            type = in.readLong();
            cityId = in.readInt();
            expiryDate = in.readString();
            dynamicPOI = in.readParcelable(PulledDynamicPOI.class.getClassLoader());
            productMsg = in.readParcelable(PulledDynamicPOI.class.getClassLoader());
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(id);
            out.writeLong(type);
            out.writeInt(cityId);
            out.writeString(expiryDate);
            out.writeParcelable(this.dynamicPOI, flags);
            out.writeParcelable(this.productMsg, flags);
        }
        
        public static XMapInitializer<Message> Initializer = new XMapInitializer<Message>() {
            
            @Override
            public Message init(XMap data) throws APIException {
                return new Message(data);
            }
        };
    }

    private int requestIntervalDays;
    private long recordMessageUpperLimit;
    private List<Message> messageList;
    
    public PullMessage(XMap data) throws APIException {
        super(data);

        messageList = getListFromData(FIELD_MESSAGE, Message.Initializer);
        requestIntervalDays = (int)getLongFromData(FIELD_REQUEST_INTERVAL_DAYS);
        this.recordMessageUpperLimit = getLongFromData(FIELD_RECORD_MESSAGE_UPPER_LIMIT);
    }

    public int getRequsetIntervalDays() {
        return requestIntervalDays;
    }

    public void setRequsetIntervalDays(int requestIntervalDays) {
        this.requestIntervalDays = requestIntervalDays;
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

    public List<Message> getMessageList() {
        return messageList;
    }

//    public void setMessage(Message message) {
//        this.message = message;
//    }
    
}
