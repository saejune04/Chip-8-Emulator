package chip8_java.src;

public class Main {
    public static void main(String[] args) {
        String romFilePath = "chip8_java\\roms\\tests\\5-quirks.ch8";
        Emulator emulator = new Emulator("schip");
        emulator.loadROM(romFilePath);
        emulator.run();
    } 

}
