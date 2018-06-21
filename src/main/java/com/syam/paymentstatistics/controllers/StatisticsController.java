package com.syam.paymentstatistics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syam.paymentstatistics.pojo.BasicResponse;
import com.syam.paymentstatistics.pojo.StatisticsDataResponse;
import com.syam.paymentstatistics.pojo.StatisticsDataResponseWithTimeStamp;
import com.syam.paymentstatistics.pojo.TransactionRequest;
import com.syam.paymentstatistics.utils.Logger;

@RestController
@RequestMapping("/")
public class StatisticsController {

	@Autowired
	private StatisticsService statisticsService;

	@Autowired
	private TestService testService;

	@RequestMapping(value = "/transactions", method = RequestMethod.POST)
	public BasicResponse createNewVideo(@RequestBody TransactionRequest transactionRequest) {
		System.out
				.println("Request : " + transactionRequest.toString() + " received at : " + System.currentTimeMillis());
		BasicResponse response = new BasicResponse();
		statisticsService.registerTransaction(transactionRequest);
		Logger.log("Response for transaction : " + transactionRequest.toString() + " done at : "
				+ System.currentTimeMillis());
		return response;
	}

	@RequestMapping(value = "/statistics", method = RequestMethod.GET)
	public StatisticsDataResponse getStatistics(@RequestParam(name = "test", required = false) Boolean test) {
		long current_time = System.currentTimeMillis();
		Logger.log("Request for statistics received at : ");

		StatisticsDataResponse response = statisticsService.getStatistics();
		if (Boolean.TRUE.equals(test)) {
			response = new StatisticsDataResponseWithTimeStamp(response, current_time);
		}

		Logger.log("Response for statistics done at : " + System.currentTimeMillis() + " response : "
				+ response.toString());
		return response;
	}

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public BasicResponse testStatistics() throws InterruptedException {
		testService.testRandomStatistics();
		return new BasicResponse();
	}

}
