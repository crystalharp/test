/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */
package com.tigerknows.common;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.CalendarUtil;

import android.content.Context;
import android.text.TextUtils;

import java.text.SimpleDateFormat;

/**
 * 用户行为日志类
 * @author pengwenyue
 *
 */
public class ActionLog extends LogUpload {

    static final String TAG = "ActionLog";
    
    public static final String SEPARATOR_STAET = "_";
    public static final String SEPARATOR_MIDDLE = "-";
    
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    
    private static ActionLog sActionLog;
    public static ActionLog getInstance(Context context) {
        if (sActionLog == null) {
            sActionLog = new ActionLog(context, "action.log", FeedbackUpload.SERVER_PARAMETER_ACTION_LOG);
        }
        return sActionLog;
    }

    // 物理返回键
    public static final String KeyCodeBack = "ZA";
    
    // 对话框
    public static final String Dialog = "ZB";
    public static final String DialogLeftBtn = Dialog + "BA";
    public static final String DialogRightBtn = Dialog + "BB";

    // 底部导航栏
    public static final String MenuPOI = "ZC";
    public static final String MenuDiscover = "ZD";
    public static final String MenuTraffic = "ZE";
    public static final String MenuMore = "ZF";
    
    /**
     * 网络查询
     * 例如 _324-ZG-s-324733-324833-324900-serverrefuse
     * 发起http请求 的毫秒时刻
     * 收到服务器的数据 的毫秒时刻
     * 收完服务器的数据 的毫秒时刻
     * 失败时候填的内容 注意，为避免异常字符干扰日志格式，请做正则替换reg_replace_all(FAIL, "[0-9a-zA-Z]+", "")，去除所有非数字和字母字符。
     * 
     * 补充说明：
     * 1、这个需求实际关心的是 “我们联网服务质量是否稳定”，暂不考虑底层DNS花多少时间。
     * 2、只关心联网步骤之间的时间差，REQ（请求） 的起始时间设置为多少，根据客户端的实现方便即可，设置为0也可以。由我这边计算时间差。
     * 3、必须记录的at有 p（换乘）,q（驾车）,s（搜索/发现）,dh（热点),dc(类别),f(登陆),lm（定位）,b(线路）,边看边下载（td,rq等）、及以后任何较重要的联网服务。
     * 4、无论成功失败都必须记录联网请求。
     * 5、由于边看边下载的请求数可能会比较多，为了保证 客户端日志总长度20K的限制 可以合理运行，规则做了修改。请查看上文“序列长度超过20K”处情况说明。
     */
    public static final String NetworkAction = "ZG";
    
    // 服务器返回的结果
    public static final String Response = "ZH";
    
    // 顶部导航栏
    public static final String TitleLeftButton = "ZA";
    public static final String TitleRightButton = "ZB";
    
    // PopupWindow
    public static final String PopupWindowFilter = "ZC";
    public static final String PopupWindowFilterGroup = PopupWindowFilter + "BA";
    public static final String PopupWindowFilterChild = PopupWindowFilter + "BB";
    
    public static final String PopupWindowTitle = "ZD";
    
    // UI关闭
    public static final String Dismiss = "ZE";
    
    // ViewPage
    public static final String ViewPageSelected = "ZF";
    
    // ListView
    public static final String ListViewItem = "ZG";
    public static final String ListViewItemHistory = "ZH";
    public static final String ListViewItemSuggest = "ZI";
    public static final String ListViewItemHistoryClear = "ZJ";
    public static final String ListViewItemMore = "ZK";
    public static final String ListViewItemLong = "ZL";
    
    // Filter
    public static final String FilterArea = "ZM";
    public static final String FilterCategory = "ZO";
    public static final String FilterOrder = "ZP";
    public static final String FilterDo = "ZQ";
    public static final String FilterCancel = "ZR";
    
    // EditText
    public static final String EditTextDelete = "ZS";
    
    // 触屏重试
    public static final String TouchRetry = "ZT";
    
    // 分享
    public static final String Share = "ZU";
    
    // 到这里去
    public static final String GotoHere = "ZV";

    // 日志超长
    public static final String LogOut = "ZW";
    
