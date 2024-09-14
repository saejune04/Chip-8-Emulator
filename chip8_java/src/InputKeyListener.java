package chip8_java.src;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputKeyListener implements KeyListener {
    private Emulator emulator; // The emulator this keylistener is attacehd to
    public InputKeyListener(Emulator emulator) {
        this.emulator = emulator;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        emulator.handleUserInput(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        emulator.handleUserInput(e, false);
    }
    
}
