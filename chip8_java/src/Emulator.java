package chip8_java.src;

import javax.swing.*;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Arrays;

// TODO: implement SUPER-CHIP scrolling
// TODO: implement flags to swap between chip-8, super-chip, xo-chip (See test 5)
// TODO: Check Sound code (its all gpt, it seems to work tho)

public class Emulator {    
    // Config constants
    private enum ChipType {
        CHIP8,
        SCHIP,
        XOCHIP
    }
    public static final int PIXEL_SCALE = 16; // How big each 'pixel' of the chip is on the user's actualy screen.
    public static final int INSTRUCTIONS_PER_SECOND = 720; // Number of CPU instructions per second. Default: 720
    public static final int FPS = 60; // Display and timer refresh rate. Default: 60
    
    
    // User interface
    private JFrame frame;
    private Display display;
    private InputKeyListener keyListener;
    private static final int[] KEYPAD = {
        KeyEvent.VK_X, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3,
        KeyEvent.VK_Q, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_A,
        KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_Z, KeyEvent.VK_C,
        KeyEvent.VK_4, KeyEvent.VK_R, KeyEvent.VK_F, KeyEvent.VK_V
    };
    private boolean drawReady;
    
    // Chip
    private ChipType chipType;
    private Chip chip; // The chip whos state this emulator manipulates
    
    // Sound player
    private String soundFilePath;

    // ignore these i just have immense skill issue so i don't know how to handle this lol
    private boolean[] oldDown;
    private boolean[] newDown;
    
    // Is the chip halted?
    private boolean halted;
    
    // Resolution
    private enum Resolution {
        HI,
        LO
    }
    // Default resolution
    public static final int DISPLAY_COLS_LO_RES = 64;
    public static final int DISPLAY_ROWS_LO_RES = 32;
    
    // SuperChip high-res mode
    public static final int DISPLAY_COLS_HI_RES = 128;
    public static final int DISPLAY_ROWS_HI_RES = 64;
    
    private Resolution resolutionMode;

    // Config variables
    private Config config;

    public Emulator(String chipMode) {
        // Setup
        this.soundFilePath = "chip8_java\\sounds\\pluck.wav";
        if (chipMode.equals("chip8")) {
            this.chipType = ChipType.CHIP8;
        } else if (chipMode.equals("schip")) {
            this.chipType = ChipType.SCHIP;
        } else {
            this.chipType = ChipType.XOCHIP;
        }

        this.config = new Config(chipMode);

        // Set up Display and Keylistener
        this.frame = new JFrame("Chip8 Java Emulator");
        this.display = new Display();
        this.keyListener = new InputKeyListener(this); // Create a keylistener for this emulator
        this.frame.addKeyListener(keyListener);
        this.drawReady = true;

        // Set display size
        this.frame.getContentPane().setPreferredSize(new Dimension(PIXEL_SCALE * (Chip.DISPLAY_COLS), PIXEL_SCALE * (Chip.DISPLAY_ROWS)));
        this.frame.pack();

        // Make close button terminate program
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize chip
        this.chip = new Chip();

        // Misc. keyboard input stuff that I don't know how to factor out because im bad
        this.oldDown = new boolean[KEYPAD.length];
        this.newDown = new boolean[KEYPAD.length];
        this.halted = false;

        // Init resolution to lores
        this.resolutionMode = Resolution.LO;
    }

