package com.lotuslabs.eip.processor.resequencer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import smile.data.parser.IOUtils;

public class LongResequencerTest {

	enum IntervalType {
		NUM_ELEM(3),
		NS_TIME(50000);
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

	public LongResequencerTest() {
	}

	@Before
	public void setUp() throws IOException {
		it = IntervalType.NUM_ELEM;
		int softLimit = 100;
		int hardLimit = 0;
		System.out.println("intervalType: " + it.name() + " "  + it.value());
		System.out.println("softLimit=" + softLimit);
		System.out.println("hardLimit=" + hardLimit);
		System.out.println("-------------------------");
		r = new LongResequencer<>(softLimit, hardLimit);

		InputStream resourceAsStream = LongResequencerTest.class.getResourceAsStream("cmx_tran_id.log");
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
			if (it == IntervalType.NS_TIME && System.nanoTime()-timer >= it.value()) {
				if (r.isPending()) r.consume(null);
				timer = System.nanoTime();
			}
			// If elementInterval > 0 then check based element.size
			if (it == IntervalType.NUM_ELEM && (i % it.value() == 0)) {
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


	@After
	public void tearDown() {

	}
}
