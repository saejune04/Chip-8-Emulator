package chip8_java.src;

public class Main {
    public static void main(String[] args) {
        String romFilePath = "chip8_java\\roms\\tests\\6-keypad.ch8";
        Emulator emulator = new Emulator("chip8");
        emulator.loadROM(romFilePath);
        emulator.run();
    } 

}
