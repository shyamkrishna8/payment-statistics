package com.syam.paymentstatistics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syam.paymentstatistics.pojo.BasicResponse;
import com.syam.paymentstatistics.pojo.StatisticsData;
import com.syam.paymentstatistics.pojo.StatisticsDataWithTimeStamp;
import com.syam.paymentstatistics.pojo.TransactionRequest;

@RestController
@RequestMapping("/")
public class StatisticsController {

	@Autowired
	private StatisticsService statisticsService;

	@RequestMapping(value = "/transactions", method = RequestMethod.POST)
	public BasicResponse createNewVideo(@RequestBody TransactionRequest transactionRequest) {
		System.out.println("Request : " + transactionRequest.toString() + " received at : " + System.currentTimeMillis());
		BasicResponse response = new BasicResponse();
		statisticsService.registerTransaction(transactionRequest);
		return response;
	}

	@RequestMapping(value = "/statistics", method = RequestMethod.GET)
	public StatisticsData getStatistics(@RequestParam(name="test") Boolean test) {
		long current_time = System.currentTimeMillis();
		System.out.println("Request  for statistics received at : " );

		StatisticsData response = statisticsService.getStatistics();
		if (Boolean.TRUE.equals(test)) {
			response = new StatisticsDataWithTimeStamp(response, current_time);
		}

		return response;
	}
}
