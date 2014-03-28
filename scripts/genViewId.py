#!/usr/bin/python
import sys, re

IDs = {
	"ID_view_infowindow":"com.tigerknows.ui.InfoWindowFragment",
	"ID_view_more_home":"com.tigerknows.ui.more.MoreHomeFragment",
	"ID_view_home":"com.tigerknows.ui.HomeFragment",
	"ID_view_more_my_order":"com.tigerknows.ui.more.MyOrderFragment",
	"ID_view_result_map":"com.tigerknows.ui.common.ResultMapFragment",
	"ID_view_browser":"com.tigerknows.ui.common.BrowserFragment",
	"ID_view_more_favorite":"com.tigerknows.ui.more.FavoriteFragment",
	"ID_view_more_history":"com.tigerknows.ui.more.HistoryFragment",
	"ID_view_poi_result":"com.tigerknows.ui.poi.POIResultFragment",
	"ID_view_poi_detail":"com.tigerknows.ui.poi.POIDetailFragment",
	"ID_view_poi_input_search":"com.tigerknows.ui.poi.InputSearchFragment",
	"ID_view_poi_nearby_search":"com.tigerknows.ui.poi.NearbySearchFragment",
	"ID_view_poi_custom_category":"com.tigerknows.ui.poi.CustomCategoryFragment",
	"ID_view_traffic_result_transfer":"com.tigerknows.ui.traffic.TrafficResultFragment",
	"ID_view_traffic_result_detail":"com.tigerknows.ui.traffic.TrafficDetailFragment",
	"ID_view_traffic_busline_line_result":"com.tigerknows.ui.traffic.BuslineResultLineFragment",
	"ID_view_traffic_busline_station_result":"com.tigerknows.ui.traffic.BuslineResultStationFragment",
	"ID_view_traffic_busline_detail":"com.tigerknows.ui.traffic.BuslineDetailFragment",
	"ID_view_traffic_home":"com.tigerknows.ui.traffic.TrafficQueryFragment",
	"ID_view_more_common_places":"com.tigerknows.ui.more.CommonPlaceFragment",
	"ID_view_traffic_search_history":"com.tigerknows.ui.traffic.TrafficSearchHistoryFragment",
	"ID_view_traffic_fetch_favorite_poi":"com.tigerknows.ui.traffic.FetchFavoriteFragment",
	"ID_view_subway_map":"com.tigerknows.ui.traffic.SubwayMapFragment",
	"ID_view_user_home":"com.tigerknows.ui.user.UserHomeFragment",
	"ID_view_user_my_comment_list":"com.tigerknows.ui.user.MyCommentListFragment",
	"ID_view_more_go_comment":"com.tigerknows.ui.more.GoCommentFragment",
	"ID_view_discover_list":"com.tigerknows.ui.discover.DiscoverListFragment",
	"ID_view_discover_tuangou_detail":"com.tigerknows.ui.discover.TuangouDetailFragment",
	"ID_view_discover_child_list":"com.tigerknows.ui.discover.DiscoverChildListFragment",
	"ID_view_discover_yanchu_detail":"com.tigerknows.ui.discover.YanchuDetailFragment",
	"ID_view_discover_dianying_detail":"com.tigerknows.ui.discover.DianyingDetailFragment",
	"ID_view_discover_zhanlan_detail":"com.tigerknows.ui.discover.ZhanlanDetailFragment",
	"ID_view_hotel_home":"com.tigerknows.ui.hotel.HotelHomeFragment",
	"ID_view_hotel_pick_location":"com.tigerknows.ui.hotel.PickLocationFragment",
	"ID_view_hotel_order_write":"com.tigerknows.ui.hotel.HotelOrderWriteFragment",
	"ID_view_hotel_credit_assure":"com.tigerknows.ui.hotel.HotelOrderCreditFragment",
	"ID_view_hotel_order_list":"com.tigerknows.ui.hotel.HotelOrderListFragment",
	"ID_view_hotel_order_success":"com.tigerknows.ui.hotel.HotelOrderSuccessFragment",
	"ID_view_hotel_order_detail":"com.tigerknows.ui.hotel.HotelOrderDetailFragment",
	"ID_view_hotel_order_detail_2":"com.tigerknows.ui.hotel.HotelOrderDetailFragment",
	"ID_view_coupon_list":"com.tigerknows.ui.poi.CouponListFragment",
	"ID_view_coupon_detail":"com.tigerknows.ui.poi.CouponDetailFragment",
	"ID_view_measure_distance":"com.tigerknows.ui.common.MeasureDistanceFragment",
	"ID_view_alarm_list":"com.tigerknows.ui.alarm.AlarmListFragment",
	"ID_view_alarm_add":"com.tigerknows.ui.alarm.AlarmAddFragment",
	"ID_view_edit_text":"com.tigerknows.ui.common.EditTextFragment"

		}

classesTemp = """
	private static final Class<?>[] ViewID2Class = {
%s
	};
"""

facadeFuncTemp = """
	public final %s get%s() {
		return getFragment(%s);
	} 
"""

if __name__ == "__main__":
	i = 0
	path = "../src/com/tigerknows/"
	filename = file(path + "TKFragmentManager.java", 'r+')
	key_head_str = "\tpublic static final int %s = %d;\n"
	class_str = "\n\t\t%s.class,"
	#imports_str = "import %s;\n"
	classes = ""
	#imports = ""
	facadeFunc = ""

	keys = key_head_str % ("ID_view_invalid", -1)

	content = filename.read()
	m = re.search("[\s\S]+//-*generate.+start.+\n", content)
	start = m and m.group(0) or ""
	m = re.search("//-*generate.+end[\s\S]+", content)
	end = m and m.group(0) or ""

	for k,v in IDs.items():
		class_name = v.split(".")[-1]
		keys += key_head_str % (k, i)
		classes += class_str % class_name
		#imports += imports_str % v
		func_name = class_name + (k[-1].isdigit() and k[-1] or "")
		facadeFunc += facadeFuncTemp % (class_name, func_name, k)
		i += 1

	keys += key_head_str % ("ID_max", i)
	classes = classes.rstrip(',')
	keys_content = keys
	class_content = classesTemp % classes
	script_name = sys.argv[0].split('/')[-1]
	final_content = '\n'.join([start, keys_content, class_content, facadeFunc, end])
	#print final_content
	filename.seek(0)
	filename.truncate()
	filename.write(final_content)