    public static final String HistoryWordInput = "ZX";
    
    // == 共用 ==
    public static final String CommonAddress = "ZX";
    public static final String CommonTelphone = "ZY";
    public static final String CommonFavorite = "ZZ";
    public static final String CommonShare = "YA";
    public static final String CommonErrorRecovery = "YB";

    public static final String HotelDate = "YC";
    public static final String HotelDateDone = HotelDate+"BA";
    public static final String HotelDateCheckin = HotelDate+"BB";
    public static final String HotelDateCheckout = HotelDate+"BC";
    
    public static final String PickPhoto = "YD";
    public static final String CameraPhoto = "YF";
    public static final String FilterAreaChar = "YG";
    
    // POI主页 
    public static final String POIHome = "AA";
    public static final String POIHomeChangeCityBtn = "BA";
    public static final String POIHomeInputEdt = "BB";
    public static final String POIHomeCategory = "BC";    
    public static final String POIHomeSubcategoryPressed = "BD";
    public static final String POIHomeSubcategoryOpenedOnFling = "BE";
    public static final String POIHomeSubcategoryOpenedOnClick = "BF";
    public static final String POIHomeSubcategoryClosedOnFling = "BG";
    public static final String POIHomeSubcategoryClosedOnClick = "BH";
    public static final String POIHomeSubcategoryClosedOnBack = "BI";
    // BG_{dataType}
    public static final String POIHomeDynamicButtonClick = "BJ";
    public static final String POIHomeMyLocation = "BK";
    public static final String POIHomeAppointedArea = "BL";
    public static final String POIHomeSubwayMap = "BM";
    public static final String POIHomeDish = "BN";
    
    // POI输入查询页
    public static final String POIHomeInputQuery = "AB";
    public static final String POIHomeInputQueryBtn = "BA";

    // POI结果列表页
    public static final String POIList = "AC";
    public static final String POIListAddMerchanty = "BA";
    
    // Poi详情页
    public static final String POIDetail = "AD";
    public static final String POIDetailShow = POIDetail+"BA";
    public static final String POIDetailSearch = "BF";
    public static final String POIDetailAllComment = "BG";
    public static final String POIDetailComment = "BH";
    public static final String POIDetailInput = "BJ";
    public static final String POIDetailYanchu = "BK";
    public static final String POIDetailZhanlan = "BL";
    public static final String POIDetailTuangou = "BM";
    public static final String POIDetailPullFailed = "BN";
    public static final String POIDetailDianying = "BO";
    public static final String POIDetailDianyingMore = "BP";
    public static final String POIDetailFromWeixin = "BQ";
    public static final String POIDetailWeixinRequest = "BR";
    public static final String POIDetailWeixinSend = "BS";
    public static final String POIDetailHotelClickDate = "BT";
    public static final String POIDetailHotelClickRoomItem = "BU";
    public static final String POIDetailHotelRoomBookBtn = "BV";
    public static final String POIDetailHotelMoreRoom = "BW";
    public static final String POIDetailHotelImage = "BX";
    public static final String POIDetailHotelIntro = "BY";
    public static final String POIDetailHotelFailRetry = "BZ";
    public static final String POIDetailCouponSingle = "CA";
    public static final String POIDetailCouponMulti = "CB";
    public static final String POIDetailBusstop = "CD";
    public static final String POIDetailMoreSameCategory = "CE";
    public static final String POIDetailSameCategory = "CF";
    public static final String POIDetailDish = "CG";
    public static final String POIDetailShopkeeper = "CH";
    public static final String POIDetailDishList = "CI";

    // POI点评列表页
    public static final String POICommentList = "AE";
    public static final String POICommentListUrl = "BA";
    public static final String POICommentListMyComment = "BB";
    public static final String POICommentListInput = "BC";
    public static final String POICommentListAllComment = "BD";
    public static final String POICommentListHotComment = "BE";
    public static final String POICommentListCommend = "BF";

