/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tigerknows.view;

import com.tigerknows.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView.OnEditorActionListener;


/**
 * Custom EditText with delete button. */
public class TKEditText extends LinearLayout implements OnClickListener {
    static final int IMEOPTION_ACTIONUNSPECIFIED=0x00000000;
    static final int IMEOPTION_ACTIONNONE=0x00000001;
    static final int IMEOPTION_ACTIONGO=0x00000002;
    static final int IMEOPTION_ACTIONSEARCH=0x00000003;
    static final int IMEOPTION_ACTIONSEND=0x00000004;
    static final int IMEOPTION_ACTIONNEXT=0x00000005;
    static final int IMEOPTION_ACTIONDONE=0x00000006;
    static final int IMEOPTION_FLAGNOEXTRACTUI=0x10000000;
    static final int IMEOPTION_FLAGNOACCESSORYACTION=0x20000000;
    static final int IMEOPTION_FLAGNOENTERACTION=0x40000000;
    
    private EditText mInputEdt;
    private Button mDeleteBtn;
        
    public TKEditText(Context context) {
        this(context, null);
    }

    public TKEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.tk_edittext, this, // we are the parent
                true);
        
        findViews();
        setListener();
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.tkEditText);
        if (a != null) {
            int maxLength = a.getInt(R.styleable.tkEditText_maxLength, -1);
            if (maxLength > 0) {
                mInputEdt.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
            }
            CharSequence hint = a.getText(R.styleable.tkEditText_hint);
            mInputEdt.setHint(hint);
            int paddingLeft = a.getDimensionPixelSize(R.styleable.tkEditText_paddingLeft, 0);
            if (paddingLeft != 0) {
                mInputEdt.setPadding(paddingLeft, 0, 0, 0);
            }
            Drawable drawable = a.getDrawable(R.styleable.tkEditText_background);
            if (drawable != null) {
                mInputEdt.setBackgroundDrawable(drawable);
            }
            int imeOptions = a.getInt(R.styleable.tkEditText_imeOptions, IMEOPTION_ACTIONUNSPECIFIED);
            int androidImeOption = EditorInfo.IME_NULL;
            switch (imeOptions) {
                case IMEOPTION_ACTIONDONE:
                    androidImeOption = EditorInfo.IME_ACTION_DONE;
                    break;
                case IMEOPTION_ACTIONGO:
                    androidImeOption = EditorInfo.IME_ACTION_GO;
                    break;
                case IMEOPTION_ACTIONNEXT:
                    androidImeOption = EditorInfo.IME_ACTION_NEXT;
                    break;
                case IMEOPTION_ACTIONSEARCH:
                    androidImeOption = EditorInfo.IME_ACTION_SEARCH;
                    break;
                case IMEOPTION_ACTIONSEND:
                    androidImeOption = EditorInfo.IME_ACTION_SEND;
                    break;
                case IMEOPTION_ACTIONUNSPECIFIED:
                    androidImeOption = EditorInfo.IME_ACTION_UNSPECIFIED;
                    break;
                case IMEOPTION_FLAGNOACCESSORYACTION:
                    androidImeOption = EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION;
                    break;
                case IMEOPTION_FLAGNOENTERACTION:
                    androidImeOption = EditorInfo.IME_FLAG_NO_ENTER_ACTION;
                    break;
                case IMEOPTION_FLAGNOEXTRACTUI:
                    androidImeOption = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
                    break;
                default:
                    break;
            }
            mInputEdt.setImeOptions(androidImeOption);
            a.recycle();
        }
        
        mInputEdt.setSingleLine(true);
        mDeleteBtn.setVisibility(View.GONE);
    }

    protected void findViews() {
        mInputEdt = (EditText) findViewById(R.id.tk_input_edt);
        mDeleteBtn = (Button) findViewById(R.id.tk_delete_btn);
    }
    
    protected void setListener() {
        mDeleteBtn.setOnClickListener(this);
        mInputEdt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
    
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    showDelete();
                } else {
                    hideDelete();
                }
            }
    
            public void afterTextChanged(Editable s) {
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        /* Consume all touch events so they don't get dispatched to the view
         * beneath this view.
         */
        return true;
    }
    
    public void showDelete() {
        if (mDeleteBtn.getVisibility() == View.VISIBLE)
            return;
        fade(View.VISIBLE, 0.0f, 1.0f);
    }
    
    public void hideDelete() {
        if (mDeleteBtn.getVisibility() != View.VISIBLE)
            return;
        fade(View.GONE, 1.0f, 0.0f);
    }
    
    private void fade(int visibility, float startAlpha, float endAlpha) {
        AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
        anim.setDuration(500);
        mDeleteBtn.startAnimation(anim);
        mDeleteBtn.setVisibility(visibility);
    }
    
    @Override
    public boolean hasFocus() {
        return mInputEdt.hasFocus() || mDeleteBtn.hasFocus();
    }

    @Override
    public void onClick(View view) {
        setText(null);
    }
    
    public void setOnEditorActionListener(OnEditorActionListener l) {
        mInputEdt.setOnEditorActionListener(l);
    }
    
    public void addTextChangedListener(TextWatcher watcher) {
        mInputEdt.addTextChangedListener(watcher);
    }
    
    public void setText(CharSequence text) {
        mInputEdt.setText(text);
        if (text != null) {
            mInputEdt.setSelection(text.length());
        } else {
            mInputEdt.setSelection(0);
        }
    }
    
    public Editable getText() {
        return mInputEdt.getText();
    }
    
    public void setOnTouchListener(OnTouchListener onTouchListener) {
        mInputEdt.setOnTouchListener(onTouchListener);
    }
}

