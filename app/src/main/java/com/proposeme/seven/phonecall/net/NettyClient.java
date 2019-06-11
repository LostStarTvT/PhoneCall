package com.proposeme.seven.phonecall.net;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Describe: 使用netty框架构建音频传输客户端
 * 此代理只是监听本地端口，然后在发送数据的时候需要指定对方ip和对方端口
 */
public class NettyClient {
    private NettyReceiverHandler handler;

    private int localPort = 7777;//绑定本地端口
    private String targetIp = "127.0.0.1";//对方ip地址
    private int targetport = 7777; // 对方端口
    private NettyReceiverHandler.FrameResultedCallback frameResultedCallback;
    private static NettyClient mNettyClient;
    private EventLoopGroup group;


    //关闭连接
    public void shutDownBootstrap(){
        group.shutdownGracefully();
    }
    //构造者模式进行构建ip和端口。
    private NettyClient(Builder builder) {
        localPort = builder.localPort;
        targetIp = builder.targetIp;
        targetport = builder.targetport;
        frameResultedCallback =builder.frameResultedCallback;
        init();
        mNettyClient = this;
    }

    /**
     * 初始化
     */
    private void init() {
        //初始化receiverHandler.
        handler = new NettyReceiverHandler();

        if (frameResultedCallback!=null){
            handler.setOnFrameCallback(frameResultedCallback);
        }

        //启动客户端进行发送数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                group = new NioEventLoopGroup();
                try {
                    //设置netty的连接属性。
                    b.group(group)
                            .channel(NioDatagramChannel.class) //异步的 UDP 连接
                            .option(ChannelOption.SO_BROADCAST, true)
                            .option(ChannelOption.SO_RCVBUF, 1024 * 1024)//接收区2m缓存
                            .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))//加上这个，里面是最大接收、发送的长度
                            .handler(handler); //设置数据的处理器

                    b.bind(localPort).sync().channel().closeFuture().await();
                    //构造一个劲监听本地端口的netty代理
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    group.shutdownGracefully();
                }
            }
        }).start();
    }

    /**
     * 调用NettyReceiverHandler.sendData（）发送数据
     * @param data
     */
    public void sendData(Object data, String msgType) {
        //调用 NettyReceiverHandler.sendData（）方法的调用
        handler.sendData(targetIp, targetport, data, msgType);
    }

    //通过指定IP进行发送数据
    public void UserIPSendData(String targetIp,Object data, String msgType) {
        //这个就是NettyReceiverHandler.sendData（）方法的调用，也即是说，在NettyReceiverHandler这里面可以
        //即是处理接受数据的也是处理，发送数据的。
        handler.sendData(targetIp, targetport, data, msgType);
    }

    //构造者模式
    public static final class Builder {
        private int localPort;
        private String targetIp;
        private int targetport;
        private NettyReceiverHandler.FrameResultedCallback frameResultedCallback;

        public Builder() {
        }

        public Builder localPort(int val) {
            localPort = val;
            return this;
        }

        public Builder targetIp(String val) {
            targetIp = val;
            return this;
        }

        public Builder targetport(int val) {
            targetport = val;
            return this;
        }

        public Builder frameResultedCallback(NettyReceiverHandler.FrameResultedCallback val) {
            frameResultedCallback = val;
            return this;
        }

        public NettyClient build() {
            return new NettyClient(this);
        }
    }
}
