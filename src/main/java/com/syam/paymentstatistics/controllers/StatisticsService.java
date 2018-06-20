package com.syam.paymentstatistics.controllers;

import org.springframework.stereotype.Service;

import com.syam.paymentstatistics.pojo.StatisticsData;
import com.syam.paymentstatistics.pojo.TransactionRequest;

@Service
public class StatisticsService {

	private static final StatisticsData STATISTICS_DATA = new StatisticsData();

	public void registerTransaction(TransactionRequest transactionRequest) {
		// Validate request
		transactionRequest.validate();

		// Apply the transaction amount on the statistics data object
		
	}

	public StatisticsData getStatistics() {
		return STATISTICS_DATA;
	}

	public synchronized void applyTransaction(double amount) {
		
	}

	
}
