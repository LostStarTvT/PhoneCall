package com.proposeme.seven.phonecall.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Describe: 保存用户信息工具类
 */
public class UserNameUtil {
    public static void saveUsername(Context context, String username){
        SharedPreferences preferences = context.getSharedPreferences("username",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("key",username);
        editor.apply();
    }
    public static String getUsername(Context context){
        SharedPreferences preferences = context.getSharedPreferences("username",Context.MODE_PRIVATE);
        String username = preferences.getString("key",null);
        return username;
    }
}
