package com.proposeme.seven.phonecall.utils.mixAduioUtils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Describe: 文件工具类
 * 实现获取基础路径，实现将文件以字节的方式进行读取到byte数组中。
 */
public class FileUtils {

    private static String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record/";

    public static String getFileBasePath(){
        return basePath;
    }

    //将文件流读取到数组中，
    public static byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            Log.d("ccc","file too big...");
            return null;
        }
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead = 0;
        //while循环会使得read一直进行读取，fi.read()在读取完数据以后会返回-1
        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        //确保所有数据均被读取
        if (offset != buffer.length) {
            throw new IOException("Could not completely read file "
                    + file.getName());
        }
        fi.close();
        return buffer;
    }

}