    // 点评输入页
    public static final String POIComment = "AF";
    public static final String POICommentGrade = "BA";
    public static final String POICommentContent = "BB";
    public static final String POICommentAvg = "BC";
    public static final String POICommentTaste = "BD";
    public static final String POICommentEnvironment = "BE";
    public static final String POICommentQos = "BF";
    public static final String POICommentRestair = "BG";
    public static final String POICommentRecommend = "BH";
    public static final String POICommentSina = "BI";
    public static final String POICommentQZone = "BJ";

    // 周边搜索页
    public static final String POINearbySearch = "AG";
    public static final String POINearbySearchCategory = "BA";
    public static final String POINearbySearchSubimt = "BB";
    public static final String POINearbySearchInput = "BC";
    
    // POI纠错页
    public static final String POIReportError = "AH";
    public static final String POIReportErrorName = "BA";
    public static final String POIReportErrorTelAdd = "BB";
    public static final String POIReportErrorTel = "BC";
    public static final String POIReportErrorNotExist = "BD";
    public static final String POIReportErrorAddress = "BE";
    public static final String POIReportErrorRedundancy = "BF";
    public static final String POIReportErrorLocation = "BG";
    public static final String POIReportErrorOther = "BH";
    public static final String POIReportErrorSubmit = "BI";
    public static final String POIReportErrorNext = "BJ";
    
    // 我的点评页
    public static final String MyComment = "AI";
    public static final String MyCommentPOI = "BA";

    // 我要点评页
    public static final String GoComment = "AJ";
    public static final String GoCommentInput = "BA";
    
    // POI列表地图页
    public static final String POIListMap = "AK";
    
    // POI详情地图页
    public static final String POIDetailMap = "AL";
    public static final String POIHotelListMap = "AU";
    public static final String POIHotelDetailMap = "AV";

    public static final String Dish = "AX";
    public static final String DishRecomend = "BA";
    public static final String DishAll = "BB";
    public static final String DishMyLike = "BC";
    public static final String DishTigerRecommend = "BD";
    public static final String DishLike = "BE";
    public static final String DishCancelLike = "BF";
    
    // 优惠券列表页
    public static final String CouponList = "AM";
    
    // 优惠券详情页
    public static final String CouponDetail = "AN";
    public static final String CouponQrimg = "BA";

    // 更多分类页
    public static final String POIMoreCategory = "AO";
    public static final String POIMoreCategoryLevelOne = "BA";
    public static final String POIMoreCategoryLevelTwo = "BB";
    
    // POI纠错详情页-公共部分
    public static final String POIErrorDetailMain = "BA";
    public static final String POIErrorDetailContact = "BB";
    public static final String POIErrorDetailSubmit = "BC";
    
    // POI纠错-名称错误页
    public static final String POINameError = "AO";
    public static final String POINameErrorType = "BD";

    // POI纠错-地址错误页
    public static final String POIAddressError = "AP";

    // POI纠错-添加电话页
    public static final String POIAddTel = "AQ";

    // POI纠错-电话错误页
    public static final String POITelError = "AR";
    public static final String POITelErrorConnect = "BD";
    public static final String POITelErrorNotthis = "BE";
    // POI纠错-地点不存在页
    
    public static final String POINotExist = "AS";
    public static final String POINotExistStop = "BD";
    public static final String POINotExistChai = "BE";
    public static final String POINotExistMove = "BF";
    public static final String POINotExistFind = "BG";
    public static final String POINotExistOther = "BH";

    // POI纠错-其他错误页
    public static final String POIOtherError = "AT";

    // 更多频道
    public static final String More = "CA";
    public static final String MoreGoComment = "BA";
    public static final String MoreLoginRegist = "BB";
    public static final String MoreChangeCity = "BC";
    public static final String MoreMapDownload = "BD";
    public static final String MoreAppRecommend = "BE";
    public static final String MoreFavorite = "BF";
    public static final String MoreHistory = "BG";
    public static final String MoreSetting = "BH";
    public static final String MoreFeedback = "BI";
    public static final String MoreHelp = "BJ";
    public static final String MoreAboutUs = "BK";
    public static final String MoreUserHome = "BM";
    public static final String MoreSoftwareUpdate = "BN";
    public static final String MoreGiveFavourableComment = "BO";
    public static final String MoreAddMerchant = "BQ";
    public static final String MoreSatisfyRate = "BR";
    public static final String MoreAppDownload = "BS";
    public static final String MoreNotice = "BT";
    public static final String MoreDingdan = "BU";
    
