package chip8_java.src;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

public class Display extends JPanel {
    public boolean[][] display;
    public int scale = 1;
    public boolean isHighRes = false;

    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // do your superclass's painting routine first, and then paint on top of it.
        if (isHighRes) {
            for (int i = 0; i < Chip.DISPLAY_ROWS_HI_RES; i++) {
                for (int j = 0; j < Chip.DISPLAY_COLS_HI_RES; j++) {
                    if (display[i][j]) {
                        g.setColor(Color.WHITE); // On pixel == white
                        g.fillRect(j * scale, i * scale, scale, scale);
                    } else {
                        g.setColor(Color.BLACK); // Off pixel == black
                        g.fillRect(j * scale, i * scale, scale, scale);
                    }
                }
            }
        } else {
            // Low res
            for (int i = 0; i < Chip.DISPLAY_ROWS_LO_RES; i++) {
                for (int j = 0; j < Chip.DISPLAY_COLS_LO_RES; j++) {
                    if (display[i][j]) {
                        g.setColor(Color.WHITE); // On pixel == white
                        g.fillRect(j * scale * 2, i * scale * 2, scale * 2, scale * 2);
                    } else {
                        g.setColor(Color.BLACK); // Off pixel == black
                        g.fillRect(j * scale * 2, i * scale * 2, scale * 2, scale * 2);
                    }
                }
            }
        }
    }

    public void setDisplayData(boolean[][] pixels, int pixelSize, boolean isHighRes) {
        display = pixels;
        scale = pixelSize;
        this.isHighRes = isHighRes;
    }
}
