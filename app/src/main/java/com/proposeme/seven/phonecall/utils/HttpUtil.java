package com.proposeme.seven.phonecall.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Describe: 和服务器进行连接的网络类
 */
public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
