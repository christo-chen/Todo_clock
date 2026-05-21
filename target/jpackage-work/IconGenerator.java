import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class IconGenerator {
    public static void main(String[] args) throws Exception {
        int size = 1024;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(30, 36, 54), size, size, new Color(32, 108, 92)));
        g.fillRoundRect(96, 96, 832, 832, 180, 180);

        g.setColor(new Color(248, 250, 252));
        g.fill(new RoundRectangle2D.Double(216, 252, 592, 292, 42, 42));
        g.setColor(new Color(17, 24, 39));
        g.fill(new RoundRectangle2D.Double(246, 282, 532, 232, 28, 28));
        g.setColor(new Color(248, 250, 252, 72));
        g.setStroke(new BasicStroke(8f));
        g.drawLine(276, 398, 748, 398);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 152));
        String text = "12";
        FontMetrics metrics = g.getFontMetrics();
        int x = 512 - metrics.stringWidth(text) / 2;
        int y = 282 + ((232 - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setColor(new Color(248, 250, 252));
        g.drawString(text, x, y);

        g.setColor(new Color(255, 205, 86));
        g.setStroke(new BasicStroke(34f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(300, 664, 402, 766);
        g.drawLine(402, 766, 724, 588);

        g.dispose();
        ImageIO.write(image, "png", new File(args[0]));
    }
}
