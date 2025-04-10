package com.bobby.rpc.core.common.codec;

import com.bobby.rpc.core.common.RpcResponse;
import com.bobby.rpc.core.common.codec.serializer.ISerializer;
import com.bobby.rpc.core.common.constants.BRpcConstants;
import com.bobby.rpc.core.common.enums.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/4/10
 */
public class DecoderTest {
    @Test
    public void testDecodeWithHalfPackets() {
        // 1. 准备解码器和测试用的 EmbeddedChannel
        CommonDecoder decoder = new CommonDecoder();
        EmbeddedChannel channel = new EmbeddedChannel(decoder);

        RpcResponse response = RpcResponse.builder()
                .data(1L)
                .dataType(Long.class)
                .message("hhhh")
                .code(200)
                .build();
        ISerializer serializer = ISerializer.getSerializerByCode(1);
        byte[] responseBytes = serializer.serialize(response);

        // 2. 构造一个完整的合法帧（假设总长度 4+2+2+4+serialize.length 字节）
        int totalLength = 4 + 2 + 2 + 4 + responseBytes.length;
        ByteBuf fullFrame = Unpooled.buffer();
        fullFrame.writeInt(BRpcConstants.MAGIC_NUMBER); // 4字节 魔数
        fullFrame.writeShort(MessageType.RESPONSE.getCode()); // 2字节 消息类型
        fullFrame.writeShort(1); // 2字节 序列化类型
        fullFrame.writeInt(responseBytes.length); // 4字节 数据长度
        fullFrame.writeBytes(responseBytes); //

        // 3. 模拟拆包：分 3 次写入（每次只写部分数据）
        ByteBuf slice1 = Unpooled.copiedBuffer(fullFrame.slice(0, 5));
        ByteBuf slice2 = Unpooled.copiedBuffer(fullFrame.slice(5, 10)); // 从 5 开始，读取长度为 10
        ByteBuf slice3 = Unpooled.copiedBuffer(fullFrame.slice(15, totalLength-15));

        // 4. 分次写入，检查解码器是否正确处理
        channel.writeInbound(slice1); // 第一次：数据不足，应该不触发 decode
        Object o1 = channel.readInbound();
        assertNull(o1); // 无输出

        channel.writeInbound(slice2); // 第二次：仍然不足（缺少剩余数据）
        Object o2 = channel.readInbound();
        assertNull(o2); // 无输出

        channel.writeInbound(slice3); // 第三次：数据完整，应解码成功
        Object decoded = channel.readInbound();
        assertNotNull(decoded); // 成功解码

        // 5. 释放资源
        fullFrame.release();
    }
}
