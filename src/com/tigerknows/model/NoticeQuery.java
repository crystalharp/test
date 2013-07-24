package com.tigerknows.model;

import java.util.List;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;

public class NoticeQuery extends BaseQuery {
	
	static final String TAG = "NoticeQuery";

	public NoticeQuery(Context context) {
		super(context, API_TYPE_NOTICE);
	}
	
	@Override
	protected void makeRequestParameters() throws APIException {
		super.makeRequestParameters();
	}
	
	@Override
	protected void createHttpClient() {
		super.createHttpClient();
		String url = String.format(TKConfig.getNoticeUrl(), TKConfig.getNoticeHost());
		httpClient.setURL(url);
	}

	@Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        // TODO: 
    }
    protected void launchTest() {
        super.launchTest();
        // TODO:
    }
    
    public static class NoticeResultResponse extends Response {
    	
    	public static final byte FIELD_NOTICE_RESULT = 0x02;
    	
    	private NoticeResult noticeResult;

		public NoticeResultResponse(XMap data) throws APIException {
			super(data);
			
			this.noticeResult = getObjectFromData(FIELD_NOTICE_RESULT, NoticeResult.Initializer);
			// TODO Auto-generated constructor stub
		}
		
		public static class NoticeResult extends XMapData {
			
			// 0x01 x_int 总数
			public static final byte FIELD_NUM = 0x01;
			
			// 0x02 x_array<x_map> x_array<消息通知>
			public static final byte FIELD_NOTICE = 0x02;
			
			// 0x03 x_string 信息提示
			public static final byte FIELD_NOTE = 0x03;
			
			private long num;
			
			private List<Notice> notice;
			
			private String note;
			
			public long getNum() {
				return num;
			}
			
			public List<Notice> getNotice() {
				return notice;
			}
			
			public String getNote() {
				return note;
			}
			
			public NoticeResult(XMap data) throws APIException {
				super(data);
				num = getLongFromData(FIELD_NUM);
				notice = getListFromData(FIELD_NOTICE, Notice.Initializer);
				note = getStringFromData(FIELD_NOTE);
			}
			
			public static class Notice extends XMapData {
				
				// 0x01 x_int 操作类型
				public static final byte FIELD_OPERATION_TYPE = 0x01;
				
				// 0x02 x_string 文字描述
				public static final byte FIELD_NOTICE_DESCRIPTION = 0x02;
				
				// 0x03 x_string url
				public static final byte FIELD_URL = 0x03;
				
				// 0x04 x_string 图片url
				public static final byte FIELD_PIC_URL = 0x04;
				
				private long operationType;
				
				private String noticeDescription;
				
				private String url;
				
				private String picUrl;

				public long getOperationType() {
					return operationType;
				}

				public String getDescription() {
					return noticeDescription;
				}

				public String getUrl() {
					return url;
				}

				public String getPicUrl() {
					return picUrl;
				}
				
				public Notice(XMap data) throws APIException{
					super(data);
					operationType = getLongFromData(FIELD_OPERATION_TYPE);
					noticeDescription = getStringFromData(FIELD_NOTICE_DESCRIPTION);
					url = getStringFromData(FIELD_URL);
					picUrl = getStringFromData(FIELD_PIC_URL);
				}
				
				public static XMapInitializer<Notice> Initializer = new XMapInitializer<Notice>() {

					@Override
					public Notice init(XMap data) throws APIException {
						return new Notice(data);
					}
				};

			}
			
			public static XMapInitializer<NoticeResult> Initializer = new XMapInitializer<NoticeResult>(){
				
				@Override
				public NoticeResult init(XMap data) throws APIException {
					return new NoticeResult(data);
				}
			};
		}
    	
    }
}