    public void run() {
        // Timers and display refreshes at rate of FPS
        // Instructions occur at a rate of IPS
        while (true) {
            // Update timers
            this.chip.updateTimers();
            if (this.chip.soundTimer > 0) {
                // All the sound code is GPTed lol
                Thread audioThread = new Thread(new AudioPlayer(soundFilePath));
                audioThread.start();
            }

            // Execute multiple instructions per frame
            for (int instruction = 0; instruction < (INSTRUCTIONS_PER_SECOND / FPS); instruction++) {
                fetchInstruction();
                decodeExecuteInstruction();
            }
            
            // Redraw display
            if (this.drawReady) {
                draw();
                this.frame.repaint();
                this.drawReady = false;
            }

            // Keeps track of internal clock for steady FPS
            try {
                Thread.sleep(1000 / FPS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void loadROM(String filepath) {
        this.chip.pc = Chip.PROGRAM_START_ADDRESS;
        try {
            InputStream inputstream = new FileInputStream(filepath);
            int data;
            while ((data = inputstream.read()) != -1) {
                this.chip.memory[chip.pc] = (byte) data;
                this.chip.pc += 1;
            }
            inputstream.close();
            this.chip.pc = Chip.PROGRAM_START_ADDRESS;
        } catch (IOException e) {
            System.out.println("Failed to read the file " + filepath);
        }
        
    }
    
    public void handleUserInput(KeyEvent e, boolean down) {
        for (int i = 0; i < KEYPAD.length; i++) {
            if (KEYPAD[i] == e.getKeyCode()) {
                this.chip.inputs[i] = down;
                return;
            }
        }
    }
    
    public void draw() {
        boolean[][] pixels = this.chip.display;
        this.display.setDisplayData(pixels, PIXEL_SCALE);
        this.frame.add(this.display);
        this.frame.setVisible(true);
    }

    /**
     * Sets the chip's opcode to the next instruction
     */
    private void fetchInstruction() {
        this.chip.opcode = 0;
        this.chip.opcode = (((chip.memory[chip.pc] & 0xFF) << 8) | (chip.memory[chip.pc + 1]) & 0xFF) & 0xFFFF; // Truncate to 16-bits
        this.chip.pc += 2;
    }

    /**
     * Decodes and executes instructions based on the chip's current opcode
     */
    private void decodeExecuteInstruction() {
        // DECODE 

        // opcode is the next 2 bytes of memory
        // Even though we work with nibbles, Java doesnt have unsigned values so we use an int to avoid the problem
        // The opcode is split into categories below:

        int opcode = this.chip.opcode; // 16-bit opcode
        int category = (opcode & 0xF000) >> 12; // First nibble
        int X = (opcode & 0x0F00) >> 8; // Second nibble
        int Y = (opcode & 0x00F0) >> 4; // Third nibble
        int N = opcode & 0x000F; // Fourth nibble
        int NN = opcode & 0x00FF; // Third + Fourth nibble
        int NNN = opcode & 0x0FFF; // Second + Third + Fourth nibble

        // System.out.println(Integer.toHexString(opcode));

        // Each value can be used in the following instructions:
        // EXECUTE
        int oldX = 0;
        switch (category) {
            case 0x0:
                switch (NN) {
                    case 0xE0: 
                        // 00E0: Clear Screen
                        this.chip.display = new boolean[Chip.DISPLAY_ROWS][Chip.DISPLAY_COLS];
                        this.drawReady = true; // Chip needs to signify a draw refresh to update to empty display
                        break;
                    case 0xEE:
                        // 00EE: Returning from a subroutine. Pops last address from stack and set PC to it
                        this.chip.pc = this.chip.stack.pop() & 0xFFFF;
                        break;
                    case 0xFF:
                        // 00FF: Enable high resolution display
                        // TODO: actually update the chip's display to accomodate
                        this.resolutionMode = Resolution.HI;
                        break;
                    case 0xFE:
                        // 00FE: Enable low resolution display
                        // TODO: actually update chip's display
                        this.resolutionMode = Resolution.LO;
                        break;
                    case 0xFB:
                        // 00FB: Scroll right 4 pixels
                        scrollDisplayRight();
                        break;
                    case 0xFC:
                        // 00FC: Scroll left 4 pixels
                        scrollDisplayLeft();
                        break;
                }
                if (Y == 0xC) {
                    // 00CN: Scroll down N pixels (0-15)
                    scrollDisplayDown(N);
                }
                break;

            case 0x1: 
                // 1NNN: Jump to NNN
                this.chip.pc = NNN;
                break;
        
            case 0x2:
                // 2NNN: Calls subroutine at memory location NNN
                // Should jump PC to NNN, but push current PC to stack so subroutine can return later
                this.chip.stack.push(chip.pc);
                this.chip.pc = NNN;
                break;
                
            case 0x3:
                // 3XNN: Skip one 2-byte instruction if value in VX == NN. Do nothing otherwise.
                // i.e. executes next instruction iff the condition is false
                if (this.chip.registers[X] == NN) {
                    this.chip.pc += 2;
                }
                break;

            case 0x4:
                // 4XNN: Skip one 2-byte instruction if value in VX != NN. Do nothing otherwise.
                // i.e. executes next instruction iff the condition is false
                if (this.chip.registers[X] != NN) {
                    this.chip.pc += 2;
                }
                break;

            case 0x5:
                // 5XY0: Skip one 2-byte instruction if value VX == VY. Do nothing otherwise.
                if (this.chip.registers[X] == this.chip.registers[Y]) {
                    this.chip.pc += 2;
                }
                break;
        
            case 0x6: 
                // 6XNN: Set register VX to NN
                this.chip.registers[X] = NN;
                break;

            case 0x7:
                // 7XNN: Add value NN to register VX
                this.chip.registers[X] += NN;
                this.chip.registers[X] &= 0xFF; // Truncate to 8-bits because java bad
                break;

            // Arithmetic and logical operations
            case 0x8:
                switch(N) {
                    case 0: 
                        // 8XY0: Set Register VX to value of VY
                        this.chip.registers[X] = this.chip.registers[Y];
                        break;
                    case 1:
                        // 8XY1: Binary OR. Set register VX to (VX | VY). VY is not changed
                        this.chip.registers[X] |= this.chip.registers[Y];
                        break;
                    case 2:
                        // 8XY2: Binary AND. Set register VX to (VX & VY). VY is not changed
                        this.chip.registers[X] &= this.chip.registers[Y];
                        break;
                    case 3:
                        // 8XY3: Binary XOR. Set register VX to (VX ^ VY). VY is not changed.
                        this.chip.registers[X] ^= this.chip.registers[Y];
                        break;
                    case 4:
                        // 8XY4: Add. Set register VX to value in (VX + VY). VY is not changed.
                        // Unlike 7XNN, will affect carry flag if overflow
                        this.chip.registers[X] += this.chip.registers[Y];
                        if (this.chip.registers[X] > 0xFF) {
                            this.chip.registers[0xF] = 1;
                        } else {
                            this.chip.registers[0xF] = 0;
                        }
                        this.chip.registers[X] &= 0xFF; // Truncates to 8-bits because java bad
                        break;
                    case 5:
                        // 8XY5: Subtract. Set register VX to (VX - VY). VY is not changed.
                        // Sets carry flag VF to 1 if first operand >= second operand.
                        oldX = this.chip.registers[X];
                        this.chip.registers[X] -= this.chip.registers[Y];
                        this.chip.registers[X] &= 0xFF; // Truncates to 8-bits because java bad
                        if (oldX >= this.chip.registers[Y]) {
                            this.chip.registers[0xF] = 1;
                        } else {
                            this.chip.registers[0xF] = 0;
                        }
                        break;
                    case 6:
                        // 8XY6: Shift. Bit shift VX 1 bit to the right, set VF to the bit that was shifted out.
                        // If config says to do so, put VY into VX then shift. In either case, VY is not affected.
                        if (config.SHIFT_USE_VY) {
                            this.chip.registers[X] = this.chip.registers[Y];   
                        }
                        oldX = this.chip.registers[X];
                        this.chip.registers[X] >>>= 1; // unsigned bit shift

                        // Make sure to update carry bit last
                        this.chip.registers[0xF] = oldX & 1;
                        break;
                    case 7:
                        // 8XY7: Subtract. Set register VX to (VY - VX). VY is not changed.
                        // Sets carry flag VF to 1 if first operand >= second operand.
                        oldX = this.chip.registers[X];
                        this.chip.registers[X] = this.chip.registers[Y] - this.chip.registers[X];
                        this.chip.registers[X] &= 0xFF; // Truncates to 8-bits because java bad

                        // Make sure to update carry bit last
                        if (this.chip.registers[Y] >= oldX) {
                            this.chip.registers[0xF] = 1;
                        } else {
                            this.chip.registers[0xF] = 0;
                        }
                        break;
                    case 0xE:
                        // 8XYE: Shift. Bit shift VX 1 bit to the left, set VF to the bit that was shifted out.
                        // If config says to do so, put VY into VX then shift. In either case, VY is not affected.
                        if (config.SHIFT_USE_VY) {
                            this.chip.registers[X] = this.chip.registers[Y];   
                        }
                        oldX = this.chip.registers[X];
                        this.chip.registers[X] <<= 1;
                        this.chip.registers[X] &= 0xFF; // Make sure it's register is still 8-bits

                        // Make sure to update carry bit last
                        this.chip.registers[0xF] = (oldX >>> 7) & 1;
                        break;
                }
                break;

            case 0x9:
                // 9XY0: Skip one 2-byte instruction if value VX != VY. Do nothing otherwise.
                if (this.chip.registers[X] != this.chip.registers[Y]) {
                    this.chip.pc += 2;
                }
                break;
        
            case 0xA:
                // ANNN: Set index register I to NNN
                this.chip.I = NNN;
                break;

            case 0xB:
                // BNNN / BXNN: Jump with offset. 
                // If config jump_nnn=true, jumps to NNN + value in V0. Otherwise jump to XNN + value in VX.
                if (config.JUMP_NNN) {
                    this.chip.pc = NNN + this.chip.registers[0];
                } else {
                    this.chip.pc = NNN + this.chip.registers[X];
                }
                break;

            case 0xC:
                // CXNN: Generate random number, binary AND it with value NN, put result into VX.
                // Creates random int between 0 and 255 (max of 8-bit)
                int randNum = (int)(Math.random() * (0xFF + 1));
                this.chip.registers[X] = (randNum & NN) & 0xFF;
                break;
            
            case 0xD:
                if (N == 0 && this.resolutionMode == Resolution.HI) {
                    // DXY0: SCHIP 16 pixel draw.

                } else {
                    // DXYN: Display/Draw
                    /** Draws an N pixel tall sprite from memory location indicated by I register.
                     * Horizontal X coordinate in VX, vertical Y coordinate in VY register
                     * "On" pixels will flip the current pixels on the screen (read bits left to right, most to least sig bit).
                     * If any pixels were turned off after flipping, set VF to 1, otherwise set to 0.
                     */
                    int X_coord = this.chip.registers[X] % Chip.DISPLAY_COLS;
                    int Y_coord = this.chip.registers[Y] % Chip.DISPLAY_ROWS;
                    this.chip.registers[0xF] = 0; // Set VF to 0 for now

                    // Scan through each of the N rows of the sprite
                    for (int row = 0; row < N; row++) {
                        byte nth_byte = this.chip.memory[this.chip.I + row]; // Represents one row of the sprite
                        if (row + Y_coord >= Chip.DISPLAY_ROWS) break;

                        // Scan through every bit within the current row
                        for (int col = 0; col < 8; col++) {
                            if (col + X_coord >= Chip.DISPLAY_COLS) break;

                            // Find out whether or not we flip the current pixel
                            boolean flip = ((nth_byte & (0x80 >>> col)) != 0);
                            if (flip) {
                                if (this.chip.display[Y_coord + row][X_coord + col]) {
                                    // We are going to flip a pixel to off so we have to set VF to 1
                                    this.chip.registers[0xF] = 1;
                                }
                                // Flip the pixel
                                this.chip.display[Y_coord + row][X_coord + col] ^= true;
                            }
                        }
                    }
                    this.drawReady = true;
                }
                break;

            case 0xE:
                // Skip if key is pressed
                switch (N) {
                    case 0xE:
                        // EX9E: If key corresponding to value in VX is pressed, skip one instruction
                        if (this.chip.inputs[this.chip.registers[X]]) {
                            this.chip.pc += 2;
                        }
                        break;
                    case 0x1:
                        // EXA1: If key corresponding to value in VX is NOT pressed, skip one instruction
                        if (!this.chip.inputs[this.chip.registers[X]]) {
                            this.chip.pc += 2;
                        }
                        break;
                }
                break;

            case 0xF:
                switch (NN) {
                    case 0x07:
                        // FX07: Set VX to value of delay timer
                        this.chip.registers[X] = this.chip.delayTimer;
                        break;
                    case 0x15:
                        // FX15: Set delay timer to value in VX
                        this.chip.delayTimer = this.chip.registers[X];
                        break;
                    case 0x18:
                        // FX18: Set sound timer to value in VX
                        this.chip.soundTimer = this.chip.registers[X];
                        break;
                    case 0x1E:
                        // FX1E: Add value in VX to index register I
                        // Does not affect VF on overflow, but you can set VF if you really want like some interpretors
                        this.chip.I += this.chip.registers[X];
                        break;
                    case 0x0A:
                        // FX0A: Get Key. Stops further instruction execution until a key is pressed.
                        // The hex value of the key pressed will be put in VX once pressed and execution will resume
                        // Does not stop timers from decreasing

                        // Note: on the original COSMAC VIP a key was only registered when pressed then released.
                        //      
                        // Further note: This operation is halting. It should wait for a key to be pressed and not just
                        //               return a currently pressed key
                        
                        if (!this.halted) {
                            this.halted = true;
                            // Get list of all keys currently pressed
                            this.oldDown = Arrays.copyOf(this.chip.inputs, this.chip.inputs.length);

                            // Keeps track of all newly pressed keys
                            this.newDown = new boolean[KEYPAD.length];
                        }
                        boolean keyPressed = false;

                        // Scans through all current inputs for differences
                        for (int i = 0; i < this.chip.inputs.length; i++) {
                            if (this.chip.inputs[i]) {
                                // Key is pressed even when it wasn't pressed earlier.
                                // New keypress down: we can continue!
                                if (!this.oldDown[i]) {
                                    this.oldDown[i] = true; // Just so this doesnt run again
                                    this.newDown[i] = true;
                                }
                            } else {
                                // Key was already being pressed, now is unpressed
                                if (this.oldDown[i]) {
                                    this.oldDown[i] = false;
                                } else if (this.newDown[i]) {
                                    // Key was freshly pressed and now is released
                                    this.chip.registers[X] = i;
                                    keyPressed = true;
                                    break;
                                }
                            }
                        }
                        // Repeat this instruction until a keydown is detected
                        if (!keyPressed) {
                            this.chip.pc -= 2;
                        } else {
                            this.halted = false;
                        }
                        break;
                    case 0x29:
                        // FX29: Font character. Sets index register I to address of the hex character in VX
                        // note: 8-bit register can hold 2 hex numbers. These hex numbers represent the address that points to 1 character,
                        //       so we only have to actually look at the last nibble of VX
                        this.chip.I = Chip.FONT_START_ADDRESS + (this.chip.registers[X] & 0xF) * Chip.CHARACTER_FONT_HEIGHT;
                        break;
                    case 0x33:
                        // FX33: Binary-coded decimal conversion. Take number in VX and convert it to 3 decimal digits.
                        // VX stores 1 byte (000-255). Store the digits in memory starting at index register I (I -> I + 2)
                        this.chip.memory[chip.I + 2] = (byte)(this.chip.registers[X] % 10);
                        this.chip.memory[chip.I + 1] = (byte)((this.chip.registers[X] / 10) % 10);
                        this.chip.memory[chip.I] = (byte)((this.chip.registers[X] / 100) % 10);
                        break;
                    case 0x55:
                        // FX55: Store registers to memory. Stores registers from V0 -> VX (inclusive) in memory
                        // starting at index register I. 
                        
                        // Increment I to end up at value I + X + 1 iff config says to do so.
                        // If increment, can run older games. If not, can run newer ones.
                        for (int i = 0; i <= X; i++) {
                            this.chip.memory[chip.I + i] = (byte)(this.chip.registers[i]);
                        }
                        if (config.STORE_MEMORY_INCREMENTS_INDEX) {
                            this.chip.I += (X + 1);
                        }
                        break;
                    case 0x65:
                        // FX65: Loads registers from memory.
                        for (int i = 0; i <= X; i++) {
                            this.chip.registers[i] = Byte.toUnsignedInt(this.chip.memory[this.chip.I + i]); // Java bad so we have to ensure unsigned conversion
                        }
                        if (config.STORE_MEMORY_INCREMENTS_INDEX) {
                            this.chip.I += (X + 1);
                        }
                        break;
                }
                break;

            default:
                System.out.println("Unknown opcode: " + opcode);
        }
    }

    /*
     * Scroll the display 4 pixels to the left, leaving 'off' pixels where new pixels were added
     */
    private void scrollDisplayLeft() {
        int displayWidth = getDisplayWidth();
        int displayHeight = getDisplayHeight();
        boolean[][] oldDisplay = this.chip.display;
        this.chip.display = new boolean[displayHeight][displayWidth];

        for (int i = 0; i < displayHeight; i++) {
            for (int j = 0; j < displayWidth - 4; j++) {
                this.chip.display[i][j + 4] = oldDisplay[i][j];
            }
        }
    }
    
    /*
     * Scroll the display 4 pixels to the right, leaving 'off' pixels where new pixels were added
     */
    private void scrollDisplayRight() {
        int displayWidth = getDisplayWidth();
        int displayHeight = getDisplayHeight();
        boolean[][] oldDisplay = this.chip.display;
        this.chip.display = new boolean[displayHeight][displayWidth];

        for (int i = 0; i < displayHeight; i++) {
            for (int j = 4; j < displayWidth; j++) {
                this.chip.display[i][j - 4] = oldDisplay[i][j];
            }
        }
    }
    
    /*
     * Scroll the display n pixels down, leaving 'off' pixels where new pixels were added
     */
    private void scrollDisplayDown(int n) {
        int displayWidth = getDisplayWidth();
        int displayHeight = getDisplayHeight();
        boolean[][] oldDisplay = this.chip.display;
        this.chip.display = new boolean[displayHeight][displayWidth];

        for (int i = 0; i < displayHeight - n; i++) {
            for (int j = 0; j < displayWidth; j++) {
                this.chip.display[i][j + n] = oldDisplay[i][j];
            }
        }
    }
    
    /*
     * Gets the display's current width in pixels
     */
    private int getDisplayWidth() {
        return this.resolutionMode == Resolution.LO ? DISPLAY_COLS_LO_RES : DISPLAY_COLS_HI_RES;
    }
    
    /*
     * Gets the display's current height in pixels
     */
    private int getDisplayHeight() {
        return this.resolutionMode == Resolution.LO ? DISPLAY_ROWS_LO_RES: DISPLAY_COLS_HI_RES;
    }
}