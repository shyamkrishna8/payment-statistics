package com.syam.paymentstatistics.pojo;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;
import com.syam.paymentstatistics.utils.CommonUtils;

public class StatisticsData {
	/*
	 * "sum": 1000, "avg": 100, "max": 200, "min": 50, "count": 10
	 */

	private AtomicDouble sum;
	private AtomicDouble avg;
	private AtomicDouble max;
	private AtomicDouble min;
	private AtomicInteger count;

	public StatisticsData() {
		super();
		resetValues();
	}

	public StatisticsData(StatisticsData s) {
		super();
		this.sum = new AtomicDouble(s.getSum().doubleValue());
		this.avg = new AtomicDouble(s.getAvg().doubleValue());
		this.max = new AtomicDouble(s.getMax().doubleValue());
		this.min = new AtomicDouble(s.getMin().doubleValue());
		this.count = new AtomicInteger(s.getCount().get());
	}

	public synchronized void addTranscation(double amount) {
		this.sum.compareAndSet(this.sum.doubleValue(), CommonUtils.roundDoubleValue(this.sum.doubleValue() + amount));
		this.count.incrementAndGet();
		// count will never be zero after a transaction addition
		this.avg = new AtomicDouble(CommonUtils.roundDoubleValue(this.sum.doubleValue() / this.count.intValue()));
		this.min = this.min.doubleValue() > amount ? new AtomicDouble(amount) : this.min;
		this.max = this.max.doubleValue() < amount ? new AtomicDouble(amount) : this.max;
	}

	public AtomicDouble getSum() {
		return sum;
	}

	public void setSum(AtomicDouble sum) {
		this.sum = sum;
	}

	public AtomicDouble getAvg() {
		return avg;
	}

	public void setAvg(AtomicDouble avg) {
		this.avg = avg;
	}

	public AtomicDouble getMax() {
		return max;
	}

	public void setMax(AtomicDouble max) {
		this.max = max;
	}

	public AtomicDouble getMin() {
		return min;
	}

	public void setMin(AtomicDouble min) {
		this.min = min;
	}

	public AtomicInteger getCount() {
		return count;
	}

	public void setCount(AtomicInteger count) {
		this.count = count;
	}

	public synchronized void removeTranscation(double amount) {
		this.sum.compareAndSet(this.sum.doubleValue(), CommonUtils.roundDoubleValue(this.sum.doubleValue() - amount));
		this.count.decrementAndGet();
		if (this.count.intValue() > 0) {
			this.avg = new AtomicDouble(CommonUtils.roundDoubleValue(this.sum.doubleValue() / this.count.intValue()));
		} else {
			this.avg = new AtomicDouble();
		}
	}

	public void resetValues() {
		this.sum = new AtomicDouble();
		this.avg = new AtomicDouble();
		this.min = new AtomicDouble();
		this.max = new AtomicDouble();
		this.count = new AtomicInteger();
	}

	@Override
	public String toString() {
		return "StatisticsData [sum=" + sum.doubleValue() + ", avg=" + avg.doubleValue() + ", max=" + max.doubleValue()
				+ ", min=" + min.doubleValue() + ", count=" + count.intValue() + ", toString()=" + super.toString()
				+ "]";
	}
}
