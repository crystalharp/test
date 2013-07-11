/*
 * @(#)NetSearcher.java  下午03:42:06 2013-06-05 2013
 * 
 *  Copyright (C) 2013 Beijing Tigerknows Science and Technology Ltd. All rights
 *  reserved.
 *  
 */

package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.Utility;

public class Coupon extends BaseData{
	
	public static final String NEED_FIELD = "0001020304050607080910";
	
	// 0x00 x_string 优惠券uid uid
	public static final byte FIELD_UID = 0x00;
	
	// 0x01 x_string 优惠描述 description
	public static final byte FIELD_DESCRIPTION = 0x01;
	
	// 0x02 x_string 优惠券概要图片url
	public static final byte FIELD_BRIEF_PIC = 0x02;
	
	//	0x03	 x_int	 优惠券使用人数
	public static final byte FIELD_HOT = 0x03;
	
	//	0x04	 x_string	 优惠列表名称
	public static final byte FIELD_LIST_NAME = 0x04;
	
	//	0x05	 x_string	 优惠详情图片
	public static final byte FIELD_DETAIL_PIC = 0x05;
	
	//	0x06	 x_string	 优惠详情
	public static final byte FIELD_DETAIL = 0x06;
	
	//	0x07	 x_string	 二维码图片
	public static final byte FIELD_2D_CODE = 0x07;
	
	//	0x08	 x_string	 优惠提供商logo图片
	public static final byte FIELD_LOGO = 0x08;
	
	//	0x09	 x_string	 优惠提供商提示图片
	public static final byte FIELD_HINT_PIC = 0x09;
	
	//	0x10	 x_string	 优惠提供商备注文字
	public static final byte FIELD_REMARK = 0x10;
	
	private String uid;
	private String description;
	private String briefPic;
    private TKDrawable briefPicTKDrawable;
	private long hot;
	private String listName;
    private String detailPic;
    private TKDrawable detailPicTKDrawable;
	private String detail;
	private String qrImg;
	private TKDrawable qrimgTKDrawable;
    private String logo;
    private TKDrawable logoTKDrawable;
    private String hintPic;
    private TKDrawable hintPicTKDrawable;
	private String remark;
	
	public Coupon(XMap data) throws APIException {
		super(data);
		init(data, false);
	}
	
	public void init(XMap data, boolean reset)throws APIException{
		super.init(data, reset);
		this.uid = getStringFromData(FIELD_UID, reset ? null : this.uid);
		this.description = getStringFromData(FIELD_DESCRIPTION, reset ? null : this.description);
		this.briefPic = getStringFromData(FIELD_BRIEF_PIC, reset ? null : this.briefPic);
		if (this.briefPic != null) {
		    TKDrawable tkDrawable = new TKDrawable();
		    tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.briefPic, Globals.getPicWidthHeight(TKConfig.PICTURE_COUPON_LIST)));
		    briefPicTKDrawable = tkDrawable;
		}
		this.hot = getLongFromData(FIELD_HOT, reset ? null : this.hot);
		this.listName = getStringFromData(FIELD_LIST_NAME, reset ? null : this.listName);
		this.detailPic = getStringFromData(FIELD_DETAIL_PIC, reset ? null : this.detailPic);
        if (this.detailPic != null) {
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.detailPic, Globals.getPicWidthHeight(TKConfig.PICTURE_COUPON_DETAIL)));
            detailPicTKDrawable = tkDrawable;
        }
		this.detail = getStringFromData(FIELD_DETAIL, reset ? null : this.detail);
		this.qrImg = getStringFromData(FIELD_2D_CODE, reset ? null : this.qrImg);
		if (this.qrImg != null){
			TKDrawable tkDrawable = new TKDrawable();
			tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.qrImg, Globals.getPicWidthHeight(TKConfig.PICTURE_COUPON_QRIMG)));
			qrimgTKDrawable = tkDrawable;
		}
		this.logo = getStringFromData(FIELD_LOGO, reset ? null : this.logo);
        if (this.logo != null) {
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.logo, Globals.getPicWidthHeight(TKConfig.PICTURE_COUPON_LOGO)));
            logoTKDrawable = tkDrawable;
        }
		this.hintPic = getStringFromData(FIELD_HINT_PIC, reset ? null : this.hintPic);
        if (this.hintPic != null) {
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.hintPic, Globals.getPicWidthHeight(TKConfig.PICTURE_COUPON_HINT)));
            hintPicTKDrawable = tkDrawable;
        }
		this.remark = getStringFromData(FIELD_REMARK, reset ? null : this.remark);
	}
	
    public String getUid() {
		return uid;
	}

	public String getDescription() {
		return description;
	}

	public long getHot() {
		return hot;
	}

	public String getListName() {
		return listName;
	}

	public String getDetail() {
		return detail;
	}

	public String getQrImg() {
		return qrImg;
	}

	public String getRemark() {
		return remark;
	}

	public TKDrawable getBriefPicTKDrawable() {
        return briefPicTKDrawable;
    }

    public TKDrawable getDetailPicTKDrawable() {
        return detailPicTKDrawable;
    }

    public TKDrawable getLogoTKDrawable() {
        return logoTKDrawable;
    }

    public TKDrawable getHintPicTKDrawable() {
        return hintPicTKDrawable;
    }
    
    public TKDrawable getQrimgTKDrawable() {
    	return qrimgTKDrawable;
    }

    public static XMapInitializer<Coupon> Initializer = new XMapInitializer<Coupon>() {

        @Override
        public Coupon init(XMap data) throws APIException {
            return new Coupon(data);
        }
    };
}