/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.task.map;

import static org.agrona.BitUtil.*;

import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.zeebe.broker.task.processor.TaskSubscription;
import io.zeebe.logstreams.snapshot.ZbMapSnapshotSupport;
import io.zeebe.map.Loggers;
import io.zeebe.map.Long2BytesZbMap;
import io.zeebe.map.ZbMapSerializer;
import io.zeebe.map.iterator.Long2BytesZbMapEntry;
import io.zeebe.util.buffer.BufferWriter;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Maps <b>task instance key</b> to
 *
 * <li> state
 * <li> lock owner length
 * <li> lock owner (max 64 chars)
 */
public class TaskInstanceMap
{
    private static final int MAP_VALUE_SIZE = SIZE_OF_SHORT + SIZE_OF_INT + SIZE_OF_CHAR * TaskSubscription.LOCK_OWNER_MAX_LENGTH;

    private static final int STATE_OFFSET = 0;
    private static final int LOCK_OWNER_LENGTH_OFFSET = STATE_OFFSET + SIZE_OF_SHORT;
    private static final int LOCK_OWNER_OFFSET = LOCK_OWNER_LENGTH_OFFSET + SIZE_OF_INT;

    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[MAP_VALUE_SIZE]);
    private final UnsafeBuffer lockOwnerBuffer = new UnsafeBuffer(0, 0);

    private final Long2BytesZbMap map;
    private final Long2BytesZbMap map2;
    private final ZbMapSnapshotSupport<Long2BytesZbMap> snapshotSupport;

    private long key;
    private boolean isRead = false;
    private final ZbMapSerializer serializer;

    enum Op {
        PUT,
        REMOVE
    }

    class Command {
        final Op op;
        final long key;

        Command(final Op op, final long key)
        {
            this.op = op;
            this.key = key;
        }
    }

    private final List<Command> commands = new ArrayList<>();

    public TaskInstanceMap()
    {
        this.map = new Long2BytesZbMap(MAP_VALUE_SIZE);
        this.map2 = new Long2BytesZbMap(MAP_VALUE_SIZE);
        this.snapshotSupport = new ZbMapSnapshotSupport<>(map);
        serializer = new ZbMapSerializer();
        serializer.wrap(map2);
    }

    public ZbMapSnapshotSupport<Long2BytesZbMap> getSnapshotSupport()
    {
        return snapshotSupport;
    }

    public void reset()
    {
        isRead = false;
    }

    public void remove(final long taskInstanceKey)
    {
        commands.add(new Command(Op.REMOVE, taskInstanceKey));
        try
        {
            map.remove(taskInstanceKey);
        }
        catch (final IllegalArgumentException e)
        {
            Loggers.ZB_MAP_LOGGER.info("Remove task instance key: {}", taskInstanceKey);
            System.out.println(map2.toString());
            System.out.println(map2.getBucketBufferArray().toString());
            try (FileOutputStream output = new FileOutputStream("/tmp/taskinstance.map"))
            {

                serializer.writeToStream(output);
            }
            catch (final IOException e1)
            {
                e1.printStackTrace();
            }

            System.out.println("COMMAND LOG:");
            try (PrintWriter writer = new PrintWriter(new FileWriter("/tmp/map.commands.csv")))
            {
                for (final Command command : commands)
                {
                    switch (command.op)
                    {
                    case PUT:
                        writer.println("put;" + command.key);
                        break;
                    case REMOVE:
                        writer.println("remove;" + command.key);
                        break;
                    }
                }

            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }

            throw e;
        }

        map2.remove(taskInstanceKey);
    }

    public TaskInstanceMap wrapTaskInstanceKey(final long key)
    {
        final DirectBuffer result = map.get(key);
        if (result != null)
        {
            buffer.putBytes(0, result, 0, result.capacity());
        }

        this.isRead = result != null;
        this.key = key;

        return this;
    }

    public short getState()
    {
        return isRead ? buffer.getShort(STATE_OFFSET, BYTE_ORDER) : -1;
    }

    public DirectBuffer getLockOwner()
    {
        if (isRead)
        {
            final int length = buffer.getInt(LOCK_OWNER_LENGTH_OFFSET, BYTE_ORDER);
            lockOwnerBuffer.wrap(buffer, LOCK_OWNER_OFFSET, length);
        }
        else
        {
            lockOwnerBuffer.wrap(0, 0);
        }
        return lockOwnerBuffer;
    }

    public TaskInstanceMap newTaskInstance(final long taskInstanceKey)
    {
        key = taskInstanceKey;
        isRead = true;
        return this;
    }

    public void write()
    {
        ensureRead();
        commands.add(new Command(Op.PUT, key));
        map.put(key, buffer);
        map2.put(key, buffer);
    }

    public TaskInstanceMap setState(final short state)
    {
        ensureRead();
        buffer.putShort(STATE_OFFSET, state, BYTE_ORDER);
        return this;
    }

    public TaskInstanceMap setLockOwner(final DirectBuffer lockOwner)
    {
        ensureRead();
        buffer.putInt(LOCK_OWNER_LENGTH_OFFSET, lockOwner.capacity(), BYTE_ORDER);
        buffer.putBytes(LOCK_OWNER_OFFSET, lockOwner, 0, lockOwner.capacity());
        return this;
    }

    private void ensureRead()
    {
        if (!isRead)
        {
            throw new IllegalStateException("must call wrapTaskInstanceKey() before");
        }
    }

    public void close()
    {
        map.close();
    }

}
