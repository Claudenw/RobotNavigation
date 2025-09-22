package org.xenei.robot.rpi.sensors.mmc3416xpj;

public enum Frequency {
	HZ1_5(0x0), HZ13(0x4), HZ25(0x8), HZ50(0xC);

	public final byte value;

	Frequency(int value) {
		this.value = (byte) value;
	}
}
