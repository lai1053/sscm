package com.kakarote.admin.oceanengine.util;

import com.github.promeg.pinyinhelper.Pinyin;

public class PinyinHelper {

    /**
     * 把中文转成全拼，小写，去掉空格
     */
    public static String toPinyin(String chinese) {
        if (chinese == null) {
            return "";
        }
        String pinyin = Pinyin.toPinyin(chinese, "");
        return pinyin.toLowerCase();
    }
}
