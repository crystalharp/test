package com.tigerknows;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import android.view.View;

import com.tigerknows.ui.hotel.HotelOrderListFragment;
import com.tigerknows.ui.discover.TuangouDetailFragment;
import com.tigerknows.ui.user.MyCommentListFragment;
import com.tigerknows.ui.discover.DiscoverListFragment;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.HomeBottomFragment;
import com.tigerknows.ui.HomeFragment;
import com.tigerknows.ui.TitleFragment;
import com.tigerknows.ui.poi.POIDetailFragment;
import com.tigerknows.ui.more.FavoriteFragment;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.traffic.BuslineResultLineFragment;
import com.tigerknows.ui.traffic.TrafficSearchHistoryFragment;
import com.tigerknows.ui.more.CommonPlaceFragment;
import com.tigerknows.ui.more.HistoryFragment;
import com.tigerknows.ui.more.GoCommentFragment;
import com.tigerknows.ui.poi.CouponDetailFragment;
import com.tigerknows.ui.traffic.TrafficResultFragment;
import com.tigerknows.ui.poi.CouponListFragment;
import com.tigerknows.ui.hotel.HotelOrderDetailFragment;
import com.tigerknows.ui.discover.DiscoverChildListFragment;
import com.tigerknows.ui.traffic.BuslineDetailFragment;
import com.tigerknows.ui.alarm.AlarmAddFragment;
import com.tigerknows.ui.poi.CustomCategoryFragment;
import com.tigerknows.ui.traffic.SubwayMapFragment;
import com.tigerknows.ui.common.BrowserFragment;
import com.tigerknows.ui.common.EditTextFragment;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.ui.traffic.FetchFavoriteFragment;
import com.tigerknows.ui.traffic.BuslineResultStationFragment;
import com.tigerknows.ui.hotel.HotelOrderCreditFragment;
import com.tigerknows.ui.user.UserHomeFragment;
import com.tigerknows.ui.alarm.AlarmListFragment;
import com.tigerknows.ui.InfoWindowFragment;
import com.tigerknows.ui.common.ResultMapFragment;
import com.tigerknows.ui.hotel.PickLocationFragment;
import com.tigerknows.ui.more.MoreHomeFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.more.MyOrderFragment;
import com.tigerknows.ui.discover.ZhanlanDetailFragment;
import com.tigerknows.ui.discover.YanchuDetailFragment;
import com.tigerknows.ui.discover.DianyingDetailFragment;
import com.tigerknows.ui.traffic.TrafficDetailFragment;
import com.tigerknows.ui.hotel.HotelOrderWriteFragment;
import com.tigerknows.ui.poi.NearbySearchFragment;
import com.tigerknows.ui.hotel.HotelOrderSuccessFragment;
import com.tigerknows.ui.poi.POIResultFragment;
import com.tigerknows.ui.common.MeasureDistanceFragment;


/**
 * @author:xupeng
 */
public class TKFragmentManager {
    
//----generate start----

	public static final int ID_view_invalid = -1;
	public static final int ID_view_hotel_order_list = 0;
	public static final int ID_view_discover_tuangou_detail = 1;
	public static final int ID_view_user_my_comment_list = 2;
	public static final int ID_view_discover_list = 3;
	public static final int ID_view_home = 4;
	public static final int ID_view_poi_detail = 5;
	public static final int ID_view_more_favorite = 6;
	public static final int ID_view_hotel_home = 7;
	public static final int ID_view_traffic_busline_line_result = 8;
	public static final int ID_view_traffic_search_history = 9;
	public static final int ID_view_more_common_places = 10;
	public static final int ID_view_more_history = 11;
	public static final int ID_view_more_go_comment = 12;
	public static final int ID_view_coupon_detail = 13;
	public static final int ID_view_traffic_result_transfer = 14;
	public static final int ID_view_coupon_list = 15;
	public static final int ID_view_hotel_order_detail_2 = 16;
	public static final int ID_view_discover_child_list = 17;
	public static final int ID_view_traffic_busline_detail = 18;
	public static final int ID_view_alarm_add = 19;
	public static final int ID_view_poi_custom_category = 20;
	public static final int ID_view_hotel_order_detail = 21;
	public static final int ID_view_subway_map = 22;
	public static final int ID_view_browser = 23;
	public static final int ID_view_traffic_home = 24;
	public static final int ID_view_traffic_fetch_favorite_poi = 25;
	public static final int ID_view_traffic_busline_station_result = 26;
	public static final int ID_view_hotel_credit_assure = 27;
	public static final int ID_view_user_home = 28;
	public static final int ID_view_alarm_list = 29;
	public static final int ID_view_infowindow = 30;
	public static final int ID_view_result_map = 31;
	public static final int ID_view_hotel_pick_location = 32;
	public static final int ID_view_more_home = 33;
	public static final int ID_view_poi_input_search = 34;
	public static final int ID_view_more_my_order = 35;
	public static final int ID_view_discover_zhanlan_detail = 36;
	public static final int ID_view_discover_yanchu_detail = 37;
	public static final int ID_view_discover_dianying_detail = 38;
	public static final int ID_view_traffic_result_detail = 39;
	public static final int ID_view_edit_text = 40;
	public static final int ID_view_hotel_order_write = 41;
	public static final int ID_view_poi_nearby_search = 42;
	public static final int ID_view_hotel_order_success = 43;
	public static final int ID_view_poi_result = 44;
	public static final int ID_view_measure_distance = 45;
	public static final int ID_max = 46;


