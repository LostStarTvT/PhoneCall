package com.proposeme.seven.phonecall.audio;

import android.util.Log;

import com.gyz.voipdemo_speex.util.Speex;
import com.proposeme.seven.phonecall.provider.ApiProvider;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Describe: 监听收集到的音频流，并且进行编码，让后调用EncodeProvider 将数据发送出去
 */
public class AudioEncoder implements Runnable{
    String LOG = "AudioEncoder";
    //单例模式构造对象
    private static AudioEncoder encoder;
    //是否正在编码
    private volatile boolean isEncoding = false;

    //每一帧的音频数据的集合
    private List<AudioData> dataList = null;

    public static AudioEncoder getInstance() {
        if (encoder == null) {
            encoder = new AudioEncoder();
        }
        return encoder;
    }

    private AudioEncoder() {
        dataList = Collections.synchronizedList(new LinkedList<AudioData>());
    }

    //存放录音的数据
    public void addData(short[] data, int size) {
        AudioData rawData = new AudioData();
        rawData.setSize(size);
        short[] tempData = new short[size];
        System.arraycopy(data, 0, tempData, 0, size);
        rawData.setRealData(tempData);
        dataList.add(rawData);
    }

    /**
     * 开始编码。
     */
    public void startEncoding() {

        Log.e("ccc", "编码子线程启动");
        if (isEncoding) {
            Log.e(LOG, "encoder has been started  !!!");
            return;
        }
        //开子线程
        new Thread(this).start();
    }

    /**
     * end encoding	停止编码
     */
    public void stopEncoding() {
        this.isEncoding = false;
    }

    @Override
    public void run() {
        int encodeSize = 0;
        byte[] encodedData;
        isEncoding = true;
        while (isEncoding) {
            if (dataList.size() == 0) { //如果没有编码数据则进行等待并且释放线程
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            if (isEncoding) {
                AudioData rawData = dataList.remove(0);
                encodedData = new byte[Speex.getInstance().getFrameSize()];
                encodeSize = Speex.getInstance().encode(rawData.getRealData(),
                        0, encodedData, rawData.getSize());
                if (encodeSize > 0) {
                    //实现发送数据。
                    if (ApiProvider.getProvider()!=null)
                        ApiProvider.getProvider().sendAudioFrame(encodedData); //发送录音数据
                }
            }
        }
    }
}
