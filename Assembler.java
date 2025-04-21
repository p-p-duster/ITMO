import java.io.*;
import java.util.*;

public class Assembler {
    private final Map<String, Integer> registerMap;
    private final Map<String, Integer> instructions = new HashMap<>();
    private final List<Integer> commands = new ArrayList<>();

    public Assembler(Map<String, Integer> registerMap) {
        this.registerMap = registerMap;
    }

    public void writeBinaryFile(String outputFileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFileName);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            for (Integer cmd : commands) {
                byte[] bytes = toLittleEndian(cmd);
                bos.write(bytes);
            }
        }
    }

    public void parse(List<String> asmCode) {
        for (String line : asmCode) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            String[] parts = trimmedLine.split("\s+", 2);
            String command = parts[0];
            String args = parts.length > 1 ? parts[1] : "";

            String[] arguments = args.split("\s*,\s*");

            int machineCode = translateToMachineCode(command, String.join(",", arguments));
            commands.add(machineCode);
        }
    }

    public List<Integer> getCommands() {
        return commands;
    }

    private int translateToMachineCode(String command, String args) {
        String[] operands = args.split("[ ,()]+");
        operands = Arrays.stream(operands).filter(s -> !s.isEmpty()).toArray(String[]::new);

        switch (command) {
            // RV32M
            case "mul":
                return encodeRType(0, 0x1, operands);
            case "mulh":
                return encodeRType(1, 0x1, operands);
            case "mulhsu":
                return encodeRType(2, 0x1, operands);
            case "mulhu":
                return encodeRType(3, 0x1, operands);
            case "div":
                return encodeRType(4, 0x1, operands);
            case "divu":
                return encodeRType(5, 0x1, operands);
            case "rem":
                return encodeRType(6, 0x1, operands);
            case "remu":
                return encodeRType(7, 0x1, operands);

            // RV32I R-type
            case "add":
                return encodeRType(0, 0x0, operands);
            case "sub":
                return encodeRType(0, 0x20, operands);
            case "sll":
                return encodeRType(1, 0x0, operands);
            case "slt":
                return encodeRType(2, 0x0, operands);
            case "sltu":
                return encodeRType(3, 0x0, operands);
            case "xor":
                return encodeRType(4, 0x0, operands);
            case "srl":
                return encodeRType(5, 0x0, operands);
            case "sra":
                return encodeRType(5, 0x20, operands);
            case "or":
                return encodeRType(6, 0x0, operands);
            case "and":
                return encodeRType(7, 0x0, operands);

            // S-type
            case "sb":
                return encodeSType(0, operands);
            case "sh":
                return encodeSType(1, operands);
            case "sw":
                return encodeSType(2, operands);

            // B-type
            case "beq":
                return encodeBType(0, operands);
            case "bne":
                return encodeBType(1, operands);
            case "blt":
                return encodeBType(4, operands);
            case "bge":
                return encodeBType(5, operands);
            case "bltu":
                return encodeBType(6, operands);
            case "bgeu":
                return encodeBType(7, operands);

            // I-type
            case "addi":
                return encodeIType(0x13, 0, operands, 12);
            case "slti":
                return encodeIType(0x13, 2, operands, 12);
            case "sltiu":
                return encodeIType(0x13, 3, operands, 12);
            case "xori":
                return encodeIType(0x13, 4, operands, 12);
            case "ori":
                return encodeIType(0x13, 6, operands, 12);
            case "andi":
                return encodeIType(0x13, 7, operands, 12);
            case "slli":
                return encodeIType(0x13, 1, operands, 5);
            case "srli", "srai":
                return encodeIType(0x13, 5, operands, 5);

            // Load
            case "lb":
                return encodeLoad(0, operands);
            case "lh":
                return encodeLoad(1, operands);
            case "lw":
                return encodeLoad(2, operands);
            case "lbu":
                return encodeLoad(4, operands);
            case "lhu":
                return encodeLoad(5, operands);

            // J-type
            case "jal":
                return encodeJType(operands);

            // U-type
            case "lui":
                return encodeUType(0x37, operands);
            case "auipc":
                return encodeUType(0x17, operands);

            // I-type (JALR)
            case "jalr":
                return encodeIType(0x67, 0, operands, 12);

            // Environment
            case "ecall":
                return 0x00000073;
            case "ebreak":
                return 0x00100073;
            case "pause":
                return 0x100000F;
            case "fence.tso":
                return 0x8330000F;
            case "fence":
                return encodeFence(operands);

            default:
                throw new IllegalArgumentException("Unknown instruction: " + command);
        }
    }

    private int parseImmediate(String number) {
        int value;
        if (number.startsWith("-0x") | number.startsWith("-0X")) {
            value = Integer.parseInt(number.substring(3), 16) * -1;
        } else if (number.startsWith("0x") | number.startsWith("0X")) {
            value = Integer.parseInt(number.substring(2), 16);
        } else {
            value = Integer.parseInt(number);
        }
        return value;
    }

    private int encodeRType(int funct3, int funct7, String[] operands) {
        int rd = getRegister(operands[0]);
        int rs1 = getRegister(operands[1]);
        int rs2 = getRegister(operands[2]);
        return (funct7 << 25) | (rs2 << 20) | (rs1 << 15) | (funct3 << 12) | (rd << 7) | 51;
    }

    private int encodeIType(int opcode, int funct3, String[] operands, int immBits) {
        int rd = getRegister(operands[0]);
        int rs1 = getRegister(operands[1]);
        int imm = parseImmediate(operands[2]);
        imm = imm & ((1 << immBits) - 1);
        return (imm << 20) | (rs1 << 15) | (funct3 << 12) | (rd << 7) | opcode;
    }

    private int encodeLoad(int funct3, String[] operands) {
        int rd = getRegister(operands[0]);
        int offset = parseImmediate(operands[1]);
        int rs1 = getRegister(operands[2]);
        offset = offset & 0xFFF;
        return (offset << 20) | (rs1 << 15) | (funct3 << 12) | (rd << 7) | 3;
    }

    private int encodeSType(int funct3, String[] operands) {
        int rs2 = getRegister(operands[0]);
        int offset = parseImmediate(operands[1]);
        int rs1 = getRegister(operands[2]);
        int imm11_5 = (offset >> 5) & 0x7F;
        int imm4_0 = offset & 0x1F;
        return (imm11_5 << 25) | (rs2 << 20) | (rs1 << 15) | (funct3 << 12) | (imm4_0 << 7) | 35;
    }

    private int encodeBType(int funct3, String[] operands) {
        int rs1 = getRegister(operands[0]);
        int rs2 = getRegister(operands[1]);
        int offset = parseImmediate(operands[2]);
        int imm12 = (offset >> 12) & 0x1;
        int imm10_5 = (offset >> 5) & 0x3F;
        int imm4_1 = (offset >> 1) & 0xF;
        int imm11 = (offset >> 11) & 0x1;
        return (imm12 << 31) | (imm10_5 << 25) | (rs2 << 20) | (rs1 << 15) | (funct3 << 12) | (imm4_1 << 8) | (imm11 << 7) | 99;
    }

    private int encodeUType(int opcode, String[] operands) {
        int rd = getRegister(operands[0]);
        int imm = parseImmediate(operands[1]);
        imm = imm & 0xFFFFF;
        return (imm << 12) | (rd << 7) | opcode;
    }

    private int encodeJType(String[] operands) {
        int rd = getRegister(operands[0]);
        int offset = parseImmediate(operands[1]);
        int imm20 = (offset >> 20) & 0x1;
        int imm10_1 = (offset >> 1) & 0x3FF;
        int imm11 = (offset >> 11) & 0x1;
        int imm19_12 = (offset >> 12) & 0xFF;
        return (imm20 << 31) | (imm10_1 << 21) | (imm11 << 20) | (imm19_12 << 12) | (rd << 7) | 0x6F;
    }

    private int encodeFence(String[] operands) {
        String predS = operands[0];
        String succS = operands[1];
        long pred = 0L, succ = 0L;
        try {
            pred = Long.decode(predS);
        } catch (NumberFormatException e) {
            for (int i = 0; i < predS.length(); i++) {
                if ('r' == predS.charAt(i)) {
                    pred += 1;
                } else if ('w' == predS.charAt(i)) {
                    pred += 2;
                } else if ('i' == predS.charAt(i)) {
                    pred += 4;
                } else if ('o' == predS.charAt(i)) {
                    pred += 8;
                }
            }
        }
        try {
            succ = Long.decode(succS);
        } catch (NumberFormatException e) {
            for (int i = 0; i < succS.length(); i++) {
                if ('r' == succS.charAt(i)) {
                    succ += 1;
                } else if ('w' == succS.charAt(i)) {
                    succ += 2;
                } else if ('i' == succS.charAt(i)) {
                    succ += 4;
                } else if ('o' == succS.charAt(i)) {
                    succ += 8;
                }
            }
        }
        long imm = (succ << 4) | pred;
        return (int) (imm << 20 | 0b0001111);
    }

    private int getRegister(String name) {
        name = name.trim().toLowerCase();
        if (!registerMap.containsKey(name)) {
            throw new IllegalArgumentException("Unknown register: " + name);
        }
        return registerMap.get(name);
    }

    private byte[] toLittleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }
}
