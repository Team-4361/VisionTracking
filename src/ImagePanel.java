import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Graphics;

public class ImagePanel extends JPanel
{
    BufferedImage updImage = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);

    public void updateImage(BufferedImage img)
    {
        updImage = img;
        repaint();
    }

    public void paintComponent(Graphics g) 
    {
        g.drawImage(updImage, 0, 0, getWidth(), getHeight() -150 , 0, 0, updImage.getWidth(), updImage.getHeight(), null);
    }
    
}
