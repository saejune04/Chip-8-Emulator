package chip8_java.src;

import java.util.Stack;
import java.lang.Math;

public class Chip {
    
    // Default resolution
    public static final int DISPLAY_COLS = 64;
    public static final int DISPLAY_ROWS = 32;
    
    // SuperChip high-res mode
    public static final int DISPLAY_COLS_HI_RES = 128;
    public static final int DISPLAY_ROWS_HI_RES = 64;
    
    // Memory
    public static final int MEMORY_SIZE = 4096; // 4KB of memory
    public static final int PROGRAM_START_ADDRESS = 0x200; // Load programs into memory starting at 0x200

    // Font data
    public static final int FONT_START_ADDRESS = 0x50; // Fonts start at 0x50 and end at 0x9F 
    public static final int FONT_SIZE = 80; 
    public static final int CHARACTER_FONT_HEIGHT = 5;

    // States
    public static final int NUM_INPUTS = 16;
    public static final int NUM_REGISTERS = 16;

    // Each character is 4 pixels wide and 5 pixels tall
    public static final int[] FONT_SET = {
        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
        0x20, 0x60, 0x20, 0x20, 0x70, // 1
        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
        0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    public byte[] memory;
    public boolean[][] display;
    public int pc; // 16-bit program counter
    public int I; // 16-bit address
    public int stack_pointer; // 16-bit stack pointer

    public Stack<Integer> stack; 
    public int delayTimer; // 8-bit timer
    public int soundTimer; // 8-bit timer
    public int[] registers; // 16 8-bit registers, VF is used as a flag register: set to 1 or 0 based on rules (e.g. carry flag)
    public boolean[] inputs; // 16 possible inputs
    public int opcode;

    public Chip() {
        this.memory = new byte[MEMORY_SIZE];
        this.stack = new Stack<>();
        this.registers = new int[NUM_REGISTERS];
        this.display = new boolean[DISPLAY_ROWS][DISPLAY_COLS];
        this.inputs = new boolean[NUM_INPUTS];
        this.pc = PROGRAM_START_ADDRESS;
    }

    public void init() {
        for (int i = 0; i < FONT_SIZE; i++) {
            this.memory[i + FONT_START_ADDRESS] = (byte) FONT_SET[i];
        }
    }

    public void updateTimers() {
        this.delayTimer = Math.max(0, this.delayTimer - 1);
        this.soundTimer = Math.max(0, this.soundTimer - 1);
    }

}

