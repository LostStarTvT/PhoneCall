package com.proposeme.seven.phonecall.net;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Describe: 保存IP地址信息
 */
public class IPSave {
    public static void saveIP(Context context, String ip){
        SharedPreferences preferences = context.getSharedPreferences("user_ip",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("ip",ip);
        editor.apply();
    }

    public static String getIP(Context context){
        SharedPreferences preferences = context.getSharedPreferences("user_ip",Context.MODE_PRIVATE);
        return preferences.getString("ip",null);
    }
}