    // 切换城市页
    public static final String ChangeCity = "CB";
    public static final String ChangeCityInput =  "BA";
    public static final String ChangeCityCity = "BB";
    public static final String ChangeCityProvince = "BC";
    public static final String ChangeCitySuggest = "BD";

    // 地图下载页
    public static final String MapDownload = "CC";
    public static final String MapDownloadAddCityBtn = "BA";
    public static final String MapDownloadDownloadListCity = "BB";
    public static final String MapDownloadDownloadListProvince = "BC";
    public static final String MapDownloadOpertorDelete = "BD";
    public static final String MapDownloadOpertorDownload = "BE";
    public static final String MapDownloadOpertorUpgrade = "BF";
    public static final String MapDownloadOpertorPause = "BG";
    public static final String MapDownloadInput = "BH";
    public static final String MapDownloadCloseWifi = "BI";
    public static final String MapDownloadAllStart = "BJ";
    public static final String MapDownloadAllPause = "BK";
    public static final String MapDownloadAllUpdate = "BL";
    public static final String MapDownloadSuggest = "BM";
    public static final String MapDownloadAddListCity = "BN";
    public static final String MapDownloadAddListProvince = "BO";

    // 收藏夹页
    public static final String Favorite = "CD";
    public static final String FavoriteMenuDelete = "BA";
    public static final String FavoriteMenuRename = "BB";
    public static final String FavoritePOI = "BC";
    public static final String FavoriteTraffic = "BD";

    // 历史浏览页
    public static final String History = "CE";
    public static final String HistoryMenuDelete = "BA";
    public static final String HistoryPOI = "BC";
    public static final String HistoryTraffic = "BD";

    // 系统设置页
    public static final String Setting = "CF";
    public static final String SettingGPS = "BA";
    public static final String SettingWakeLock = "BB";
    public static final String SettingRadar = "BC";
    public static final String SettingClearCache = "BD";
    public static final String SettingHelp = "BE";
    public static final String SettingAboutMe = "BF";

    // 意见反馈页 
    public static final String Feedback = "CG";
    public static final String FeedBackContent = "BA";
    public static final String FeedbackMobile = "BB";

    // 帮助页
    public static final String Help = "CH";

    // 关于我们页
    public static final String AboutUs = "CI";
    public static final String AboutUsWeb = "BA";
    public static final String AboutUsWeibo = "BB";
    public static final String AboutUsBBS = "BC";
    public static final String AboutUsTelephone = "BD";

    // 应用推荐 AO
    public static final String AppRecommend = "CJ";
    public static final String AppRecommendSnda = "BA";

    // 添加商户页
    public static final String AddMerchant = "CK";
    public static final String AddMerchantType = "BA";
    public static final String AddMerchantName = "BB";
    public static final String AddMerchantAddress = "BC";
    public static final String AddMerchantTelephone = "BD";
    public static final String AddMerchantTime = "BE";
    public static final String AddMerchantMobile = "BF";
    public static final String AddMerchantAddressDescription = "BG";
    public static final String AddMerchantDate = "BH";
    public static final String AddMerchantPhoto = "BI";
    public static final String AddMerchantDeletePhoto = "BJ";
    public static final String AddMerchantConfirmPhoto = "BK";
    public static final String AddMerchantCancelPhoto = "BL";
    public static final String AddMerchantConfirmTime = "BM";
    public static final String AddMerchantCity = "BN";
    public static final String AddMerchantBackPhoto = "BO";

    // 添加图片页
    public static final String AddPicture = "CN";
    public static final String AddPictureCancel = "BA";
    public static final String AddPictureConfirm = "BB";
    
    // 满意度评分页
    public static final String SatisfyRate = "CL";
    public static final String[] SatisfyRatingBar = {"BA", "BB", "BC", "BD", "BE", "BF", "BG"};
    public static final String SatisfyRateSubmit = "BH";
    
    // 我的订单页
    public static final String MyAllOrder = "CM";
    public static final String MyAllOrderTuangou = "BA";
    public static final String MyAllOrderHotel = "BB";

