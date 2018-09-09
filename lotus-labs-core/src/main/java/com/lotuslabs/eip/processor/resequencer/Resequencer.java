/*
 * Licensed to surti-labs under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Surti-labs licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.lotuslabs.eip.processor.resequencer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Resequence keys in order the Sequence Supplier dictates
 *
 * @author psurti
 *
 * @param <K>
 * @param <V>
 */
public class Resequencer<K,V> {

	private static final Logger logger = LogManager.getLogger(Resequencer.class.getName());

	private final Sequence<K, V> seq;
	private final Comparator<K> comparator;
	private final SequenceSupplier<K> expector;
	private final int softLimit;
	private final int hardLimit;
	private K expectKey;
	private List<K> ignoredKeys;
	private final ConcurrentSkipListMap<Integer,AtomicInteger> histo;
	private int total = 0;

	/**
	 * Constructor
	 * @param softLimit
	 * @param hardLimit
	 * @param comparator
	 * @param expector
	 */
	public Resequencer(int softLimit, int hardLimit, Comparator<K> comparator, SequenceSupplier<K> expector) {
		this.seq = new Sequence<>();
		this.comparator = comparator;
		this.expector = expector;
		this.hardLimit = hardLimit;
		this.softLimit = (hardLimit > 0) ? hardLimit : softLimit;
		this.expectKey = this.expector.get(null);
		this.ignoredKeys = new ArrayList<>();
		this.histo = new ConcurrentSkipListMap<>();
	}

	/**
	 * Dump Stats to the outputstream
	 *
	 * @param os
	 * @throws IOException
	 */
	public void dumpStats(OutputStream os) throws IOException {
		for (Map.Entry<Integer, AtomicInteger> entry : histo.entrySet()) {
			os.write( (entry.getKey() + ":" + entry.getValue() + "\n").getBytes());
		}
		os.write( ("Ignored ids.size:" + ignoredKeys.size() + "\n" + ignoredKeys + "\n").getBytes() );
		os.write( ("Max.size=" + this.histo.lastKey() + "\n").getBytes());
		os.write( ("Total=" + this.total + "\n").getBytes());
		os.write( ("Pending=" + this.seq.size() +"\n").getBytes());
	}

	/**
	 * Check if any pending work
	 * in the buffer
	 *
	 * @return
	 */
	public boolean isPending() {
		return !this.seq.isEmpty();
	}

	/*
	 * Internal collection of buffer
	 * queue
	 */
	private void checkBufferSize(int bufSize) {
		AtomicInteger cnts = histo.get(bufSize);
		if (cnts == null) {
			cnts = new AtomicInteger(1);
			histo.put(bufSize, cnts);
		} else {
			cnts.incrementAndGet();
		}
	}

	/**
	 * Put Key and Value to the Buffer
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public V put(K key, V value) {
		V prevValue = null;
		if (this.total == 0) {
			this.expectKey = key;
		}
		total++;
		int compare = this.comparator.compare(key, this.expectKey);
		if (compare < 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("....IgnoreOld element :" + key + " compare="+compare);
			}
			ignoredKeys.add(key);
		} else {
			if ((hardLimit == 0) || (hardLimit > 0 && this.seq.size() < hardLimit)) {
				prevValue = this.seq.put(key, value);
				if (logger.isDebugEnabled()) {
					logger.debug( "...Adding element :" + key + " compare="+compare);
				}
			} else  {
				if (logger.isDebugEnabled()) {
					logger.debug( "...Crossed Hard Limit - Dropped:" + key);
				}
			}
		}
		return prevValue;
	}

	/**
	 * Consume data by the Cosumer
	 * @param c
	 */
	public void consume(Consumer<K,V> c) {
		long start = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug( "buffer.size=" + seq.size());
		}
		checkBufferSize(this.seq.size());
		boolean loop = true;
		while(loop) {
			if ((!this.seq.isEmpty()) &&
					this.comparator.compare(this.seq.firstKey(), this.expectKey) < 0) {
				Entry<K, V> oldEntry = this.seq.pollFirstEntry();
				if (logger.isDebugEnabled()) {
					logger.debug("---Removed Old Entry:" + oldEntry.getKey());
				}
				continue;
			}
			V remove = this.seq.remove(this.expectKey);
			if (remove != null) {
				if (c != null) c.accept(this.expectKey, remove);
				this.expectKey = this.expector.get(this.expectKey);
				if (logger.isDebugEnabled()) {
					logger.debug( "Processed : " + remove );
				}
			} else {
				if (seq.size() >= softLimit) {
					Map.Entry<K, V> first = seq.pollFirstEntry();
					if (logger.isDebugEnabled()) {
						logger.debug( "Processed(skip) : " + first.getValue());
					}
					if (c != null)
						c.accept(first.getKey(), first.getValue());
					this.expectKey = this.expector.get(first.getKey());
				} else {
					loop = false;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug( "++consume.time=" + (System.currentTimeMillis()-start));
		}
	}
}
