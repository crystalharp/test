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
    
    // UI显示
    public static final String UI = "ZA";
    
    // 对话框显示
    public static final String DIALOG = "ZB";

    // 回退键
    public static final String KEYCODE = "ZC";
    public static final String SOFTINPUT_ACTION = "ZD";
    public static final String SOFTINPUT_KEY = "ZE";
    
    public static final String CONTROL_ONCLICK = "ZF";
    public static final String LISTVIEW_ITEM_ONCLICK = "ZG";
    public static final String LISTVIEW_CHILD_ITEM_ONCLICK = "ZH";
    public static final String LISTVIEW_GROUP_ITEM_ONCLICK = "ZI";
    public static final String EDITTEXT_DELELE_ONCLICK = "ZJ";
    public static final String FLING = "ZK";
    public static final String VIEWPAGER_SELECTED = "ZL";
    
    // 结果框  ZI-含义-成功/失败原因
    public static final String RESULT = "ZM";
    // 重试
    public static final String RETRY = "ZN";
    
    // 选择筛选条件
    public static final String FILTER_SELECTED = "ZO";
    // 取消筛选
    public static final String FILTER_CANCEL = "ZP";
    
    public static final String POPUPWINDOW = "ZQ";
    
    // 日志超长 EXCP
    public static final String LOG_OUT = "EXCP";
    
    // 搜索首页 AP
    public static final String SearchHome = "AP";
    // 搜索词输入页 AQ
    public static final String SearchInput = "AQ";
    // 结果列表页 AR
    public static final String SearchResult = "AR";
    // Poi详情页 AE
    public static final String POIDetail = "AE";
    // POI详情页显示 AEAA-poi uuid-名称
    public static final String POIDetailShow = POIDetail + "AA";
    // 周边搜索页 AS
    public static final String SearchNearby = "AS";
    // 更多频道
    public static final String More = "AF";
    // 切换城市页
    public static final String ChangeCity = "AG";
    // 下载地图页
    public static final String DownloadMap = "AH";
    // 收藏夹页
    public static final String Favorite = "AI";
    // 历史浏览页
    public static final String History = "AJ";
    // 系统设置页
    public static final String Setting = "AK";
    // 意见反馈页 AL
    public static final String Feedback = "AL";
    // 帮助页
    public static final String Help = "AN";
    // 关于我们页 AM
    public static final String AboutUs = "AM";
    
    // 地图
    public static final String Map = "BH";
    public static final String MapDoubleClick = Map + "AB";
    public static final String MapLongClick = Map + "AE";
    public static final String MapMove = Map + "AH";
    public static final String MapPOIBubble = Map + "AI";
    public static final String MapTrafficBubble = Map + "AJ";
    public static final String MapFirstLocation = Map + "AO";

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
    public static final String LifecycleSelectCity = Lifecycle + "AF";
        
    // 交通频道页显示    TH
    public static final String TrafficHome = "BO";
    // 全屏地图页显示    TM
    public static final String TrafficMap = "BP";
    // 交通输入页显示    TI
    public static final String TrafficInput = "BQ";
    public static final String Traffic = "BY";
    public static final String TrafficQueryTraffic = Traffic + "AA";
    public static final String TrafficQueryBusline = Traffic + "AB";
    public static final String TrafficHomeToMap = Traffic + "BA";
    // 显示选择收藏的POI点页面 TFF
    public static final String TrafficFetchFavorite = "BR";
    // 交通备选站点页显示    TA
    public static final String TrafficAlternative = "BS";
    // 交通方案列表页显示    TR
    public static final String TrafficResult = "BT";
    // 交通方案详情页显示    TD
    public static final String TrafficDetail = "BU";
    // 交通线路列表页显示   TB
    public static final String TrafficBusline = "BV";
    // 交通站点列表页显示	TS
    public static final String TrafficStation = "BW";
    // 线路详情页    TG
    public static final String TrafficLineDetail = "BX";
    // 微博授权界面    
    public static final String WeiboAuthorize = "BJ";
    // 微博发送界面    
    public static final String WeiboSend = "BK";
 	// 微博图片对话框
    public static final String WeiboImage = "BL";
    // QQ空间发送页面
    public static final String QzoneSend = "BM";
    // POI纠错页
    public static final String POIErrorRecovery = "AW";
    // 交通换乘纠错页
    public static final String TransferErrorRecovery = "AX";

    // == 帐户模块通用 ==
    public static final String User = "BG";
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
    
    // == 登录或注册页面 ==
    public static final String UserCommentAfter = "BF";
    // == 登录页面 ==
    public static final String UserLogin = "AY";
    // == 注册页面 ==
    public static final String UserRegist = "AZ";
    // == 重置密码页面 ==
    public static final String UserResetPassword = "BD";
    // == 个人中心页面 == 
    public static final String UserHome = "BA";
    // == 更换手机号 ==
    public static final String UserUpdatePhone = "BB";
    // == 修改昵称 ==
    public static final String UserUpdateNickName = "BC";
    // == 修改密码页面 ==
    public static final String UserUpdatePassword = "BE";
    // 我的点评页 AA
    public static final String MyComment = "AA";
    // 点评输入页 CI
    public static final String POIComment = "AB";
    // 我要点评页 AC
    public static final String GoComment = "AC";
    // POI点评列表页 AD
    public static final String POICommentList = "AD";
    // 应用推荐 AO
    public static final String AppRecommend = "AU";
    // 发现首页显示 CA
    public static final String DiscoverHome = "CA";
    // 团购结果列表页
    public static final String TuangouList = "CB";
    // 电影结果列表页
    public static final String DianyingList = "CR";
    // 展览结果列表页
    public static final String ZhanlanList = "CN";
    // 展览结果列表页
    public static final String YanchuList = "CJ";
    // 团购详情页显示 CE
    public static final String TuangouXiangqing = "CE";
    // 电影详情页显示 CS
    public static final String DianyingXiangqing = "CS";
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
    // 线路地图页显示      CW 
    public static final String MapBusline = "CW";
    // 交通换乘地图页显示    CX 
    public static final String MapTrafficTransfer = "CX";
    // 交通自驾地图页显示    CY 
    public static final String MapTrafficDrive = "CY";
    // 交通步行地图页显示    CZ 
    public static final String MapTrafficWalk = "CZ";
    // POI地图页显示     BI 
    public static final String MapPOI = "BI";
    // 团购商户页显示 CG
    public static final String FendianList = "CG";
    // 影院列表页显示 CU
    public static final String YingxunList = "CU";
    // 团购购买页显示 CI
    public static final String Browser = "CI";
    // 添加商户 BZ
    public static final String AddMerchant = "BZ";
    
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
    public static final String NetworkAction = "CNET";
    
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
            addAction(NetworkAction, apiType, String.valueOf(reqTime-mStartMillis), String.valueOf(revTime - mStartMillis), String.valueOf(resTime-mStartMillis), fail);
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
