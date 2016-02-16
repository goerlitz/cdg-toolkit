package com.github.goerlitz.cdg.toolkit;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CDGWriter {

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

	private DataOutputStream dout;
	private int instCount = 0;

	public CDGWriter(String fileName) throws IOException {
		dout = new DataOutputStream(new FileOutputStream(new File(fileName)));
	}

	public void writeColorTable(byte[] colorMap, boolean high) throws IOException {
		byte[] packet = new byte[24];
		packet[0] = CDG_CMD;
		if (high) {
			packet[1] = CDG_LOAD_COLOR_TABLE_HIGH;
		} else {
			packet[1] = CDG_LOAD_COLOR_TABLE_LOW;
		}
		for (int i = 0; i < 16; i++) {
			packet[4+i] = colorMap[i];
		}
		dout.write(packet);
		
		instCount++;
	}

	public void writeTile(int col0, int col1, int row, int column, byte[] tileData, boolean xor) throws IOException {
		byte[] packet = new byte[24];
		packet[0] = CDG_CMD;
		if (xor) {
			packet[1] = CDG_TILE_BLOCK_XOR;
		} else {
			packet[1] = CDG_TILE_BLOCK;
		}
		packet[4] = (byte) col0;
		packet[5] = (byte) col1;
		packet[6] = (byte) row;
		packet[7] = (byte) column;

		for (int y = 0; y < 12; y++) {
			packet[8+y] = tileData[y];
		}

		dout.write(packet);
		
		instCount++;
	}
	
	public int getInstructionCount() {
		return this.instCount;
	}

	public void finish() throws IOException {
		dout.close();
	}
}
