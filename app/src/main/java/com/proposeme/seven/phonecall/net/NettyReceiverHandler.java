package com.proposeme.seven.phonecall.net;

import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.proposeme.seven.phonecall.utils.MLOC;
import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/**
 * Describe: 进行语音数据的发送和接收执行者。
 */
public class NettyReceiverHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private ChannelHandlerContext channelHandlerContext;
    //数据返回接口注册
    private FrameResultedCallback frameCallback;

    public void setOnFrameCallback(FrameResultedCallback callback) {
        this.frameCallback = callback;
    }

    //收到数据时候进行开始触发
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
            throws Exception {
        //服务器推送对方IP和PORT
        ByteBuf buf = (ByteBuf) packet.copy().content(); //字节缓冲区
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String str = new String(req, "UTF-8");
        Message message = JSON.parseObject(str,Message.class);  //同一类中不需要进行导包

//        Log.e("ccc","信息流" + str);
        //只有发送文字信息的时候才会返回对方ip。
        //对应各自的回调。
        if (message.getMsgtype().equals(Message.MES_TYPE_NOMAL)){
            if (frameCallback !=null){
                frameCallback.onTextMessage(message.getMsgBody());
                frameCallback.onGetRemoteIP(message.getMsgIp());
            }
        }else if (message.getMsgtype().equals(Message.MES_TYPE_AUDIO)){
            if (frameCallback !=null){
                frameCallback.onAudioData(message.getFrame());
            }
        }
    }

    //当通道激活时候进行触发,
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channelHandlerContext = ctx;
        Log.e("ccc", "nettyReceiver启动");
    }

    //发生异常时候进行调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Log.e("ccc", "同道异常关闭 ");
    }

    //根据传递过来的ip和port 进行发送数据。
    public void sendData(String ip, int port, Object data, String type) {
        Message message = null;
        if (data instanceof byte[]) {
            message = new Message();
            message.setFrame((byte[]) data);
            message.setMsgtype(type);
            message.setTimestamp(System.currentTimeMillis());
        }else if (data instanceof String){
            //在发送文本的时候也需要将本地ip地址发送过去。
            message = new Message();
            message.setMsgBody((String) data);
            message.setMsgtype(type);
            message.setTimestamp(System.currentTimeMillis());
            message.setMsgIp(MLOC.localIpAddress);
        }

        //进行数据的发送
        if (channelHandlerContext != null) {
            channelHandlerContext.writeAndFlush(new DatagramPacket(
                    Unpooled.copiedBuffer(JSON.toJSONString(message).getBytes()),
                    new InetSocketAddress(ip, port)));
        }
    }

    public interface FrameResultedCallback {
        void onTextMessage(String msg); //返回文本信息
        void onAudioData(byte[] data);  //返回音频信息
        void onGetRemoteIP(String ip);  //返回对方ip 只是在发送文字信息的时候接受到
    }
}
