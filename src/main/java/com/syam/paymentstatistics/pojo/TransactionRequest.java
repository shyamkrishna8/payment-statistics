package com.syam.paymentstatistics.pojo;

import com.syam.paymentstatistics.exceptions.BadRequestException;
import com.syam.paymentstatistics.exceptions.NoContentResponse;
import com.syam.paymentstatistics.utils.Constants;

public class TransactionRequest {
	private Double amount;
	private long timestamp;
	private Boolean test;

	public TransactionRequest() {
		super();
	}

	public TransactionRequest(Double amount, long timestamp, Boolean test) {
		super();
		this.amount = amount;
		this.timestamp = timestamp;
		this.test = test;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Boolean getTest() {
		return test;
	}

	public void setTest(Boolean test) {
		this.test = test;
	}

	public long getExpiryTime() {
		return this.timestamp + Constants.STATISTICS_TIME_WINDOW;
	}

	public void addTransaction(TransactionRequest request) {
		if (this.timestamp == request.getTimestamp()) {
			this.amount += request.getAmount();
		}
	}

	public void validate() {
		if (this.amount == null || this.amount < 0d) {
			throw new BadRequestException("Amount value is invalid.");
		}

		if (this.timestamp < System.currentTimeMillis() - Constants.TRANSACTION_ACCEPTANCE_DURATION) {
			throw new NoContentResponse("Transaction is older than ");
		}
	}

	@Override
	public String toString() {
		return "TranscationRequest [amount=" + amount + ", timestamp=" + timestamp + ", toString()=" + super.toString()
				+ "]";
	}
}
