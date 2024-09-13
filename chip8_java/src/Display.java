package chip8_java.src;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

public class Display extends JPanel {
    public boolean[][] display;
    public int scale = 1;

    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // do your superclass's painting routine first, and then paint on top of it.
        for (int i = 0; i < display.length; i++) {
            for (int j = 0; j < display[i].length; j++) {
                if (display[i][j]) {
                    g.setColor(Color.WHITE); // On pixel == white
                    g.fillRect(j * scale, i * scale, scale, scale);
                } else {
                    g.setColor(Color.BLACK); // Off pixel == black
                    g.fillRect(j * scale, i * scale, scale, scale);
                }
            }
        }
    }

    public void setDisplayData(boolean[][] pixels, int pixelSize) {
        display = pixels;
        scale = pixelSize;
    }
}
