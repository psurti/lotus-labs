package com.lotuslabs.eip.processor.resequencer;

import java.util.Comparator;

public class LongResequencer<V> extends Resequencer<Long, V>{

	public LongResequencer() {
		super(100, 0, DEFAULT_COMPARATOR, DEFAULT_EXPECTOR);
	}


	public LongResequencer(int softLimit, int hardLimit) {
		super(softLimit, hardLimit, DEFAULT_COMPARATOR, DEFAULT_EXPECTOR);
	}

	private static final Comparator<Long> DEFAULT_COMPARATOR = new Comparator<Long>() {
		@Override
		public int compare(Long o1, Long o2) {
			/*
			 * (o1==o2):-1,(o1 < o2):-1 ,(o1 > o2):+1
			 */
			return Long.compare(o1, o2);
		}
	};

	private static final SequenceSupplier<Long> DEFAULT_EXPECTOR = new SequenceSupplier<Long>() {
		@Override
		public Long get(Long previous) {
			//initial value
			if (previous == null) {
				return Long.valueOf(0);
			}
			return Long.valueOf(previous.longValue()+1);
		}
	};
};