    // 软件生命周期  AO
    public static final String Lifecycle = "FA";
    public static final String LifecycleCreate = Lifecycle + "BA";
    public static final String LifecycleDestroy = Lifecycle + "BB";
    public static final String LifecycleResume = Lifecycle + "BC";
    public static final String LifecyclePause = Lifecycle + "BD";
    public static final String LifecycleStop = Lifecycle + "BE";
    public static final String LifecycleSelectCity = Lifecycle + "BF";
    public static final String LifecycleUserReadSuccess = Lifecycle + "BG";
    public static final String LifecycleFirstLocationSuccess = Lifecycle + "BH";
    public static final String LifecycleWeixinWeb = Lifecycle + "BI";
    public static final String LifecycleWeixinRequest = Lifecycle + "BJ";
    
    // 地图
    public static final String Map = "FB";
    public static final String MapLocation = Map + "BA";
    public static final String MapDoubleClick = Map + "BB";
    public static final String MapLongClick = Map + "BC";
    public static final String MapMove = Map + "BD";
    public static final String MapStepUp = Map + "BE";
    public static final String MapStepDown = Map + "BF";
    public static final String MapZoomIn = Map + "BG";
    public static final String MapZoomOut = Map + "BH";
    public static final String MapInfoWindowBody = Map + "BI";
    public static final String MapInfoWindowTraffic = Map + "BJ";
    public static final String MapInfoWindowStart = Map + "BK";
    public static final String MapInfoWindowEnd = Map + "BL";
    public static final String MapMultiTouchZoom = Map + "BM";
    public static final String MapMore = Map + "BN";
    public static final String MapTakeScreenshot = Map + "BO";
    public static final String MapDistance = Map + "BP";
    public static final String MapCompass = Map + "BQ";
    
    // 微博发送界面    
    public static final String WeiboSend = "FC";
    public static final String WeiboSendDelWord = "BA";
    public static final String WeiboSendPic = "BB";
    
    // 显示微博图片对话框
    public static final String WeiboImage = "FD";
    public static final String WeiboImageDelPic = "BA";
    
    // QQ空间发送页面
    public static final String QzoneSend = "FE";
        
    // 雷达推送
    public static final String Radar = "FF";
    public static final String RadarShow = Radar + "BA";
    public static final String RadarClick = Radar + "BB";
    public static final String RadarPushFailed = Radar + "BC";
    public static final String RadarPushSucceeded = Radar + "BD";
    
    // 交通模块通用
    public static final String TrafficTransferTab = "AA";
    public static final String TrafficDriveTab = "AB";
    public static final String TrafficWalkTab = "AC";
    public static final String TrafficBusLineTab = "AD";
    public static final String TrafficTrafficBtn = "AE";
    public static final String TrafficBuslineBtn = "AF";
    public static final String TrafficStartEdt = "AG";
    public static final String TrafficEndEdt = "AH";
    public static final String TrafficBusLineEdt = "AI";
    public static final String TrafficStartBtn = "AJ";
    public static final String TrafficEndBtn = "AK";
    public static final String TrafficResultTraffic = "AN";
    public static final String TrafficResultBusline = "AO";

    // 交通查询首页
    public static final String TrafficHomeNormal = "BA";
    
    // 交通查询地图页
    public static final String TrafficHomeMap = "BB";
    
    // 交通查询输入页
    public static final String TrafficHomeInput = "BC";
    
    // 交通查询单击选点
    public static final String TrafficHomeSelectPoint = "BD";
    
    // 收藏的POI列表页
    public static final String TrafficFetchFavorite = "BE";
    
    // 交通备选站点页显示 
    public static final String TrafficAlternative = "BF";
    public static final String TrafficAlterStart = "BA";
    public static final String TrafficAlterEnd = "BB";

    // 交通方案列表页显示
    public static final String TrafficResult = "BG";

    // 交通方案详情页显示
    public static final String TrafficDetail = "BH";

    // 交通线路列表页显示
    public static final String TrafficBusline = "BI";

    // 交通站点列表页显示
    public static final String TrafficStation = "BJ";

