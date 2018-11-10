package com.lotuslabs.eip.processor.resequencer;

/**
 * Interface that
 *
 */
public interface SequenceSupplier<K> {

	/**
	 * Returns the next
	 * @param current
	 * @return
	 */
	public K get(K current);

}