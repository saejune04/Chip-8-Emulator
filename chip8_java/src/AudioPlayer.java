package chip8_java.src;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer implements Runnable {
    private String filePath;

    public AudioPlayer(String filePath) {
        this.filePath = filePath;    
    } 

    @Override
    public void run() {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start(); // Start playing the audio
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
