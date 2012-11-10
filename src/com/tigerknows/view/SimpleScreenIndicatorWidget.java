package com.tigerknows.view;
/*
 * Copyright 2011 tfdroid
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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.tigerknows.R;

public class SimpleScreenIndicatorWidget extends LinearLayout implements ScrollScreen.ScreenIndicator {
        
        public SimpleScreenIndicatorWidget(Context context, AttributeSet attrs) {
                super(context, attrs);
        }

        @Override
        public void addIndicator(){
                ImageView slidePot = new ImageView(getContext());
                slidePot.setPadding(5, 5, 5, 5);
                addView(slidePot);
        }
        
        @Override
        public void setCurrentScreen(int index) {
                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                        ImageView point = (ImageView) getChildAt(i);
                        
                        if (i == index) {
                                point.setBackgroundResource(R.drawable.ic_learn_dot_selected);
                        } else {
                                point.setBackgroundResource(R.drawable.ic_learn_dot_normal);
                        }
                }
        }
}
