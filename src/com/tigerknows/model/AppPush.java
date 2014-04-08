package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.service.download.AppService;

public class AppPush extends BaseData{
	
    // 0x01 x_string    软件名称
    public static final byte FIELD_NAME = 0x01;
    
    // 0x02 x_string    包名
    public static final byte FIELD_PACKAGE_NAME = 0x02;
    
    // 0x03 x_string    图标url
    public static final byte FIELD_ICON = 0x03;

    // 0x04 x_string    推荐语 
    public static final byte FIELD_DESCRIPTION = 0x04;
    
    // 0x05 x_string    优先级 
    public static final byte FIELD_PRIOR = 0x05;

    // 0x06 x_string    软件下载url 
    public static final byte FIELD_DOWNLOAD_URL = 0x06;



	private String name;
    private String packageName;
    private String icon;
    private String iconFileName;
    private String description;
    private String prior;
    private String downloadUrl;

    
    public String getIconFileName() {
		return iconFileName;
	}

	public String getName() {
		return name;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getIcon() {
		return icon;
	}

	public String getDescription() {
		return description;
	}

	public String getPrior() {
		return prior;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}
	
	public AppPush(XMap data) throws APIException {
		super(data);
		init(data, true);
	}
	/**
	 * 把AppPush订单转化成XMap
	 * 所有的域必须存在并合法，否则报APIExecption
	 * @return
	 * @throws APIException
	 */	
	public XMap toXMapForStorage() throws APIException{
		XMap map = new XMap();
		if(name != null){
			map.put(FIELD_NAME, name);
		}else{
			throw new APIException("FIELD_NAME");
		}
		if(packageName != null){
			map.put(FIELD_PACKAGE_NAME, packageName);
		}else{
			throw new APIException("FIELD_PACKAGE_NAME");
		}
		if(icon != null){
			map.put(FIELD_ICON, icon);
		}else{
			throw new APIException("FIELD_ICON");
		}
		if(description != null){
			map.put(FIELD_DESCRIPTION, description);
		}else{
			throw new APIException("FIELD_DESCRIPTION");
		}
		if(downloadUrl != null){
			map.put(FIELD_DOWNLOAD_URL, downloadUrl);
		}else{
			throw new APIException("FIELD_DOWNLOAD_URL");
		}		
		return map;
	}
	
	public void init(XMap data, boolean reset) throws APIException{
		super.init(data, reset);
		this.name = getStringFromData(FIELD_NAME, reset ? null : this.name);
		this.packageName = getStringFromData(FIELD_PACKAGE_NAME, reset ? null : this.packageName);
		this.icon = getStringFromData(FIELD_ICON, reset ? null : this.name);
		if (this.icon != null) {
			this.iconFileName = this.icon.substring(this.icon.lastIndexOf("/") + 1).replaceAll("[?]", "@");
		}else{
			this.iconFileName = null;
		}
		this.description = getStringFromData(FIELD_DESCRIPTION, reset ? null : this.description);
		this.prior = getStringFromData(FIELD_PRIOR, reset ? null : this.prior);
		this.downloadUrl = getStringFromData(FIELD_DOWNLOAD_URL, reset ? null : this.downloadUrl);
	}
    
    public static XMapInitializer<AppPush> Initializer = new XMapInitializer<AppPush>() {

        @Override
        public AppPush init(XMap data) throws APIException {
            return new AppPush(data);
        }
    };
}
