package com.bobby.rpc.core.client.transport.netty;

import com.bobby.rpc.core.common.codec.CommonDecoder;
import com.bobby.rpc.core.common.codec.CommonEncoder;
import com.bobby.rpc.core.common.codec.serializer.ISerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 通过 handler 获取客户端的结果
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
    private final String serializerTypeName;

    public NettyClientInitializer() {
        this.serializerTypeName = ISerializer.SerializerType.JSON.name();
    }

    public NettyClientInitializer(String serializerTypeName) {
        this.serializerTypeName = serializerTypeName.toUpperCase();
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

//        // 消息格式 [长度][消息体], 解决粘包问题
//        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
//        // 计算当前待发送消息的长度，写入到前4个字节中
//        pipeline.addLast(new LengthFieldPrepender(4));
//
//        // 这里使用的还是java 序列化方式， netty的自带的解码编码支持传输这种结构
//        pipeline.addLast(new ObjectEncoder());
//        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
//            @Override
//            public Class<?> resolve(String className) throws ClassNotFoundException {
//                return Class.forName(className);
//            }
//        }));


        // 使用自定义的编解码器
        pipeline.addLast(new CommonDecoder());
        // 编码需要传入序列化器，这里是json，还支持ObjectSerializer，也可以自己实现其他的
//        pipeline.addLast(new CommonEncoder(ISerializer.getSerializerByCode(ISerializer.SerializerType.PROTOBUF.getCode())));
        pipeline.addLast(new CommonEncoder(ISerializer.getSerializer(ISerializer.SerializerType.valueOf(serializerTypeName))));
//        pipeline.addLast(new CommonEncode(SerializerSpiLoader.getInstance(serializer)));

        pipeline.addLast(new NettyClientHandler());

        // v6 心跳机制，使链接存活
        pipeline.addLast(new IdleStateHandler(0, 8, 0, TimeUnit.SECONDS));
        pipeline.addLast(new ClientHeartbeatHandler());

    }
}