package com.tigerknows.widget;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.TKWord;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;
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
/**
 * 
 * @author xupeng
 * 历史词建议词的Adapter
 */

public class SuggestArrayAdapter extends ArrayAdapter<TKWord> {
    public static final int TEXTVIEW_RESOURCE_ID = R.layout.suggest_list_item;
    
    public interface BtnEventHandler {
        public void onBtnClicked(TKWord tkWord, int position);
    }
    
    private Context context;
    private BtnEventHandler inputBtnEventHandler;
    public String key;
    
    public void setInputBtnEventHandler(BtnEventHandler btnEventHandler) {
        this.inputBtnEventHandler = btnEventHandler;
    }

    public SuggestArrayAdapter(Context context, List<TKWord> objects) {
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
        TextView text1Txv = (TextView)convertView.findViewById(R.id.text1_txv);
        Button inputBtn = (Button)convertView.findViewById(R.id.input_btn);
        
        final TKWord tkWord = getItem(position);
        if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
            iconImv.setVisibility(View.VISIBLE);
            inputBtn.setVisibility(View.VISIBLE);
            iconImv.setImageResource(R.drawable.ic_time);
            textTxv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            Utility.formatText(textTxv, tkWord.word, key, TKConfig.COLOR_BLACK_LIGHT);
            if (TextUtils.isEmpty(tkWord.address) == false) {
                text1Txv.setText(tkWord.address);
                text1Txv.setVisibility(View.VISIBLE);
            } else {
                text1Txv.setVisibility(View.GONE);
            }
        } else if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST) {
            iconImv.setVisibility(View.VISIBLE);
            inputBtn.setVisibility(View.VISIBLE);
            text1Txv.setVisibility(View.GONE);
            iconImv.setImageResource(R.drawable.ic_suggest);
            textTxv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            Utility.formatText(textTxv, tkWord.word, key, TKConfig.COLOR_BLACK_LIGHT);
        } else if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
            iconImv.setVisibility(View.INVISIBLE);
            inputBtn.setVisibility(View.INVISIBLE);
            text1Txv.setVisibility(View.GONE);
            textTxv.setGravity(Gravity.CENTER);
            textTxv.setText(tkWord.word);
        }
        if (inputBtnEventHandler != null) {
            inputBtn.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    inputBtnEventHandler.onBtnClicked(tkWord, position);
                }
            });
        }
        
        return convertView;
    }
}