    // 线路详情页
    public static final String TrafficLineDetail = "BK";
    
    // 交通换乘纠错页
    public static final String TransferErrorRecovery = "BL";
    
    public static final String TrafficBuslineMap = "BM";
    public static final String TrafficTransferMap = "BN";
    public static final String TrafficDriveMap = "BO";
    public static final String TrafficWalkMap = "BP";
    
    // 交通指南针页
    public static final String TrafficCompass = "BQ";
    public static final String TrafficCompassGPS = "BA";
    public static final String TrafficCompassShare = "BB";
    
    // 地铁图页
    public static final String TrafficSubwayMap = "BR";
    public static final String TrafficSubwayMapClickPOI = "BA";
    public static final String TrafficSubwayMapClickSearch = "BB";
    
    // == 用户模块通用 ==
    public static final String UserCommonLoginBtn = "AA";
    public static final String UserCommonRegisterBtn = "AB";
    public static final String UserCommonForgetPasswordBtn = "AC";
    public static final String UserCommonValidNumBtn = "AD";
    public static final String UserCommonConfirmBtn = "AE";
    
    // == 登录页面 ==
    public static final String UserLogin = "EA";
    public static final String UserGoRegist = "BA";
    
    // == 注册页面 ==
    public static final String UserRegist = "EB";
    public static final String UserGoLogin = "BA";
    
    // == 个人中心页面 == 
    public static final String UserHome = "EC";
    public static final String UserHomeMyComment = "BA";
    public static final String UserHomeUpdatePhone = "BB";
    public static final String UserHomeUpdateName = "BC";
    public static final String UserHomeUpdatePassword = "BD";
    public static final String UserHomeLogout = "BE";
    //BF 已使用，下一个定义请从BG开始
    //public static final String User????? = "BG";

    
    // == 更换手机号 ==
    public static final String UserUpdatePhone = "ED";
    
    // == 修改昵称 ==
    public static final String UserUpdateNickName = "EE";
    
    // == 重置密码页面 ==
    public static final String UserResetPassword = "EF";
    
    // == 修改密码页面 ==
    public static final String UserUpdatePassword = "EG";
    
    // == 点评跳转过来的登录页面 ==
    public static final String UserCommentAfter = "EH";
    
    // == 发现通用 ==
    public static final String DiscoverCommonBranch = "AA";
    public static final String DiscoverCommonDianyingToday = "AB";
    public static final String DiscoverCommonDianyingTomorrow = "AC";
    public static final String DiscoverCommonDianyingAfterTomorrow = "AD";

    // 发现首页显示
    public static final String DiscoverHome = "DA";
    public static final String DiscoverHomeItem = "BA";
    
    // 团购结果列表页
    public static final String TuangouList = "DB";
    public static final String TuangouListDingdan = "BA";
    
    // 订单查询页
    public static final String DingdanList = "DC";
    
    // 团购列表地图页
    public static final String ResultMapTuangouList = "DD";

    // 团购详情页
    public static final String TuangouDetail = "DE";
    public static final String TuangouDetailBuy = "BA";
    public static final String TuangouDetailCustomService = "BB";
    public static final String TuangouDetailRefund = "BC";
    public static final String TuangouDetailPicutreTextDetail = "BD";
    
    // 团购详情页地图页
    public static final String ResultMapTuangouDetail = "DF";

    // 团购商户页
    public static final String FendianList = "DG";
    
    // 团购分店列表地图页
    public static final String ResultMapTuangouBranchList = "DH";

    // 团购购买页
    public static final String Browser = "DI";
    public static final String BrowserForward = "BA";
    public static final String BrowserBack = "BB";
    public static final String BrowserRefresh = "BC";
    public static final String BrowserBuy = "BD";
    public static final String BrowserReadyAlipay = "BE";
    public static final String BrowserLaunchAlipay = "BF";
    
    // 展览结果列表页
    public static final String YanchuList = "DJ";
    
    // 展览结果列表地图页
    public static final String ResultMapYanchuList = "DK";
    
    // 演出详情页
    public static final String YanchuDetail = "DL";
    
    // 演出详情地图页
    public static final String ResultMapYanchuDetail = "DM";
    
