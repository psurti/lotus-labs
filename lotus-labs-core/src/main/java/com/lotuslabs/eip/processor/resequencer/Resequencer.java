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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resequence keys in order the Sequence Supplier dictates
 *  _________________________________________________
 * |                              |                  |
 * |                              |                  }
 * |______________________________|__________________|
 * |                              |                  |
 * |<=====Soft Window Size==========>
 * <========================Hard Window Limit=======>|
 * @author psurti
 *
 * @param <K>
 * @param <V>
 */
public class Resequencer<K,V> {

	private static final Logger logger = LoggerFactory.getLogger(Resequencer.class);

	private final Sequence<K, V> seq;
	private final Comparator<K> comparator;
	private final SequenceSupplier<K> expector;
	private final int softLimit;
	private final int hardLimit;
	private K expectKey;
	private List<K> discardKeys;
	private List<K> skippedKeys;
	private List<K> droppedKeys;
	private ConcurrentSkipListMap<Integer,AtomicInteger> histo;
	private int total = 0;
	private int maxSeqSize =0;

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
	}

	/*
	 * Public enable metric collection
	 */
	public Resequencer<K, V> enableMetrics() {
		this.discardKeys = new ArrayList<>();
		this.skippedKeys = new ArrayList<>();
		this.droppedKeys = new ArrayList<>();
		this.histo = new ConcurrentSkipListMap<>();
		return this;
	}

	/**
	 * Dump Stats to the outputstream
	 *
	 * @param os
	 * @throws IOException
	 */
	public void dumpStats(OutputStream os) throws IOException {
		if (this.histo != null) {
			for (Map.Entry<Integer, AtomicInteger> entry : histo.entrySet()) {
				os.write( (entry.getKey() + ":" + entry.getValue() + "\n").getBytes());
			}
		}
		if (this.discardKeys != null) {
			os.write( ("[DISCARD].Keys:" + discardKeys + "\n[DISCARD].Size=" + discardKeys.size() + "\n").getBytes() );
		}
		if (this.skippedKeys != null) {
			os.write( ("[SKIPPED].Keys:" + skippedKeys + "\n[SKIPPED].Size=" + skippedKeys.size() + "\n").getBytes() );
		}
		if (this.droppedKeys != null) {
			os.write( ("[DROPPED].Keys:" + droppedKeys + "\n[DROPPED].Size=" + droppedKeys.size() + "\n").getBytes() );
		}
		os.write( ("Max.Unseq.Size=" + this.maxSeqSize + "\n").getBytes());
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
	private void updateHistogramMetric(int bufSize) {
		if (histo != null) {
			AtomicInteger cnts = histo.get(bufSize);
			if (cnts == null) {
				cnts = new AtomicInteger(1);
				histo.put(bufSize, cnts);
			} else {
				cnts.incrementAndGet();
			}
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
				logger.debug("....Add Old Event[Discard] :" + key + " compare="+compare);
			}
			if (discardKeys != null) this.discardKeys.add(key);
		} else {
			if ((hardLimit == 0) || (hardLimit > 0 && this.seq.size() < hardLimit)) {
				prevValue = this.seq.put(key, value);
				if (logger.isDebugEnabled()) {
					logger.debug( "...Add New Event[Added] :" + key + " compare="+compare);
				}
			} else  {
				if (logger.isDebugEnabled()) {
					logger.debug( "...Crossed Hard Limit[Dropped] :" + key);
				}
				if (droppedKeys != null) this.droppedKeys.add(key);
			}
		}
		return prevValue;
	}

	/**
	 * Consume data base on limits
	 * @param c
	 */
	public void consume(Consumer<K,V> c) {
		consume(c, false);
	}

	/**
	 * Consume data by the Cosumer
	 * @param c
	 */
	private void consume(Consumer<K,V> c, boolean flush) {
		long start = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug( "buffer.size=" + seq.size());
		}
		if (this.histo != null) updateHistogramMetric(this.seq.size());
		boolean loop = true;
		while(loop) {
			maxSeqSize = Math.max(maxSeqSize, seq.size());
			if ((!this.seq.isEmpty()) &&
					this.comparator.compare(this.seq.firstKey(), this.expectKey) < 0) {
				Entry<K, V> oldEntry = this.seq.pollFirstEntry();
				if (logger.isDebugEnabled()) {
					logger.debug("---Old Entry[Removed] :" + oldEntry.getKey());
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
				if (flush && !seq.isEmpty() || seq.size() >= softLimit) {
					Map.Entry<K, V> first = seq.pollFirstEntry();
					if (first != null) {
						if (logger.isDebugEnabled()) {
							logger.debug( "Processed[SkipAt] : " + first.getValue());
						}
						if (this.skippedKeys != null) this.skippedKeys.add(this.expectKey);
						if (c != null)
							c.accept(first.getKey(), first.getValue());
						this.expectKey = this.expector.get(first.getKey());
					}
				} else {
					loop = false;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug( "++consume.time=" + (System.currentTimeMillis()-start));
		}
	}

	/**
	 * Call flush to processing any
	 * pending data
	 *
	 * @param c
	 */
	public void flush(Consumer<K,V> c) {
		consume(c, true);
	}
}
