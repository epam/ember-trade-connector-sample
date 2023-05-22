package deltix.connector.fix.mapper;

import deltix.anvil.util.AbstractByteSequence;
import deltix.anvil.util.AsciiStringBuilder;
import deltix.anvil.util.ObjectPool;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.util.collections.CharSeqToLongMap;
import deltix.util.collections.CircularBufferOfInt;
import deltix.util.collections.generated.LongToObjectHashMap;

import java.util.Arrays;


public class OrderIdCache {

    protected static final long NOT_FOUND = -1;

    protected final AsciiStringBuilder internalKey = new AsciiStringBuilder(40);

    protected final ActiveCache activeCache;
    protected final InactiveCache inactiveCache;

    public OrderIdCache(final int activeCacheInitialCapacity, final int inactiveCacheCapacity) {
        this.activeCache = new ActiveCache(activeCacheInitialCapacity);
        this.inactiveCache = new InactiveCache(inactiveCacheCapacity);
    }

    public void put(final long sequence, final @Alphanumeric long sourceId, final CharSequence orderId) throws OrderIdMappingException {
        makeInternalKey(sourceId, orderId, internalKey);

        if (!activeCache.put(internalKey, sequence)) {
            throw new OrderIdMappingException(
                    new AsciiStringBuilder()
                            .append("Duplicate sequence: ")
                            .append(sequence)
                            .append(" or order identity: ")
                            .appendAlphanumeric(sourceId)
                            .append(':')
                            .append(orderId)
                            .toString()
            );
        }
    }

    public void get(final long sequence, final OrderIdFlyweight flyweight) throws OrderIdMappingException {
        AsciiStringBuilder internalKey = activeCache.getInternalKey(sequence);

        if (internalKey == null) {
            internalKey = inactiveCache.getInternalKey(sequence);

            if (internalKey == null) {
                throw new OrderIdMappingException("Can't find order identity by sequence: " + sequence);
            }
        }

        splitInternalKey(internalKey, flyweight);
    }

    public long get(final @Alphanumeric long sourceId, final CharSequence orderId) throws OrderIdMappingException {
        makeInternalKey(sourceId, orderId, internalKey);
        long sequence = activeCache.getExternalKey(internalKey, NOT_FOUND);

        if (sequence == NOT_FOUND) {
            sequence = inactiveCache.getExternalKey(internalKey, NOT_FOUND);

            if (sequence == NOT_FOUND) {
                throw new OrderIdMappingException(
                        new AsciiStringBuilder()
                                .append("Can't find sequence by order identity: ")
                                .appendAlphanumeric(sourceId)
                                .append(':')
                                .append(orderId)
                                .toString()
                );
            }
        }

        return sequence;
    }

    public void remove(final long sequence) {
        final CharSequence internalKey = activeCache.removeByExternalKey(sequence);

        if (internalKey != null) {
            inactiveCache.put(internalKey, sequence);
        }
    }

    public void remove(final @Alphanumeric long sourceId, final CharSequence orderId) {
        makeInternalKey(sourceId, orderId, internalKey);
        final long sequence = activeCache.removeByInternalKey(internalKey, NOT_FOUND);

        if (sequence != NOT_FOUND) {
            inactiveCache.put(internalKey, sequence);
        }
    }

    protected static void makeInternalKey(final @Alphanumeric long sourceId, final CharSequence orderId, final AsciiStringBuilder key) {
        key.clear()
                .append((char) (sourceId >>> 56))
                .append((char) ((sourceId >>> 48) & 0xFF))
                .append((char) ((sourceId >>> 40) & 0xFF))
                .append((char) ((sourceId >>> 32) & 0xFF))
                .append((char) ((sourceId >>> 24) & 0xFF))
                .append((char) ((sourceId >>> 16) & 0xFF))
                .append((char) ((sourceId >>> 8) & 0xFF))
                .append((char) (sourceId & 0xFF))
                .append(orderId);
    }

