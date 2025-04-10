package com.bobby.rpc.core.common.codec;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.RpcResponse;
import com.bobby.rpc.core.common.codec.serializer.ISerializer;
import com.bobby.rpc.core.common.constants.BRpcProtocolConstants;
import com.bobby.rpc.core.common.enums.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 依次按照自定义的消息格式写入，传入的数据为request或者response
 * 需要持有一个serialize器，负责将传入的对象序列化成字节数组
 *
 * BRPC 协议格式
 *
 * +----------------+----------------+----------------+----------------+----------------+
 * |     魔数       |   消息类型     |  序列化类型    |    消息长度    |     消息体      |
 * +----------------+----------------+----------------+----------------+----------------+
 * |   4 Byte       |    2 Byte      |    2 Byte      |    4 Byte      |   N Byte       |
 * +----------------+----------------+----------------+----------------+----------------+
 *
 * 字段说明：
 * 1. 魔数(BRPC)  : 固定值 0x42525043 (ASCII 'BRPC')，用于标识协议起始
 * 2. 消息类型    : 标识请求/响应等消息类型 (如 0x01=请求, 0x02=响应)
 * 3. 序列化类型  : 标识消息体的序列化方式 (如 0x01=JSON, 0x02=Protobuf)
 * 4. 消息长度    : 消息体的实际字节长度 (大端序)
 * 5. 消息体      : 实际负载数据，长度由[消息长度]字段指定
 */
@AllArgsConstructor
@Slf4j
public class CommonEncoder extends MessageToByteEncoder {
    private ISerializer serializer;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        log.debug("MyEncode$encode mgs of Type: {}", msg.getClass());



//        // version10. 写入 trace 消息头
//        String traceMsg = String.format("%s;%s", TraceContext.getTraceId(), TraceContext.getSpanId());
//        byte[] traceBytes = traceMsg.getBytes();
//
//        // 写入 traceMsg 长度
//        out.writeInt(traceBytes.length);
//        // 写入 traceMsg
//        out.writeBytes(traceBytes);


        // 增加魔数机制
        writeMagicNumber(out);

        // 写入消息类型
        if (msg instanceof RpcRequest) {
            out.writeShort(MessageType.REQUEST.getCode());
        } else if (msg instanceof RpcResponse) {
            out.writeShort(MessageType.RESPONSE.getCode());
        }
        // 写入序列化方式
        out.writeShort(serializer.getType());
        // 得到序列化数组
        byte[] serialize = serializer.serialize(msg);
        // 写入长度
        out.writeInt(serialize.length);
        // 写入序列化字节数组
        out.writeBytes(serialize);
    }

    private void writeMagicNumber(ByteBuf out) {
        // 0x42: B
        // 0x52: R
        // 0x50: P
        // 0x43: C
        out.writeInt(BRpcProtocolConstants.MAGIC_NUMBER);
    }
}