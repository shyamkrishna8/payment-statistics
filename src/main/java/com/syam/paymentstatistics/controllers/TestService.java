package com.syam.paymentstatistics.controllers;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.syam.paymentstatistics.pojo.TransactionRequest;
import com.syam.paymentstatistics.utils.CommonUtils;
import com.syam.paymentstatistics.utils.Logger;

@Service
public class TestService {

	@Autowired
	private StatisticsService statisticsService;

	public void testConcurrentSkipListMap() {
		Map<Integer, String> treemap = new ConcurrentSkipListMap<Integer, String>();
		treemap.put(Integer.valueOf(3), "Three");
		treemap.put(Integer.valueOf(1), "One");
		treemap.put(Integer.valueOf(4), "Four");
		treemap.put(Integer.valueOf(7), "Seven");
		treemap.put(Integer.valueOf(6), "Six");

		for (Integer key : treemap.keySet()) {
			Logger.log("key : " + key + " value : " + treemap.get(key));
		}
	}

	public void testRandomStatistics() throws InterruptedException {
		int i = 3;
		Random random = new Random();
		long currentTime = System.currentTimeMillis();
		while (i > 0) {
			statisticsService
					.registerTransaction(new TransactionRequest(CommonUtils.roundDoubleValue(random.nextDouble() * 100),
							currentTime - i*10, true));
			Thread.sleep(random.nextInt(400));
			Logger.log("Statistics at : " + (System.currentTimeMillis() - currentTime) + " statistics : "
					+ statisticsService.getStatistics().toString());
			i--;
		}

		Logger.log("Statistics at end : " + (System.currentTimeMillis() - currentTime) + " statistics : "
				+ statisticsService.getStatistics().toString());
		Thread.sleep(120000);
		Logger.log("Statistics after sleep at end : " + (System.currentTimeMillis() - currentTime)
				+ " statistics : " + statisticsService.getStatistics().toString());
	}

}
