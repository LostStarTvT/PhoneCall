package com.proposeme.seven.phonecall.utils.mixAduioUtils;

import android.util.Log;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Describe: 混音工作类
 */

public class MixAudioUtil {
    /**
     * 采用简单的平均算法 average audio mixing algorithm
     * 测试发现这种算法会降低 录制的音量
     * 混合pcm的算法，并且作为文件进行保存
     * 原理：量化的语音信号的叠加等价于空气中声波的叠加，反应到音频数据上，也就是把同一个声道的数值进行简单的相加
     */

    public static byte[] averageMix(String file1,String file2) throws IOException {

        byte[][] bMulRoadAudioes =  new byte[][]{
                FileUtils.getContent(file1),    //第一个文件
                FileUtils.getContent(file2)     //第二个文件
        };


        byte[] realMixAudio = bMulRoadAudioes[0]; //保存混音之后的数据。
        Log.e("ccc", " bMulRoadAudioes length " + bMulRoadAudioes.length); //2
        //判断两个文件的大小是否相同，如果不同进行补齐操作
        for (int rw = 0; rw < bMulRoadAudioes.length; ++rw) { //length一直都是等于2.依次检测file长度和file2长度
            if (bMulRoadAudioes[rw].length != realMixAudio.length) {
                Log.e("ccc", "column of the road of audio + " + rw + " is diffrent.");
                if (bMulRoadAudioes[rw].length<realMixAudio.length){
                    realMixAudio = subBytes(realMixAudio,0,bMulRoadAudioes[rw].length); //进行数组的扩展
                }
                else if (bMulRoadAudioes[rw].length>realMixAudio.length){
                    bMulRoadAudioes[rw] = subBytes(bMulRoadAudioes[rw],0,realMixAudio.length);
                }
            }
        }

        int row = bMulRoadAudioes.length;       //行
        int column = realMixAudio.length / 2;   //列
        short[][] sMulRoadAudioes = new short[row][column];
        for (int r = 0; r < row; ++r) {         //前半部分
            for (int c = 0; c < column; ++c) {
                sMulRoadAudioes[r][c] = (short) ((bMulRoadAudioes[r][c * 2] & 0xff) | (bMulRoadAudioes[r][c * 2 + 1] & 0xff) << 8);
            }
        }
        short[] sMixAudio = new short[column];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < column; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < row; ++sr) {
                mixVal += sMulRoadAudioes[sr][sc];
            }
            sMixAudio[sc] = (short) (mixVal / row);
        }

        //合成混音保存在realMixAudio
        for (sr = 0; sr < column; ++sr) { //后半部分
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }

        //保存混合之后的pcm
        FileOutputStream fos = null;
        //保存合成之后的文件。
        File saveFile = new File(FileUtils.getFileBasePath()+ "averageMix.pcm" );
        if (saveFile.exists()) {
            saveFile.delete();
        }
        fos = new FileOutputStream(saveFile);// 建立一个可存取字节的文件
        fos.write(realMixAudio);
        fos.close();// 关闭写入流
        return realMixAudio; //返回合成的混音。
    }

    //合并两个音轨。
    private static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }
}

