package com.syam.paymentstatistics.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	private static final ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(100);

	private static volatile ScheduledFuture<?> SCHEDULED_TASK_FUTURE;

	private static TreeMap<Long, List<TransactionRequest>> REMAINING_REQUESTS = new TreeMap<Long, List<TransactionRequest>>();

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
	private static final ReadWriteLock STATISTICS_DATA_LOCK = new ReentrantReadWriteLock();

	private static final ReadWriteLock REMAINING_REQUESTS_LOCK = new ReentrantReadWriteLock();

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
		STATISTICS_DATA_LOCK.readLock().lock();
		StatisticsDataResponse statisticsData = new StatisticsDataResponse(STATISTICS_DATA);
		STATISTICS_DATA_LOCK.readLock().unlock();
		return statisticsData;
	}

	private static void applyTransaction(TransactionRequest transactionRequest) {
		Logger.log("Applying transaction : " + transactionRequest.getAmount());
		// Update statistics object with the transaction values
		STATISTICS_DATA_LOCK.writeLock().lock();
		STATISTICS_DATA.addTranscation(transactionRequest.getAmount());
		STATISTICS_DATA_LOCK.writeLock().unlock();

		// Schedule a task in future (60s after the time stamp field) for deducting the
		// amount from STATISTICS_DATA object
		addNewRequestsToRemoveTranscation(transactionRequest);
		scheduleTaskToRemoveTranscationIfRequired();
		Logger.log("Applying transaction done : " + transactionRequest.getAmount());
	}

	private static void removeTransaction(List<TransactionRequest> requests) {
		// Update statistics object with the transaction values
		STATISTICS_DATA_LOCK.writeLock().lock();
		requests.forEach(a -> STATISTICS_DATA.removeTranscation(a));
		STATISTICS_DATA_LOCK.writeLock().unlock();
	}

	private static void addNewRequestsToRemoveTranscation(TransactionRequest request) {
		// Add new requests
		REMAINING_REQUESTS_LOCK.writeLock().lock();
		List<TransactionRequest> existingRequests = new ArrayList<>();
		if (REMAINING_REQUESTS.containsKey(Long.valueOf(request.getExpiryTime()))) {
			existingRequests = REMAINING_REQUESTS.get(Long.valueOf(request.getExpiryTime()));
		}
		existingRequests.add(request);
		REMAINING_REQUESTS.put(Long.valueOf(request.getExpiryTime()), existingRequests);
		REMAINING_REQUESTS_LOCK.writeLock().unlock();
	}

	private static synchronized void scheduleTaskToRemoveTranscationIfRequired() {
		if (SCHEDULED_TASK_FUTURE == null) {
			REMAINING_REQUESTS_LOCK.writeLock().lock();
			try {
				SCHEDULED_TASK_FUTURE = SCHEDULED_POOL.schedule(new WorkerThread(),
						REMAINING_REQUESTS.firstEntry().getKey() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				REMAINING_REQUESTS_LOCK.writeLock().unlock();
			} catch (RejectedExecutionException e) {
				removeTransaction(REMAINING_REQUESTS.firstEntry().getValue());

				// Schedule for remaining entries
				if (!REMAINING_REQUESTS.isEmpty()) {
					REMAINING_REQUESTS_LOCK.writeLock().unlock();
					scheduleTaskToRemoveTranscationIfRequired();
					return;
				}
			}
		}
	}

	public static class WorkerThread implements Runnable {

		public WorkerThread() {
			super();
		}

		@Override
		public void run() {
			REMAINING_REQUESTS_LOCK.writeLock().lock();
			if (!REMAINING_REQUESTS.isEmpty()) {
				List<TransactionRequest> requests = REMAINING_REQUESTS.pollFirstEntry().getValue();
				Logger.log("Removing transaction : " + requests.size() + " exipiry time:"
						+ requests.get(0).getExpiryTime() + " requests:" + requests.toString());
				removeTransaction(requests);

				// Schedule next if present
				if (!REMAINING_REQUESTS.isEmpty()) {
					try {
						WorkerThread newRunnable = new WorkerThread();
						SCHEDULED_TASK_FUTURE = SCHEDULED_POOL.schedule(newRunnable,
								REMAINING_REQUESTS.firstKey() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
					} catch (RejectedExecutionException e) {
						// Rejection happens if the scheduled time is in the past.
						// TODO : Check if there any other cases when the scheduling fails apart from
						// the above case
						Logger.log("Task rejected as it is in the past");
						REMAINING_REQUESTS_LOCK.writeLock().unlock();
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
			REMAINING_REQUESTS_LOCK.writeLock().unlock();
		}
	}

}
