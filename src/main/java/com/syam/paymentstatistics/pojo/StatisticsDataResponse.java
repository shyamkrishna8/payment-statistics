package com.syam.paymentstatistics.pojo;

public class StatisticsDataResponse {

	private double sum;
	private double avg;
	private double max;
	private double min;
	private int count;

	public StatisticsDataResponse() {
		super();
	}

	public StatisticsDataResponse(StatisticsData sd) {
		super();
		this.sum = sd.getSum().doubleValue();
		this.avg = sd.getAvg().doubleValue();
		this.max = sd.getMax().doubleValue();
		this.min = sd.getMin().doubleValue();
		this.count = sd.getCount().intValue();
	}

	public StatisticsDataResponse(StatisticsDataResponse sds) {
		super();
		this.sum = sds.getSum();
		this.avg = sds.getAvg();
		this.max = sds.getMax();
		this.min = sds.getMin();
		this.count = sds.getCount();
	}

	public double getSum() {
		return sum;
	}

	public void setSum(double sum) {
		this.sum = sum;
	}

	public double getAvg() {
		return avg;
	}

	public void setAvg(double avg) {
		this.avg = avg;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "StatisticsDataResponse [sum=" + sum + ", avg=" + avg + ", max=" + max + ", min=" + min + ", count="
				+ count + ", toString()=" + super.toString() + "]";
	}
}
