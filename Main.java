import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

public class Main {
    static final int MEM_SIZE = 1 << 18; // ADDR_LEN = log2(MEM_SIZE)
    final int ADDR_LEN = 18; //по условию
    final int CACHE_WAY = 32 / 8; // CACHE_WAY = CACHE_LINE_COUNT / CACHE_SETS; (ассоциативность - кол-во блоков)
    final int CACHE_TAG_LEN = 18 - 3 - 6; // ADDR_LEN = CACHE_INDEX_LEN + CACHE_OFFSET_LEN + CACHE_TAG_LEN;
    final int CACHE_INDEX_LEN = 3; //по условию (длина индекса - для поиска нужной кэш-линии в блоке)
    final int CACHE_OFFSET_LEN = 6; // CACHE_OFFSET_SIZE = log2(CACHE_LINE_SIZE); (смещение - для поиска байта внутри кэш-линии)
    final int CACHE_SIZE = 64 * 32 * 8; // CACHE_SIZE = CACHE_LINE_SIZE * CACHE_LINE_COUNT
    final int CACHE_LINE_SIZE = 64 * 8; //по условию
    final int CACHE_LINE_COUNT = 32; //по условию (всего кэш-линий)
    final int CACHE_SETS = 8; // CACHE_SETS = 2^CACHE_INDEX_LEN (кэш-линий в одном блоке)
    private static final Map<String, Integer> registerMap = new HashMap<>();
    private static final Map<String, Integer> instructionsMap = new HashMap<>();

    static {
        // Инициализация регистров
        String[] abiNames = {
                "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
                "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
                "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
                "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
        };
        for (int i = 0; i < abiNames.length; i++) {
            registerMap.put(abiNames[i], i);
        }

        String[] instructions = {
                "addi", "add", "slli", "srli", "srai", "sub", "sll", "slt",
                "sltu", "xor", "srl", "sra", "or", "and", "mul", "mulh",
                "mulhsu", "mulhu", "div", "divu", "rem", "remu", "jalr", "andi",
                "ori", "xori", "slti", "sltiu", "lb", "lh", "lw", "lbu", "lhu",
                "beq", "bne", "bge", "blt", "bltu", "bgeu", "jal", "auipc", "lui",
                "sb", "sh", "sw", "fence", "fence.tso", "pause", "ecall", "ebreak"
        };

        for (String instruction : instructions) {
            instructionsMap.put(instruction, 0);
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("No expected arguments. Usage: --asm <input_file> --bin <output_file>");
            System.exit(1);
        }

        String inputFileName = null;
        String outputFileName = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--asm")) {
                inputFileName = args[++i];
            } else if (args[i].equals("--bin")) {
                outputFileName = args[++i];
            }
        }

        if (inputFileName == null) {
            System.err.println("Expected input file name. Usage: --asm <input_file> --bin <output_file>");
            System.exit(1);
        }

        Assembler parser = new Assembler(registerMap);
        List<String> instructions = readFile(inputFileName);
        parser.parse(instructions);
        List<Integer> commands = parser.getCommands();

        Memory RAM = new Memory(MEM_SIZE);
        CPU processor = new CPU(RAM, registerMap);
        processor.executeProgram(commands);

        if (outputFileName == null) {
            System.err.println("Compiling asm code is not supported");
            System.exit(0);
        } else {
            parser.writeBinaryFile(outputFileName);
        }
    }


    public static List<String> readFile(String fileName) throws IOException {
        MyScanner scanner = new MyScanner(fileName, StandardCharsets.UTF_8);
        IntPredicate symbolIsLegal = (int c) -> Character.isLetter(c) ||
                Character.isDigit(c) || c == '-' || c == ',' || c == '.';

        List<String> lines = new ArrayList<>();
        StringBuilder command = new StringBuilder();
        command.append(scanner.nextToken(symbolIsLegal));

        while (scanner.hasNext()) {
            String ch = scanner.nextToken(symbolIsLegal);

            if (instructionsMap.containsKey(ch)) {
                lines.add(command.toString());
                command = new StringBuilder(ch);
            } else {
                command.append(" ").append(ch);
            }
        }

        lines.add(command.toString());
        scanner.close();
        return lines;
    }
}
