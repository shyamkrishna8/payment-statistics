package com.syam.paymentstatistics.pojo;

public class BasicResponse {
	private boolean success = true;

	public BasicResponse() {
		super();
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	public String toString() {
		return "BasicResponse [success=" + success + ", toString()=" + super.toString() + "]";
	}
}