	private static final Class<?>[] ViewID2Class = {

		HotelOrderListFragment.class,
		TuangouDetailFragment.class,
		MyCommentListFragment.class,
		DiscoverListFragment.class,
		HomeFragment.class,
		POIDetailFragment.class,
		FavoriteFragment.class,
		HotelHomeFragment.class,
		BuslineResultLineFragment.class,
		TrafficSearchHistoryFragment.class,
		CommonPlaceFragment.class,
		HistoryFragment.class,
		GoCommentFragment.class,
		CouponDetailFragment.class,
		TrafficResultFragment.class,
		CouponListFragment.class,
		HotelOrderDetailFragment.class,
		DiscoverChildListFragment.class,
		BuslineDetailFragment.class,
		AlarmAddFragment.class,
		CustomCategoryFragment.class,
		HotelOrderDetailFragment.class,
		SubwayMapFragment.class,
		BrowserFragment.class,
		TrafficQueryFragment.class,
		FetchFavoriteFragment.class,
		BuslineResultStationFragment.class,
		HotelOrderCreditFragment.class,
		UserHomeFragment.class,
		AlarmListFragment.class,
		InfoWindowFragment.class,
		ResultMapFragment.class,
		PickLocationFragment.class,
		MoreHomeFragment.class,
		InputSearchFragment.class,
		MyOrderFragment.class,
		ZhanlanDetailFragment.class,
		YanchuDetailFragment.class,
		DianyingDetailFragment.class,
		TrafficDetailFragment.class,
		EditTextFragment.class,
		HotelOrderWriteFragment.class,
		NearbySearchFragment.class,
		HotelOrderSuccessFragment.class,
		POIResultFragment.class,
		MeasureDistanceFragment.class
	};


	public final HotelOrderListFragment getHotelOrderListFragment() {
		return getFragment(ID_view_hotel_order_list);
	} 

	public final TuangouDetailFragment getTuangouDetailFragment() {
		return getFragment(ID_view_discover_tuangou_detail);
	} 

	public final MyCommentListFragment getMyCommentListFragment() {
		return getFragment(ID_view_user_my_comment_list);
	} 

	public final DiscoverListFragment getDiscoverListFragment() {
		return getFragment(ID_view_discover_list);
	} 

	public final HomeFragment getHomeFragment() {
		return getFragment(ID_view_home);
	} 

	public final POIDetailFragment getPOIDetailFragment() {
		return getFragment(ID_view_poi_detail);
	} 

	public final FavoriteFragment getFavoriteFragment() {
		return getFragment(ID_view_more_favorite);
	} 

	public final HotelHomeFragment getHotelHomeFragment() {
		return getFragment(ID_view_hotel_home);
	} 

	public final BuslineResultLineFragment getBuslineResultLineFragment() {
		return getFragment(ID_view_traffic_busline_line_result);
	} 

	public final TrafficSearchHistoryFragment getTrafficSearchHistoryFragment() {
		return getFragment(ID_view_traffic_search_history);
	} 

	public final CommonPlaceFragment getCommonPlaceFragment() {
		return getFragment(ID_view_more_common_places);
	} 

	public final HistoryFragment getHistoryFragment() {
		return getFragment(ID_view_more_history);
	} 

	public final GoCommentFragment getGoCommentFragment() {
		return getFragment(ID_view_more_go_comment);
	} 

	public final CouponDetailFragment getCouponDetailFragment() {
		return getFragment(ID_view_coupon_detail);
	} 

	public final TrafficResultFragment getTrafficResultFragment() {
		return getFragment(ID_view_traffic_result_transfer);
	} 

	public final CouponListFragment getCouponListFragment() {
		return getFragment(ID_view_coupon_list);
	} 

	public final HotelOrderDetailFragment getHotelOrderDetailFragment2() {
		return getFragment(ID_view_hotel_order_detail_2);
	} 

	public final DiscoverChildListFragment getDiscoverChildListFragment() {
		return getFragment(ID_view_discover_child_list);
	} 

	public final BuslineDetailFragment getBuslineDetailFragment() {
		return getFragment(ID_view_traffic_busline_detail);
	} 

	public final AlarmAddFragment getAlarmAddFragment() {
		return getFragment(ID_view_alarm_add);
	} 

	public final CustomCategoryFragment getCustomCategoryFragment() {
		return getFragment(ID_view_poi_custom_category);
	} 

	public final HotelOrderDetailFragment getHotelOrderDetailFragment() {
		return getFragment(ID_view_hotel_order_detail);
	} 

	public final SubwayMapFragment getSubwayMapFragment() {
		return getFragment(ID_view_subway_map);
	} 

