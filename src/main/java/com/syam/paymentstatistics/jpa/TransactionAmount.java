package com.syam.paymentstatistics.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.syam.paymentstatistics.pojo.TransactionRequest;

@Entity
@Table(name = "transactionamount")
public class TransactionAmount {
	@org.springframework.data.annotation.Id // For spring data
	@Id // for JPA
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	private long created = System.currentTimeMillis();
	private double amount;
	private long time_stamp;

	public TransactionAmount() {
		super();
	}

	public TransactionAmount(TransactionRequest request, long requestTime) {
		super();
		this.amount = request.getAmount();
		this.time_stamp = request.getTimestamp();
		this.created = requestTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public long getTime_stamp() {
		return time_stamp;
	}

	public void setTime_stamp(long time_stamp) {
		this.time_stamp = time_stamp;
	}

	@Override
	public String toString() {
		return "TransactionAmount [id=" + id + ", created=" + created + ", amount=" + amount + ", time_stamp="
				+ time_stamp + ", toString()=" + super.toString() + "]";
	}
}
