package deltix.connector.fix.mapper;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.anvil.util.BitUtil;
import deltix.anvil.util.buffer.BufferUtil;
import deltix.anvil.util.buffer.MutableBuffer;
import deltix.anvil.util.buffer.UnsafeBuffer;
import deltix.util.collections.generated.LongToLongHashMap;

import static deltix.anvil.util.BitUtil.SIZE_OF_LONG;

/**
 * Indexed ring buffer with the fixed entry size and capacity, eviction by insert order.
 * Footprint is about to 96 * capacity bytes.
 */
public class CancelIdCache {

    protected final static int MAX_CAPACITY = 8 * 1024 * 1024;
    protected final static int ENTRY_SIZE = 64;
    protected final static int MAX_REQUEST_ID_SIZE = 56;

    protected static final long NOT_FOUND = -1;

    protected final LongToLongHashMap indexMap;
    protected final MutableBuffer buffer;
    protected final int capacity;

    protected int index = 0;

    public CancelIdCache(final int capacity) {
        if (capacity < 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity is out of range [0, 8 * 1024 * 1024]: " + capacity);
        }

        this.indexMap = new LongToLongHashMap(capacity);
        this.buffer = UnsafeBuffer.allocateDirectedAlignedAndPadded(capacity * ENTRY_SIZE, BitUtil.DOUBLE_CACHE_LINE_SIZE);
        this.capacity = capacity;
    }

    public void put(final long sequence, final CharSequence requestId) {
        final int length = requestId.length();

        assert length >= 0 && !indexMap.containsKey(sequence);

        if (length > MAX_REQUEST_ID_SIZE) {
            throw new IllegalArgumentException("request id length: " + length + " exceeds max: " + MAX_REQUEST_ID_SIZE);
        }

        if (capacity == indexMap.size()) {
            final long sequenceToRemove = buffer.getLong(index);
            indexMap.remove(sequenceToRemove);
        }

        final long value = ((long) index << 32) | length;
        indexMap.put(sequence, value);

        buffer.putLong(index, sequence);
        BufferUtil.putCharSequence(requestId, 0, buffer, index + SIZE_OF_LONG, length);

        index += ENTRY_SIZE;

        if (index == buffer.capacity()) {
            index = 0;
        }
    }

    public AsciiStringFlyweight get(final long sequence, final AsciiStringFlyweight flyweight) {
        AsciiStringFlyweight result = null;
        final long value = indexMap.get(sequence, NOT_FOUND);

        if (value != NOT_FOUND) {
            final int index = (int) (value >>> 32);
            final int length = (int) value;

            flyweight.wrap(buffer, index + SIZE_OF_LONG, length);
            result = flyweight;
        }

        return result;
    }


}
