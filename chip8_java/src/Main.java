package chip8_java.src;

public class Main {
    public static void main(String[] args) {
        String soundFilePath = "chip8_java\\sounds\\pluck.wav";
        String romFilePath = "chip8_java\\roms\\animations\\Trip8 Demo.ch8";
        Emulator emulator = new Emulator(soundFilePath);
        emulator.loadROM(romFilePath);
        emulator.run();
    } 

}
