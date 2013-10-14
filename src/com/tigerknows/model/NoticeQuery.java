package com.tigerknows.model;

import java.util.List;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.NoticeQueryTest;
import com.tigerknows.model.xobject.XMap;
import android.content.Context;
import android.text.TextUtils;

public class NoticeQuery extends BaseQuery {
	
	static final String TAG = "NoticeQuery";

	public NoticeQuery(Context context) {
		super(context, API_TYPE_NOTICE);
	}
	
	@Override
	protected void addCommonParameters() {
		super.addCommonParameters();
        addSessionId();
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
        response = new NoticeResultResponse(responseXMap);
    }
    protected void launchTest() {
        super.launchTest();
        responseXMap = NoticeQueryTest.launchNoticeResultResponse(context);
    }
    
    public static class NoticeResultResponse extends Response {
    	
    	public static final byte FIELD_NOTICE_RESULT = 0x02;
    	
    	private NoticeResult noticeResult;

		public NoticeResultResponse(XMap data) throws APIException {
			super(data);
			
			this.noticeResult = getObjectFromData(FIELD_NOTICE_RESULT, NoticeResult.Initializer);
		}
		
		public NoticeResult getNoticeResult() {
			return noticeResult;
		}
		
		public static class NoticeResult extends XMapData {
			
			// 0x01 x_int 总数
			public static final byte FIELD_NUM = 0x01;
			
			// 0x02 x_array<x_map> x_array<消息通知>
			public static final byte FIELD_NOTICE = 0x02;
			
			// 0x03 x_string 信息提示
			public static final byte FIELD_NOTE = 0x03;
			
			private long num;
			
			private List<Notice> noticeList;
			
			private String note;
			
			public long getNum() {
				return num;
			}
			
			public List<Notice> getNoticeList() {
				return noticeList;
			}
			
			public String getNote() {
				return note;
			}
			
			public NoticeResult(XMap data) throws APIException {
				super(data);
				num = getLongFromData(FIELD_NUM);
				noticeList = getListFromData(FIELD_NOTICE, Notice.Initializer);
				note = getStringFromData(FIELD_NOTE);
			}
			
			public static class Notice extends XMapData {
				
				// 0x01 x_int 消息ID，用于客户端记录行为日志
				public static final byte FIELD_NOTICE_ID = 0x01;
				
				// 0x02 x_int 操作类型
				public static final byte FIELD_OPERATION_TYPE = 0x02;
				
				// 0x03 x_string 消息标题
				public static final byte FIELD_NOTICE_TITLE = 0x03;
				
				// 0x04 x_string 文字描述
				public static final byte FIELD_NOTICE_DESCRIPTION = 0x04;
				
				// 0x05 x_string 图片url
				public static final byte FIELD_PIC_URL = 0x05;
				
				// 0x06 x_string url
				public static final byte FIELD_URL = 0x06;
				
				// 0x07 x_string web页标题
				public static final byte FIELD_WEB_TITLE = 0x07;
				
				private long noticeId;
				
				private long operationType;
				
				private String noticeTitle;
				
				private String noticeDescription;
				
				private String url;
				
				private String picUrl;
				
				private String webTitle;
				
				private long localType;
				
				private int localLayoutType;
				
				private TKDrawable picTKDrawable;
				
				public static final int INVALID = -1;
				
				public long getNoticeId() {
					return noticeId;
				}

				public long getOperationType() {
					return operationType;
				}
				
				public String getNoticeTitle(){
					return noticeTitle;
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
				
				public String getWebTitle() {
					return webTitle;
				}
				
				public long getLocalType() {
					return localType;
				}
				
				public int getLocalLayoutType() {
					return localLayoutType;
				}
				
				public TKDrawable getpicTkDrawable() {
					return picTKDrawable;
				}
				
				public Notice(XMap data) throws APIException{
					super(data);
					init(data, false, 0);
				}
				
				public Notice(XMap data, long localType) throws APIException{
					super(data);
					init(data, false, localType);
				}
				
				public void init(XMap data, boolean reset, long localType) throws APIException{
					super.init(data, reset);
					this.noticeId = getLongFromData(FIELD_NOTICE_ID);
					this.operationType = getLongFromData(FIELD_OPERATION_TYPE);
					this.noticeTitle = getStringFromData(FIELD_NOTICE_TITLE);
					this.noticeDescription = getStringFromData(FIELD_NOTICE_DESCRIPTION);
					this.url = getStringFromData(FIELD_URL);
					this.picUrl = getStringFromData(FIELD_PIC_URL);
					this.webTitle = getStringFromData(FIELD_WEB_TITLE);
					this.localType = localType;
					if(this.picUrl != null){
						TKDrawable tkDrawable = new TKDrawable();
						tkDrawable.setUrl(this.picUrl);
						picTKDrawable = tkDrawable;
					}
					if(localType == 1){
						this.localLayoutType = 3;
					}
					else{
						this.localLayoutType = 0;
						if(this.noticeTitle != null && !TextUtils.isEmpty(this.noticeTitle)){
							this.localLayoutType += 1;
						}
						if(this.picUrl != null && !TextUtils.isEmpty(this.picUrl)){
							this.localLayoutType += 2;
						}
						if(this.noticeDescription != null && !TextUtils.isEmpty(this.noticeDescription)){
							this.localLayoutType += 4;
						}
					}
					switch(this.localLayoutType){
					case 1:
					case 3:
					case 2:
					case 5:
					case 7:
						break;
					default:
						this.localLayoutType = INVALID;
					}
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

    @Override
    protected void checkRequestParameters() throws APIException {
        // TODO Auto-generated method stub
        
    }
}
