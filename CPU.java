import java.util.List;
import java.util.Map;

public class CPU {
    final int MEM_SIZE = 1 << 18;
    static final int CACHE_INDEX_LEN = 3;
    static final int CACHE_OFFSET_LEN = 6;
    private final Memory memory;
    private final int[] registers = new int[32];
    private final Map<String, Integer> registerMap;
    private int PC;
    private boolean ra = false;

    public static long lruInstrHits = 0;
    public static long plruInstrHits = 0;
    public static long lruDataHits = 0;
    public static long plruDataHits = 0;
    public static long totalInstrHits = 0;
    public static long totalDataHits = 0;

    static class Address {
        int tag;
        int index;
        int offset;

        public Address(int address) {
            tag = address >>> (CACHE_OFFSET_LEN + CACHE_INDEX_LEN);
            index = (address >>> CACHE_OFFSET_LEN) & ((1 << CACHE_INDEX_LEN) - 1);
            offset = address & ((1 << CACHE_OFFSET_LEN) - 1);
        }
    }

    public CPU(Memory memory, Map<String, Integer> registerMap) {
        this.memory = memory;
        this.registerMap = registerMap;
    }

    public void executeProgram(List<Integer> commands) {
        int startAddress = 0x10000;
        memory.loadPrograms(startAddress, commands);
        registers[registerMap.get("ra")] = startAddress + 4 * commands.size();
        LruCache lruCache = new LruCache(memory);
        BitPLruCache plruCache = new BitPLruCache(memory);
        PC = startAddress;

        while (PC >= startAddress && PC < startAddress + 4 * commands.size()) {
            int instruction = fetchInstruction(lruCache, plruCache);
            executeInstruction(instruction, lruCache, plruCache);
            if (ra) {
                break;
            }
            PC += 4;
        }
        printCacheStatistics();
    }

    public int fetchInstruction(LruCache lruCache, BitPLruCache plruCache) {
        Address address = new Address(PC);

        if (lruCache.accessLRU(address.tag, address.index)) {
            lruInstrHits++;
        }
        if (plruCache.accessPLRU(address.tag, address.index)) {
            plruInstrHits++;
        }
        totalInstrHits++;

        if (PC + 3 >= MEM_SIZE) {
            System.err.println("Instruction fetch out of bounds at address: " + PC);
            return 0;
        }

        return (memory.read(PC) & 0xFF) | ((memory.read(PC + 1) & 0xFF) << 8)
                | ((memory.read(PC + 2) & 0xFF) << 16) | ((memory.read(PC + 3) & 0xFF) << 24);
    }

    public void executeInstruction(int instruction, LruCache lruCache, BitPLruCache plruCache) {
        int funct7 = (instruction >> 25) & 0x7F;
        int rs2 = (instruction >> 20) & 0x1F;
        int rs1 = (instruction >> 15) & 0x1F;
        int funct3 = (instruction >> 12) & 0x7;
        int rd = (instruction >> 7) & 0x1F;
        int opcode = instruction & 0x7F;
        int imm;

        if (opcode == 0b0001111 && funct3 == 0b000) { // pause, fence, fence.tso
            return;
        }

        if (opcode == 0b1110011 && funct3 == 0b000) { // ecall, ebreak
            return;
        }

        switch (opcode) {
            case 0x33: // add, sub, xor, or, and, sll, srl, sra, slt, sltu, mul, mulh, mulhsu, mulhu, div, divu, rem, remu
                executeRType(funct3, funct7, rs1, rs2, rd);
                break;
            case 0x13: // addi, xori, ori, andi, slli, srli, srai, slti, sltiu
                imm = decodeImmI(instruction);
                executeIType(funct3, funct7, rs1, imm, rd);
                break;
            case 0x3: // lb, lh, lw, lbu, lhu
                imm = decodeImmI(instruction);
                executeLoad(funct3, rs1, imm, rd, lruCache, plruCache);
                break;
            case 0x23: // sb, sh, sw
                imm = decodeImmS(instruction);
                executeSType(funct3, rs1, rs2, imm, lruCache, plruCache);
                break;
            case 0x63: // beq, bne, blt, bge, bltu, bgeu
                imm = decodeImmB(instruction);
                executeBType(funct3, rs1, rs2, imm);
                break;
            case 0x6F: // jal
                imm = decodeImmJ(instruction);
                executeJal(imm, rd);
                break;
            case 0x67: // jalr
                imm = decodeImmI(instruction);
                executeJalr(rs1, imm, rd);
                break;
            case 0x37: // lui
                imm = decodeImmU(instruction);
                executeLui(imm, rd);
                break;
            case 0x17: // auipc
                imm = decodeImmU(instruction);
                executeAuipc(imm, rd);
                break;
            default:
                System.err.println("Incorrect opcode: " + opcode);
        }
    }

