package com.syam.paymentstatistics.pojo;

import com.syam.paymentstatistics.exceptions.BadRequestException;
import com.syam.paymentstatistics.exceptions.NoContentResponse;
import com.syam.paymentstatistics.utils.Constants;

public class TransactionRequest {
	private Double amount;
	private long timestamp;

	public TransactionRequest() {
		super();
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
