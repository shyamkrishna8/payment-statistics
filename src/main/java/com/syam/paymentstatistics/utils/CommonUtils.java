package com.syam.paymentstatistics.utils;

public class CommonUtils {

	public static double roundDoubleValue(double value) {
		return Double.parseDouble(String.format("%." + Constants.DOUBLE_PRECISION + "f", value));
	}
}