    private int decodeImmI(int instruction) {
        return instruction >> 20;
    }

    private int decodeImmS(int instruction) {
        int imm11_5 = (instruction >> 25) & 0x7F;
        int imm4_0 = (instruction >> 7) & 0x1F;
        return (imm11_5 << 5) | imm4_0;
    }

    private int decodeImmB(int instruction) {
        int imm = ((instruction >> 31) & 0x1) << 12;
        imm |= ((instruction >> 7) & 0x1) << 11;
        imm |= ((instruction >> 25) & 0x3F) << 5;
        imm |= ((instruction >> 8) & 0xF) << 1;
        return (imm << 19) >> 19;
    }

    private int decodeImmJ(int instruction) {
        int imm = ((instruction >> 31) & 0x1) << 20;
        imm |= (((instruction >> 12) & 0xFF) << 12);
        imm |= (((instruction >> 20) & 0x1) << 11);
        imm |= (((instruction >> 21) & 0x3FF) << 1);
        imm = imm << 12;
        return imm >> 12;
    }

    private int decodeImmU(int instruction) {
        return instruction >> 12;
    }

    private void executeRType(int funct3, int funct7, int rs1, int rs2, int rd) {
        if (rd == 0) {
            return;
        }
        if (funct7 == 0x00) {
            switch (funct3) {
                case 0:
                    // add
                    registers[rd] = registers[rs1] + registers[rs2];
                    break;
                case 1:
                    // sll
                    registers[rd] = registers[rs1] << registers[rs2];
                    break;
                case 2:
                    // slt
                    registers[rd] = (registers[rs1] < registers[rs2]) ? 1 : 0;
                    break;
                case 3:
                    // sltu
                    registers[rd] = ((int) (registers[rs1] & 0xFFFFFFFFL) < (int) (registers[rs2] & 0xFFFFFFFFL)) ? 1 : 0;
                    break;
                case 4:
                    // xor
                    registers[rd] = registers[rs1] ^ registers[rs2];
                    break;
                case 5:
                    // srl
                    registers[rd] = (int) (registers[rs1] & 0xFFFFFFFFL) >> (registers[rs2] & 0x1F);
                    break;
                case 6:
                    // or
                    registers[rd] = registers[rs1] | registers[rs2];
                    break;
                case 7:
                    // and
                    registers[rd] = registers[rs1] & registers[rs2];
                    break;
                default:
                    System.err.println("Incorrect funct3: " + funct3);
            }
        } else if (funct7 == 0x20) {
            if (funct3 == 0) {
                // sub
                registers[rd] = registers[rs1] - registers[rs2];
            } else if (funct3 == 5) {
                // sra
                registers[rd] = registers[rs1] >> (registers[rs2] & 0x1F);
            } else {
                System.err.println("Incorrect funct3: " + funct3);
            }
        } else if (funct7 == 0x01) {
            switch (funct3) {
                case 0:
                    // mul
                    registers[rd] = registers[rs1] * registers[rs2];
                    break;
                case 1:
                    // mulh
                    registers[rd] = (int) (((long) registers[rs1] * (long) registers[rs2]) >> 32);
                    break;
                case 2:
                    // mulhsu
                    registers[rd] = (int) ((long) registers[rs1] * (Integer.toUnsignedLong(registers[rs2])) >> 32);
                    break;
                case 3:
                    // mulhu
                    registers[rd] = (int) ((registers[rs1] & 0xFFFFFFFFL) * (registers[rs2] & 0xFFFFFFFFL) >> 32);
                    break;
                case 4:
                    // div
                    registers[rd] = registers[rs1] / registers[rs2];
                    break;
                case 5:
                    // divu
                    registers[rd] = Integer.divideUnsigned(registers[rs1], registers[rs2]);
                    break;
                case 6:
                    // rem
                    registers[rd] = Math.floorMod(registers[rs1], registers[rs2]);
                    break;
                case 7:
                    // remu
                    registers[rd] = Integer.remainderUnsigned(registers[rs1], registers[rs2]);
                    break;
                default:
                    System.err.println("Incorrect funct3: " + funct3);
                    break;
            }
        } else {
            System.err.println("Incorrect funct7: " + funct7);
        }
    }

