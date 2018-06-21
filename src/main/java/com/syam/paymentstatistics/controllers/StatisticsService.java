package com.syam.paymentstatistics.controllers;

import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.syam.paymentstatistics.jpa.TransactionAmount;
import com.syam.paymentstatistics.jpa.TransactionAmountRepository;
import com.syam.paymentstatistics.pojo.StatisticsData;
import com.syam.paymentstatistics.pojo.StatisticsDataResponse;
import com.syam.paymentstatistics.pojo.TransactionRequest;
import com.syam.paymentstatistics.utils.Logger;

@Service
public class StatisticsService {

	private static final StatisticsData STATISTICS_DATA = new StatisticsData();

	private static final ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(1);

	private static volatile ScheduledFuture<?> SCHEDULED_TASK_FUTURE;

	private static TreeMap<Long, TransactionRequest> REMAINING_REQUESTS = new TreeMap<Long, TransactionRequest>();

	// 1. If we read the STATISTICS_DATA when a write operation is going on, the
	// values in the object may be corrupted with only sum field getting updated but
	// not the subsequent fields. So it is important not to read when there is a
	// write operation.
	// 2. Having multiple writes simultaneously can result in data loss as the
	// latest write can override the values instead of adding to it. So it is
	// important to maintain mutual exclusive lock on writes.
	// 3. Multiple reads can happen simultaneously when there is write happening as
	// it wont be modifying the object.
	// Hence ReentrantReadWriteLock is the right lock for this use case.
	private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();

	@Autowired
	private TransactionAmountRepository transactionRepository;

	public void registerTransaction(TransactionRequest transactionRequest) {
		Logger.log("Registering request : " + transactionRequest.toString());
		long requestTime = System.currentTimeMillis();

		// Validate request
		transactionRequest.validate();

		// If test mode is true, save the transaction in db for tracking
		if (Boolean.TRUE.equals(transactionRequest.getTest())) {
			TransactionAmount entry = new TransactionAmount(transactionRequest, requestTime);
			transactionRepository.save(entry);
		}

		// Apply the transaction amount on the statistics data object
		applyTransaction(transactionRequest);
		Logger.log("Registering done for request : " + transactionRequest.toString());
	}

	public StatisticsDataResponse getStatistics() {
		READ_WRITE_LOCK.readLock().lock();
		StatisticsDataResponse statisticsData = new StatisticsDataResponse(STATISTICS_DATA);
		READ_WRITE_LOCK.readLock().unlock();
		return statisticsData;
	}

	private static void applyTransaction(TransactionRequest transactionRequest) {
		Logger.log("Applying transaction : " + transactionRequest.getAmount());
		// Update statistics object with the transaction values
		READ_WRITE_LOCK.writeLock().lock();
		STATISTICS_DATA.addTranscation(transactionRequest.getAmount());
		READ_WRITE_LOCK.writeLock().unlock();

		// Schedule a task in future (60s after the time stamp field) for deducting the
		// amount from STATISTICS_DATA object
		scheduleTaskToRemoveTranscation(transactionRequest);
		Logger.log("Applying transaction done : " + transactionRequest.getAmount());
	}

	private static void removeTransaction(TransactionRequest transactionRequest) {
		// Update statistics object with the transaction values
		READ_WRITE_LOCK.writeLock().lock();
		STATISTICS_DATA.removeTranscation(transactionRequest.getAmount());
		READ_WRITE_LOCK.writeLock().unlock();
	}

	private static synchronized void scheduleTaskToRemoveTranscation(TransactionRequest transactionRequest) {
		if (SCHEDULED_TASK_FUTURE == null) {
			Logger.log("Schedule task is null : " + transactionRequest.getAmount());
			try {
				REMAINING_REQUESTS.put(transactionRequest.getExpiryTime(), transactionRequest);
				Logger.log(
						"Scheduling time delay : " + (transactionRequest.getExpiryTime() - System.currentTimeMillis()));
				SCHEDULED_TASK_FUTURE = SCHEDULED_POOL.schedule(new WorkerThread(),
						transactionRequest.getExpiryTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			} catch (RejectedExecutionException e) {
				removeTransaction(transactionRequest);
			}
		} else {
			Logger.log("Schedule task is not null : " + transactionRequest.getAmount());
			if (!SCHEDULED_TASK_FUTURE.cancel(false)) {
				Logger.log("Schedule task is canceled : " + transactionRequest.getAmount());
				scheduleTaskToRemoveTranscation(transactionRequest);
			}

			// Check if tasks are remaining
			TransactionRequest entryToAdd = REMAINING_REQUESTS.get(transactionRequest.getExpiryTime());
			if (entryToAdd != null) {
				Logger.log("Request Entry already exists : " + transactionRequest.getAmount());
				entryToAdd.addTransaction(transactionRequest);
			} else {
				Logger.log("Request Entry does not exists : " + transactionRequest.getAmount());
				entryToAdd = transactionRequest;
			}

			REMAINING_REQUESTS.put(entryToAdd.getExpiryTime(), entryToAdd);

			SCHEDULED_TASK_FUTURE = null;
			scheduleTaskToRemoveTranscation(REMAINING_REQUESTS.firstEntry().getValue());
		}
	}

	public static class WorkerThread implements Runnable {

		public WorkerThread() {
			super();
		}

		@Override
		public void run() {
			if (!REMAINING_REQUESTS.isEmpty()) {
				TransactionRequest request = REMAINING_REQUESTS.pollFirstEntry().getValue();
				Logger.log("Removing transaction : " + request.toString());
				removeTransaction(request);

				// Schedule next if present
				if (!REMAINING_REQUESTS.isEmpty()) {
					TransactionRequest nextRequest = REMAINING_REQUESTS.firstEntry().getValue();
					Logger.log("Next request found transaction : " + nextRequest.toString());
					if (nextRequest.getExpiryTime() < System.currentTimeMillis()) {
						run();
						return;
					}

					try {
						WorkerThread newRunnable = new WorkerThread();
						SCHEDULED_TASK_FUTURE = SCHEDULED_POOL.schedule(newRunnable,
								nextRequest.getExpiryTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
					} catch (RejectedExecutionException e) {
						// Rejection happens if the scheduled time is in the past.
						// TODO : Check if there any other cases when the scheduling fails apart from
						// the above case
						Logger.log("Task rejected as it is in the past");
						run();
						return;
					}
				} else {
					Logger.log("No next request found");
				}
			}

			if (REMAINING_REQUESTS.isEmpty()) {
				SCHEDULED_TASK_FUTURE = null;
			}
		}
	}

}
