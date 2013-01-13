
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.SqliteWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

/**
 * 写入数据库表和需要收藏的父类
 * 
 * @author pengwenyue
 */
public class BaseData extends XMapData {
    
    protected long id = -1;

    protected long parentId = -1;

    protected int storeType = -1;

    protected long dateTime = -1;
    
    public BaseData() {
    }
    
    public BaseData(XMap data) throws APIException {
        super(data);
    }
    
    public void init(XMap data) throws APIException {
        this.data = data;
        if (this.data == null) {
            throw new APIException(APIException.RESPONSE_DATA_IS_EMPTY);
        }
    }

    public long getId() {
        return id;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public int getStoreType() {
        return storeType;
    }

    public void setStoreType(int storeType) {
        this.storeType = storeType;
    }

    public long getDateTime() {
        return dateTime;
    }
    
    public Uri writeToDatabases(Context context, long parentId, int storeType) {
        return null;
    }
    
    public BaseData checkStore(Context context, int storeType) {
        return null;
    }
    
    protected Uri writeToDatabasesInternal(Context context, Uri insertUri, ContentValues values, int storeType, int type) {
        boolean isFailed = false;
        Uri uri = null;
        values.put(Tigerknows.TransitPlan.STORE_TYPE, storeType);
        if (storeType == Tigerknows.STORE_TYPE_FAVORITE) {
            com.tigerknows.model.Favorite favorite = new com.tigerknows.model.Favorite();
            uri = favorite.writeToDatabases(context, type);
            if (uri != null) {
                parentId = favorite.id;
                values.put(Tigerknows.TransitPlan.PARENT_ID, parentId);
            } else {
                isFailed = true;
            }
        } else if (storeType == Tigerknows.STORE_TYPE_HISTORY) {
            
            History history = new History();
            uri = history.writeToDatabases(context, type);
            if (uri != null) {
                parentId = history.id;
                values.put(Tigerknows.TransitPlan.PARENT_ID, parentId);
            } else {
                isFailed = true;
            }
        }
        
        uri = null;
        if (!isFailed) {
            uri = SqliteWrapper.insert(context, context.getContentResolver(), insertUri, values);
            if (uri != null) {
                id = Integer.parseInt(uri.getPathSegments().get(1));
            } else {
                if (storeType == Tigerknows.STORE_TYPE_FAVORITE) {
                    SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, "_id="+parentId, null);
                } else if (storeType == Tigerknows.STORE_TYPE_HISTORY) {
                    SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.History.CONTENT_URI, "_id="+parentId, null);
                    
                }
            }
        }
        return uri;
    }
    
    public boolean checkFavorite(Context context) {
        return checkStore(context, Tigerknows.STORE_TYPE_FAVORITE) != null;
    }

    public int deleteFavorite(Context context) {
        int count = 0;
        BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_FAVORITE);
        if (baseData != null) {
            count = SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, "_id="+baseData.parentId, null);
        }
        return count;
    }
    
    public boolean checkHistory(Context context) {
        return checkStore(context, Tigerknows.STORE_TYPE_HISTORY) != null;
    }

    public int deleteHistory(Context context) {
        int count = 0;
        BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_HISTORY);
        if (baseData != null) {
            count = SqliteWrapper.delete(context, context.getContentResolver(),
                Tigerknows.History.CONTENT_URI, "_id=" + baseData.parentId, null);
        }
        return count;
    }
    
}
