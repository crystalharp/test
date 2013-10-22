/**
 * Copyright (C) 2011 Pinyin for Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tigerknows.util;

import com.tigerknows.TKConfig;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class is pinyin4android main interface .
 * there are two methods you can call them to convert the chinese to pinyin.
 * PinyinUtil.toPinyin(Context context,char c);
 * PinyinUtil.toPinyin(Context context,String hanzi);
 * <p/>
 * User: Ryan
 * Date: 11-5-29
 * Time: 21:13
 */
public abstract class PinyinUtil {

    /**
     * to convert chinese to pinyin
     *
     * @param context Android Context
     * @param hanzi   the chinese string
     * @return pinyin
     */
    public static String toPinyin(Context context, String hanzi) {
        StringBuffer sb = new StringBuffer("");
        RandomAccessFile is = null;
        try {
            is = new RandomAccessFile(new File(TKConfig.getDataPath(false)+"pinyin4android"), "r");
            for (int i = 0; i < hanzi.length(); i++) {
                char ch = hanzi.charAt(i);
                if (ch >= 'A' && ch <= 'Z') {
                    sb.append((char) (ch + 32));
                    continue;
                }
                if (ch >= 'a' && ch <= 'z') {
                    sb.append(ch);
                    continue;
                }
                if (ch >= '0' && ch <= '9') {
                    sb.append('*');
                    continue;
                }
                if (ch == 0x3007) {
                    sb.append("ling").append(' ');
                } else if (ch >= 0x4E00 && ch <= 0x9FA5) {
                    long sp = (ch - 0x4E00) * 6;
                    is.seek(sp);
                    byte[] buf = new byte[6];
                    is.read(buf);
                    sb.append(new String(buf).trim()).append(' ');
                }else{
                    sb.append(ch);
                    continue;
                }
                
            }
        } catch (IOException e) {
            //
        } finally {
            try {
                if (null != is) is.close();
            } catch (IOException e) {
                //
            }
        }
        return sb.toString().trim();
    }
}
