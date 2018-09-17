package com.lotuslabs.eip.processor.resequencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lotuslabs.log.Log4J2;

import smile.data.parser.IOUtils;

public class LongResequencerTest {
	// initialize log4j2
	static {
		Log4J2.init();
	}

	private static Logger logger = LoggerFactory.getLogger(LongResequencerTest.class);

	enum IntervalType {
		FIXED_BATCH(3),
		FIXED_TIME_NS(10000),
		FIXED_TIMER_MS(10);
		private int val = 0;
		IntervalType(int val) {
			this.val = val;
		}
		public int value() {
			return this.val;
		}
	}

	enum State {
		PRODUCER_STARTED,
		PRODUCER_COMPLETED,
		CONSUMER_STARTED,
		CONSUMER_COMPLETED;
	}

	//processor
	private LongResequencer<String> r;
	//input
	private List<String> readLines;

	//consumer stuff
	private AtomicInteger consumed;
	final Consumer<Long,String> c = new Consumer<Long, String>() {
		@Override
		public void accept(Long k, String v) {
			consumed.incrementAndGet();
			if (consumerDelayMS == 0)
				return;

			try {
				Thread.sleep(consumerDelayMS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	//parameters
	private IntervalType it;
	private int softLimit;
	private int hardLimit;
	private boolean collectMetrics;
	private long consumerDelayMS;

	public LongResequencerTest() {
	}

	@Before
	public void setUp() throws IOException {
		it = IntervalType.FIXED_BATCH;
		softLimit = 100;
		hardLimit = 0;
		consumerDelayMS = 0;
		consumed = new AtomicInteger(0);

		r = new LongResequencer<>(softLimit, hardLimit);
		//		collectMetrics = r.enableMetrics() != null;

		InputStream resourceAsStream = new FileInputStream(
				System.getProperty("user.dir") +
				File.separator + "data" +
				File.separator + "cmx_tran_id.log");

		readLines = IOUtils.readLines(resourceAsStream);
		assert !readLines.isEmpty();
	}

	private void startTest(String name) {
		String val = String.format("%0" + 40 + "d", 0).replace("0", "=");
		logger.info( val );
		logger.info(String.format("%10s", name));
		logger.info("intervalType: " + it.name() + " of "  + it.value());
		logger.info("softLimit=" + softLimit);
		logger.info("hardLimit=" + hardLimit);
		logger.info("consumerDelay(ms)=" + consumerDelayMS);
		logger.info("collectMetrics=" + collectMetrics);
		logger.info( val );
	}

	@Test
	public void testConsumeFixedNanoTimeDiff() throws IOException {
		it = IntervalType.FIXED_TIME_NS;
		startTest("Consume by FIXED TIME(nano) - 1 Thread");
		testConsume();
	}

	@Test
	public void testConsumeFixedBatch() throws IOException {
		it = IntervalType.FIXED_BATCH;
		startTest("Consume By FIXED BATCH - 1 Thread");
		testConsume();
	}


	private void testConsume() throws IOException {
		long start = System.currentTimeMillis();
		int i = 0;
		long timer = System.nanoTime();

		for (String line : readLines) {
			String[] pairs = line.split(":");
			String value = pairs[1].trim();
			Long key = Long.valueOf(value);
			r.put(key, value);
			// If elementInterval == 0 then check based on timer
			if (it == IntervalType.FIXED_TIME_NS && System.nanoTime()-timer >= it.value()) {
				if (r.isPending()) r.consume(c);
				timer = System.nanoTime();
			}
			// If elementInterval > 0 then check based element.size
			if (it == IntervalType.FIXED_BATCH && (i % it.value() == 0)) {
				if (r.isPending()) r.consume(c);
			}
			i++;
		}
		r.flush(c);
		long etime = (System.currentTimeMillis()-start);
		r.dumpStats(System.out);
		timeTaken(etime);
		logger.info( "Consumed:" + consumed.get());
	}

	@Test
	public void testConsumeTimer() throws IOException, InterruptedException {
		it = IntervalType.FIXED_TIMER_MS;
		startTest("Consume with FIXED TIMER(ms) Task Thread");

		Timer t = new Timer();
		final AtomicReference<State> state = new AtomicReference<>(State.PRODUCER_STARTED);
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				if (logger.isDebugEnabled())
					logger.debug("---TIMER---");
				if (r.isPending()) {
					if (state.get() == State.PRODUCER_STARTED) {
						r.consume(c);
					} else {
						r.flush(c);
						state.set(State.CONSUMER_COMPLETED);
					}
				} else {
					if (state.get() == State.PRODUCER_COMPLETED) {
						state.set(State.CONSUMER_COMPLETED);
					}
				}
			}
		};
		t.schedule(task, 0, it.val);

		long start = System.currentTimeMillis();
		for (String line : readLines) {
			String[] pairs = line.split(":");
			String value = pairs[1].trim();
			Long key = Long.valueOf(value);
			r.put(key, value);
		}
		state.set(State.PRODUCER_COMPLETED);
		while (state.get() != State.CONSUMER_COMPLETED) {
			Thread.sleep(10);
		}
		long etime = (System.currentTimeMillis()-start);
		t.cancel();
		r.dumpStats(System.out);
		timeTaken(etime);
		logger.info( "Consumed:" + consumed.get());
	}

	@Test
	/*
	 * Two separate threads - one adds events and the other
	 * retrieves every n nano-seconds.
	 */
	public void testConsumeNanoTimer() throws IOException, InterruptedException {
		it = IntervalType.FIXED_TIME_NS;
		startTest("Consume with FIXED TIMER(nano) Thread");
		final AtomicReference<State> state = new AtomicReference<>(State.PRODUCER_STARTED);
		final AtomicLong timer = new AtomicLong(System.nanoTime());
		Runnable task = new Runnable() {
			@Override
			public void run() {
				while (state.get() == State.PRODUCER_STARTED) {
					long startTimer = System.nanoTime();
					long delta = startTimer-timer.get();
					if (delta < it.val)
						continue;
					if (logger.isDebugEnabled()) {
						logger.debug( "---TIMER--- delta:" + delta + " pending=" + r.isPending());
					}
					{
						if (r.isPending()) {
							r.consume(c);
						}
						timer.set(startTimer);
					}
				}

				r.flush(c);
			}
		};
		Thread thr = new Thread(task);
		thr.start();
		long start = System.currentTimeMillis();
		for (String line : readLines) {
			String[] pairs = line.split(":");
			String value = pairs[1].trim();
			Long key = Long.valueOf(value);
			{
				r.put(key, value);
			}
		}
		state.set(State.PRODUCER_COMPLETED);
		thr.join();
		long etime = (System.currentTimeMillis()-start);
		r.dumpStats(System.out);
		timeTaken(etime);
		logger.info( "Consumed:" + consumed.get());
	}

	private void timeTaken(long durationInMillis) {
		long millis = durationInMillis % 1000;
		long second = (durationInMillis / 1000) % 60;
		long minute = (durationInMillis / (1000 * 60)) % 60;
		long hour = (durationInMillis / (1000 * 60 * 60)) % 24;
		String time = String.format("%02dh:%02dm:%02d.%ds", hour, minute, second, millis);
		logger.info("time taken:" + time);
	}

	@After
	public void tearDown() {
		logger.info("\n\n");
	}
}
