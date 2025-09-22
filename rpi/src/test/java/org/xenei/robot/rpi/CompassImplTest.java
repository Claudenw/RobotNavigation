package org.xenei.robot.rpi;

import org.junit.jupiter.api.Test;
import org.xenei.robot.common.ScaleInfo;

public class CompassImplTest {

	// @ParameterizedTest
	// @MethodSource("coordPairParameters")
	// public void headingText(double x, double y, double angle) {
	// assertEquals(angle, CompassImpl.heading(x, y), AngleUtils.TOLERANCE);
	// }

	// private static void processStream(List<Arguments> lst, double[] args) {
	// Location l = Location.from(args[CoordUtilsTest.X], args[CoordUtilsTest.Y]);
	// lst.add(Arguments.of(args[CoordUtilsTest.X], args[CoordUtilsTest.Y],
	// args[CoordUtilsTest.RAD]));
	// ;
	// }
	//
	// private static Stream<Arguments> coordPairParameters() {
	//
	// List<Arguments> lst = new ArrayList<Arguments>();
	//
	// Arrays.stream(CoordUtilsTest.arguments()).forEach(s -> processStream(lst,
	// s));
	//
	// return Stream.of(lst.toArray(new Arguments[0]));
	// }

	@Test
	public void x() {
		ScaleInfo info = ScaleInfo.DEFAULT;
		System.out.println(info.decimalPlaces());
		System.out.println(info.round(0.123456789));
	}
}
