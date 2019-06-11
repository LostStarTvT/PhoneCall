package com.proposeme.seven.phonecall.audio;

/**
 * Describe: 音频数据
 */
public class AudioData {
    int size;
    short[] realData;
    byte[] receiverdata;

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public short[] getRealData()
    {
        return realData;
    }

    public void setRealData(short[] realData)
    {
        this.realData = realData;
    }

    public byte[] getReceiverdata() {
        return receiverdata;
    }

    public void setReceiverdata(byte[] receiverdata) {
        this.receiverdata = receiverdata;
    }
}
