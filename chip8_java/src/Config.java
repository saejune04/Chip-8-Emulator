package chip8_java.src;

public class Config {
    public boolean SHIFT_USE_VY; // For instructions 0x8XY6 and 0x8XYE. false is more modern (chip-48, super-chip)
    public boolean STORE_MEMORY_INCREMENTS_INDEX; // For instructions 0xFX55 and 0xFX65. false is more modern
    public boolean JUMP_NNN; // For instruction 0xBNNN/0xBXNN. false is more modern
    public boolean RESET_FLAG_REG_ON_BIT_MANIP; // For AND, OR, XOR (8xy1, 8xy2, 8xy3). Reset flag register to 0 if true

    public Config(String chipType) {
        if (chipType.equals("chip8")) {
            SHIFT_USE_VY = true;
            STORE_MEMORY_INCREMENTS_INDEX = true;
            JUMP_NNN = true;
            RESET_FLAG_REG_ON_BIT_MANIP = true;
        } else if (chipType.equals("schip")) {
            SHIFT_USE_VY = false;
            STORE_MEMORY_INCREMENTS_INDEX = false;
            JUMP_NNN = false;
            RESET_FLAG_REG_ON_BIT_MANIP = false;
        } else {
            JUMP_NNN = true;
        }
    }
}
