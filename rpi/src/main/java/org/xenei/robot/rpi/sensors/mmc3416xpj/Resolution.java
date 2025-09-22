package org.xenei.robot.rpi.sensors.mmc3416xpj;

/**
 * The resolution of the measurements.
 */
public enum Resolution {
	_16bits_8ms(2048f, (byte) 0), _16bits_4ms(2048f, (byte) 1), _14bits_2ms(0512f, (byte) 2), _12bits_1ms(0128f,
			(byte) 3);

	/**
	 * The number of counts per G
	 */
	public final float counts_per_gauss;
	/**
	 * The register value for the device call
	 */
	public final byte flag;

	Resolution(float counts_per_gauss, byte flag) {
		this.counts_per_gauss = counts_per_gauss;
		this.flag = flag;
	}
}
