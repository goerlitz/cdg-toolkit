import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;


public class ImageWriter {
	
	public static final String imageFile = "test.png";
	public static final String cdgFile = "test.cdg";
	
	private static final int CDG_WIDTH  = 300;
	private static final int CDG_HEIGHT = 216;
	private static final int CDG_TILE_COLUMNS = 49;
	private static final int CDG_TILE_ROWS = 17;
	private static final int CDG_TILE_WIDTH = 6;
	private static final int CDG_TILE_HEIGHT = 12;
	
	
    byte[] r = new byte[16];
    byte[] g = new byte[16];
    byte[] b = new byte[16];
	
	public static void main(String[] args) {
		
		try {
			CDGWriter cdg = new CDGWriter(cdgFile);
			BufferedImage img = ImageIO.read(new File(imageFile));
//		    System.out.println("image: " + img.getWidth() + "/" + img.getHeight());
			
			ColorModel colorModel = img.getColorModel();
			if (colorModel instanceof IndexColorModel) {
				new ImageWriter().transform(img.getData(), (IndexColorModel) colorModel, cdg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void encodeColor(int index ,int r, int g, int b, byte[] colors) {
		colors[index*2] = (byte) (r << 2 | g >> 2);
		colors[index*2+1] = (byte) (((g & 0x3) << 4) | b) ;
	}
	
	private String debugColors(Integer[] colors, String text) {
		StringBuffer buf = new StringBuffer(text);
		buf.append("|");
		for (int i=0; i < 4; i++) {
			int col = colors[i];
			buf.append(Integer.toHexString(col).toUpperCase());
			buf.append(":");
			String binary = Integer.toBinaryString(col);
			for (int l=binary.length(); l < 4; l++)
				buf.append("0");
			buf.append(binary);
			buf.append("|");
		}
		return buf.toString();
	}
	
	private void writeColorTable(IndexColorModel icm, CDGWriter cdg) throws IOException {
		
    	System.out.println("index palette size: " + icm.getMapSize());
		
    	byte[] r = new byte[16];
    	byte[] g = new byte[16];
    	byte[] b = new byte[16];
    	icm.getReds(r);
    	icm.getGreens(g);
    	icm.getBlues(b);
    	byte[] colorMap = new byte[16];
    	for (int i = 0; i < 8; i++) {
    		encodeColor(i, (0xFF & r[i]) >> 4, (0xFF & g[i]) >> 4, (0xFF & b[i]) >> 4, colorMap);
//    		System.out.println("color" + i + ": " + ((0xFF & r[i]) >> 4) + "/" + ((0xFF & g[i]) >> 4) + "/" + ((0xFF & b[i]) >> 4));
    	}
    	cdg.writeColorTable(colorMap, false);
    	for (int i = 0; i < 8; i++) {
    		encodeColor(i, (0xFF & r[i+8]) >> 4, (0xFF & g[i+8]) >> 4, (0xFF & b[i+8]) >> 4, colorMap);
//    		System.out.println("color" + i + ": " + ((0xFF & r[i]) >> 4) + "/" + ((0xFF & g[i]) >> 4) + "/" + ((0xFF & b[i]) >> 4));
    	}
    	cdg.writeColorTable(colorMap, true);
	}
	
	// get set of colors per tile
	private List<Integer> getTileColors(int row, int column, Raster data) {
		int[] colors = new int[16];
		for (int yTile = 0; yTile < CDG_TILE_HEIGHT; yTile++) {
			for (int xTile = 0; xTile < CDG_TILE_WIDTH; xTile++) {
				int x = column * CDG_TILE_WIDTH + xTile;
				int y = row * CDG_TILE_HEIGHT + yTile;
				colors[data.getSample(x, y, 0)]++;
			}
		}
		List<Integer> colorList = new ArrayList<Integer>();
		for (int i = 0; i < 16; i++) {
			if (colors[i] != 0) {
				colorList.add(i);
			}
		}
		return colorList;
	}
	
	private void paint4XORTile(CDGWriter cdg, Raster data, int row, int column, Integer[] col) throws IOException {
		// 0: A ^ C
		// 1: A ^ D
		// 2: B ^ D
		// 3: B ^ C
		
		// 0 1 2 3 4 5
		// A A A B B B
		// C C D D D C
		// 0 0 0 0 0 0
		
		// 2 layers: 2^2 =  4 possible combinations (1:AC, 2:AD, 3:BC, 4:BD) -> 1^2^3^4 = 0
		// 3 layers: 2^3 =  8 possible combinations (ACE, ACF, ADE, ADF, BCE, BCF, BDE, BDF)
		// 4 layers: 2^4 = 16 possible combinations
		
		int colA = 0;
		int colB = col[2] ^ col[1];
		int colC = col[0];
		int colD = col[1];
		
		byte[] tileMap = getTileMap2(data, row, column, col[0], col[1]);
		cdg.writeTile(colB, colA, row, column, tileMap, false);
		tileMap = getTileMap2(data, row, column, col[1], col[2]);
		cdg.writeTile(colC, colD, row, column, tileMap, true);
	}
	
	private void countColorBitmasks(List<Integer> colorList, Map<Integer, Integer> counts) {
		
		// create a color bitmask by setting the i-th bit for the i-th color
		int bitmask = 0;
		for (int i = 0; i < colorList.size(); i++) {
			bitmask |= (1 << colorList.get(i));
		}
		
		// increase counter for bitmask
		Integer count = counts.get(bitmask);
		if (count == null)
			counts.put(bitmask, 1);
		else
			counts.put(bitmask, ++count);
	}
	
	private void printColorBitmaskFrequency(final Map<Integer, Integer> counts) {
    	List<Integer> keys = new ArrayList<Integer>(counts.keySet());
    	Collections.sort(keys, new Comparator<Integer>() {
			@Override
			public int compare(Integer key1, Integer key2) {
				return -counts.get(key1).compareTo(counts.get(key2));
			}
		});
    	
    	for (Integer bitmask : keys) {
    		int count = counts.get(bitmask);
    		String binary = Integer.toBinaryString(bitmask);
    		StringBuffer buf = new StringBuffer();
    		for (int i = binary.length(); i < 16; i++)
    			buf.append(0);
    		buf.append(binary);
    		System.out.println(count + " - " + buf);
    	}
	}
	
	public void analyzeColorCombinations(Raster data) {
		
		final Map<Integer, Integer> counts4 = new HashMap<Integer, Integer>();
		final Map<Integer, Integer> counts5 = new HashMap<Integer, Integer>();
		
    	for (int row = 0; row < CDG_TILE_ROWS; row++) {
    		for (int column = 0; column < CDG_TILE_COLUMNS; column++) {
    			
    			// get set of distinct colors in tile
    			List<Integer> colorList = getTileColors(row, column, data);
    			
    			if (colorList.size() == 4) {
    				countColorBitmasks(colorList, counts4);
    			}
    			if (colorList.size() == 5) {
    				countColorBitmasks(colorList, counts5);
    			}
    		}
    	}
    	
//    	System.out.println(counts4.size() + " 4color combinations:");
//    	printColorBitmaskFrequency(counts4);
//    	
//    	System.out.println(counts5.size() + " 5color combinations:");
//    	printColorBitmaskFrequency(counts5);
	}
	
	public ImageWriter() {}
	
	public void transform(BufferedImage img, int backgroundColor) {
		
	}
	
	public void transform(Raster data, IndexColorModel icm, CDGWriter cdg) {
		try {

			writeColorTable(icm, cdg);
			analyzeColorCombinations(data);

			int[] colorTileFreq = new int[17];
			int[] colorOptCount = new int[17];
			
			// go through all tiles and create appropriate tile_block instructions
			for (int row = 0; row < CDG_TILE_ROWS; row++) {
				for (int column = 0; column < CDG_TILE_COLUMNS; column++) {

					// get set of distinct colors in tile
					List<Integer> colorList = getTileColors(row, column, data);

					// count tiles with the same number of distinct colors
					colorTileFreq[colorList.size()]++;

					if (colorList.size() == 1) {
						cdg.writeTile(colorList.get(0), colorList.get(0), row, column, new byte[12], false);
						continue;
					}
					if (colorList.size() == 2) {
						byte[] tileMap = getTileMap(colorList.get(1), data, row, column);
						cdg.writeTile(colorList.get(0), colorList.get(1), row, column, tileMap, false);
						continue;
					}

					// at least 4 distinct colors in a tile
					if (colorList.size() >= 4) {
						
						Iterator<Integer[]> _4colors = new XORColorCombinationIterator(colorList);
						Integer[] col4 = _4colors.next();
						if (col4 != null) {
							paint4XORTile(cdg, data, row, column, col4);
							
							colorOptCount[colorList.size()]++;  // debug
							
							_4colors.remove();
							for (int i = 0; i < colorList.size(); i++) {
								int xorColor = colorList.get(i);
								byte[] tileMap = getTileMap(xorColor, data, row, column);
								cdg.writeTile(0, col4[3]^xorColor, row, column, tileMap, true);
							}
							continue;
						}
					}
					
//					// 6 distinct colors in a tile
//					if (colorCount == 6) {
//						
//						String combinations = "siXor combinations: ";
//						Iterator<Integer[]> _4colors = new FourXORColorIterator(colorList);
//						List<Integer[]> combList = new ArrayList<Integer[]>();
//						while (_4colors.hasNext()) {
//							Integer[] col4 = _4colors.next();
//							combinations += Arrays.asList(col4) + ", ";
//							combList.add(col4);
//						}
//						System.out.println(combinations);
//						if (combList.size() > 1)
//							cc++;
//
//					}

					// paint one normal tile first (color 0 is used as background)
					int col0 = colorList.get(0);
					int col1 = colorList.get(1);
					byte[] tileMap = getTileMap(col1, data, row, column);
					cdg.writeTile(col0, col1, row, column, tileMap, false);

					// then paint xor tiles for remaining colors (xor with background color 0)
					for (int i = 2; i < colorList.size(); i++) {
						col1 = colorList.get(i);
						tileMap = getTileMap(col1, data, row, column);
						cdg.writeTile(0, col0^col1, row, column, tileMap, true);
					}
				}
			}
			
			int instructions = 0; // colorTileFreq[1];
			for (int i=1; i <= 16; i++) {
				if (i == 1)
					instructions = colorTileFreq[1];
				else
					instructions += (i-1)*colorTileFreq[i];
				System.out.println("Frequency of colors per Tile: " + i + " color = " + colorTileFreq[i] + ", optimizable: " + colorOptCount[i]);
			}
			System.out.println("number of instructions: " + instructions + " -> " + (instructions/300) + " seconds");

			cdg.finish();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private byte[] getTileMap(int color, Raster data, int row, int column) {
		byte[] tileMap = new byte[12];
		
		for (int yTile = 0; yTile < CDG_TILE_HEIGHT; yTile++) {
			for (int xTile = 0; xTile < CDG_TILE_WIDTH; xTile++) {
				int x = column * CDG_TILE_WIDTH + xTile;
				int y = row * CDG_TILE_HEIGHT + yTile;
				if (data.getSample(x, y, 0) == color) {
					tileMap[yTile] |= 1;
				}
				tileMap[yTile] <<= 1;
			}
			tileMap[yTile] >>= 1;
		}
		
		return tileMap;
	}
	
	private byte[] getTileMap2(Raster data, int row, int column, int col0, int col1) {
		byte[] tileMap = new byte[12];
		
		for (int yTile = 0; yTile < CDG_TILE_HEIGHT; yTile++) {
			for (int xTile = 0; xTile < CDG_TILE_WIDTH; xTile++) {
				int x = column * CDG_TILE_WIDTH + xTile;
				int y = row * CDG_TILE_HEIGHT + yTile;
				int color = data.getSample(x, y, 0);
				if (color == col0 || color == col1) {
					tileMap[yTile] |= 1;
				}
				tileMap[yTile] <<= 1;
			}
			tileMap[yTile] >>= 1;
		}
		
		return tileMap;
	}
	
	
}
