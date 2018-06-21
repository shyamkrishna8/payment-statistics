package com.syam.paymentstatistics.pojo;

public class StatisticsDataWithTimeStamp extends StatisticsData {

	private long computed_time;

	public StatisticsDataWithTimeStamp() {
		super();
	}

	public StatisticsDataWithTimeStamp(StatisticsData statisticsData, long computed_time) {
		super(statisticsData);
		this.computed_time = computed_time;
	}

	public long getComputed_time() {
		return computed_time;
	}

	public void setComputed_time(long computed_time) {
		this.computed_time = computed_time;
	}

	@Override
	public String toString() {
		return "StatisticsDataWithTimeStamp [computed_time=" + computed_time + ", toString()=" + super.toString() + "]";
	}
}
