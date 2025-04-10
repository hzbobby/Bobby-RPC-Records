package com.bobby.rpc.core.common.codec;

import com.bobby.rpc.core.common.codec.serializer.ISerializer;
import com.bobby.rpc.core.common.constants.BRpcProtocolConstants;
import com.bobby.rpc.core.common.enums.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 按照自定义的消息格式解码数据
 */
@Slf4j
@AllArgsConstructor
public class CommonDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.debug("MyDecode$decode");
        Object frame = decode(ctx, in);
        if (frame != null) {
            out.add(frame);
        }
    }

    private Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        // 模仿 LengthFieldBasedFrameDecoder 防止拆包的思路
        // 先进行校验，如果数据长度不足，先返回 null

        // 0. 标记当前读指针位置（防拆包回退）
        in.markReaderIndex();

        // 1. 检查是否足够读取魔数（4字节）
        if (in.readableBytes() < 4) {
            return null; // 等待更多数据
        }

        // 2. 读取并验证魔数
        int magicNumber = readMagicNumber(in);
        if (magicNumber != BRpcProtocolConstants.MAGIC_NUMBER) {
            in.resetReaderIndex();
            log.error("非法数据包: 魔数不匹配, 实际: 0x{}, 预期: 0x5250434D",
                    Integer.toHexString(magicNumber));
            throw new RuntimeException(String.format("Invalid Magic Number: 0x{}", Integer.toHexString(magicNumber)));
        }

        // 3. 检查剩余数据是否足够读取消息类型+序列化类型+长度（2+2+4=8字节）
        if (in.readableBytes() < 8) {
            in.resetReaderIndex(); // 回退起始位置
            return null;
        }
        // 4. 读取元数据 (消息类型，序列化类型，消息长度)
        short messageType = in.readShort();
        if (!isValidMessageType(messageType)) {
            in.resetReaderIndex();
            log.error("暂不支持此种数据: {}", messageType);
            throw new RuntimeException("暂不支持此种数据");
        }
        short serializerType = in.readShort();
        int length = in.readInt();

        // 5. 检查 length 是否合法
        if (length < 0) {
            in.resetReaderIndex(); // 回退起始位置
            return null;
        }

        // 6. 检查是否足够读取实际数据
        if (in.readableBytes() < length) {
            in.resetReaderIndex(); // 回退起始位置
            return null;
        }

        ISerializer serializer = ISerializer.getSerializerByCode(serializerType);
        // 7. 读取序列化数组
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        // 用对应的序列化器解码字节数组
        Object frame = serializer.deserialize(bytes, messageType);
        return frame;
    }

    private int readMagicNumber(ByteBuf in) {
        return in.readInt();    // 我们魔术是定义 4 个字节
    }

    private boolean isValidMessageType(short type) {
        return type == MessageType.REQUEST.getCode() ||
                type == MessageType.RESPONSE.getCode();
    }

//
//    private void serializeTraceMsg(byte[] traceByte){
//        String traceMsg=new String(traceByte);
//        String[] msgs=traceMsg.split(";");
//        if(!msgs[0].equals("")) TraceContext.setTraceId(msgs[0]);
//        if(!msgs[1].equals("")) TraceContext.setParentSpanId(msgs[1]);
//    }
}