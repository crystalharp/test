/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */
package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.CommonUtils;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;

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
    
    // 提示对话框出现  ZA-提示内容
    public static final String DIALOG = "ZA-%s";
    // 点击对话框响应按钮   ZAAA-含义
    public static final String DIALOG_BUTTON_ONCLICK = "ZAAA-%s";

    // 回退键
    public static final String KeyCodeBack = "ZB";    
    // 标题栏左上角按钮
    public static final String Title_Left_Back = "ZC";
    // 标题栏右上角按钮
    public static final String Title_Right_Button = "ZD";
    // 结果框  ZI-含义-成功/失败原因
    public static final String RESULT = "ZI-%s-%s-%s";
    // 重试
    public static final String RETRY = "ZL";
    
    // 点击区域筛选框 
    public static final String FILTER_AREA_ONCLICK = "ZM";
    // 点击分类筛选框 
    public static final String FILTER_CATEGORY_ONCLICK = "ZN";
    // 点击排序筛选框 
    public static final String FILTER_ORDER_ONCLICK = "ZO";
    // 选择筛选条件
    public static final String FILTER_SELECTED = "ZP-%s-%s";
    // 取消筛选
    public static final String FILTER_CANCEL = "ZQ";
    
    // 翻页
    public static final String LOAD_MORE_TRIGGER = "ZR";
    public static final String LISTVIEW_ITEM_ONCLICK = "ZS-%s-%s";

    // 日志超长 EXCP
    public static final String LOG_OUT = "EXCP";
    
    // 到这里去对话框
    public static final String DIALOG_COME = "DB";
    
    public static final String DIALOG_COME_HERE_TRANSFER = "AA";
    
    public static final String DIALOG_COME_HERE_DRIVE = "AB";
    
    public static final String DIALOG_COME_HERE_WALK = "AC";
    
    public static final String DIALOG_COME_HERE_CUSTOM_START = "AD";
    
    // 搜索频道 
    // 搜索首页 AP
    public static final String SearchHome = "AP";
    // 搜索首页显示 AP
    // 点击切换城市 APAA
    public static final String SearchHomeChangeCity = SearchHome + "AA";
    // 点击搜索文本框 APAB
    public static final String SearchHomeSearchInput = SearchHome + "AB";
    // 点击美食标签 APAC
    public static final String SearchHomeFood = SearchHome + "AC";
    // 点击娱乐标签 APAD
    public static final String SearchHomeYuLe = SearchHome + "AD";
    // 点击购物标签 APAE
    public static final String SearchHomeBuy = SearchHome + "AE";
    // 点击酒店标签 APAF
    public static final String SearchHomeHotel = SearchHome + "AF";
    // 点击运动标签 APAG
    public static final String SearchHomeSprot = SearchHome + "AG";
    // 点击银行标签 APAH
    public static final String SearchHomeBank = SearchHome + "AH";
    // 点击丽人标签 APAI
    public static final String SearchHomeLiRen = SearchHome + "AI";
    // 点击旅游标签 APAJ
    public static final String SearchHomeLuYou = SearchHome + "AJ";
    // 点击交通标签 APAK
    public static final String SearchHomeTraffic = SearchHome + "AK";
    // 点击医疗标签 APAL
    public static final String SearchHomeYiLiao = SearchHome + "AL";
    
    // 搜索词输入页 AQ
    public static final String SearchInput = "AQ";
    // 点击历史词 AQAA-历史词-下标
    public static final String SearchInputHistoryWord = SearchInput + "AA-%s-%s";
    // 点击联想词 AQAB-联想词-下标
    public static final String SearchInputSuggestWord = SearchInput + "AB-%s-%s";
    // 点击清除历史数据 AQAC
    public static final String SearchInputCleanHistory = SearchInput + "AC";
    // 点击搜索按钮 AQAD-搜索词
    public static final String SearchInputSubmit = SearchInput + "AD-%s";

    // 结果列表页 AR
    public static final String SearchResult = "AR";
    // 点击某一个POI ARAF-下标
    public static final String SearchResultSelect = SearchResult + "AF-%s-%s-%s";
    
    // Poi详情页 AE
    public static final String POIDetail = "AE";
    // POI详情页显示 AEAA-poi uuid-名称
    public static final String POIDetailShow = POIDetail + "AA-%s-%s-%s";
    // 点击地址 AEAD
    public static final String POIDetailAddress = POIDetail + "AD";
    // 点击电话 AEAE
    public static final String POIDetailTelephone = POIDetail + "AE";
    // 点击收藏 AEAF
    public static final String POIDetailFavorite = POIDetail + "AF-%s";
    // 点击分享 AEAG
    public static final String POIDetailShare = POIDetail + "AG";
    // 点击搜索周边 AEAJ
    public static final String POIDetailSearch = POIDetail + "AJ";
    // 点击全部点评 AEAK
    public static final String POIDetailAllComment = POIDetail + "AK";
    // 点击我的点评 AEAL
    public static final String POIDetailMyComment = POIDetail + "AL";
    // 点击其他点评 AEAM
    public static final String POIDetailOtherComment = POIDetail + "AM";
    // 点击纠错 AEAN
    public static final String POIDetailErrorRecovery = POIDetail + "AN";
    // 点击点评输入框 AEAO
    public static final String POIDetailInputBox = POIDetail + "AO";
    public static final String POIDetailYanchu = POIDetail + "AP";
    public static final String POIDetailZhanlan = POIDetail + "AQ";
    public static final String POIDetailTuangou = POIDetail + "AR";

    // 周边搜索页 AS
    public static final String SearchNearby = "AS";
    // 点击分类tab按钮 ASAA
    public static final String SearchNearbyCategory = SearchNearby + "AA";
    // 点击美食 ASAB
    public static final String SearchNearbyFood = SearchNearby + "AB";
    // 点击娱乐 ASAC
    public static final String SearchNearbyYuLe = SearchNearby + "AC";
    // 点击购物 ASAD
    public static final String SearchNearbyBuy = SearchNearby + "AD";
    // 点击酒店 ASAE
    public static final String SearchNearbyHotel = SearchNearby + "AE";
    // 点击运动 ASAF
    public static final String SearchNearbySprot = SearchNearby + "AF";
    // 点击银行 ASAG
    public static final String SearchNearbyBank = SearchNearby + "AG";
    // 点击丽人 ASAH
    public static final String SearchNearbyLiRen = SearchNearby + "AH";
    // 点击旅游 ASAI
    public static final String SearchNearbyLuYou = SearchNearby + "AI";
    // 点击交通 ASAJ
    public static final String SearchNearbyTraffic = SearchNearby + "AJ";
    // 点击医疗 ASAK
    public static final String SearchNearbyYiLiao = SearchNearby + "AK";
    // 点击输入tab按钮 ASAL
    public static final String SearchNearbyJingQue = SearchNearby + "AL";
    // 在输入页点击搜索 ASAM
    public static final String SearchNearbySubimt = SearchNearby + "AM-%s";
    // 点击联想词 ASAN-联想词-下标
    public static final String SearchNearSuggestWord = SearchNearby + "AN-%s-%s";
    // 点击历史词 ASAO-历史词-下标
    public static final String SearchNearHistoryWord = SearchNearby + "AO-%s-%s";
    // 点击清除历史数据 ASAP
    public static final String SearchNearbyCleanHistory = SearchNearby + "AP";

    // 更多频道
    public static final String More = "AF";
    // 更多频道页显示 AF
    // 点击点评引导banner AFAA
    public static final String MoreUpdateComment = More + "AA";
    // 点击注册/登录 AFAB
    public static final String MoreLoginRegist = More + "AB";
    // 点击切换城市 AFAC
    public static final String MoreChangeCity = More + "AC";
    // 点击下载地图 AFAD
    public static final String MoreDownloadMap = More + "AD";
    // 点击应用推荐 AFAE
    public static final String MoreAppRecommend = More + "AE";
    // 点击收藏夹 AFAF
    public static final String MoreFavorite = More + "AF";
    // 点击最近浏览 AFAG
    public static final String MoreHistory = More + "AG";
    // 点击系统设置 AFAH
    public static final String MoreSetting = More + "AH";
    // 点击意见反馈 AFAI
    public static final String MoreFeedback = More + "AI";
    // 点击帮助 AFAJ
    public static final String MoreHelp = More + "AJ";
    // 点击关于我们 AFAK
    public static final String MoreAboutUs = More + "AK";
    // 点击地图更新 AFAN
    public static final String MoreUpdateMap = More + "AN";
    // 地图更新窗口点击手动更新 AFAO
    public static final String MoreUpdateMapManual = More + "AO";
    // 地图更新窗口点击更新全部    AFAP
    public static final String MoreUpdateMapAll = More + "AP";    
    // 点击个人中心  AFAQ
    public static final String MoreUserHome = More + "AQ";
    // 点击软件更新 AFAR
    public static final String MoreUpdateSoft = More + "AR";
    public static final String MoreGiveFavourableComment = More + "AS";

    // 切换城市页
    public static final String ChangeCity = "AG";
    // 切换城市页 AG
    // 点击“返回”按钮 AGZA
    // 点击查询城市输入框 AGAA
    public static final String ChangeCityInputBox = ChangeCity + "AA";
    // 选择“我的位置”城市AGAB
    public static final String ChangeCityCurrent = ChangeCity + "AB-%s";
    // 选择“全部城市”中的某城市 AGAC
    public static final String ChangeCitySelect = ChangeCity + "AC-%s";

    // 下载地图页
    public static final String DownloadMap = "AH";
    // 下载地图页 AH
    // 点击“添加城市”tab AHAA
    public static final String DownloadMapAdd = DownloadMap + "AA";
    // 长按下载列表中某城市 AHAC
    public static final String DownloadMapSelect = DownloadMap + "AC-%s";
    // 点击弹出窗口中“删除地图” AHAD
    public static final String DownloadMapDelete = DownloadMap + "AD-%s";
    // 点击弹出窗口中“开始下载” AHAE
    public static final String DownloadMapDownload = DownloadMap + "AE-%s";
    // 点击弹出窗口中“升级地图” AHAF
    public static final String DownloadMapUpgrade = DownloadMap + "AF-%s";
    // 点击弹出窗口中”暂停下载” AHAG
    public static final String DownloadMapPause = DownloadMap + "AG-%s";
    // 点击查询城市输入框 AHAI
    public static final String DownloadMapInputBox = DownloadMap + "AI";
    // 点击按省份查询某一城市 AHAL
    public static final String DownloadMapCloseOnClick = DownloadMap + "AM";

    // 收藏夹页
    public static final String Favorite = "AI";
    // 点击地点tab AIAB
    public static final String FavoritePOI = Favorite + "AB";
    // 点击交通tab AIAC
    public static final String FavoriteTraffic = Favorite + "AC";
    // 点击某一条地点收藏 AIAD
    public static final String FavoriteSelectPOI = Favorite + "AD-%s";
    // 点击某一条交通收藏 AIAE
    public static final String FavoriteSelectTraffic = Favorite + "AE-%s";
    // 编辑状态下点击删除icon AIAF
    public static final String FavoriteDelete = Favorite + "AF-%s";
    // 编辑状态下点击全选按钮 AIAG-%s
    public static final String FavoriteAllSelect = Favorite + "AS-%s";
    // 重命名页点击文本输入框 AIAH
    public static final String FavoriteRename = Favorite + "AH";
    // 点击右上角删除按钮 AIAI
    public static final String FavoriteRightDelete = Favorite + "AI-%s";
    // 往上拉加载更多 AIAJ
    public static final String FavoriteMore = Favorite + "AJ-%s";

    // 历史浏览页
    public static final String History = "AJ";
    // 点击左上角返回按钮 AIZA
    // 点击右上角删除按钮 AIAA
    // 点击地点tab AIAB
    public static final String HistoryPOI = History + "AB";
    // 点击交通tab AIAC
    public static final String HistoryTraffic = History + "AC";
    // 点击某一条地点收藏 AIAD
    public static final String HistorySelectPOI = History + "AD-%s";
    // 点击某一条交通收藏 AIAE
    public static final String HistorySelectTraffic = History + "AE-%s";
    // 编辑状态下点击删除icon AIAF
    public static final String HistoryDelete = History + "AF-%s";
    // 编辑状态下点击全选按钮 AIAG-%s
    public static final String HistoryAllSelect = History + "AS-%s";
    // 重命名页点击文本输入框 AIAH
    // 点击右上角删除按钮 AIAI
    public static final String HistoryRightDelete = History + "AI-%s";
    // 往上拉加载更多 AIAJ
    public static final String HistoryMore = History + "AJ-%s";

    // 系统设置页
    public static final String Setting = "AK";
    // 系统设置页 AK
    // 点击返回按钮 AKZA
    // 开启屏幕常亮 AKAA
    public static final String SettingAcquireWakeLockYes = Setting + "AA";
    // 关闭屏幕常亮 AKAB
    public static final String SettingAcquireWakeLockNo = Setting + "AB";
    // 点击GPS按钮 AKAC-%s
    public static final String SettingGPS = Setting + "AC-%s";

    // 意见反馈页 AL
    public static final String Feedback = "AL";
    // 点击返回按钮 ALZA
    // 点击老虎地图微博按钮 ALAA
    public static final String FeedbackWeibo = Feedback + "AA";
    // 点击意见输入框 ALAB
    public static final String FeedBackContent = Feedback + "AB";
    // 点击输入手机号 ALAC
    public static final String FeedbackMobile = Feedback + "AC";
    // 点击提交    ALAD
    public static final String FeedbackSubmit = Feedback + "AD";

    // 帮助页
    public static final String Help = "AN";

    // 关于我们页
    // 关于我们页 AM
    public static final String AboutUs = "AM";
    // 点击官网“打开网址” AMAA
    public static final String AboutUsWeb = AboutUs + "AA";
    // 点击官方微博“打开网址” AMAB
    public static final String AboutUsWeibo = AboutUs + "AB";
    // 点击论坛“打开网址” AMAC
    public static final String AboutUsBBS = AboutUs + "AC";
    // 点击wap网址“打开网址” AMAD
    public static final String AboutUsWap = AboutUs + "AD";
    // 选择电话“拨打电话” AMAE
    public static final String AboutUsTelephone = AboutUs + "AE";
    
    // 地图
    public static final String Map = "BH";
    // 点击定位按钮 MPL%s U，表示点击后没有定位信息 ；N，表示点击后进入到导航模式；R，表示点击后进入到转动模式
    public static final String MapLocation = Map + "AA-%s";
    // 双击放大地图 MPD
    public static final String MapDoubleClick = Map + "AB";
    // 多点触摸放大 MPTZI
    public static final String MapTouchZoomIn = Map + "AC";
    // 多点触摸缩小 MPTZO
    public static final String MapTouchZoomOut = Map + "AD";
    // 长按地图 MPLC
    public static final String MapLongClick = Map + "AE";
    // 长按地图选为起点 MPS
    public static final String MapSelectStart = Map + "AF";
    // 长按地图选为终点 MPE
    public static final String MapSelectEnd = Map + "AG";
    // 移动地图 MPM
    public static final String MapMove = Map + "AH";
    // 点击POI气泡 MPPB
    public static final String MapPOIBubble = Map + "AI";
    // 点击交通气泡 MPTB
    public static final String MapTrafficBubble = Map + "AJ";
    // 点击上一个 MPSU
    public static final String MapStepUp = Map + "AK";
    // 点击下一个 MPSD
    public static final String MapStepDown = Map + "AL";
    // 点击放大 MPCZI
    public static final String MapClickZoomIn = Map + "AM";
    // 点击缩小 MPCZO
    public static final String MapClickZoomOut = Map + "AN";
    public static final String MapFirstLocation = Map + "AO-%s";

    // Menu
    public static final String Menu = "AT";
    // 点击搜索 ATAA
    public static final String MenuSearch = Menu + "AA";
    // 点击交通 ATAB
    public static final String MenuTraffic = Menu + "AB";
    // 点击更多 ATAC
    public static final String MenuMore = Menu + "AC";
    // 点击发现 ATAD
    public static final String MenuDiscover = Menu + "AD";

    // 软件生命周期  AO
    public static final String Lifecycle = "AO";
    // 软件启动AA
    public static final String LifecycleCreate = Lifecycle + "AA";
    // 软件退出AB
    public static final String LifecycleDestroy = Lifecycle + "AB";
    // Resume AC
    public static final String LifecycleResume = Lifecycle + "AC";
    // Pause AD
    public static final String LifecyclePause = Lifecycle + "AD";
    // Stop AE
    public static final String LifecycleStop = Lifecycle + "AE";
    public static final String LifecycleSelectCity = Lifecycle + "AF-%s";
        
    // 交通频道页显示    TH
    public static final String TrafficHome = "BO";
    // 全屏地图页显示    TM
    public static final String TrafficMap = "BP";
    // 交通输入页显示    TI
    public static final String TrafficInput = "BQ";
    
    public static final String Traffic = "BY";
    // 切换到公交tab    TBT
    public static final String TrafficBusTab = Traffic + "AA";
    // 切换到驾车tab    TDT
    public static final String TrafficDriveTab = Traffic + "AB";
    // 切换到步行tab    TWT
    public static final String TrafficWalkTab = Traffic + "AC";
    // 切换到线路tab    TLT
    public static final String TrafficLineTab = Traffic + "AD";
    // 点击查询按钮     TQBT-%s-%s(公交-驾车-步行-%s起点，%s终点)
    public static final String TrafficQueryBtnT = Traffic + "AE-%s-%s-%s";
    // 点击查询按钮     TQBB-%s(线路-%s线路)
    public static final String TrafficQueryBtnB = Traffic + "AF-%s";
    // 调换起点终点     TEB
    public static final String TrafficExchangeBtn = Traffic + "AG";
    // 点击起点输入框   TSE
    public static final String TrafficStartEdt = Traffic + "AH";
    // 点击终点输入框   TEE
    public static final String TrafficEndEdt = Traffic + "AI";
    // 点击路线输入框   TLE
    public static final String TrafficLineEdt = Traffic + "AJ";
    // 点击起点输入框的书签按钮    TSBB
    public static final String TrafficStartBookmarkBtn = Traffic + "AK";
    // 点击终点输入框的书签按钮    TEBB
    public static final String TrafficEndBookmarkBtn = Traffic + "AL";
    // 点击书签中的“我的当前位置”   TSBF
    public static final String TrafficBookmarkFirst = Traffic + "AO";
    // 点击书签中的“在地图上选点”   TSBS
    public static final String TrafficBookmarkSecond = Traffic + "AP";
    // 点击书签中的“我收藏的地点”   TSBT
    public static final String TrafficBookmarkThird = Traffic + "AQ";
    // 点击清空历史数据    TCH
    public static final String TrafficClearHistory = Traffic + "AR";
    // 从历史词中选择，作为起点    THAS-%s(%s所选词的下标)
    public static final String TrafficHistoryAsStart = Traffic + "AS-%s";
    // 从历史词中选择，作为终点    THAE-%s(%%s所选词的下标)
    public static final String TrafficHistoryAsEnd = Traffic + "AT-%s";
    // 从历史词中选择，作为线路查询词    THAL-%s(%s所选词的下标)
    public static final String TrafficHistoryAsLine = Traffic + "AU-%s";
    // 从联想词中选择，作为起点	TSAS-%s-%s(%s所选词的类型-0历史-1联想，%s所选词的下标)
    public static final String TrafficSuggestAsStart = Traffic + "AV-%s-%s";
    // 从联想词中选择，作为终点	TSAE-%s-%s(%s所选词的类型-0历史-1联想，%s所选词的下标)
    public static final String TrafficSuggestAsEnd = Traffic + "AW-%s-%s";
    // 从联想词中选择，作为线路	TSAL-%s-%s(%s所选词的类型-0历史-1联想，%s所选词的下标)
    public static final String TrafficSuggestAsLine = Traffic + "AX-%s-%s";    
    // 单击选点作为起点	TCAS
    public static final String TrafficClickAsStart = Traffic + "AY";
    // 单击选点作为终点	TCAE
    public static final String TrafficClickAsEnd = Traffic + "AZ";
    // 在地图区域操作（引发跳转到全屏地图）THTM
    public static final String TrafficHomeToMap = Traffic + "BA";
    // 公交查询无结果   TQNT
    public static final String TrafficQueryNoResultT = Traffic + "BB";
    // 线路查询无结果   TQNB
    public static final String TrafficQueryNoResultB = Traffic + "BC";
    // 公交查询有结果   TQRT-%s(%s返回的结果数目，限制条件???)
    public static final String TrafficQueryResultT = Traffic + "BD-%s";
    // 线路查询有结果   TQRB-%s(%s返回的结果数目，限制条件???)
    public static final String TrafficQueryResultB = Traffic + "BE-%s";
    // 点击title弹出popup window
    public static final String TrafficPopupWindow = Traffic + "BF";
    // 点击popup window的某一选项
    public static final String TrafficPopupClickItem = Traffic + "BG-%s";
    
    // 显示选择收藏的POI点页面 TFF
    public static final String TrafficFetchFavorite = "BR";
    // 选择收藏POI点作为交通查询点 TFRP-%s(%s所选词的下标)
    public static final String TrafficFavoriteAsRoutePoint = TrafficFetchFavorite + "AA-%s";
    
    // 交通备选站点页显示    TA
    public static final String TrafficAlternative = "BS";
    // 确定起点    TASS-%s(%s所选词的下标)
    public static final String TrafficAlterSelectStart = TrafficAlternative + "AA-%s";
    // 确定终点    TASE-%s(%s所选词的下标)
    public static final String TrafficAlterSelectEnd = TrafficAlternative + "AB-%s";

    // 交通方案列表页显示    TR
    public static final String TrafficResult = "BT";
    // 点击某方案    TRS-%s(%s所选方案的下标)
    public static final String TrafficResultSelect = TrafficResult + "AA-%s";

    // 交通方案详情页显示    TD
    public static final String TrafficDetail = "BU";
    // 点击收藏按钮    TDF
    public static final String TrafficDetailFavorite = TrafficDetail + "AB";
    // 点击取消收藏按钮    TDCF
    public static final String TrafficDetailCancelFav = TrafficDetail + "AC";
    // 点击某一段交通路线    TDS-%s(%s所选步骤的下标)
    public static final String TrafficDetailStep = TrafficDetail + "AD-%s";
    // 点击分享按钮    TDSB 
    public static final String TrafficDetailShare = TrafficDetail + "AE";
    // 点击纠错 TDE
    public static final String TrafficDetailErrorRecovery = TrafficDetail + "AF";

    // 交通线路列表页显示   TB
    public static final String TrafficBusline = "BV";
    // 线路列表翻页    TBP
    // 点击线路    TBL-%s(%s所选线路的下标)
    public static final String TrafficBusineLine = TrafficBusline + "AB-%s";

    // 交通站点列表页显示	TS
    public static final String TrafficStation = "BW";
    // 点击站点    TSS-%s(%s所选站点的下标)    
    public static final String TrafficStationSelect = TrafficStation + "AB-%s";

    // 线路详情页    TG
    public static final String TrafficLineDetail = "BX";
    // 点击收藏按钮    TGF
    public static final String TrafficLineDetailFavorite = TrafficLineDetail + "AB";
    // 点击取消收藏按钮    TGCF
    public static final String TrafficLineDetailCancelFav = TrafficLineDetail + "AC";
    // 点击某一站点    TGS-%s(%s所选站点的下标)
    public static final String TrafficLineDetailStation = TrafficLineDetail + "AD-%s";
    // 点击分享按钮    TGSB 
    public static final String TrafficLineDetailShareBtn = TrafficLineDetail + "AE";    

    // 分享
    public static final String Share = "BN";
    // 分享到微博  SAW
    public static final String ShareWeibo = Share + "AA";
    // 分享到微博  SAQ
    public static final String ShareQzone = Share + "AB";
    // 分享到短信 SAS
    public static final String ShareSms = Share + "AC";
    // 分享到彩信 SAC
    public static final String ShareMms = Share + "AD";
    // 分享到其它方式 SAM
    public static final String ShareMore = Share + "AE";
    
    // 微博授权界面    
    public static final String WeiboAuthorize = "BJ";
    
    // 微博发送界面    
    public static final String WeiboSend = "BK";
    // 点击删除文本内容按钮
    public static final String WeiboSendClickedDelWord = WeiboSend + "AA";
    // 在删除文本内容对话框点击确认按钮
    public static final String WeiboSendClickedDelWordYes = WeiboSend + "AB";
    // 在删除文本内容对话框点击取消按钮
    public static final String WeiboSendClickedDelWordNo = WeiboSend + "AC";
    // 点击图片
    public static final String WeiboSendClickPic = WeiboSend + "AD";
    // 点击发送按钮 WSCS-%s(%s为用户发送内容)
    public static final String WeiboSendClickedSendBtn = WeiboSend + "AE-%s";
    // 点击切换账号按钮
    public static final String WeiboSendClickedLogoutBtn = WeiboSend + "AF";
    
 	// 显示微博图片对话框
    public static final String WeiboImage = "BL";
    // 微博图片对话框点击删除图片按钮
    public static final String WeiboImageClickedDelPic = WeiboImage + "AA";
    
    // QQ空间发送页面
    public static final String QzoneSend = "BM";
    // 点击发送按钮
    public static final String QzoneSendClickSendBtn = QzoneSend + "AA-%s";
    // 点击注销按钮
    public static final String QzoneSendClickLogoutbBtn = QzoneSend + "AB";
    
    // 纠错
    // POI纠错页
    public static final String POIErrorRecovery = "AW";
    
    // 交通换乘纠错页
    public static final String TransferErrorRecovery = "AX";
    
    
    // == 帐户模块通用 ==
    public static final String User = "BG";
    // 点击“手机号”输入框
    public static final String UserClickPhoneEdt = User + "AA";
    // 点击“请输入(旧)密码”(旧密码, 已有密码)输入框
    public static final String UserClickPasswordEdt = User + "AB";
    // 点击“请输入密码”(新密码)输入框
    public static final String UserClickNewPasswordEdt = User + "AC";
    // 点击“请再次输入密码”(新密码)输入框
    public static final String UserClickNewRePasswordEdt = User + "AD";
    // 点击“验证码”输入框
    public static final String UserClickValidNumEdt = User + "AE";
    // 点击“昵称”输入框
    public static final String UserClickNickNameEdt = User + "AF";
    // 输入框输入为空
    public static final String UserEdtEmptyError = User + "AG";
    // 手机号输入格式错误
    public static final String UserPhoneFormatError = User + "AH";
    // 密码输入格式错误
    public static final String UserPasswordFormatError = User + "AI";
    // 两次密码不同
    public static final String UserRePasswordFormatError = User + "AJ";
    // 昵称输入格式错误
    public static final String UserNicknameFormatError = User + "AK";
    public static final String UserLoginSuccessUI = User + "AM";
    public static final String UserRegisterSuccess = User + "AN";
    public static final String UserReadSuccess = User + "AO";
    
    // 请求结果
    public static final String UserRequestResponse = "%sZE-%s";
    
    // == 登录或注册页面 ==
    public static final String UserCommentAfter = "BF";
    // 点击”登录“按钮
    public static final String UserCommentAfterLogin = UserCommentAfter + "AB";
    // 点击”注册“按钮
    public static final String UserCommentAfterRegist = UserCommentAfter + "AC";
    
    // == 登录页面 ==
    public static final String UserLogin = "AY";
    // 点击”忘记密码“
    public static final String UserLoginForgetPassword = UserLogin + "AA";
    // 点击”注册“按钮
    public static final String UserLoginRegistBtn = UserLogin + "AC";
    
    // == 注册页面 ==
    public static final String UserRegist = "AZ";
    // 点击”获取验证码“按钮
    public static final String UserRegistValidNumBtn = UserRegist + "AA";
    
    // == 重置密码页面 ==
    public static final String UserResetPassword = "BD";
    // 点击”获取验证码“按钮
    public static final String UserResetPasswordValidNumBtn = UserResetPassword + "AA";
    
    // == 个人中心页面 == 
    public static final String UserHome = "BA";
    // 点击”我的点评“
    public static final String UserHomeToMyComment = UserHome +"AA";
    // 点击”更换手机号“
    public static final String UserHomeToUpdatePhone = UserHome +"AB";
    // 点击”修改昵称“
    public static final String UserHomeToUpdateName = UserHome +"AC";
    // 点击”修改密码“
    public static final String UserHomeToUpdatePassword = UserHome +"AD";
    // 点击“退出帐户”
    public static final String UserHomeLogout = UserHome +"AE";
    public static final String UserHomeDingdan = UserHome +"AF";
    
    // == 更换手机号 ==
    public static final String UserUpdatePhone = "BB";
    // 点击“获取验证码”
    public static final String UserUpdatPhoneValidNumBtn = UserUpdatePhone + "AA";
    
    // == 修改昵称 ==
    public static final String UserUpdateNickName = "BC";
    // 点击“删除”按钮
    public static final String UserUpdateNickNameDelBtn = UserUpdateNickName + "AA";
    
    // == 修改密码页面 ==
    public static final String UserUpdatePassword = "BE";
    // 点击“忘记密码”
    public static final String UserUpdatePasswordForgetPassword = UserUpdatePassword + "AA";
    
    // 我的点评页 AA
    // 我的点评页显示 AA
    public static final String MyComment = "AA";
    // 点击返回按钮 AAZA
    // 点击我要点评按钮
    // 点击POI名称AAAA-%s (%s=POI名称)
    public static final String MyCommentClickPOI = MyComment+"AA-%s";
    // 点击点评内容AAAB-%s (%s=index)
    public static final String MyCommentClickComment = MyComment+"AB-%s";

    // 点评输入页 CI
    public static final String POIComment = "AB";
    // 点评输入页显示 CI
    // 点击返回按钮 CIZA
    // 点击整体评价笑脸 CIAA-%s
    public static final String POICommentClickGrade = POIComment+"AA-%s";
    // 点击文字输入框 CIAB
    public static final String POICommentContent = POIComment+"AB";
    // 点击人均价格 CIAE
    public static final String POICommentAvg = POIComment+"AE";
    // 点击口味 CIAF
    public static final String POICommentTaste = POIComment+"AF";
    // 点击环境 CIAG
    public static final String POICommentEnvironment = POIComment+"AG";
    // 点击服务 CIAH
    public static final String POICommentQos = POIComment+"AH";
    // 点击氛围 CIAI
    public static final String POICommentRestair = POIComment+"AI";
    // 点击氛围弹出框的确定 CIAJ
    public static final String POICommentClickRestairOK = POIComment+"AJ";
    // 点击氛围弹出框的关闭 CIAK
    public static final String POICommentClickRestairCancel = POIComment+"AK";
    // 点击推荐菜 CIAL
    public static final String POICommentRecommend = POIComment+"AL";
    // 点击同步新浪微博按钮 CIAM-%s
    public static final String POICommentClickSina = POIComment+"AM-%s";
    // 点击同步QQ空间按钮 CIAN-%s
    public static final String POICommentClickQZone = POIComment+"AN-%s";

    // 我要点评页 AC
    public static final String GoComment = "AC";
    // 点击搜索输入框 ACAA
    public static final String GoCommentClickInputBox = GoComment+"AA";
    // 点击某一个POI ACAB-%s-%s-%s
    public static final String GoCommentClickPOI = GoComment+"AB-%s-%s-%s";

    // POI点评列表页 AD
    public static final String POICommentList = "AD";
    // 点击点评来源链接 ADAA-%s
    public static final String POICommentListClickUrl = POICommentList + "AA-%s";
    // 点击我的点评 ADAC
    public static final String POICommentListClickMyComment = POICommentList + "AC";
    // 点击点评输入框 ADAD
    public static final String POICommentListClickInputBox = POICommentList + "AD";

    // 应用推荐 AO
    public static final String AppRecommend = "AU";
    // 应用推荐显示 AO
    // 点击返回按钮 AOZA
    // 点击盛大推荐 AOAA
    public static final String AppRecommendSnda = AppRecommend + "AA";
    // 点击某一个应用 AOAB-%s
    public static final String AppRecommendSelect = AppRecommend + "AB-%s";

    // 发现频道
    // 发现首页显示 CA
    public static final String DiscoverHome = "CA";
    // 向左滑动图片 CAAA
    public static final String DiscoverHomeLeftFling = DiscoverHome + "AA";
    // 向右滑动图片 CAAB
    public static final String DiscoverHomeRightFling = DiscoverHome + "AB";
    // 点击子频道 CAAC-%s
    public static final String DiscoverHomeSelectItem = DiscoverHome + "AC-%s";
    
    // 团购结果列表页
    public static final String TuangouList = "CB";
    // 点击订单按钮 CBAG
    public static final String TuangouListDingdan = TuangouList + "AG";
    // 点击TitleButton选择category AH-%d
    public static final String DiscoverListTitleSelection = "AH-%s";
    // 在发现详情页面，滑动页面 AI-%s(index)
    public static final String DiscoverDetailFling = "AI-%s";
    
    // 电影结果列表页
    public static final String DianyingList = "CR";
    // 展览结果列表页
    public static final String ZhanlanList = "CN";
    // 展览结果列表页
    public static final String YanchuList = "CJ";

    // 团购详情页
    // 团购详情页显示 CE
    public static final String TuangouXiangqing = "CE";
    // 点击地址 CEAB
    public static final String DiscoverDetailAddress = "AB";
    // 点击电话 CEAC
    public static final String DiscoverDetailTelphone = "AC";
    // 点击附近x家分店按钮 CEAD
    public static final String DiscoverDetailBranch = "AD";
    // 点击立即抢购按钮 CEAE
    public static final String TuangouXiangqingBuy = TuangouXiangqing + "AE";
    // 点击购买须知查看全部按钮 CEAF
    public static final String TuangouXiangqingNotice = TuangouXiangqing + "AF";
    // 点击客服电话 CEAG
    public static final String TuangouXiangqingCustomService = TuangouXiangqing + "AG";
    
    // 电影详情页显示 CS
    public static final String DianyingXiangqing = "CS";
    public static final String DianyingToday = "AE";
    public static final String DianyingTomorrow = "AF";
    public static final String DianyingAfterTomorrow = "AG";
    
    // 演出详情页    CL 
    public static final String YanchuXiangqing = "CL";
    
    // 展览详情页显示     CP 
    public static final String ZhanlanXiangqing = "CP";
    
    // 订单查询页 CC
    public static final String DingdanList = "CC";

    // 团购地图页显示      CD 
    public static final String MapTuangouList = "CD";
    // 团购详情地图页显示 CF
    public static final String MapTuangouXiangqing = "CF";
    // 商户地图页显示    CH 
    public static final String MapTuangouBranchList = "CH";
    // 影院地图页显示 CT
    public static final String MapDianyingXiangqing = "CT";
    // 影院地图页显示 CV
    public static final String MapDianyingBranchList = "CV";
    // 演出地图页显示 CK
    public static final String MapYanchuList = "CK";
    // 演出详情地图页显示 CM
    public static final String MapYanchuXiangqing = "CM";
    // 展览地图页显示 CO
    public static final String MapZhanlanList = "CO";
    // 展览详情地图页显示 CQ
    public static final String MapZhanlanXiangqing = "CQ";
    
    //  线路地图页显示      CW 
    public static final String MapBusline = "CW";
    //  交通换乘地图页显示    CX 
    public static final String MapTrafficTransfer = "CX";
    //  交通自驾地图页显示    CY 
    public static final String MapTrafficDrive = "CY";
    //  交通步行地图页显示    CZ 
    public static final String MapTrafficWalk = "CZ";
    //  POI地图页显示     BI 
    public static final String MapPOI = "BI";

    // 团购商户页
    // 团购商户页显示 CG
    public static final String FendianList = "CG";

    // 影院列表页
    // 影院列表页显示 CU
    public static final String YingxunList = "CU";

    // 团购购买页
    // 团购购买页显示 CI
    public static final String Browser = "CI";
    // 点击前进按钮 CIAA
    public static final String BrowserForward = Browser + "AA";
    // 点击后退按钮 CIAB
    public static final String BrowserBack = Browser + "AB";
    // 点击刷新按钮      CIAC 
    public static final String BrowserRefresh = Browser + "AC";
    
    // 添加商户 BZ
    public static final String AddMerchant = "BZ";
    // 点击商户类型 BZAA
    public static final String AddMerchantType = AddMerchant + "AA";
    // 点击商户名称 BZAB
    public static final String AddMerchantName = AddMerchant + "AB";
    // 点击商户地址 BZAC
    public static final String AddMerchantAddress = AddMerchant + "AC";
    // 点击商户电话 BZAD
    public static final String AddMerchantTelephone = AddMerchant + "AD";
    // 点击营业时间 BZAE
    public static final String AddMerchantTime = AddMerchant + "AE";
    // 点击您的电话 BZAF
    public static final String AddMerchantMobile = AddMerchant + "AF";
    
    public static final String ChangeToMyLocationCityDialog = "DA";
    public static final String ChangeToMyLocationCityDialogYes = ChangeToMyLocationCityDialog + "AA-%s";
    public static final String ChangeToMyLocationCityDialogNo = ChangeToMyLocationCityDialog + "AB";
    
    /*
          例如 _324-CNET-at=p-REQ=324733-REV=324833-RES=324900-FAIL=serverrefuse

        REQ 发起http请求 的毫秒时刻

        REV 收到服务器的数据 的毫秒时刻

        RES 收完服务器的数据 的毫秒时刻

        FAIL 失败时候填的内容 注意，为避免异常字符干扰日志格式，请做正则替换reg_replace_all(FAIL, "[0-9a-zA-Z]+", "")，去除所有非数字和字母字符。

          补充说明：

        1、这个需求实际关心的是 “我们联网服务质量是否稳定”，暂不考虑底层DNS花多少时间。

        2、只关心联网步骤之间的时间差，REQ（请求） 的起始时间设置为多少，根据客户端的实现方便即可，设置为0也可以。由我这边计算时间差。

        3、必须记录的at有 p（换乘）,q（驾车）,s（搜索/发现）,dh（热点),dc(类别),f(登陆),lm（定位）,b(线路）,边看边下载（td,rq等）、及以后任何较重要的联网服务。

        4、无论成功失败都必须记录联网请求。

        5、由于边看边下载的请求数可能会比较多，为了保证 客户端日志总长度20K的限制 可以合理运行，规则做了修改。请查看上文“序列长度超过20K”处情况说明。

     */
    public static final String NetworkAction = "_%s-CNET-at=%s-REQ=%s-REV=%s-RES=%s-FAIL=%s";
    
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
        addActionInternal(actionLog, true, args);
    }
    
    private void addActionInternal(String actionLog, boolean addTime, Object... args) {
        synchronized (mLock) {
        try {
            
            Object[] str = new String[args.length];
            int i = 0;
            for(Object object : args) {
                if (object != null) {
                    String s = object.toString().replaceAll("[-_%, \t\r{}\\[\\]\\\\&~#$*^+=()]", "@");//"[-_%, \t\r{}\\[\\]\\\\&~#$*^+=()]", "@"
                    if (TextUtils.isEmpty(s)) {
                        str[i] = "0";
                    } else {
                        str[i] = s;
                    }
                } else {
                    str[i] = "null";
                }
                i++;
            }
            addAction(String.format(actionLog, str), addTime);
        } catch (Exception e) {
            e.printStackTrace();
            LogWrapper.e("ActionLog", "addAction() actionLog="+actionLog);
        }
        }
    }
    
    public void addAction(String actionLog) {
        addAction(actionLog, true);
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
                final String str = CommonUtils.readFile(fis)+SEPARATOR_STAET+(simpleDateFormat.format(Calendar.getInstance().getTime()))+SEPARATOR_MIDDLE+LOG_OUT;
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
                    mStringBuilder.append("EXCP");
                    
                    if (TKConfig.getUserActionTrack().equals("on")) {
                        new Thread(new Runnable() {
                            
                            @Override
                            public void run() {
                                FeedbackUpload feedbackUpload = new FeedbackUpload(mContext);
                                Hashtable<String, String> criteria = new Hashtable<String, String>();
                                criteria.put(FeedbackUpload.SERVER_PARAMETER_ACTION_LOG, str);
                                CityInfo cityInfo = Globals.g_Current_City_Info;
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
    
    public void addNetworkAction(String apiType, long reqTime, long revTime, long resTime, String fail) {
        try {
            addActionInternal(NetworkAction, false, String.valueOf((reqTime-mStartMillis)/1000), apiType, String.valueOf(reqTime-mStartMillis), String.valueOf(revTime - mStartMillis), String.valueOf(resTime-mStartMillis), fail);
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
            addAction(SEPARATOR_STAET+simpleDateFormat.format(Calendar.getInstance().getTime())+SEPARATOR_MIDDLE+LifecycleCreate, false);
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
