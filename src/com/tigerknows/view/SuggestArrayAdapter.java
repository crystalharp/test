package com.tigerknows.view;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.TKWord;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SuggestArrayAdapter extends ArrayAdapter<TKWord> {
    public static final int TEXTVIEW_RESOURCE_ID = R.layout.suggest_list_item;
    
    public interface CallBack {
        public void onItemClicked(TKWord tkWord, int position);
    }
    
    private Context context;
    private CallBack callBack;
    public String key;
    
    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public SuggestArrayAdapter(Context context, int textViewResourceId, List<TKWord> objects) {
        super(context, TEXTVIEW_RESOURCE_ID, objects);
        this.context = context;
    }        

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(TEXTVIEW_RESOURCE_ID, parent, false);
        }
        
        ImageView iconImv = (ImageView) convertView.findViewById(R.id.icon_imv);
        TextView textTxv = (TextView)convertView.findViewById(R.id.text_txv);
        Button inputBtn = (Button)convertView.findViewById(R.id.input_btn);
        
        final TKWord tkWord = getItem(position);
        if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
            iconImv.setVisibility(View.VISIBLE);
            inputBtn.setVisibility(View.VISIBLE);
            textTxv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            formatText(textTxv, tkWord.word, key);
        } else if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST) {
            iconImv.setVisibility(View.INVISIBLE);
            inputBtn.setVisibility(View.VISIBLE);
            textTxv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            formatText(textTxv, tkWord.word, key);
        } else {
            iconImv.setVisibility(View.INVISIBLE);
            inputBtn.setVisibility(View.INVISIBLE);
            textTxv.setGravity(Gravity.CENTER);
            textTxv.setText(tkWord.word);
        }
        if (callBack != null) {
            inputBtn.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    callBack.onItemClicked(tkWord, position);
                }
            });
        }
        
        return convertView;
    }
    
    private void formatText(TextView textView, String word, String key) {
        if (TextUtils.isEmpty(key)) {
            textView.setText(word);
        } else {
            SpannableStringBuilder style=new SpannableStringBuilder(word);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_LIGHT),0,key.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(style);
        }
    }
}
