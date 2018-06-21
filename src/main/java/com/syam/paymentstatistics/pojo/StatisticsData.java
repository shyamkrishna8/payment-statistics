package com.syam.paymentstatistics.pojo;

public class StatisticsData {
	/*
	 * "sum": 1000, "avg": 100, "max": 200, "min": 50, "count": 10
	 */

	private double sum;
	private double avg;
	private double max;
	private double min;
	private int count;

	public StatisticsData() {
		super();

	}

	public StatisticsData(StatisticsData s) {
		super();
		this.sum = s.getSum();
		this.avg = s.getAvg();
		this.max = s.getMax();
		this.min = s.getMin();
		this.count = s.getCount();
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

	public void resetValues() {
		this.sum = 0;
		this.avg = 0;
		this.min = 0;
		this.max = 0;
		this.count = 0;
	}

	@Override
	public String toString() {
		return "StatisticsResponse [sum=" + sum + ", avg=" + avg + ", max=" + max + ", min=" + min + ", count=" + count
				+ ", toString()=" + super.toString() + "]";
	}
}