    // 展览结果列表页
    public static final String ZhanlanList = "DN";
    
    // 展览结果列表地图页
    public static final String ResultMapZhanlanList = "DO";
    
    // 展览详情页显示
    public static final String ZhanlanDetail = "DP";
    
    // 展览详情地图页
    public static final String ResultMapZhanlanDetail = "DQ";
    
    // 电影结果列表页
    public static final String DianyingList = "DR";
    
    // 电影详情页显示
    public static final String DianyingDetail = "DS";
    
    // 电影详情地图页
    public static final String ResultMapDianyingDetail = "DT";

    // 影院列表页
    public static final String YingxunList = "DU";
    
    // 影院列表地图页
    public static final String ResultMapDianyingBranchList = "DV";
    
    public static final String HotelQuery = "GA";
    public static final String HotelQueryCity = "BA";
    public static final String HotelQueryLocation = "BB";
    public static final String HotelQueryDate = "BC";
    public static final String HotelQueryCategory = "BD";
    public static final String HotelQuerySubmit = "BE";
    public static final String HotelQueryOrder = "BF";
    
    public static final String HotelPickLocation = "GB";
    public static final String HotelPickLocationInput = "BA";
    public static final String HotelPickLocationSubmit = "BB";
    public static final String HotelPickLocationAlternative = "BC";
    public static final String HotelPickLocationAlternativeSelect = "BD";
    
    public static final String ViewPicture = "GC";

    // 酒店简介页
    public static final String HotelIntro = "GD";
    
    // 订单填写页
    public static final String HotelOrderWrite = "GE";
    public static final String HotelOrderWriteHowmany = "BA";   
    public static final String HotelOrderWriteHowmanyChoose = "BB";
    public static final String HotelOrderWriteReserve = "BC";
    public static final String HotelOrderWriteBookName = "BD";
    public static final String HotelOrderWriteMobile = "BE";
    //public static final String HotelOrderWriteIdCard = "BF";
    public static final String HOTELOrderWriteSubmit = "BG";

    // 信用卡担保页
    public static final String HotelOrderCredit = "GF";
    public static final String HotelOrderCreditBank = "BA";
    public static final String HotelOrderCreditBankChoose = "BB";
    public static final String HotelOrderCreditCode = "BC";
    public static final String HotelOrderCreditOwner = "BD";
    public static final String HotelOrderCreditVerify = "BE";
    public static final String HotelOrderCreditValidate = "BF";
    public static final String HotelOrderCreditValidateChoose = "BG";
    public static final String HotelOrderCreditCertType = "BH";
    public static final String HotelOrderCreditCertCode = "BI";
    public static final String HotelOrderCreditSubmit = "BJ";
    
    // 订单列表页
    public static final String HotelOrderList = "GH";
    public static final String HotelOrderListGoThere = "BA";
    // BB-%d 被点击的订单的 index
    public static final String HotelOrderListItemClick = "BB";
    public static final String HotelOrderListServiceTel = "BK";

    // 订单详情页
    public static final String HotelOrderDetail = "GI";
    public static final String HotelOrderDetailClickName = "BA";
    public static final String HotelOrderDetailPhone = "BB";
    public static final String HotelOrderDetailCancel= "BC";
    public static final String HotelOrderDetailCancelDialogYes= "BD";
    public static final String HotelOrderDetailCancelDialogNo= "BE";
    public static final String HotelOrderDetailDelete= "BF";
    public static final String HotelOrderDetailDeleteDialogYes= "BG";
    public static final String HotelOrderDetailDeleteDialogNo = "BH";
    public static final String HotelOrderDetailIssueComment = "BI";
    public static final String HotelOrderDetailOrderAgain= "BJ";
    public static final String HotelOrderDetailServiceTel = "BK";
    
    public static final String TakeScreenshot = "FG";
    public static final String TakeScreenshotShare = "BA";
    public static final String TakeScreenshotSave = "BB";
    
    public static final String MeasureDistance = "FH";
    public static final String MeasureDistanceRevocation = "BA";
    public static final String MeasureDistanceClear = "BB";
    
    private long mStartMillis = 0;
    private String lastAction = null;
    
