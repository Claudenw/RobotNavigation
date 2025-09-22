package org.xenei.robot.ml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class BitMap {
	private static final int MAX_IDX = 8;
	private static final int ALL_OPTIONS = 0x1FF;
	private int validOptions;

	public static int getMask(final int bitIndex) {
		return 1 << bitIndex;
	}

	public BitMap() {
		this(ALL_OPTIONS);
	}

	private BitMap(int validOptions) {
		this.validOptions = validOptions;
	}

	public BitMap and(BitMap other) {
		return new BitMap(validOptions & other.validOptions);
	}

	public boolean equalTo(BitMap other) {
		return validOptions == other.validOptions;
	}

	public int[] indices() {
		int[] indices = new int[Integer.bitCount(validOptions)];
		int idx = 0;
		short mask = 1;
		for (int bitIndex = 0; bitIndex < MAX_IDX; bitIndex++) {
			if ((validOptions & bitIndex) != 0) {
				indices[idx++] = bitIndex;
				mask <<= 1;
			}
		}
		return indices;
	}

	public BitMap disable(int bit) {
		return new BitMap(validOptions & ~getMask(bit));
	}

	public BitMap enable(int bit) {
		return new BitMap(validOptions | getMask(bit));
	}

	public String toString() {
		return Arrays.toString(indices());
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeInt(validOptions);
	}

	public void read(DataInputStream in) throws IOException {
		validOptions = in.readInt();
	}

	public static class Builder {
		int accumulator = ALL_OPTIONS;

		Builder() {
		}

		public Builder add(BitMap bitMap) {
			accumulator |= bitMap.validOptions;
			return this;
		}

		public BitMap build() {
			return new BitMap(accumulator);
		}
	}
}
