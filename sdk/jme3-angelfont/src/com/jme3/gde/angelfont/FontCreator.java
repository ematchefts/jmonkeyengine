/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.angelfont;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author normenhansen
 */
public abstract class FontCreator {

    private static final Color OPAQUE_WHITE = new Color(0xFFFFFFFF, true);
    private static final Color TRANSPARENT_BLACK = new Color(0x00000000, true);

    public FontCreator() {
    }

    public static AngelFont buildFont(String fontName) {
        return buildFont(fontName, 256);
    }

    public static AngelFont buildFont(String fontName, int bitmapSize) {
        return buildFont(fontName, bitmapSize, 16);
    }

    public static AngelFont buildFont(String fontName, int bitmapSize, int fontSize) {
        return buildFont(fontName, bitmapSize, fontSize, Font.PLAIN);
    }

    public static AngelFont buildFont(String fontName, int bitmapSize, int fontSize, int style) {
        return buildFont(fontName, bitmapSize, fontSize, Font.PLAIN, false);
    }

    public static AngelFont buildFont(String fontName, int bitmapSize, int fontSize, int style, boolean debug) {
        return buildFont(fontName, fontName, bitmapSize, fontSize, style, 0, 0, 0, debug);
    }

    public static AngelFont buildFont(String fontName, String fileName, int bitmapSize, int fontSize, int style, int paddingX, int paddingY, int letterSpacing, boolean debug) {
        BufferedImage fontImage;
        Font font;

        String charLocs = "";

        font = new Font(fontName, style, fontSize); // Font Name
        // use BufferedImage.TYPE_4BYTE_ABGR to allow alpha blending
        fontImage = new BufferedImage(bitmapSize, bitmapSize,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) fontImage.getGraphics();
        g.setFont(font);
        if (!debug) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setColor(OPAQUE_WHITE);
        g.setBackground(TRANSPARENT_BLACK);

        FontMetrics fm = g.getFontMetrics();

        if (debug) {
            g.setColor(Color.WHITE);
            g.drawRect(0, 0, bitmapSize - 1, bitmapSize - 1);
        }
        int xPos = 0;
        int yPos = 0;
        int height = 0;
        for (int i = 0; i < 256; i++) {
            char ch[] = {(char) i};
            String temp = new String(ch);
            Rectangle2D bounds = fm.getStringBounds(temp, g);
            height = fm.getDescent() + fm.getAscent();
            if (yPos == 0) {
                yPos = height + (paddingY * 2);
            }
            if (xPos + bounds.getWidth() + (paddingX * 2) > bitmapSize) {
                xPos = 0;
                yPos += height + (paddingY * 2);
            }
            g.drawString(temp, xPos + paddingX, yPos + paddingY);
            if (debug) {
                g.setColor(Color.BLUE);
                g.drawRect(xPos, yPos - fm.getAscent(), (int) bounds.getWidth() + (paddingX * 2), height + (paddingY * 2));
                g.setColor(Color.WHITE);
            }
            charLocs = charLocs
                    + "char id=" + i
                    + "    x=" + xPos
                    + "    y=" + (yPos - fm.getAscent())
                    + "    width=" + ((int) bounds.getWidth() + (paddingX * 2))
                    + "    height=" + ((int) bounds.getHeight() + (paddingY * 2))
                    + "    xoffset=0"
                    + "    yoffset=0"
                    + "    xadvance=" + (((int) bounds.getWidth() + letterSpacing) - 1) + " "
                    + "    page=0"
                    + "    chnl=0\n";
            xPos += bounds.getWidth() + (paddingX * 2);
        }
        charLocs = "info face=null "
                + "size=" + fontSize + " "
                + "bold=0 "
                + "italic=0 "
                + "charset=ASCII "
                + "unicode=0 "
                + "stretchH=100 "
                + "smooth=1 "
                + "aa=1 "
                + "padding=0,0,0,0 "
                + "spacing=1,1 "
                + "\n"
                + "common lineHeight=" + height + " "
                + "base=26 "
                + "scaleW=" + bitmapSize + " "
                + "scaleH=" + bitmapSize + " "
                + "pages=1 "
                + "packed=0 "
                + "\n"
                + "page id=0 file=\"" + fileName.replaceAll(" ", "") + ".png\"\n"
                + "chars count=255\n"
                + charLocs;
        return new AngelFont(fontImage, charLocs);
    }
}
