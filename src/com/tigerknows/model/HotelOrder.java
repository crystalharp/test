package com.tigerknows.model;

import java.util.Comparator;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.tigerknows.model.xobject.XMap;

/**
 * This HotelOrder is different from other model in this package.
 * It's mainly meant for local storage
 * Additional function is passing value between different UIs
 * 	For the second, decide yourself.
 * @author jiangshuai
 *
 */
public class HotelOrder extends XMapData{
	
	// identification info
	/**
	 * 订单的id。
	 * 创建订单的时候获取的。
	 */
	private String id;
	/**
	 * 订单创建时间
	 */
	private long createTime;
	/**
	 * 状态 
	 * range(1-5)
	 */
	private int state = -1;

	// hotel info
	/**
	 * 酒店的poi的uuid
	 * 注意不是酒店动态poi的id
	 */
	private String hotelPoiUUID; 
	/**
	 * 酒店的名字
	 */
	private String hotelName;
	/**
	 * 酒店的地址
	 */
	private String hotelAddress;
	/**
	 * 酒店的位置
	 */
	private Position position;
	/**
	 * 酒店的电话
	 * 逗号分隔
	 */
	private String hotelTel;
	
	// room info
	/**
	 * 房型
	 */
	private String roomType;
	/**
	 * 房间数目
	 */
	private String roomNum;
	
	// payment info
	/**
	 * 总价
	 */
	private String totalFee;
	/**
	 * 担保类型
	 */
	private String guaranteeType;

	// time info
	/**
	 * 保留时间
	 */
	private long retentionTime;
	/**
	 * 入住时间
	 */
	private long checkinTime;
	/**
	 * 离店时间
	 */
	private long checkoutTime;

	// guest info
	/**
	 * 入住人姓名
	 */
	private String guestName;
	/**
	 * 手机号码
	 */
	private String mobileNum;

	public static final byte FIELD_ID = 0x01;
	public static final byte FIELD_CREATE_TIME = 0x02;
	public static final byte FIELD_STATE = 0x03;
	public static final byte FIELD_POIUUID = 0x04;
	public static final byte FIELD_HOTEL_NAME = 0x05;
	public static final byte FIELD_HOTEL_ADDRESS = 0x06;
	public static final byte FIELD_POSITION_X = 0x07;
	public static final byte FIELD_POSITION_Y = 0x08;
	public static final byte FIELD_HOTEL_TEL = 0x09;
	public static final byte FIELD_ROOM_TYPE = 0x0A;
	public static final byte FIELD_ROOM_NUM = 0x0B;
	public static final byte FIELD_TOTAL_FEE = 0x0C;
	public static final byte FIELD_GARANTEE_TYPE = 0x0D;
	public static final byte FIELD_RETENTION_TIME = 0x0E;
	public static final byte FIELD_CHECKIN_TIME = 0x0F;
	public static final byte FIELD_CHECKOUT_TIME = 0x10;
	public static final byte FIELD_GUEST_NAME = 0x11;
	public static final byte FIELD_MOBILE_NUM= 0x12;
	
	/**
	 * 使用XMap构建酒店订单
	 * XMap必须存在所有的域否则报APIException
	 * @param data
	 * @throws APIException
	 */
	public HotelOrder(XMap data) throws APIException{
		super(data);
		id = data.getString(FIELD_ID);
		createTime = data.getInt(FIELD_CREATE_TIME);
		state = (int) data.getInt(FIELD_STATE);
		hotelPoiUUID = data.getString(FIELD_POIUUID);
		hotelName = data.getString(FIELD_HOTEL_NAME);
		hotelAddress = data.getString(FIELD_HOTEL_ADDRESS);
		position = new Position(data.getDouble(FIELD_POSITION_Y), data.getDouble(FIELD_POSITION_X));
		hotelTel = data.getString(FIELD_HOTEL_TEL);
		roomType = data.getString(FIELD_ROOM_TYPE);
		totalFee = data.getString(FIELD_TOTAL_FEE);
		guaranteeType = data.getString(FIELD_GARANTEE_TYPE);
		retentionTime = data.getInt(FIELD_RETENTION_TIME);
		checkinTime = data.getInt(FIELD_CHECKIN_TIME);
		checkoutTime = data.getInt(FIELD_CHECKOUT_TIME);
		guestName = data.getString(FIELD_GUEST_NAME);
		mobileNum = data.getString(FIELD_MOBILE_NUM);
	}
	
	public HotelOrder() {
		super();
	}
	
	public HotelOrder(String id, long createTime, int state,
			String hotelPoiUUID, String hotelName, String hotelAddress,
			Position position, String hotelTel, String roomType,
			String roomNum, String totalFee, String guaranteeType,
			long retentionTime, long checkinTime, long checkoutTime,
			String guestName, String mobileNum) {
		super();
		this.id = id;
		this.createTime = createTime;
		this.state = state;
		this.hotelPoiUUID = hotelPoiUUID;
		this.hotelName = hotelName;
		this.hotelAddress = hotelAddress;
		this.position = position;
		this.hotelTel = hotelTel;
		this.roomType = roomType;
		this.roomNum = roomNum;
		this.totalFee = totalFee;
		this.guaranteeType = guaranteeType;
		this.retentionTime = retentionTime;
		this.checkinTime = checkinTime;
		this.checkoutTime = checkoutTime;
		this.guestName = guestName;
		this.mobileNum = mobileNum;
	}

