package com.syam.paymentstatistics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.syam.paymentstatistics.jpa.TransactionAmount;
import com.syam.paymentstatistics.jpa.TransactionAmountRepository;
import com.syam.paymentstatistics.pojo.StatisticsData;
import com.syam.paymentstatistics.pojo.TransactionRequest;

@Service
public class StatisticsService {

	@Autowired
	private TransactionAmountRepository transactionRepository;

	private static final StatisticsData STATISTICS_DATA = new StatisticsData();

	public void registerTransaction(TransactionRequest transactionRequest) {
		long requestTime = System.currentTimeMillis();

		// Validate request
		transactionRequest.validate();

		// If test mode is true, save the transaction in db for tracking
		if (Boolean.TRUE.equals(transactionRequest.getTest())) {
			TransactionAmount entry = new TransactionAmount(transactionRequest, requestTime);
			transactionRepository.save(entry);
		}

		// Apply the transaction amount on the statistics data object

	}

	public StatisticsData getStatistics() {
		return STATISTICS_DATA;
	}

	public synchronized void applyTransaction(double amount) {

	}

}
