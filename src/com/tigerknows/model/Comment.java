/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.android.app.TKApplication;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Comment extends BaseData {
    
    // 0x00    x_string    uid 点评uid   否
    public static final byte FIELD_UID = 0x00;
    
    // 0x01    x_string    content 点评内容    是
    public static final byte FIELD_CONTENT = 0x01;
    
    // 0x02    x_string    user    点评人 否
    public static final byte FIELD_USER = 0x02;
    
    // 0x03    x_string    time    点评日期    否
    public static final byte FIELD_TIME = 0x03;
    
    // 0x04    x_int   grade   点评星级    是
    public static final byte FIELD_GRADE = 0x04;
    
    // 0x06    x_string    puid    对应poi的uid   是
    public static final byte FIELD_PUID = 0x06;
    
    // 0x07    x_int   avg 人均价格    否
    public static final byte FIELD_AVG = 0x07;
    
    // 0x08    x_int   taste   口味  否
    public static final byte FIELD_TASTE = 0x08;
    
    // 0x09    x_int   qos 服务  否
    public static final byte FIELD_QOS = 0x09;
    
    // 0x0a    x_int   environment 环境  否
    public static final byte FIELD_ENVIRONMENT = 0x0a;
    
    // 0x0b    x_string    recommend   推荐菜（逗号分隔）   否
    public static final byte FIELD_RECOMMEND = 0x0b;
    
    // 0x0c    x_int   level   医疗水平    否
    public static final byte FIELD_LEVEL = 0x0c;
    
    // 0x0d    x_int   effect  观影效果    否
    public static final byte FIELD_EFFECT = 0x0d;
    
    // 0x0e    x_string   restair 餐厅氛围  
    public static final byte FIELD_RESTAIR = 0x0e;
    
    // 0x0f    x_int   poistatus   点评关联的poi是否有效    否 
    public static final byte FIELD_POI_STATUS = 0x0f;
    
    // 0x10 x_int   pattern 点评模式    否  
    public static final byte FIELD_PATTERN = 0x10;
    
    // 0x11 x_string    poiname 点评关联poi的name    否 
    public static final byte FIELD_POI_NAME = 0x11;
    
    // 0x12 x_int userId 不变的用户标识 否
    public static final byte FIELD_USER_ID = 0x12;
    
    // 0x13 x_string    citySeqId   点评相关poi的cityId（短）   否 
    public static final byte FIELD_POI_CITY_ID = 0x13;

    // 0x14 x_string url 新版客户端需要的点评url 否
    public static final byte FIELD_URL = 0x14;

    // 0x15 x_string client_uid 发表点评的客户端软件的uid  否 
    public static final byte FIELD_CLIENT_UID = 0x15;

    // 0x1f    x_string    source  点评来源    是，详细定义见点评数据表中source字段 
    public static final byte FIELD_SOURCE = 0x1f;
    
    // 0x20    x_int   likes   被赞次数    否，插入时不准提交，由数据库自动默认为0 
    public static final byte FIELD_LIKES = 0x20;

    protected static final String NEED_FIELD = "0001020304060708090a0b0c0d0e0f1011121314151f20";

    private String uid; // 0x00    x_string    uid 点评uid   否
    private String content; // 0x01    x_string    content 点评内容    是
    private String user; //0x02    x_string    user    点评人 否
    private String time; // 0x03    x_string    time    点评日期    否
    private long grade = 6; // 0x04    x_int   grade   点评星级    是
    private String puid; // 0x06    x_string    puid    对应poi的uid   是
    private long avg = 0; // 0x07    x_int   avg 人均价格    否
    private long taste = 3; // 0x08    x_int   taste   口味  否
    private long qos = 3; // 0x09    x_int   qos 服务  否
    private long environment = 3; // 0x0a    x_int   environment 环境  否
    private String recommend; // 0x0b    x_string    recommend   推荐菜（逗号分隔）   否
    private long level = 3; // 0x0c    x_int   level   医疗水平    否
    private long effect = 3; // 0x0d    x_int   effect  观影效果    否
    private String restair; // 0x0e    x_string    restair  餐厅氛围 
    private long poiStatus; // 0x0f    x_int   poistatus   点评关联的poi是否有效    否
    private long pattern; // 0x10 x_int   pattern 点评模式    否 
    private String poiName; // 0x11 x_string    poiname 点评关联poi的name    否 
    private long userId = -1; // 0x12 x_int userId 不变的用户标识 否
    private String poiCityId; // 0x13 x_string    citySeqId   点评相关poi的cityId（短）   否 
    private String url; // 0x14 x_string url 新版客户端需要的点评url 否
    private String clientUid; // 0x15 x_string client_uid 发表点评的客户端软件的uid  否 
    private String source; // 0x1f    x_string    source  点评来源   是，详细定义见点评数据表中source字段 
    private long likes = 0;
    
    private POI poi;
    private boolean isCommend = false;
    private static LocalMark sLocalMark = null;
    
    public static LocalMark getLocalMark() {
        if (sLocalMark == null) {
            sLocalMark = new LocalMark("commend", BaseQuery.DATA_TYPE_DIANPING);
        }
        return sLocalMark;
    }
    
    public boolean isCommend() {
        return isCommend;
    }

    public void addCommend(boolean isCommend) {
        if (this.isCommend == false && isCommend) {
            likes += 1;
        }
        this.isCommend = isCommend;
    }

    public void setCommend(boolean isCommend) {
        this.isCommend = isCommend;
    }

    public Comment() {
    }

    public Comment (XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.uid = getStringFromData(FIELD_UID, reset ? null : this.uid);
        this.content = getStringFromData(FIELD_CONTENT, reset ? null : this.content);
        this.user = getStringFromData(FIELD_USER, reset ? null : this.user);
        this.time = getStringFromData(FIELD_TIME, reset ? null : this.time);
        this.grade = getLongFromData(FIELD_GRADE, reset ? 6 : this.grade);
        this.puid = getStringFromData(FIELD_PUID, reset ? null : this.puid);
        this.avg = getLongFromData(FIELD_AVG, reset ? 0 : this.avg);
        this.taste = getLongFromData(FIELD_TASTE, reset ? 3 : this.taste);
        this.qos = getLongFromData(FIELD_QOS, reset ? 3 : this.qos);
        this.environment = getLongFromData(FIELD_ENVIRONMENT, reset ? 3 : this.environment);
        this.recommend = getStringFromData(FIELD_RECOMMEND, reset ? null : this.recommend);
        this.level = getLongFromData(FIELD_LEVEL, reset ? 3 : this.level);
        this.effect = getLongFromData(FIELD_EFFECT, reset ? 3 : this.effect);
        this.restair = getStringFromData(FIELD_RESTAIR, reset ? null : this.restair);
        this.poiStatus = getLongFromData(FIELD_POI_STATUS, reset ? 0 : this.poiStatus);
        this.pattern = getLongFromData(FIELD_PATTERN, reset ? 0 : this.pattern);
        this.poiName = getStringFromData(FIELD_POI_NAME, reset ? null : this.poiName);
        this.userId = getLongFromData(FIELD_USER_ID, reset ? -1 : this.userId);
        this.poiCityId = getStringFromData(FIELD_POI_CITY_ID, reset ? null : this.poiCityId);
        this.url = getStringFromData(FIELD_URL, reset ? null : this.url);
        this.clientUid = getStringFromData(FIELD_CLIENT_UID, reset ? null : this.clientUid);
        this.source = getStringFromData(FIELD_SOURCE, reset ? null : this.source);
        this.likes = getLongFromData(FIELD_LIKES, reset ? 0 : this.likes);
        
        if (reset == false) {
            this.data = null;
        }
        
        long draft = getLocalMark().findCommend(TKApplication.getInstance(), this.uid, LocalMark.STATE_DRAFT);
        if (draft > 0) {
            addCommend(true);
        } else {
            isCommend = (getLocalMark().findCommend(TKApplication.getInstance(), this.uid, LocalMark.STATE_SENT) > 0);
        }
    }
    
    public XMap getData() {
        if (this.data == null) {
            XMap data = new XMap();   
            if (!TextUtils.isEmpty(this.uid)) {
                data.put(FIELD_UID, this.uid);
            }
            if (!TextUtils.isEmpty(this.content)) {
                data.put(FIELD_CONTENT, this.content);
            }
            if (!TextUtils.isEmpty(this.user)) {
                data.put(FIELD_USER, this.user);
            }
            if (!TextUtils.isEmpty(this.time)) {
                data.put(FIELD_TIME, this.time);
            }
            if (this.grade != 6) {
                data.put(FIELD_GRADE, this.grade);
            }
            if (!TextUtils.isEmpty(this.puid)) {
                data.put(FIELD_PUID, this.puid);
            }
            if (this.avg != -1) {
                data.put(FIELD_AVG, this.avg);
            }
            if (this.taste != 3) {
                data.put(FIELD_TASTE, this.taste);
            }
            if (this.qos != 3) {
                data.put(FIELD_QOS, this.qos);
            }
            if (this.environment != 3) {
                data.put(FIELD_ENVIRONMENT, this.environment);
            }
            if (!TextUtils.isEmpty(this.recommend)) {
                data.put(FIELD_RECOMMEND, this.recommend);
            }
            if (this.level != 3) {
                data.put(FIELD_LEVEL, this.level);
            }
            if (this.effect != 3) {
                data.put(FIELD_EFFECT, this.effect);
            }
            if (!TextUtils.isEmpty(this.restair)) {
                data.put(FIELD_RESTAIR, this.restair);
            }
            if (this.poiStatus != 0) {
                data.put(FIELD_POI_STATUS, this.poiStatus);
            }
            if (this.pattern != 0) {
                data.put(FIELD_PATTERN, this.pattern);
            }
            if (!TextUtils.isEmpty(this.poiName)) {
                data.put(FIELD_POI_NAME, this.poiName);
            }
            if (this.userId != -1) {
                data.put(FIELD_USER_ID, this.userId);
            }
            if (!TextUtils.isEmpty(this.poiCityId)) {
                data.put(FIELD_POI_CITY_ID, this.poiCityId);
            }
            if (!TextUtils.isEmpty(this.url)) {
                data.put(FIELD_URL, this.url);
            }
            if (!TextUtils.isEmpty(this.clientUid)) {
                data.put(FIELD_CLIENT_UID, this.clientUid);
            }
            if (!TextUtils.isEmpty(this.source)) {
                data.put(FIELD_SOURCE, this.source);
            }
            if (this.likes != 0) {
                data.put(FIELD_LIKES, this.likes);
            }
            this.data = data;
        }
        return data;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
        getData().put(FIELD_UID, this.uid);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        getData().put(FIELD_CONTENT, this.content);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
        getData().put(FIELD_USER, this.user);
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
        getData().put(FIELD_TIME, this.time);
    }

    public long getGrade() {
        return grade;
    }

    public void setGrade(long grade) {
        this.grade = grade;
        getData().put(FIELD_GRADE, this.grade);
    }

    public String getUrl() {
        return url;
    }

    public String getPuid() {
        return puid;
    }

    public long getAvg() {
        return avg;
    }

    public void setAvg(long avg) {
        this.avg = avg;
        getData().put(FIELD_AVG, this.avg);
    }

    public long getTaste() {
        return taste;
    }

    public void setTaste(long taste) {
        this.taste = taste;
        getData().put(FIELD_TASTE, this.taste);
    }

    public long getQos() {
        return qos;
    }

    public void setQos(long qos) {
        this.qos = qos;
        getData().put(FIELD_QOS, this.qos);
    }

    public long getEnvironment() {
        return environment;
    }

    public void setEnvironment(long environment) {
        this.environment = environment;
        getData().put(FIELD_ENVIRONMENT, this.environment);
    }

    public String getRecommend() {
        return recommend;
    }

    public void setRecommend(String recommend) {
        this.recommend = recommend;
        if (TextUtils.isEmpty(recommend)) {
            getData().remove(FIELD_RECOMMEND);
        } else {
            getData().put(FIELD_RECOMMEND, this.recommend);
        }
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
        getData().put(FIELD_LEVEL, this.level);
    }

    public void setLikes(long likes) {
        this.likes = likes;
        getData().put(FIELD_LIKES, this.likes);
    }

    public long getEffect() {
        return effect;
    }

    public void setEffect(long effect) {
        this.effect = effect;
        getData().put(FIELD_EFFECT, this.effect);
    }

    public String getSource() {
        return source;
    }

    public String getRestair() {
        return restair;
    }

    public void setRestair(String restair) {
        this.restair = restair;
        if (TextUtils.isEmpty(restair)) {
            getData().remove(FIELD_RESTAIR);
        } else {
            getData().put(FIELD_RESTAIR, this.restair);
        }
    }

    public long getPOIStatus() {
        return poiStatus;
    }

    public long getPattern() {
        return pattern;
    }

    public String getPOIName() {
        return poiName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
        getData().put(FIELD_USER_ID, this.userId);
    }

    public String getPoiCityId() {
        return poiCityId;
    }

    public String getClientUid() {
        return clientUid;
    }

    public void setClientUid(String clientUid) {
        this.clientUid = clientUid;
        getData().put(FIELD_CLIENT_UID, this.clientUid);
    }
    
    public long getLikes() {
        return likes;
    }

    public POI getPOI() {
        if (poi == null) {
            poi = new POI();
            if (!TextUtils.isEmpty(puid)) {
                poi.setUUID(puid);
            }
            if (!TextUtils.isEmpty(poiName)) {
                poi.setName(poiName);
            }
            poi.setStatus(poiStatus);
            poi.setCommentPattern(pattern);
            poi.setMyComment(this);
            if (!TextUtils.isEmpty(poiCityId)) {
                try {
                    poi.ciytId = Integer.parseInt(poiCityId);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
        
        return poi;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Comment) {
            Comment other = (Comment) object;
            if((null != other.uid && !other.uid.equals(this.uid)) || (null == other.uid && other.uid != this.uid)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    @SuppressWarnings("rawtypes")
    public static Comparator COMPARATOR = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            Comment comment1 = (Comment) object1;
            Comment comment2 = (Comment) object2;

            if (isAuthorMe(comment1) > 0) {
                return -1;
            } else if (isAuthorMe(comment2) > 0) {
                return 1;
            }
            return comment2.getTime().compareTo(comment1.getTime());
        };
    };
    
    @SuppressWarnings("rawtypes")
    public static Comparator COMPARATOR_ONLY_TIME = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            Comment comment1 = (Comment) object1;
            Comment comment2 = (Comment) object2;
            return comment2.getTime().compareTo(comment1.getTime());
        };
    };
    
    /**
     * 判断评论的作者是否为自己
     * @param comment
     * @return
     */
    public static long isAuthorMe(Comment comment) {
        long attr = 0;
        if (comment == null) {
            return attr; 
        }
        String clientUID = Globals.g_ClientUID;
        User user = Globals.g_User;
        long userId = -1;
        if (user != null) {
            userId = user.getUserId();
        }
        
        // 金截
        if (userId != -1 && comment.getUserId() == userId) {
            attr = POI.ATTRIBUTE_COMMENT_USER;
            
        // 银截
        } else if (userId == -1
                && comment.getUserId() == -1
                && clientUID != null
                && clientUID.equals(comment.getClientUid())) {
            attr = POI.ATTRIBUTE_COMMENT_ANONYMOUS;
        }
        
        return attr;
    }
    
    @SuppressWarnings("rawtypes")
    public static Comparator COMPARATOR_DATETIME = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            Comment comment1 = (Comment) object1;
            Comment comment2 = (Comment) object2;
            return comment2.getTime().compareTo(comment1.getTime());
        };
    };
    
    public static DataQuery createPOICommentQuery(Context context, POI poi, int sourceViewId, int targerViewId) {

        DataQuery commentQuery = new DataQuery(context);
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
        commentQuery.setup(sourceViewId, targerViewId, null, false, false, poi);
        return commentQuery;
    }
    
    public static XMapInitializer<Comment> Initializer = new XMapInitializer<Comment>() {

        @Override
        public Comment init(XMap data) throws APIException {
            return new Comment(data);
        }
    };
    
    public Comment clone() {
        Comment other = null;
        try {
            other = new Comment(getData());
        } catch (APIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return other;
    }
    
    /**
     * 对某一对象的是否标记状态存储在本地，其标记总数存储在服务器端
     * 在合适的时机将存储在本地的标记状态信息上传到服务器
     * @author pengwenyue
     *
     */
    public static class LocalMark {
        
        public static final String SEPARATOR = " ";
        private String fileName = null;
        private List<String> sCommendList = null;
        private List<String> sDraftList = null;
        private List<String> sDeleteList = null;
        private Object writeLock = new Object();
        private File sDraftFile = null;
        private File sSentFile = null;
        private File sDeleteFile = null;
        private String dataType;

        public static final int STATE_DRAFT = 0;
        public static final int STATE_SENT = 1;
        public static final int STATE_DELETE = 2;


        public static final String LOCAL_PARAMETER_MARK = "LOCAL_PARAMETER_MARK";
        public static final String LOCAL_PARAMETER_MARK_ON = "ON";
        public static final String LOCAL_PARAMETER_MARK_OFF = "OFF";
        
        public LocalMark(String fileName, String dataType) {
            this.fileName = fileName;
            this.dataType = dataType;
            
            initCommendList(TKApplication.getInstance());
        }
        
        private File getFile(Context context, int state) {
            String name = null;
            if (state == STATE_SENT) {
                if (sSentFile != null) {
                    return sSentFile;
                }
                name = "sent";
            } else if (state == STATE_DRAFT) {
                if (sDraftFile != null) {
                    return sDraftFile;
                }
                name = "draft";
            } else if (state == STATE_DELETE) {
                if (sDeleteFile != null) {
                    return sDeleteFile;
                }
                name = "delete";
            }
            File file = new File(context.getFilesDir().getAbsolutePath() + "/" + fileName +name);
//            File file = new File(TKConfig.getDataPath(true) + "/commend"+(sent ? "sent" : "draft"));
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return file;
        }
        
        //对于已经赞过的commend列表并不会再去删除，数据量会变大，所以写入策略选用追加
        public void addCommend(Context context, String uuid, int state) {
            long time = System.currentTimeMillis();
            File file = getFile(context, state);
            List<String> list = getCommendList(state);
            for(int i = list.size()-1; i >= 0; i--) {
                String str = list.get(i);
                if (str.startsWith(uuid+SEPARATOR)) {
                    list.remove(i);
                    break;
                }
            }
            list.add(uuid + SEPARATOR + time);
            writeCommendList(file, list);
        }
        
        public long findCommend(Context context, String uuid, int state) {
            long result = 0;
            if (uuid == null) {
                return result;
            }
            List<String> list = getCommendList(state);
            for(int i = list.size()-1; i >= 0; i--) {
                String str = list.get(i);
                if (str.startsWith(uuid + SEPARATOR)) {
                    result = Long.valueOf(str.substring(str.indexOf(SEPARATOR)+1));
                    break;
                }
            }
            return result;
        }
        
        //因为只会删除draft中的id，所以它的写入策略是删掉原文件，然后整个list写进去。
        public void deleteCommend(Context context, String uuids[], int state) {
            File file = getFile(context, state);
            List<String> list = getCommendList(state);
            boolean deleted = false;
            for (String uuid : uuids) {
                for(int i = list.size()-1; i >= 0; i--) {
                    String str = list.get(i);
                    if (str.startsWith(uuid + SEPARATOR)) {
                        deleted = true;
                        list.remove(i);
                        break;
                    }
                }
            }
            if (deleted) {
                writeCommendList(file, list);
            }
            
            return;
        }
        
        private final List<String> getCommendList(int state) {
            if (state == STATE_SENT) {
                return sCommendList;
            } else if (state == STATE_DRAFT) {
                return sDraftList;
            } else {
                return sDeleteList;
            }
        }
        
        private final void initCommendList(Context context) {
            if (sDraftList == null || sCommendList == null) {
                sDraftList = new LinkedList<String>();
                sCommendList = new LinkedList<String>();
                sDeleteList = new LinkedList<String>();
                try {
                    sSentFile = getFile(context, STATE_SENT);
                    sDraftFile = getFile(context, STATE_DRAFT);
                    sDeleteFile = getFile(context, STATE_DELETE);
                    String line = null;
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sSentFile)));
                    long time = System.currentTimeMillis();
                    while((line = br.readLine()) != null) {
                        if (line.indexOf(SEPARATOR) != -1) {
                            sCommendList.add(line);
                        } else {
                            sCommendList.add(line + SEPARATOR + time);
                        }
                    }
                    br.close();
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(sDraftFile)));
                    while ((line = br.readLine()) != null) {
                        if (line.indexOf(SEPARATOR) != -1) {
                            sDraftList.add(line);
                        } else {
                            sDraftList.add(line + SEPARATOR + time);
                        }
                    }
                    br.close();
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(sDeleteFile)));
                    while ((line = br.readLine()) != null) {
                        if (line.indexOf(SEPARATOR) != -1) {
                            sDeleteList.add(line);
                        } else {
                            sDeleteList.add(line + SEPARATOR + time);
                        }
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void writeCommendList(File file, List<String> list) {
            synchronized(writeLock) {
                StringBuilder sb = new StringBuilder();
                for (String s : list) {
                    sb.append(s);
                    sb.append('\n');
                }
                Utility.writeFile(file.getAbsolutePath(), sb.toString().getBytes(), true);
            }
        }
        
        public static final String UpJsonHeader = "json{\"up\":[";
        public static final String DownJsonHeader = "json{\"down\":[";

        private static String uuid2Json(Context context, String uuid, boolean mark) {
            StringBuilder json = new StringBuilder();
            json.append(mark ? UpJsonHeader : DownJsonHeader);
            json.append('"');
            json.append(uuid);
            json.append('"');
            json.append("]}");
            return json.toString();
        }

        private String commendList2Json(Context context, int state) {
            StringBuilder json = new StringBuilder();
            json.append(state == STATE_DELETE ? DownJsonHeader : UpJsonHeader);

            int i = 0;
            
            List<String> list = getCommendList(state);
            for(;i < list.size(); i++) {
                String str = list.get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append('"');
                json.append(str.substring(0, str.indexOf(SEPARATOR)));
                json.append('"');
            }
            json.append("]}");
            
            if (i == 0) {
                return null;
            } else {
                return json.toString();
            }
        }
        
        public DataOperation makeCommendDataOperationByUUID(Context context, String uuid, boolean mark) {
            String json = uuid2Json(context, uuid, mark);
            return makeCommendDataOperation(context, dataType, json, mark);
        }
        
        public DataOperation makeCommendDataOperation(Context context, int state) {
            String json = commendList2Json(context, state);
            return makeCommendDataOperation(context, dataType, json, state == STATE_DRAFT);
        }
        
        static DataOperation makeCommendDataOperation(Context context, String dataType, String json, boolean mark) {
            DataOperation dataOperation = null;
            if (TextUtils.isEmpty(json) == false) {
                dataOperation = new DataOperation(context);
                dataOperation.addLocalParameter(LOCAL_PARAMETER_MARK, mark ? LOCAL_PARAMETER_MARK_ON : LOCAL_PARAMETER_MARK_OFF);
                dataOperation.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, dataType);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, URLEncoder.encode(json));
            }
            return dataOperation;
        }
    }
}