	/**
	 * 把酒店订单转化成XMap
	 * 所有的域必须存在并合法，否则报APIExecption
	 * @return
	 * @throws APIException
	 */
	public XMap toXMapForStorage() throws APIException{
		XMap map = new XMap();
		if (id!=null) {
			map.put(FIELD_ID, id);
		}else{
			throw new APIException("FIELD_ID");
		}

		if (createTime > 0) {
			map.put(FIELD_CREATE_TIME, createTime);
		}else{
			throw new APIException("FIELD_CREATE_TIME");
		}

		if (state>=1 && state<=5) {
			map.put(FIELD_STATE, state);
		}else{
			throw new APIException("FIELD_STATE");
		}

		if (hotelPoiUUID!=null) {
			map.put(FIELD_POIUUID, hotelPoiUUID);
		}else{
			throw new APIException("FIELD_POIUUID");
		}

		if (hotelName!=null) {
			map.put(FIELD_HOTEL_NAME, hotelName);
		}else{
			throw new APIException("FIELD_HOTEL_NAME");
		}

		if (hotelAddress!=null) {
			map.put(FIELD_HOTEL_ADDRESS, hotelAddress);
		}else{
			throw new APIException("FIELD_HOTEL_ADDRESS");
		}

		if (position!=null) {
			map.put(FIELD_POSITION_X, position.getLon());
			map.put(FIELD_POSITION_Y, position.getLat());
		}else{
			throw new APIException("FIELD_POSITION");
		}

		if (hotelTel!=null) {
			map.put(FIELD_HOTEL_TEL, hotelTel);
		}else{
			throw new APIException("FIELD_HOTEL_TEL");
		}

		if (roomType!=null) {
			map.put(FIELD_ROOM_TYPE, roomType);
		}else{
			throw new APIException("FIELD_ROOM_TYPE");
		}

		if (totalFee!=null) {
			map.put(FIELD_TOTAL_FEE, totalFee);
		}else{
			throw new APIException("FIELD_TOTAL_FEE");
		}

		if (guaranteeType!=null) {
			map.put(FIELD_GARANTEE_TYPE, guaranteeType);
		}else{
			throw new APIException("FIELD_GARANTEE_TYPE");
		}

		if (retentionTime>0) {
			map.put(FIELD_RETENTION_TIME, retentionTime);
		}else{
			throw new APIException("FIELD_RETENTION_TIME");
		}

		if (checkinTime>0) {
			map.put(FIELD_CHECKIN_TIME, checkinTime);
		}else{
			throw new APIException("FIELD_CHECKIN_TIME");
		}

		if (checkoutTime>0) {
			map.put(FIELD_CHECKOUT_TIME, checkoutTime);
		}else{
			throw new APIException("FIELD_CHECKOUT_TIME");
		}

		if (guestName!=null) {
			map.put(FIELD_GUEST_NAME, guestName);
		}else{
			throw new APIException("FIELD_GUEST_NAME");
		}

		if (mobileNum!=null) {
			map.put(FIELD_MOBILE_NUM, mobileNum);
		}else{
			throw new APIException("FIELD_MOBILE_NUM");
		}

		return map;
		
	}// end toXMap
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getCreateTime() {
		return createTime;
	}
	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public String getHotelPoiUUID() {
		return hotelPoiUUID;
	}
	public void setHotelPoiUUID(String hotelPoiUUID) {
		this.hotelPoiUUID = hotelPoiUUID;
	}
	public String getHotelName() {
		return hotelName;
	}
	public void setHotelName(String hotelName) {
		this.hotelName = hotelName;
	}
	public String getHotelAddress() {
		return hotelAddress;
	}
	public void setHotelAddress(String hotelAddress) {
		this.hotelAddress = hotelAddress;
	}
	public Position getPosition() {
		return position;
	}
	public void setPosition(Position position) {
		this.position = position;
	}
	public String getHotelTel() {
		return hotelTel;
	}
	public void setHotelTel(String hotelTel) {
		this.hotelTel = hotelTel;
	}
	public String getRoomType() {
		return roomType;
	}
	public void setRoomType(String roomType) {
		this.roomType = roomType;
	}
	public String getRoomNum() {
		return roomNum;
	}
	public void setRoomNum(String roomNum) {
		this.roomNum = roomNum;
	}
	public String getTotalFee() {
		return totalFee;
	}
	public void setTotalFee(String totalFee) {
		this.totalFee = totalFee;
	}
	public String getGuaranteeType() {
		return guaranteeType;
	}
	public void setGuaranteeType(String guaranteeType) {
		this.guaranteeType = guaranteeType;
	}
	public long getRetentionTime() {
		return retentionTime;
	}
	public void setRetentionTime(long retentionTime) {
		this.retentionTime = retentionTime;
	}
	public long getCheckinTime() {
		return checkinTime;
	}
	public void setCheckinTime(long checkinTime) {
		this.checkinTime = checkinTime;
	}
	public long getCheckoutTime() {
		return checkoutTime;
	}
	public void setCheckoutTime(long checkoutTime) {
		this.checkoutTime = checkoutTime;
	}
	public String getGuestName() {
		return guestName;
	}
	public void setGuestName(String guestName) {
		this.guestName = guestName;
	}
	public String getMobileNum() {
		return mobileNum;
	}
	public void setMobileNum(String mobileNum) {
		this.mobileNum = mobileNum;
	}
	
	/**
	 * Comparator used to sort orders loaded from database
	 */
	private static Comparator<HotelOrder> comparator = new Comparator<HotelOrder>() {

		@Override
		public int compare(HotelOrder lhs, HotelOrder rhs) {
			if( lhs.getCreateTime() > rhs.getCreateTime() ){
				return -1;
			}else if( lhs.getCreateTime() == rhs.getCreateTime() ){
				return 0;
			}else {
				return 1;
			}
		}
	};

	public static Comparator<HotelOrder> getComparator(){
		return comparator;
	}
}
