import java.util.List;

public class Memory {
    private final byte[] memory;
    private final int size;

    public Memory(int size) {
        memory = new byte[size];
        this.size = size;
    }

    public byte read(int address) {
        address = address % size;
        return memory[address];
    }

    public void write(int address, byte value) {
        address = address % size;
        memory[address] = value;
    }

    public void loadPrograms(int startAddress, List<Integer> commands) {
        int currentAddress = startAddress;
        for (int cmd : commands) {
            memory[currentAddress] = (byte) (cmd & 0xFF);
            memory[currentAddress + 1] = (byte) ((cmd >> 8) & 0xFF);
            memory[currentAddress + 2] = (byte) ((cmd >> 16) & 0xFF);
            memory[currentAddress + 3] = (byte) ((cmd >> 24) & 0xFF);
            currentAddress += 4;
        }
    }
}
