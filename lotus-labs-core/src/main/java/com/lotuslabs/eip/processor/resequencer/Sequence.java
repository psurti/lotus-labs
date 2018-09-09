package com.lotuslabs.eip.processor.resequencer;

import java.util.concurrent.ConcurrentSkipListMap;

public class Sequence<K,V> extends ConcurrentSkipListMap<K,V> {
	/**
	 * serial uid
	 */
	private static final long serialVersionUID = -6933960225877983633L;
}