    private void executeIType(int funct3, int funct7, int rs1, int imm, int rd) {
        if (rd == 0) {
            return;
        }
        switch (funct3) {
            case 0:
                // addi
                registers[rd] = registers[rs1] + imm;
                break;
            case 1:
                // slli
                registers[rd] = registers[rs1] << (imm & 0x1F);
                break;
            case 2:
                // slti
                registers[rd] = (registers[rs1] < imm) ? 1 : 0;
                break;
            case 3:
                // sltiu
                registers[rd] = ((int) (registers[rs1] & 0xFFFFFFFFL) < (int) (imm & 0xFFFFFFFFL)) ? 1 : 0;
                break;
            case 4:
                // xori
                registers[rd] = registers[rs1] ^ imm;
                break;
            case 5:
                if (funct7 == 0) {
                    // srli
                    registers[rd] = (int) (registers[rs1] & 0xFFFFFFFFL) >> (imm & 0x1F);
                } else {
                    // srai
                    registers[rd] = registers[rs1] >> (imm & 0x1F);
                }
                break;
            case 6:
                // ori
                registers[rd] = registers[rs1] | imm;
                break;
            case 7:
                // andi
                registers[rd] = registers[rs1] & imm;
                break;
            default:
                System.err.println("Incorrect funct3: " + funct3);
                break;
        }
    }

    private void executeLoad(int funct3, int rs1, int imm, int rd, LruCache lruCache, BitPLruCache plruCache) {
        int addr = Math.floorMod(registers[rs1] + imm, MEM_SIZE);
        if (addr < 0 || addr + 3 >= MEM_SIZE) {
            System.err.println("Out-of-memory address: " + addr);
            return;
        }
        Address address = new Address(addr);

        if (lruCache.accessLRU(address.tag, address.index)) {
            lruDataHits++;
        }
        if (plruCache.accessPLRU(address.tag, address.index)) {
            plruDataHits++;
        }
        totalDataHits++;

        switch (funct3) {
            case 0:
                // lb
                int byteValue = memory.read(addr) & 0xFF;
                registers[rd] = (byteValue << 24) >> 24;
                break;
            case 1:
                // lh
                int halfWord = ((memory.read(addr + 1) & 0xFF) << 8) |
                        (memory.read(addr) & 0xFF);
                registers[rd] = (halfWord << 16) >> 16;
                break;
            case 2:
                // lw
                int word = ((memory.read(addr + 3) & 0xFF) << 24) |
                        ((memory.read(addr + 2) & 0xFF) << 16) |
                        ((memory.read(addr + 1) & 0xFF) << 8) |
                        (memory.read(addr) & 0xFF);
                registers[rd] = word;
                break;
            case 4:
                // lbu
                registers[rd] = memory.read(addr) & 0xFF;
                break;
            case 5:
                // lhu
                registers[rd] = (memory.read(addr) & 0xFF) | ((memory.read(addr + 1) & 0xFF) << 8);
                break;
            default:
                System.err.println("Incorrect funct3: " + funct3);
        }
    }

