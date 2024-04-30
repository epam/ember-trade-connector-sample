package deltix.ember.connector.grpc.syneroex.session;

import deltix.anvil.util.buffer.AtomicBuffer;
import deltix.anvil.util.buffer.UnsafeBuffer;
import deltix.anvil.util.counter.Counter;
import deltix.anvil.util.pointer.UnsafeLongPointer;

import static deltix.anvil.util.BitUtil.DOUBLE_CACHE_LINE_SIZE;
import static deltix.anvil.util.BitUtil.SIZE_OF_LONG;

public final class SessionControl {

    private static final int ENABLED_OFFSET = DOUBLE_CACHE_LINE_SIZE + SIZE_OF_LONG;
    private static final int SIZE = SIZE_OF_LONG + DOUBLE_CACHE_LINE_SIZE;

    private final AtomicBuffer data = UnsafeBuffer.allocateDirect(SIZE);

    private final Counter enabled = new UnsafeLongPointer(data, ENABLED_OFFSET);

    public void updateEnabled(final boolean enabled) {
        this.enabled.setOrdered(enabled ? 1 : 0);
    }

}
