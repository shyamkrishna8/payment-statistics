package com.syam.paymentstatistics.utils;

public class Constants {

	public static final int TRANSACTION_ACCEPTANCE_DURATION = 60 * 1000; // 60 seconds. Only the requests between
																			// Current time - STATISTICS_TIME_WINDOW,
																			// are accepted by the system.
	public static final int STATISTICS_TIME_WINDOW = 60 * 1000; // 60 seconds. Only the requests between Current time -
																// STATISTICS_TIME_WINDOW, are considered in
																// STATISTICS_DATA object
	public static final int DOUBLE_PRECISION = 3; // Upto 3 decimals.
}