    private void executeBType(int funct3, int rs1, int rs2, int imm) {
        boolean condition = false;
        switch (funct3) {
            case 0:
                // beq
                condition = (registers[rs1] == registers[rs2]);
                break;
            case 1:
                // bne
                condition = (registers[rs1] != registers[rs2]);
                break;
            case 4:
                // blt
                condition = (registers[rs1] < registers[rs2]);
                break;
            case 5:
                // bge
                condition = (registers[rs1] >= registers[rs2]);
                break;
            case 6:
                // bltu
                condition = (registers[rs1] & 0xFFFFFFFFL) < (registers[rs2] & 0xFFFFFFFFL);
                break;
            case 7:
                // bgeu
                condition = (registers[rs1] & 0xFFFFFFFFL) >= (registers[rs2] & 0xFFFFFFFFL);
                break;
            default:
                System.err.println("Incorrect funct3: " + funct3);
        }

        if (condition) {
            PC += imm - 4;
        }
    }

    private void executeSType(int funct3, int rs1, int rs2, int imm, LruCache lruCache, BitPLruCache plruCache) {
        int addr = Math.floorMod(registers[rs1] + imm, MEM_SIZE);
        if (addr < 0 || addr + 3 >= MEM_SIZE) {
            System.err.println("Out-of-memory address: " + addr);
            return;
        }
        Address address = new Address(addr);
        int data = 0;

        switch (funct3) {
            case 0:
                // sb
                data = registers[rs2] & 0xFF;
                break;
            case 1:
                // sh
                data = registers[rs2] & 0xFFFF;
                break;
            case 2:
                // sw
                data = registers[rs2];
                break;
            default:
                System.err.println("Incorrect funct3: " + funct3);
        }

        if (lruCache.writeLRU(address.tag, address.index, address.offset, data)) {
            lruDataHits++;
        }
        if (plruCache.writePLRU(address.tag, address.index, data, address.offset)) {
            plruDataHits++;
        }
        totalDataHits++;
    }

    private void executeLui(int imm, int rd) {
        if (rd != 0) {
            registers[rd] = imm;
        }
    }

    private void executeAuipc(int imm, int rd) {
        if (rd != 0) {
            registers[rd] = PC + imm;
        }
    }

    private void executeJal(int imm, int rd) {
        if (rd != 0) {
            registers[rd] = PC + 4;
        }
        PC += imm - 4;
        if (registers[rd] == registers[registerMap.get("ra")]) {
            ra = true;
        }
    }

    private void executeJalr(int rs1, int imm, int rd) {
        if (rd != 0) {
            registers[rd] = PC + 4;
        }
        PC = (registers[rs1] + imm) & ~1;
        if (registers[rd] == registers[registerMap.get("ra")] || registers[rs1] == registers[registerMap.get("ra")]) {
            ra = true;
        }
    }


    private static void printCacheStatistics() {
        long totalAccesses = totalInstrHits + totalDataHits;
        long lruTotalHits = lruInstrHits + lruDataHits;
        long plruTotalHits = plruInstrHits + plruDataHits;

        System.out.printf("replacement\thit rate\thit rate (inst)\thit rate (data)%n");
        if (totalDataHits == 0) {
            System.out.printf("        LRU\t%3.5f%%\t%3.5f%%\tnan%%%n",
                    100.0 * lruTotalHits / totalAccesses,
                    100.0 * lruInstrHits / totalInstrHits);
            System.out.printf("       pLRU\t%3.5f%%\t%3.5f%%\tnan%%%n",
                    100.0 * plruTotalHits / totalAccesses,
                    100.0 * plruInstrHits / totalInstrHits);
        } else {
            System.out.printf("        LRU\t%3.5f%%\t%3.5f%%\t%3.5f%%%n",
                    100.0 * lruTotalHits / totalAccesses,
                    100.0 * lruInstrHits / totalInstrHits,
                    100.0 * lruDataHits / totalDataHits);
            System.out.printf("       pLRU\t%3.5f%%\t%3.5f%%\t%3.5f%%%n",
                    100.0 * plruTotalHits / totalAccesses,
                    100.0 * plruInstrHits / totalInstrHits,
                    100.0 * plruDataHits / totalDataHits);
        }
    }
}
