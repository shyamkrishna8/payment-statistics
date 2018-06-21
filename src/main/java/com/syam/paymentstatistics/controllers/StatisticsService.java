package com.syam.paymentstatistics.controllers;

import java.util.ArrayList;
import java.util.Iterator;
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

import com.google.common.util.concurrent.AtomicDouble;
import com.syam.paymentstatistics.jpa.TransactionAmount;
import com.syam.paymentstatistics.jpa.TransactionAmountRepository;
import com.syam.paymentstatistics.pojo.StatisticsData;
import com.syam.paymentstatistics.pojo.StatisticsDataResponse;
import com.syam.paymentstatistics.pojo.TransactionRequest;
import com.syam.paymentstatistics.utils.Logger;

@Service
public class StatisticsService {

	private static final ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(10);

	// STATISTICS_DATA_LOCK ensures the thread safety for read/write for this
	// object.
	private static final StatisticsData STATISTICS_DATA = new StatisticsData();

	// SCHEDULED_TASK_FUTURE holds the reference to the next scheduled task
	private static volatile ScheduledFuture<?> SCHEDULED_TASK_FUTURE;

	// Although tree map is not thread-safe, we are using external locks to ensure
	// no two threads read/write to this object. REMAINING_REQUESTS_LOCK variable
	// handles the locks for this map
	private static TreeMap<Long, List<TransactionRequest>> REMAINING_REQUESTS = new TreeMap<Long, List<TransactionRequest>>();

	// Although tree map is not thread-safe, we are using external locks to ensure
	// no two threads read/write to this object.
	private static TreeMap<Double, List<Long>> ELIGIBLE_MIN_MAX_MAP = new TreeMap<Double, List<Long>>();

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

	// This lock is used to ensure thread safety for REMAINING_REQUESTS map. We are
	// only using the Write lock every where for now.
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
		Logger.log("Registering done for request : " + transactionRequest.toString() + " object : "
				+ STATISTICS_DATA.toString());
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
		// Update sum and avg
		STATISTICS_DATA.addTranscation(transactionRequest.getAmount());

		// Update min and max
		if (STATISTICS_DATA.getMin() == null) {
			STATISTICS_DATA.setMin(new AtomicDouble(transactionRequest.getAmount()));
			addTrancationEntryToMinMaxMap(transactionRequest);
		} else {
			double existingMin = STATISTICS_DATA.getMin().doubleValue();
			if (transactionRequest.getAmount() < existingMin) {
				STATISTICS_DATA.setMin(new AtomicDouble(transactionRequest.getAmount()));
				addTrancationEntryToMinMaxMap(transactionRequest);
			} else {
				// Check if it is required to store the key. We don't need to store this entry
				// if there is already a minimum (than the transaction amount) element existing
				// after the transaction time stamp
				Iterator<Double> minIterator = ELIGIBLE_MIN_MAX_MAP.navigableKeySet().iterator();
				boolean addTranscationForMin = true;
				while (minIterator.hasNext()) {
					Double value = minIterator.next();
					if (value > transactionRequest.getAmount()) {
						break;
					}

					if (ELIGIBLE_MIN_MAX_MAP.get(value).stream()
							.anyMatch(a -> a.longValue() > transactionRequest.getTimestamp())) {
						addTranscationForMin = false;
						break;
					}
				}

				if (addTranscationForMin) {
					addTrancationEntryToMinMaxMap(transactionRequest);
				}
			}
		}

		if (STATISTICS_DATA.getMax() == null) {
			STATISTICS_DATA.setMax(new AtomicDouble(transactionRequest.getAmount()));
			addTrancationEntryToMinMaxMap(transactionRequest);
		} else {
			double existingMax = STATISTICS_DATA.getMax().doubleValue();
			if (transactionRequest.getAmount() > existingMax) {
				STATISTICS_DATA.setMax(new AtomicDouble(transactionRequest.getAmount()));
				addTrancationEntryToMinMaxMap(transactionRequest);
			} else {
				// Check if it is required to store the key. We dont need to store this entry if
				// there is already a
				Iterator<Double> maxIterator = ELIGIBLE_MIN_MAX_MAP.descendingKeySet().iterator();
				boolean addTranscationForMax = true;
				while (maxIterator.hasNext()) {
					Double value = maxIterator.next();
					if (value < transactionRequest.getAmount()) {
						break;
					}

					if (ELIGIBLE_MIN_MAX_MAP.get(value).stream()
							.anyMatch(a -> a.longValue() > transactionRequest.getTimestamp())) {
						addTranscationForMax = false;
						break;
					}
				}

				if (addTranscationForMax) {
					addTrancationEntryToMinMaxMap(transactionRequest);
				}
			}
		}

		STATISTICS_DATA_LOCK.writeLock().unlock();

		// Schedule a task in future (60s after the time stamp field) for deducting the
		// amount from STATISTICS_DATA object
		addNewRequestsToRemoveTranscation(transactionRequest);
		scheduleTaskToRemoveTranscationIfRequired();
		Logger.log("Applying transaction done : " + transactionRequest.getAmount());
	}

	private static void addTrancationEntryToMinMaxMap(TransactionRequest transactionRequest) {
		List<Long> timestamps = new ArrayList<>();
		if (ELIGIBLE_MIN_MAX_MAP.get(transactionRequest.getAmount()) != null
				&& !ELIGIBLE_MIN_MAX_MAP.get(transactionRequest.getAmount()).isEmpty()) {
			timestamps = ELIGIBLE_MIN_MAX_MAP.get(transactionRequest.getAmount());
		}
		timestamps.add(Long.valueOf(transactionRequest.getTimestamp()));
		ELIGIBLE_MIN_MAX_MAP.put(transactionRequest.getAmount(), timestamps);
	}

	private static void removeTrancationEntryToMinMaxMap(TransactionRequest transactionRequest) {
		List<Long> timestamps = ELIGIBLE_MIN_MAX_MAP.get(transactionRequest.getAmount());
		if (timestamps != null && !timestamps.isEmpty()
				&& timestamps.contains(Long.valueOf(transactionRequest.getTimestamp()))) {
			timestamps.removeIf(a -> a.equals(transactionRequest.getTimestamp()));
			if (timestamps.isEmpty()) {
				ELIGIBLE_MIN_MAX_MAP.remove(transactionRequest.getAmount());
			} else {
				ELIGIBLE_MIN_MAX_MAP.put(transactionRequest.getAmount(), timestamps);
			}
		}

		if (ELIGIBLE_MIN_MAX_MAP.isEmpty()) {
			STATISTICS_DATA.resetValues();
		}
	}

	private static void removeTransaction(List<TransactionRequest> requests) {
		// Update statistics object with the transaction values
		STATISTICS_DATA_LOCK.writeLock().lock();
		requests.forEach(a -> {
			STATISTICS_DATA.removeTranscation(a);
			removeTrancationEntryToMinMaxMap(a);
		});
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
