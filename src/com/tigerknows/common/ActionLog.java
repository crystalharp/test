/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */
package com.tigerknows.common;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;

/**
 * 用户行为日志类
 * @author pengwenyue
 *
 */
public class ActionLog {

    public static final String SEPARATOR_STAET = "_";
    public static final String SEPARATOR_MIDDLE = "-";
    private static final int UPLOAD_LENGTH = 10 * 1024;
    private static final int WRITE_FILE_SIZE = 1024;
    
    private SimpleDateFormat simpleDateFormat;
    
    private static ActionLog sActionLog;
    public static ActionLog getInstance(Context context) {
        if (sActionLog == null) {
            sActionLog = new ActionLog(context);
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

    public static final String HotelDate = "ZY";
    public static final String HotelDateDone = HotelDate+"BA";
    public static final String HotelDateCheckin = HotelDate+"BB";
    public static final String HotelDateCheckout = HotelDate+"BC";
    
    // == 共用 ==
    public static final String CommonAddress = "ZX";
    public static final String CommonTelphone = "ZY";
    public static final String CommonFavorite = "ZZ";
    public static final String CommonShare = "YA";
    public static final String CommonErrorRecovery = "YB";
    
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

    
    // POI输入查询页
    public static final String POIHomeInputQuery = "AB";
    public static final String POIHomeInputQueryBtn = "BA";

    // POI结果列表页
    public static final String POIList = "AC";
    
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
    public static final String POIDetailCouponSingle = "B0";
    public static final String POIDetailCouponMulti = "B1";

    // POI点评列表页
    public static final String POICommentList = "AE";
    public static final String POICommentListUrl = "BA";
    public static final String POICommentListMyComment = "BB";
    public static final String POICommentListInput = "BC";

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
    public static final String POIErrorRecovery = "AH";
    
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
    
    // 优惠券列表页
    public static final String CouponList = "AM";
    
    // 优惠券详情页
    public static final String CouponDetail = "AN";
    public static final String CouponQrimg = "BA";

    // 更多频道
    public static final String More = "CA";
    public static final String MoreMessageComment = "BA";
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
    public static final String MoreMessageMap = "BL";
    public static final String MoreUserHome = "BM";
    public static final String MoreMessageSoft = "BN";
    public static final String MoreGiveFavourableComment = "BO";
    public static final String MoreMessageUserSurvey = "BP";
    public static final String MoreAddMerchant = "BQ";

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

    // 意见反馈页 AL
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
    
    // == 用户模块通用 ==
    public static final String UserCommonLoginBtn = "AA";
    public static final String UserCommonRegisterBtn = "AB";
    public static final String UserCommonForgetPasswordBtn = "AC";
    public static final String UserCommonValidNumBtn = "AD";
    public static final String UserCommonConfirmBtn = "AE";
    
    // == 登录页面 ==
    public static final String UserLogin = "EA";
    
    // == 注册页面 ==
    public static final String UserRegist = "EB";
    
    // == 个人中心页面 == 
    public static final String UserHome = "EC";
    public static final String UserHomeMyComment = "BA";
    public static final String UserHomeUpdatePhone = "BB";
    public static final String UserHomeUpdateName = "BC";
    public static final String UserHomeUpdatePassword = "BD";
    public static final String UserHomeLogout = "BE";
    public static final String UserHomeDingdan = "BF";
    
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
    
    public static final String HotelPicture = "GC";

    // 酒店简介页
    public static final String HotelIntro = "GD";
    
    // 订单填写页
    public static final String HotelOrderWrite = "GE";
    public static final String HotelOrderWriteHowmany = "BA";   
    public static final String HotelOrderWriteHowmanyChoose = "BB";
    public static final String HotelOrderWriteReserve = "BC";
    public static final String HotelOrderWriteBookName = "BD";
    public static final String HotelOrderWriteMobile = "BE";
    public static final String HotelOrderWriteIdCard = "BF";
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
    
    // 7天酒店注册页
    public static final String HotelSeveninnRegist = "GG";
    public static final String HotelSeveninnRegistName = "BA";
    public static final String HotelSeveninnRegistPhone = "BB";
    public static final String HotelSeveninnRegistIdcard = "BC";
    public static final String HotelSeveninnRegistSubmit = "BD";

    // 订单列表页
    public static final String HotelOrderList = "GH";
    public static final String HotelOrderListGoThere = "BA";
    // BB-%d 被点击的订单的 index
    public static final String HotelOrderListItemClick = "BB";

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
    
    
    private Context mContext;
    private long mStartMillis = 0;
    private int mFileLength = 0;
    private String mPath;
    private Object mLock = new Object();
    private StringBuilder mStringBuilder = null;
    private String lastAction = null;
    
    private ActionLog(Context context) {
        synchronized (mLock) {
            mContext = context;
            mPath = TKConfig.getDataPath(false) + "action.log";
            mFileLength = 0;
            
            try {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                File file = new File(mPath);
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        LogWrapper.e("ActionLog", "ActionLog() Unable to create new file: " + mPath);
                        return;
                    }
                }
                FileInputStream fis = new FileInputStream(file);
                mFileLength = fis.available();
                fis.close();
    
                mStringBuilder = new StringBuilder();
            } catch (Exception e) {
                LogWrapper.e("ActionLog", "ActionLog() "+e.getMessage());
            }
        }
    }
    
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
            LogWrapper.e("ActionLog", "addAction() actionLog="+actionLog);
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
                LogWrapper.d("ActionLog", actionLog);
    
