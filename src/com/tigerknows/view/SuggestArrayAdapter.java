package com.tigerknows.view;

import com.tigerknows.R;
import com.tigerknows.model.TKWord;

import android.content.Context;
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
        public void onItemClicked(String text);
    }
    
    private Context context;
    private CallBack callBack;
    
    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public SuggestArrayAdapter(Context context, int textViewResourceId, List<TKWord> objects) {
        super(context, TEXTVIEW_RESOURCE_ID, objects);
        this.context = context;
    }        

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(TEXTVIEW_RESOURCE_ID, parent, false);
        }
        
        ImageView iconImv = (ImageView) convertView.findViewById(R.id.icon_imv);
        TextView textTxv = (TextView)convertView.findViewById(R.id.text_txv);
        Button inputBtn = (Button)convertView.findViewById(R.id.input_btn);
        
        final TKWord tkWord = getItem(position);
        if (tkWord.type == TKWord.TYPE_HISTORY) {
            iconImv.setVisibility(View.VISIBLE);
            inputBtn.setVisibility(View.VISIBLE);
            textTxv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        } else if (tkWord.type == TKWord.TYPE_SUGGEST) {
            iconImv.setVisibility(View.INVISIBLE);
            inputBtn.setVisibility(View.VISIBLE);
            textTxv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        } else {
            iconImv.setVisibility(View.INVISIBLE);
            inputBtn.setVisibility(View.INVISIBLE);
            textTxv.setGravity(Gravity.CENTER);
        }
        textTxv.setText(tkWord.word);
        if (callBack != null) {
            inputBtn.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    callBack.onItemClicked(tkWord.word);
                }
            });
        }
        
        return convertView;
    }
}
