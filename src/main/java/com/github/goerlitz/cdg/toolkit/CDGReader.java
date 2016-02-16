package com.github.goerlitz.cdg.toolkit;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CDGReader extends TimerTask {

	static final int FramesPerSecond = 20;  // frames displayed per second, CD+G has 300 packets per second -> 300/FPS = packets per frame
	static final int PacketsPerFrame = 300 / FramesPerSecond;
	static final int FrameInterval   = 1000 / FramesPerSecond;
	
	static final byte SC_MASK = 0x3F;
	static final byte CDG_CMD = 0x09;
	static final byte CDG_MEMORY_PRESET         =  1;
	static final byte CDG_BORDER_PRESET         =  2;
	static final byte CDG_TILE_BLOCK            =  6;
	static final byte CDG_SCROLL_PRESET         = 20;
	static final byte CDG_SCROLL_COPY           = 24;
	static final byte CDG_TRANPARENT_COLOR      = 28;
	static final byte CDG_LOAD_COLOR_TABLE_LOW  = 30;
	static final byte CDG_LOAD_COLOR_TABLE_HIGH = 31;
	static final byte CDG_TILE_BLOCK_XOR        = 38;
	
	private CDGViewer viewer = new CDGViewer();
	private final DataInputStream dis;
	private Timer timer = new Timer();
	
	private byte[] packet = new byte[24];
	
	private int[] frequency = new int[39];
	
	int packetCount = 0;
	int cdgCount = 0;
	
	int bytesRead;
	
	final long start = System.currentTimeMillis();
	

	
	public CDGReader(File file) {
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			throw new RuntimeException();
		}
	}
	
	public void process() {
		timer.scheduleAtFixedRate(this, 0, FrameInterval);
	}

	public void stop() {
		timer.cancel();
		System.out.println("time taken: " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
		try {
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		int processedPackets = 0;
		try {
			while ((processedPackets < PacketsPerFrame) && ((bytesRead = dis.read(packet)) == 24)) {
				processedPackets++;
				packetCount ++;

				if ((packet[0] & SC_MASK) == CDG_CMD) {   // CD+G?
					cdgCount++;
					frequency[packet[1] & SC_MASK]++;

					switch (packet[1] & SC_MASK) {
					case CDG_MEMORY_PRESET:
						int color = packet[4] & 0x0F;
						int repeat = packet[5] & 0x0F;
						if (repeat == 0) {
							viewer.clearScreen(color);
						}
						break;
					case CDG_BORDER_PRESET:
						int bgcol = packet[4] & 0x0F;
						viewer.clearBorder(bgcol);
						break;
					case CDG_LOAD_COLOR_TABLE_LOW:
						for (int i = 0; i<8; i++) {
							byte[] cols = decodeColor(packet[4+i*2], packet[5+i*2]);
							viewer.setColor(cols[0], cols[1], cols[2], i);
							viewer.applyColor();
						}
						break;
					case CDG_LOAD_COLOR_TABLE_HIGH:
						for (int i = 0; i<8; i++) {
							byte[] cols = decodeColor(packet[4+i*2], packet[5+i*2]);
							viewer.setColor(cols[0], cols[1], cols[2], i+8);
							viewer.applyColor();
						}
						break;
					case CDG_TILE_BLOCK:
						paintTile(packet, false);
						break;
					case CDG_TILE_BLOCK_XOR:
						paintTile(packet, true);
						break;
					default:
						break;
					}
//					viewer.repaint();
//					Thread.sleep(100);
				} else {
					// not cdg;
					StringBuffer buf = new StringBuffer();
					for (byte data : packet) {
//						int high = data >> 4;
//						int low = data & 0xF;
//						buf.append((high < 10) ? high : Character.valueOf((char) (64+high)));
//						buf.append((low < 10) ? low : Character.valueOf((char) (64+low)));
						buf.append(data);
						buf.append(" ");
					}
//					System.out.println("NO_CDG: " + buf);
				}
			}
			viewer.repaint();
			if (bytesRead == -1) {
				stop();
				System.out.println("CDG instructions: " + cdgCount);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		//				for (int i = 0; i<=38; i++)
		//					System.out.println(i + ": " + frequency[i]);

	}
	
	private void paintTile(byte[] packet, boolean xor) {
		int color0 = packet[4] & 0x0F;
		int color1 = packet[5] & 0x0F;
		int row    = packet[6] & 0x1F;
		int column = packet[7] & 0x3F;
		
		if (row >= 17 || column >= 49) {
//			System.out.println("Tile(" + row + "," + column + "): " + color0 + "/" + color1 + (xor ? " XOR" : " ...") + " - OUT OF BOUNDS");
			return;
//		} else {
//			System.out.println("Tile(" + row + "," + column + "): " + color0 + "/" + color1 + (xor ? " XOR" : " ..."));			
		}
		
		row *= 12;
		column *= 6;
		
		for (int y = 0; y < 12; y++) {
			int scanline = packet[y+8] & 0x3F;
			for (int x = 5; x >= 0; x--) {
				int color = ((scanline & 1) == 0) ? color0 : color1;
				viewer.setPixel(column + x + 3, row + y + 6, color, xor);
				scanline = scanline >> 1;
			}
		}
	}
	
	private void debugCDGInstruction(int count, String msg) {
		
	}

	public static void main(String[] args) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("CDG Files", "cdg"));
		if(JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(null)) {
			new CDGReader(chooser.getSelectedFile()).process();
		}
	}
	
	public static byte[] decodeColor(byte high, byte low) {
		return new byte[] {
				(byte) ((high & SC_MASK) >> 2),
				(byte) (((high & 0x3) << 2) | ((low >> 4) & 0x3)),
				(byte) (low & 0x0F)
		};
	}

}
