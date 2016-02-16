package com.github.goerlitz.cdg.toolkit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;


public class CDGViewer extends JPanel {
	
	private static final int CDG_WIDTH  = 300;
	private static final int CDG_HEIGHT = 216;
	
	private BufferedImage image;// = new BufferedImage(300, 216, BufferedImage.TYPE_BYTE_INDEXED);
	private DataBuffer dbuf;
	private IndexColorModel colorModel;
	
    byte[] r = new byte[16];
    byte[] g = new byte[16];
    byte[] b = new byte[16];
    
    private double scale = 2;
    private int offsetX;
    private int offsetY;
	
	public CDGViewer() {
		
        byte[] pixels = new byte[CDG_WIDTH*CDG_HEIGHT];

        // Create a data buffer using the byte buffer of pixel data.
        // The pixel data is not copied; the data buffer uses the byte buffer array.
        dbuf = new DataBufferByte(pixels, pixels.length, 0);

        // Prepare a sample model that specifies a storage 4-bits of
        // pixel datavd in an 8-bit data element
        int bitMasks[] = new int[]{(byte)0xf};
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, CDG_WIDTH, CDG_HEIGHT, bitMasks);

        // Create a raster using the sample model and data buffer
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dbuf, null);

        // Combine the color model and raster into a buffered image
        colorModel = generateColorModel();
        image = new BufferedImage(colorModel, raster, false, null);

		this.setMinimumSize(new Dimension(CDG_WIDTH, CDG_HEIGHT));
		this.setPreferredSize(new Dimension(CDG_WIDTH*2 , CDG_HEIGHT*2));
        
		final JFrame frame = new JFrame("CDG");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setContentPane(this);
		frame.pack();
		
		this.addComponentListener(new ComponentAdapter() {
			
			Dimension dim = new Dimension();
			
			@Override
			public void componentResized(ComponentEvent e) {
				frame.getContentPane().getSize(dim);
				double scaleX = (double) dim.getWidth() / CDG_WIDTH;
				double scaleY = (double) dim.getHeight() / CDG_HEIGHT;
				if (Double.compare(scaleX, scaleY) < 0) {
					scale = scaleX;
					offsetX = 0;
					offsetY = (int) (dim.getHeight() - CDG_HEIGHT*scale) / 2;
				} else {
					scale = scaleY;
					offsetX = (int) (dim.getWidth() - CDG_WIDTH*scale) / 2;
					offsetY = 0;					
				}
				frame.repaint();
			}
		});

		frame.setVisible(true);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);

//		// test
//		Graphics2D g2d=(Graphics2D) image.getGraphics();
//		Font myFont=new Font("Arial", Font.BOLD|Font.PLAIN, 15);
//		String s="I GOTTA FEELING";
//		g2d.setFont( myFont ); //Schriftart setzen
//		g2d.drawString(s,70,79); //String rendern
		
		// scale image to screen size
		AffineTransform tx = new AffineTransform();
		tx.scale(scale, scale);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		BufferedImage scaled = op.filter(image, null);
		g.drawImage(scaled, offsetX, offsetY, null);
	}
	
	public void setColor(byte r, byte g, byte b, int index) {
//		this.r[index] = (byte) (r * 17);
//		this.g[index] = (byte) (g * 17);
//		this.b[index] = (byte) (b * 17);
		this.r[index] = (byte) (r << 4 | r);
		this.g[index] = (byte) (g << 4 | g);
		this.b[index] = (byte) (b << 4 | b);
//		System.out.println("Assign color: " + index + ": " + this.r[index] + "/" + this.g[index] + "/" + this.b[index]);
	}
	
	public void applyColor() {
		this.image = new BufferedImage(new IndexColorModel(4, 16, this.r, this.g, this.b), image.getRaster(), false, null);
		repaint();
	}
	
	public void setPixel(int x, int y, int color, boolean xor) {
		if (xor) {
			color ^= image.getRaster().getDataBuffer().getElem(y*300+x);
		}
		image.getRaster().getDataBuffer().setElem(y*300+x, color);
	}
	
//	public void setPixelXOR(int x, int y, int color) {
//		int val = image.getRaster().getDataBuffer().getElem(y*300+x);
//		image.getRaster().getDataBuffer().setElem(y*300+x, color ^ val);
//	}
	
	public void clearBorder(int col) {
//		System.out.println("Clear Border with color: " + col + ": " + (0xFF & r[col]) + "/" + (0xFF & g[col]) + "/" + (0xFF & b[col]));
		this.setBackground(new Color(0xFF & r[col], 0xFF & g[col], 0xFF & b[col]));
		int pos = 0;
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 300; x++) {
				image.getRaster().getDataBuffer().setElem(pos++, col);
			}
		}
		for (int y = 6; y < 210; y++) {
			for (int x = 0; x < 3; x++) {
				image.getRaster().getDataBuffer().setElem(pos++, col);
			}
			pos += 294;
			for (int x = 297; x < 300; x++) {
				image.getRaster().getDataBuffer().setElem(pos++, col);
			}			
		}
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 300; x++) {
				image.getRaster().getDataBuffer().setElem(pos++, col);
			}
		}
	}
	
	public void clearScreen(int col) {
//		System.out.println("Clear Screen with color: " + col + ": " + (0xFF & r[col]) + "/" + (0xFF & g[col]) + "/" + (0xFF & b[col]));
		for (int y = 6; y < 210; y++) {
			for (int x = 3; x < 297; x++) {
				image.getRaster().getDataBuffer().setElem(y*300+x, col);
			}
		}
	}
	
    private IndexColorModel generateColorModel() {
        return new IndexColorModel(4, 16, r, g, b);
    }

}