    protected static void splitInternalKey(final AsciiStringBuilder key, final OrderIdFlyweight orderIdFlyweight) {
        @Alphanumeric long sourceId = 0;

        sourceId += (((long) key.byteAt(0) & 0xFF) << 56);
        sourceId += (((long) key.byteAt(1) & 0xFF) << 48);
        sourceId += (((long) key.byteAt(2) & 0xFF) << 40);
        sourceId += (((long) key.byteAt(3) & 0xFF) << 32);
        sourceId += (((long) key.byteAt(4) & 0xFF) << 24);
        sourceId += (((long) key.byteAt(5) & 0xFF) << 16);
        sourceId += (((long) key.byteAt(6) & 0xFF) << 8);
        sourceId += (((long) key.byteAt(7) & 0xFF));

        orderIdFlyweight.sourceId = sourceId;
        orderIdFlyweight.orderId.clear().append(key, 8, key.length());
    }


    protected static class ActiveCache {

        protected final LongToObjectHashMap<AsciiStringBuilder> externalMap;
        protected final CharSeqToLongMap<AsciiStringBuilder> internalMap;
        protected final ObjectPool<AsciiStringBuilder> internalKeys;

        public ActiveCache(final int initialCapacity) {
            this.externalMap = new LongToObjectHashMap<>(initialCapacity);
            // it's critical to use equals(ByteSequence) since we set negative bytes for alphanumeric
            this.internalMap = new CharSeqToLongMap<>(initialCapacity, AbstractByteSequence::hashCode, AbstractByteSequence::equals);
            this.internalKeys = new ObjectPool<>(initialCapacity, () -> new AsciiStringBuilder(40));
        }

        public boolean put(final CharSequence internalKey, final long externalKey) {
            final AsciiStringBuilder internalKeyCopy = internalKeys.borrow();
            internalKeyCopy.clear().append(internalKey);

            if (!internalMap.putIfEmpty(internalKeyCopy, externalKey)) {
                internalKeys.release(internalKeyCopy);
                return false;
            }

            if (!externalMap.putIfEmpty(externalKey, internalKeyCopy)) {
                internalMap.remove(internalKeyCopy);
                internalKeys.release(internalKeyCopy);
                return false;
            }

            return true;
        }

        public AsciiStringBuilder getInternalKey(final long externalKey) {
            return externalMap.get(externalKey, null);
        }

        public long getExternalKey(final AsciiStringBuilder internalKey, final long missingExternalKey) {
            return internalMap.get(internalKey, missingExternalKey);
        }

        public CharSequence removeByExternalKey(final long externalKey) {
            final AsciiStringBuilder internalKey = externalMap.remove(externalKey, null);

            if (internalKey != null) {
                internalMap.remove(internalKey);
                internalKeys.release(internalKey);
            }

            return internalKey;
        }

        public long removeByInternalKey(final AsciiStringBuilder internalKey, final long missingExternalKey) {
            final long externalKey = internalMap.remove(internalKey, missingExternalKey);

            if (externalKey != missingExternalKey) {
                final AsciiStringBuilder internalKeyCopy = externalMap.remove(externalKey, null);
                internalKeys.release(internalKeyCopy);
            }

            return externalKey;
        }

    }

    protected static class InactiveCache implements LongToObjectCache.Consumer, CharSeqToLongCache.Consumer {

        protected final LongToObjectCache externalMap;
        protected final CharSeqToLongCache internalMap;
        protected final ObjectPool<AsciiStringBuilder> internalKeys;

        public InactiveCache(final int capacity) {
            this.externalMap = new LongToObjectCache(capacity, this);
            this.internalMap = new CharSeqToLongCache(capacity, this);
            this.internalKeys = new ObjectPool<>(capacity, () -> new AsciiStringBuilder(40));
        }

        public void put(final CharSequence internalKey, final long externalKey) {
            final AsciiStringBuilder internalKeyCopy = internalKeys.borrow();
            internalKeyCopy.clear().append(internalKey);

            internalMap.put(internalKeyCopy, externalKey);
            externalMap.put(externalKey, internalKeyCopy);
        }


        public AsciiStringBuilder getInternalKey(final long externalKey) {
            return externalMap.get(externalKey, null);
        }

        public long getExternalKey(final AsciiStringBuilder internalKey, final long missingExternalKey) {
            return internalMap.get(internalKey, missingExternalKey);
        }

