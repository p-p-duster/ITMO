import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LruCache {
    private static final int CACHE_SETS = 8;
    private static final int CACHE_WAY = 4;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int CACHE_INDEX_LEN = 3;
    private static final int CACHE_OFFSET_LEN = 6;
    private final Memory memory;
    private final List<List<CacheLine>> sets;
    private final List<LinkedList<Integer>> accessOrder;

    private static class CacheLine {
        boolean valid = false;
        boolean dirty = false;
        int tag;
        int[] data = new int[CACHE_LINE_SIZE];
    }

    public LruCache(Memory memory) {
        this.memory = memory;
        this.sets = initializeCacheSets();
        this.accessOrder = initializeAccessOrder();
    }

    private List<List<CacheLine>> initializeCacheSets() {
        List<List<CacheLine>> cache = new ArrayList<>(CACHE_SETS);
        for (int i = 0; i < CACHE_SETS; i++) {
            List<CacheLine> set = new ArrayList<>(CACHE_WAY);
            for (int j = 0; j < CACHE_WAY; j++) {
                set.add(new CacheLine());
            }
            cache.add(set);
        }
        return cache;
    }

    private List<LinkedList<Integer>> initializeAccessOrder() {
        List<LinkedList<Integer>> order = new ArrayList<>(CACHE_SETS);
        for (int i = 0; i < CACHE_SETS; i++) {
            LinkedList<Integer> queue = new LinkedList<>();
            for (int j = 0; j < CACHE_WAY; j++) {
                queue.add(j);
            }
            order.add(queue);
        }
        return order;
    }

    public boolean accessLRU(int tag, int index) {
        List<CacheLine> set = sets.get(index);
        LinkedList<Integer> order = accessOrder.get(index);

        for (int i = 0; i < CACHE_WAY; i++) {
            CacheLine line = set.get(i);
            if (line.valid && line.tag == tag) {
                updateAccessOrder(order, i);
                return true;
            }
        }

        return handleCacheMiss(set, order, tag);
    }

    public boolean writeLRU(int tag, int index, int offset, int data) {
        List<CacheLine> set = sets.get(index);
        LinkedList<Integer> order = accessOrder.get(index);

        for (int i = 0; i < CACHE_WAY; i++) {
            CacheLine line = set.get(i);
            if (line.valid && line.tag == tag) {
                line.data[offset] = data;
                line.dirty = true;
                updateAccessOrder(order, i);
                return true;
            }
        }

        return handleWriteMiss(set, order, tag, offset, data);
    }


    private boolean handleCacheMiss(List<CacheLine> set, LinkedList<Integer> order, int tag) {
        int displacedWay = order.removeLast();
        CacheLine displacedLine = set.get(displacedWay);

        displacedLine.tag = tag;
        displacedLine.dirty = false;
        displacedLine.valid = true;

        order.addFirst(displacedWay);
        return false;
    }

    private boolean handleWriteMiss(List<CacheLine> set, LinkedList<Integer> order,
                                    int tag, int offset, int data) {
        int displacedWay = order.removeLast();
        CacheLine line = set.get(displacedWay);

        if (line.valid && line.dirty) {
            writeBackToMemory(line);
        }

        line.tag = tag;
        line.valid = true;
        line.dirty = true;
        line.data[offset] = data;

        order.addFirst(displacedWay);
        return false;
    }

    private void updateAccessOrder(LinkedList<Integer> order, int way) {
        order.remove(Integer.valueOf(way));
        order.addFirst(way);
    }

    private void writeBackToMemory(CacheLine line) {
        int address = line.tag << (CACHE_INDEX_LEN + CACHE_OFFSET_LEN);
        for (int i = 0; i < CACHE_LINE_SIZE; i++) {
            memory.write(address + 4 * i, (byte) (line.data[i] & 0xFF));
            memory.write(address + 4 * i + 1, (byte) ((line.data[i] >> 8) & 0xFF));
            memory.write(address + 4 * i + 2, (byte) ((line.data[i] >> 16) & 0xFF));
            memory.write(address + 4 * i + 3, (byte) ((line.data[i] >> 24) & 0xFF));
        }
    }
}
