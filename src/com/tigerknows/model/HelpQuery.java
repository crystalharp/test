/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;

import android.content.Context;

public class HelpQuery extends BaseQuery {
    
    static final String TAG = "HelpQuery";
    
    public HelpQuery(Context context) {
        super(context, API_TYPE_HELP);
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        // TODO Auto-generated method stub
        
    }
}