                // 判断是否要写入文件
                if (mStringBuilder.length() > WRITE_FILE_SIZE) {
                    write();
                    
                    // 判断是否要上传
                    if (mFileLength > UPLOAD_LENGTH) {
                        upload();
                    }
                }
                
            } catch (Exception e) {
                LogWrapper.e("ActionLog", "addAction() e="+e.getMessage());
            }
        }
    }
    
    private void upload() {
        synchronized (mLock) {
            try {
                if (mStringBuilder == null) {
                    return;
                }
                File file = new File(mPath);
                FileInputStream fis = new FileInputStream(file);
                final String str = Utility.readFile(fis)+SEPARATOR_STAET+(simpleDateFormat.format(Calendar.getInstance().getTime()))+SEPARATOR_MIDDLE+LogOut;
                fis.close();
                
                if (file.delete()) {
                    if (!file.createNewFile()) {
                        LogWrapper.e("ActionLog", "ActionLog() Unable to create new file: " + mPath);
                        return;
                    }
                    mFileLength = 0;
                    mStringBuilder = new StringBuilder();
                    mStringBuilder.append(SEPARATOR_STAET);
                    mStringBuilder.append(simpleDateFormat.format(Calendar.getInstance().getTime()));
                    mStringBuilder.append(SEPARATOR_MIDDLE);
                    mStringBuilder.append(LogOut);
                    
                    if (TKConfig.getUserActionTrack().equals("on")) {
                        new Thread(new Runnable() {
                            
                            @Override
                            public void run() {
                                FeedbackUpload feedbackUpload = new FeedbackUpload(mContext);
                                Hashtable<String, String> criteria = new Hashtable<String, String>();
                                criteria.put(FeedbackUpload.SERVER_PARAMETER_ACTION_LOG, str);
                                CityInfo cityInfo = Globals.getCurrentCityInfo();
                                int cityId = MapEngine.CITY_ID_BEIJING;
                                if (cityInfo != null) {
                                    cityId = cityInfo.getId();
                                }
                                feedbackUpload.setup(criteria, cityId);
                                feedbackUpload.query();
                            }
                        }).start();
                    }
                }
            } catch (Exception e) {
                LogWrapper.e("ActionLog", "addAction() e="+e.getMessage());
            }
        }
    }
    
    public void addNetworkAction(String apiType, long reqTime, long revTime, long resTime, String fail, String detail, int signal, String radioType, boolean isStop) {
        try {
            addAction(NetworkAction, apiType, String.valueOf(reqTime - mStartMillis), String.valueOf(revTime - mStartMillis), String.valueOf(resTime-mStartMillis), fail, detail, signal, radioType, isStop);
        } catch (Exception e) {
            LogWrapper.e("ActionLog", "addNetworkAction() e="+e.getMessage());
        }
    }
    
    public void onCreate() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            if (mFileLength > WRITE_FILE_SIZE) {
                upload();
            }

            mStartMillis = System.currentTimeMillis();
            addAction(SEPARATOR_STAET+simpleDateFormat.format(Calendar.getInstance().getTime())+SEPARATOR_MIDDLE+LifecycleCreate+SEPARATOR_MIDDLE+TKConfig.getClientSoftVersion(), false);
        }
    }
    
    public void onResume() {
        addAction(LifecycleResume);
    }
    
    public void onPause() {
        addAction(LifecyclePause);
    }
    
    public void onStop() {
        addAction(LifecycleStop);
    }
    
    public void onDestroy() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            addAction(SEPARATOR_STAET+simpleDateFormat.format(Calendar.getInstance().getTime())+SEPARATOR_MIDDLE+LifecycleDestroy, false);
            write();
        }
    }
    
    public void onTerminate() {
        synchronized (mLock) {
            write();
            mFileLength = 0;
            lastAction = null;
        }
    }
    
    private void write() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            if (mStringBuilder.length() > 0) {
                try {
                    File file = new File(mPath); 
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    byte[] data = mStringBuilder.toString().getBytes();
                    fileOutputStream.write(data);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    mStringBuilder = new StringBuilder();
                    mFileLength += data.length;
                } catch (Exception e) {
                    mStringBuilder = null;
                    e.printStackTrace();
                }
            }
        }
    }
}
