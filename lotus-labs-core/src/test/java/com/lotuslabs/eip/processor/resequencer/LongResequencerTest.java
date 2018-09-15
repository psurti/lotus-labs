package com.lotuslabs.eip.processor.resequencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lotuslabs.log.Log4J2;

import smile.data.parser.IOUtils;

public class LongResequencerTest {

	enum IntervalType {
		BY_FIXED_BATCH(3),
		BY_TIMER_NS(50000);
		private int val = 0;
		IntervalType(int val) {
			this.val = val;
		}
		public int value() {
			return this.val;
		}
	}

	private LongResequencer<String> r;
	private List<String> readLines;
	private IntervalType it;

	public LongResequencerTest() throws FileNotFoundException, IOException {
		Log4J2.init();
	}

	@Before
	public void setUp() throws IOException {
		it = IntervalType.BY_FIXED_BATCH;
		int softLimit = 1000;
		int hardLimit = 1000;
		System.out.println("intervalType: " + it.name() + " of "  + it.value());
		System.out.println("softLimit=" + softLimit);
		System.out.println("hardLimit=" + hardLimit);
		System.out.println("-------------------------");
		r = new LongResequencer<>(softLimit, hardLimit);
		r.enableMetrics();

		InputStream resourceAsStream = new FileInputStream(
				System.getProperty("user.dir") +
				File.separator + "data" +
				File.separator + "cmx_tran_id.log");

		readLines = IOUtils.readLines(resourceAsStream);
		assert !readLines.isEmpty();
	}

	@Test
	public void testConsume() throws IOException {
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
				if (r.isPending()) r.consume(null);
				timer = System.nanoTime();
			}
			// If elementInterval > 0 then check based element.size
			if (it == IntervalType.BY_FIXED_BATCH && (i % it.value() == 0)) {
				if (r.isPending()) r.consume(null);
			}
			i++;
		}

		if (r.isPending()) {
			r.consume(null);
		}
		long etime = (System.currentTimeMillis()-start);
		r.dumpStats(System.out);
		System.out.println("time taken(ms):" + etime);

	}

	@Test
	public void testConsumeTimer() throws IOException, InterruptedException {
		long start = System.currentTimeMillis();
		Timer t = new Timer();
		final AtomicInteger emptyIterations = new AtomicInteger(0);
		final AtomicInteger consumed = new AtomicInteger(0);

		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				System.out.println( "---TIMER---");
				if (r.isPending()) {
					r.consume(new Consumer<Long, String>() {

						@Override
						public void accept(Long k, String v) {
							consumed.incrementAndGet();
						}
					});
				} else {
					emptyIterations.incrementAndGet();
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

		while (emptyIterations.get() < 5 ) {
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
	 * This test is not working correctly whe hard limit=100
	 */
	public void testConsumeNanoTimer() throws IOException, InterruptedException {
		System.in.read();
		long start = System.currentTimeMillis();
		final AtomicInteger emptyIterations = new AtomicInteger(0);
		final AtomicInteger consumed = new AtomicInteger(0);
		final AtomicLong timer = new AtomicLong(System.nanoTime());
		final Consumer<Long,String> c = new Consumer<Long, String>() {
			@Override
			public void accept(Long k, String v) {
				consumed.incrementAndGet();
			}
		};
		Runnable task = new Runnable() {
			@Override
			public void run() {
				while (emptyIterations.get() < 1) {
					long startTimer = System.nanoTime();
					long delta = startTimer-timer.get();
					if (delta < 10_000)
						continue;
					System.out.println( "---TIMER--- delta:" + delta + " pending=" + r.isPending());
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

	}
}
