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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lotuslabs.log.Log4J2;

import smile.data.parser.IOUtils;

public class LongResequencerTest {
	// initialize log4j2
	static {
		Log4J2.init();
	}

	private static final Logger logger = LogManager.getLogger(LongResequencerTest.class.getSimpleName());

	enum IntervalType {
		BY_FIXED_BATCH(3),
		BY_TIMER_NS(10000);
		private int val = 0;
		IntervalType(int val) {
			this.val = val;
		}
		public int value() {
			return this.val;
		}
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
		}
	};
	//parameters
	private IntervalType it;
	private int softLimit;
	private int hardLimit;
	private boolean collectMetrics;

	public LongResequencerTest() {
	}

	@Before
	public void setUp() throws IOException {
		it = IntervalType.BY_FIXED_BATCH;
		softLimit = 1000;
		hardLimit = 1000;
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
		System.out.println( val );
		System.out.format("%10s\n", name);
		System.out.println("intervalType: " + it.name() + " of "  + it.value());
		System.out.println("softLimit=" + softLimit);
		System.out.println("hardLimit=" + hardLimit);
		System.out.println("collectMetrics=" + collectMetrics);
		System.out.println( val );
	}

	@Test
	public void testConsumeNanoTimeDiff() throws IOException {
		it = IntervalType.BY_TIMER_NS;
		startTest("Consume by TimeDiff(nano) with single Thread");
		testConsume();
	}

	@Test
	public void testConsumeFixedBatch() throws IOException {
		it = IntervalType.BY_FIXED_BATCH;
		startTest("Consume By FIXED BATCH with single Thread");
		testConsume();
	}


	private void testConsume() throws IOException {
		long start = System.currentTimeMillis();
		int i = 0;
		long timer = System.nanoTime();

		for (String line : readLines) {
			String[] pairs = line.split(":");
			String value = pairs[1].trim();
			Long key = new Long(Long.valueOf(value));
			r.put(key, value);
			// If elementInterval == 0 then check based on timer
			if (it == IntervalType.BY_TIMER_NS && System.nanoTime()-timer >= it.value()) {
				if (r.isPending()) r.consume(c);
				timer = System.nanoTime();
			}
			// If elementInterval > 0 then check based element.size
			if (it == IntervalType.BY_FIXED_BATCH && (i % it.value() == 0)) {
				if (r.isPending()) r.consume(c);
			}
			i++;
		}
		r.flush(c);
		long etime = (System.currentTimeMillis()-start);
		r.dumpStats(System.out);
		System.out.println("time taken(ms):" + etime);
		System.out.println( "Consumed:" + consumed.get());
	}

	@Test
	public void testConsumeTimer() throws IOException, InterruptedException {
		startTest("Consume with FIXED TIMER TASK Thread");

		long start = System.currentTimeMillis();
		Timer t = new Timer();
		final AtomicInteger emptyIterations = new AtomicInteger(0);

		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				if (logger.isDebugEnabled())
					logger.debug("---TIMER---");
				if (r.isPending()) {
					if (emptyIterations.get() == 0) {
						r.consume(c);
					} else {
						r.flush(c);
						emptyIterations.set(-1);
					}
				}
			}
		};
		t.schedule(task, 0, 10);
		for (String line : readLines) {
			String[] pairs = line.split(":");
			String value = pairs[1].trim();
			Long key = new Long(Long.valueOf(value));
			r.put(key, value);
		}

		emptyIterations.incrementAndGet();

		while (emptyIterations.get() >= 0) {
			System.out.println( "emptyIter=" + emptyIterations.get());
			Thread.sleep(10);
		}
		t.cancel();
		long etime = (System.currentTimeMillis()-start);
		r.dumpStats(System.out);
		System.out.println("time taken(ms):" + etime);
		System.out.println( "Consumed:" + consumed.get());
	}

	@Test
	/*
	 * Two separate threads - one adds events and the other
	 * retrieves every n nano-seconds.
	 */
	public void testConsumeNanoTimer() throws IOException, InterruptedException {
		startTest("Consume with NANO TIMER Thread");
		long start = System.currentTimeMillis();
		final AtomicInteger emptyIterations = new AtomicInteger(0);
		final AtomicLong timer = new AtomicLong(System.nanoTime());
		Runnable task = new Runnable() {
			@Override
			public void run() {
				while (emptyIterations.get() < 1) {
					long startTimer = System.nanoTime();
					long delta = startTimer-timer.get();
					if (delta < 10_000)
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
		for (String line : readLines) {
			String[] pairs = line.split(":");
			String value = pairs[1].trim();
			Long key = new Long(Long.valueOf(value));
			{
				r.put(key, value);
			}
		}
		emptyIterations.set(1);//completed
		thr.join();
		System.out.println( "empty.iter=" + emptyIterations.get());
		long etime = (System.currentTimeMillis()-start);
		r.dumpStats(System.out);
		System.out.println("time taken(ms):" + etime);
		System.out.println( "Consumed:" + consumed.get());
	}


	@After
	public void tearDown() {
		System.out.println("\n\n");
	}
}