	public final BrowserFragment getBrowserFragment() {
		return getFragment(ID_view_browser);
	} 

	public final TrafficQueryFragment getTrafficQueryFragment() {
		return getFragment(ID_view_traffic_home);
	} 

	public final FetchFavoriteFragment getFetchFavoriteFragment() {
		return getFragment(ID_view_traffic_fetch_favorite_poi);
	} 

	public final BuslineResultStationFragment getBuslineResultStationFragment() {
		return getFragment(ID_view_traffic_busline_station_result);
	} 

	public final HotelOrderCreditFragment getHotelOrderCreditFragment() {
		return getFragment(ID_view_hotel_credit_assure);
	} 

	public final UserHomeFragment getUserHomeFragment() {
		return getFragment(ID_view_user_home);
	} 

	public final AlarmListFragment getAlarmListFragment() {
		return getFragment(ID_view_alarm_list);
	} 

	public final InfoWindowFragment getInfoWindowFragment() {
		return getFragment(ID_view_infowindow);
	} 

	public final ResultMapFragment getResultMapFragment() {
		return getFragment(ID_view_result_map);
	} 

	public final PickLocationFragment getPickLocationFragment() {
		return getFragment(ID_view_hotel_pick_location);
	} 

	public final MoreHomeFragment getMoreHomeFragment() {
		return getFragment(ID_view_more_home);
	} 

	public final InputSearchFragment getInputSearchFragment() {
		return getFragment(ID_view_poi_input_search);
	} 

	public final MyOrderFragment getMyOrderFragment() {
		return getFragment(ID_view_more_my_order);
	} 

	public final ZhanlanDetailFragment getZhanlanDetailFragment() {
		return getFragment(ID_view_discover_zhanlan_detail);
	} 

	public final YanchuDetailFragment getYanchuDetailFragment() {
		return getFragment(ID_view_discover_yanchu_detail);
	} 

	public final DianyingDetailFragment getDianyingDetailFragment() {
		return getFragment(ID_view_discover_dianying_detail);
	} 

	public final TrafficDetailFragment getTrafficDetailFragment() {
		return getFragment(ID_view_traffic_result_detail);
	} 

	public final EditTextFragment getEditTextFragment() {
		return getFragment(ID_view_edit_text);
	} 

	public final HotelOrderWriteFragment getHotelOrderWriteFragment() {
		return getFragment(ID_view_hotel_order_write);
	} 

	public final NearbySearchFragment getNearbySearchFragment() {
		return getFragment(ID_view_poi_nearby_search);
	} 

	public final HotelOrderSuccessFragment getHotelOrderSuccessFragment() {
		return getFragment(ID_view_hotel_order_success);
	} 

	public final POIResultFragment getPOIResultFragment() {
		return getFragment(ID_view_poi_result);
	} 

	public final MeasureDistanceFragment getMeasureDistanceFragment() {
		return getFragment(ID_view_measure_distance);
	} 

//----generate end----

    private HashMap<Integer, BaseFragment> mFragmentHashmap = new HashMap<Integer, BaseFragment>();
    private Object mUILock = new Object();
    private Sphinx mSphinx;
    private TitleFragment mTitleFragment;
    private HomeBottomFragment mHomeBottomFragment;
    
    public TKFragmentManager(Sphinx s) {
        mSphinx = s;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getFragment(int id) {

        synchronized (mUILock) {
            Integer key = id;
            if (id < ID_max && id > ID_view_invalid) {
                if (mFragmentHashmap.containsKey(key)) {
                    return (T)mFragmentHashmap.get(key);
                } else {
                    Constructor<?> a;
                    BaseFragment f = null;
                    Class<?> c = ViewID2Class[id];
                    try {
                        a = c.getConstructor(Sphinx.class);
                        f = (BaseFragment) a.newInstance(mSphinx);
                        f.setId(id);
                        f.onCreate(null);
                        mFragmentHashmap.put(key, f);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return (T)f;
                }
            } else {
                return null;
            }
        }
    }
    
    public final BaseFragment getBaseFragment(int id) {
        return getFragment(id);
    }
    
    public TitleFragment getTitleFragment() {
        synchronized (mUILock) {
            if (mTitleFragment == null) {
                TitleFragment fragment = new TitleFragment(mSphinx);
                fragment.onCreate(null);
                fragment.setVisibility(View.GONE);
                mSphinx.getTitleView().addView(fragment);
                mTitleFragment = fragment;
            }
            return mTitleFragment;
        }
    }

    public HomeBottomFragment getHomeBottomFragment() {
        synchronized (mUILock) {
            if (mHomeBottomFragment == null) {
                HomeBottomFragment fragment = new HomeBottomFragment(mSphinx);
                fragment.onCreate(null);
                mHomeBottomFragment = fragment;
            }
            return mHomeBottomFragment;
        }
    }
    
    public final boolean hasCreatedFragment(int id) {
        return mFragmentHashmap.containsKey(id);
    }
    
    public final void clearFragment(int id) {
        mFragmentHashmap.remove(id);
    }
    
}