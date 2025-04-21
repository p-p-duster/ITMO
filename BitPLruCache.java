import java.util.ArrayList;
import java.util.List;

public class BitPLruCache {
    private static final int CACHE_SETS = 8;
    private static final int CACHE_WAY = 4;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int CACHE_INDEX_LEN = 3;
    private static final int CACHE_OFFSET_LEN = 6;
    private final Memory memory;

    private static class CacheLine {
        boolean valid = false;
        boolean dirty = false;
        boolean mru = false;
        int tag;
        int[] data = new int[CACHE_LINE_SIZE];

        CacheLine() {
        }
    }

    private final List<List<CacheLine>> sets;

    public BitPLruCache(Memory memory) {
        this.memory = memory;
        this.sets = initializeCacheSets();
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

    public boolean accessPLRU(int tag, int index) {
        List<CacheLine> set = sets.get(index);

        for (int i = 0; i < CACHE_WAY; i++) {
            CacheLine line = set.get(i);
            if (line.valid && line.tag == tag) {
                line.mru = true;
                return true;
            }
        }

        handleCacheMiss(set, tag);
        return false;
    }

    public boolean writePLRU(int tag, int index, int data, int offset) {
        List<CacheLine> set = sets.get(index);

        for (int i = 0; i < CACHE_WAY; i++) {
            CacheLine line = set.get(i);
            if (line.valid && line.tag == tag) {
                line.dirty = true;
                line.mru = true;
                line.data[offset] = data;
                return true;
            }
        }

        handleWriteMiss(set, tag, offset, data);
        return false;
    }

    private void handleCacheMiss(List<CacheLine> set, int tag) {
        for (int i = 0; i < CACHE_WAY; i++) {
            CacheLine line = set.get(i);
            if (!line.valid || !line.mru) {
                line.valid = true;
                line.mru = true;
                line.tag = tag;
                resetBits(set, i);
                return;
            }
        }

        set.get(0).valid = true;
        set.get(0).mru = true;
        set.get(0).tag = tag;
        resetBits(set, 0);
    }

    private void handleWriteMiss(List<CacheLine> set, int tag, int offset, int data) {
        for (int i = 0; i < CACHE_WAY; i++) {
            CacheLine line = set.get(i);
            if (!line.valid || !line.mru) {
                if (line.dirty && line.valid) {
                    writeBackToMemory(line);
                }
                line.valid = true;
                line.dirty = true;
                line.mru = true;
                line.tag = tag;
                line.data[offset] = data;
                resetBits(set, i);
                return;
            }
        }

        if (set.get(0).valid && set.get(0).dirty) {
            writeBackToMemory(set.get(0));
        }

        set.get(0).valid = true;
        set.get(0).dirty = true;
        set.get(0).mru = true;
        set.get(0).tag = tag;
        set.get(0).data[offset] = data;
        resetBits(set, 0);
    }

    private void resetBits(List<CacheLine> set, int usedIndex) {
        for (int i = 0; i < CACHE_WAY; i++) {
            if (i != usedIndex) {
                set.get(i).mru = false;
            }
        }
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
