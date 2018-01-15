package io.zeebe.broker.clustering.management;

import static org.agrona.BitUtil.SIZE_OF_BYTE;
import static org.agrona.BitUtil.SIZE_OF_INT;

import java.nio.ByteOrder;
import java.util.List;

import io.zeebe.raft.state.RaftState;
import io.zeebe.transport.SocketAddress;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 *
 */
public final class ClusteringHelper
{
    public static DirectBuffer writeAPIAddressesIntoBuffer(SocketAddress managementApi,
                                                           SocketAddress replicationApi,
                                                           SocketAddress clientApi)
    {
        int messageLength = 0;
        messageLength += SIZE_OF_INT; // length of host
        messageLength += managementApi.hostLength(); // host
        messageLength += SIZE_OF_INT; // port

        messageLength += SIZE_OF_INT; // length of host
        messageLength += clientApi.hostLength(); // host
        messageLength += SIZE_OF_INT; // port

        messageLength += SIZE_OF_INT; // length of host
        messageLength += replicationApi.hostLength(); // host
        messageLength += SIZE_OF_INT; // port

        final MutableDirectBuffer directBuffer = new UnsafeBuffer(new byte[messageLength]);

        int offset = 0;
        offset = writeApiAddressIntoBuffer(offset, managementApi, directBuffer);
        offset = writeApiAddressIntoBuffer(offset, clientApi, directBuffer);
        writeApiAddressIntoBuffer(offset, replicationApi, directBuffer);

        return directBuffer;
    }

    private static int writeApiAddressIntoBuffer(int offset, SocketAddress apiAddress, MutableDirectBuffer directBuffer)
    {
        directBuffer.putInt(offset, apiAddress.hostLength(), ByteOrder.LITTLE_ENDIAN);
        offset += SIZE_OF_INT;

        directBuffer.putBytes(offset, apiAddress.getHostBuffer(), 0, apiAddress.hostLength());
        offset += apiAddress.hostLength();

        directBuffer.putInt(offset, apiAddress.port(), ByteOrder.LITTLE_ENDIAN);
        offset += SIZE_OF_INT;
        return offset;
    }

    public static int readFromBufferIntoSocketAddress(int offset, DirectBuffer directBuffer, SocketAddress apiAddress)
    {
        final int hostLength = directBuffer.getInt(offset, ByteOrder.LITTLE_ENDIAN);
        offset += SIZE_OF_INT;

        final byte[] host = new byte[hostLength];
        directBuffer.getBytes(offset, host);
        offset += hostLength;

        final int port = directBuffer.getInt(offset, ByteOrder.LITTLE_ENDIAN);
        offset += SIZE_OF_INT;

        apiAddress.host(host, 0, hostLength);
        apiAddress.port(port);

        return offset;
    }

    public static DirectBuffer writeRaftsIntoBuffer(List<RaftStateComposite> rafts)
    {
        final int raftCount = rafts.size();
        final ExpandableArrayBuffer directBuffer = new ExpandableArrayBuffer();

        int offset = 0;
        directBuffer.putInt(offset, raftCount, ByteOrder.LITTLE_ENDIAN);
        offset += SIZE_OF_INT;

        for (int i = 0; i < raftCount; i++)
        {
            final RaftStateComposite raft = rafts.get(i);

            directBuffer.putInt(offset, raft.getPartition(), ByteOrder.LITTLE_ENDIAN);
            offset += SIZE_OF_INT;

            final DirectBuffer currentTopicName = raft.getTopicName();
            directBuffer.putInt(offset, currentTopicName.capacity(), ByteOrder.LITTLE_ENDIAN);
            offset += SIZE_OF_INT;

            directBuffer.putBytes(offset, currentTopicName, 0, currentTopicName.capacity());
            offset += currentTopicName.capacity();

            directBuffer.putByte(offset, raft.getRaftState() == RaftState.LEADER ? (byte) 1 : (byte) 0);
            offset += SIZE_OF_BYTE;
        }

        return directBuffer;
    }
}
