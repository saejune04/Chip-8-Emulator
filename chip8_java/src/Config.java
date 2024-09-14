package chip8_java.src;

public class Config {
    public boolean SHIFT_USE_VY; // For instructions 0x8XY6 and 0x8XYE. false is more modern (chip-48, super-chip)
    public boolean STORE_MEMORY_INCREMENTS_INDEX; // For instructions 0xFX55 and 0xFX65. false is more modern
    public boolean JUMP_NNN; // For instruction 0xBNNN/0xBXNN. false is more modern

    public Config(String chipType) {
        if (chipType.equals("chip8")) {
            SHIFT_USE_VY = true;
            STORE_MEMORY_INCREMENTS_INDEX = true;
            JUMP_NNN = true;
        } else if (chipType.equals("schip")) {
            SHIFT_USE_VY = false;
            STORE_MEMORY_INCREMENTS_INDEX = false;
            JUMP_NNN = false;
        } else {
            
        }
    }
}