    private ActionLog(Context context, String logFileName, String serverParameterKey) {
        super(context, logFileName, serverParameterKey);
    }
    
    /**
     * 记录行为日志
     * @param actionLog
     * @param args
     */
    public void addAction(String actionLog, Object... args) {
        synchronized (mLock) {
        try {
            Object[] str = new String[args.length];
            int i = 0;
            for(Object object : args) {
                if (object != null) {
                    String s = object.toString().replaceAll("[-_%, \t\r{}\\[\\]\\\\&~#$*^+=()]", "@");//"[-_%, \t\r{}\\[\\]\\\\&~#$*^+=()]", "@"
                    if (TextUtils.isEmpty(s)) {
                        str[i] = "none";
                    } else {
                        str[i] = s;
                    }
                } else {
                    str[i] = "null";
                }
                actionLog += SEPARATOR_MIDDLE;
                actionLog += "%s";
                i++;
            }
            addAction(String.format(actionLog, str), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    }
    
    private void addAction(String actionLog, boolean addTime) {
        synchronized (mLock) {
            try {
                if (mStringBuilder == null) {
                    return;
                }
                if (TextUtils.isEmpty(actionLog) || actionLog.equals(lastAction)) {
                    return;
                }
    
                if (addTime) {
                    long current = System.currentTimeMillis();
                    mStringBuilder.append(SEPARATOR_STAET);
                    mStringBuilder.append((current-mStartMillis)/1000);
                    mStringBuilder.append(SEPARATOR_MIDDLE);
                    lastAction = actionLog;
                }
                mStringBuilder.append(actionLog);
                LogWrapper.d(TAG, actionLog);
    
                tryUpload();
                
            } catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean canUpload() {
        return TKConfig.getUserActionTrack().equals("on");
    }

    @Override
    protected String getLogOutToken() {
        return SEPARATOR_STAET+(simpleDateFormat.format(CalendarUtil.getExactTime(mContext)))+SEPARATOR_MIDDLE+LogOut;
    }

    @Override
    protected void onLogOut() {
        super.onLogOut();
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            StringBuilder s = mStringBuilder;
            mStringBuilder = new StringBuilder();
            mStringBuilder.append(getLogOutToken());
            mStringBuilder.append(s);
            LogWrapper.d(TAG, LogOut);
        }
    }
    
    /**
     * 记录网络行为日志
     * @param apiType
     * @param reqTime
     * @param revTime
     * @param resTime
     * @param fail
     * @param detail
     * @param signal
     * @param radioType
     * @param isStop
     * @param uuid
     * @param reqSize
     * @param revSize
     */
    public void addNetworkAction(String apiType, long reqTime, long revTime, long resTime, String fail, String detail, int signal, String radioType, boolean isStop, String uuid, long reqSize, long revSize) {
        try {
            addAction(NetworkAction, apiType, String.valueOf(reqTime - mStartMillis), String.valueOf(revTime - mStartMillis), String.valueOf(resTime-mStartMillis), fail, detail, signal, radioType, isStop, uuid, reqSize, revSize);
        } catch (Exception e) {
            LogWrapper.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (mLock) {
            mStartMillis = System.currentTimeMillis();
            addAction(SEPARATOR_STAET+simpleDateFormat.format(CalendarUtil.getExactTime(mContext))+SEPARATOR_MIDDLE+LifecycleCreate+SEPARATOR_MIDDLE+TKConfig.getClientSoftVersion(), false);
        }
    }

    /**
     * 在主Acitivty执行onResume()时调用
     */
    public void onResume() {
        addAction(LifecycleResume);
    }

    /**
     * 在主Acitivty执行onPause()时调用
     */
    public void onPause() {
        addAction(LifecyclePause);
    }

    /**
     * 在主Acitivty执行onStop()时调用
     */
    public void onStop() {
        addAction(LifecycleStop);
    }
    
    @Override
    public void onDestroy() {
        addAction(SEPARATOR_STAET+simpleDateFormat.format(CalendarUtil.getExactTime(mContext))+SEPARATOR_MIDDLE+LifecycleDestroy, false);
        super.onDestroy();
    }
}