        @Override
        public void consume(final long externalKey, final AsciiStringBuilder internalKeyCopy) {
            internalKeys.release(internalKeyCopy);
        }

        @Override
        public void consume(final AsciiStringBuilder key, final long value) {
        }

    }

    private static final class LongToObjectCache extends LongToObjectHashMap<AsciiStringBuilder> {

        private final CircularBufferOfInt insertOrder;
        private final Consumer evictionConsumer;

        public LongToObjectCache(final int capacity, final Consumer evictionConsumer) {
            super(capacity);

            this.insertOrder = new CircularBufferOfInt(keys.length);
            this.evictionConsumer = evictionConsumer;
        }

        @Override
        public boolean put(final long key, final AsciiStringBuilder value) {
            int hidx = hashIndex(key);
            int idx = find(hidx, key);

            if (idx != NULL) {
                evict(idx);
                values[idx] = value;
                return false;
            }

            if (freeHead == NULL) {
                final int removeIndex = insertOrder.tail();
                free(removeIndex);
            }

            idx = allocEntry(hidx);

            values[idx] = value;
            putKey(idx, key);

            insertOrder.add(idx);
            return (true);
        }

        @Override
        protected void free(final int idx) {
            evict(idx);
            super.free(idx);
        }

        protected void evict(final int idx) {
            evictionConsumer.consume(keys[idx], (AsciiStringBuilder) values[idx]);
        }

        protected interface Consumer {

            void consume(final long key, final AsciiStringBuilder value);

        }

    }

    protected static final class CharSeqToLongCache extends CharSeqToLongMap<AsciiStringBuilder> {

        private final int[] insertList;
        private int insertFirst = NULL;
        private int insertLast = NULL;

        private final Consumer evictionConsumer;

        public CharSeqToLongCache(final int capacity, final Consumer evictionConsumer) {
            // it's critical to use equals(ByteSequence) since we use negative bytes
            super(capacity, AbstractByteSequence::hashCode, AbstractByteSequence::equals);

            this.evictionConsumer = evictionConsumer;
            this.insertList = new int[2 * keys.length];

            Arrays.fill(insertList, NULL);
        }

        @Override
        public boolean put(final AsciiStringBuilder key, final long value) {
            int hidx = hashIndex(key);
            int idx = find(hidx, key);

            if (idx != NULL) {
                evict(idx);
                values[idx] = value;

                if (idx != insertLast) { // is not last inserted
                    final int prevIndex = insertList[2 * idx];     // curr.prev
                    final int nextIndex = insertList[2 * idx + 1]; // curr.next

                    if (prevIndex == NULL) {
                        insertFirst = nextIndex;                   // first = curr.next
                    } else {
                        insertList[2 * prevIndex + 1] = nextIndex; // prev.next = curr.next
                    }

                    insertList[2 * nextIndex] = prevIndex;         // next.prev = curr.prev
                    insertList[2 * insertLast + 1] = idx;          // last.next = curr

                    insertList[2 * idx] = insertLast;              // curr.prev = last
                    insertList[2 * idx + 1] = NULL;                // curr.next = null

                    insertLast = idx;                              // last = curr
                }

                return (false);
            }

            if (freeHead == NULL) {
                final int index = insertFirst;                   // curr
                final int nextIndex = insertList[2 * index + 1]; // next

                insertList[2 * nextIndex] = NULL; // next.prev = null
                insertFirst = nextIndex;          // first = next

                free(index);
            }

            idx = allocEntry(hidx);                     // curr

            if (insertLast == NULL) {
                insertFirst = idx;                      // first = curr
            } else {
                insertList[2 * insertLast + 1] = idx;   // last.next = curr
            }

            insertList[2 * idx] = insertLast;           // curr.prev = last
            insertList[2 * idx + 1] = NULL;             // curr.next = null

            insertLast = idx;                           // last = curr

            values[idx] = value;
            putKey(idx, key);

            return (true);
        }

        @Override
        protected void free(final int idx) {
            evict(idx);
            super.free(idx);
        }

        protected void evict(final int idx) {
            evictionConsumer.consume((AsciiStringBuilder) keys[idx], values[idx]);
        }

        protected interface Consumer {

            void consume(final AsciiStringBuilder key, final long value);

        }

    }

}
