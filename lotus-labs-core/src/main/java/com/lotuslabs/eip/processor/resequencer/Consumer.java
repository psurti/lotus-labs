package com.lotuslabs.eip.processor.resequencer;

/**
 * Consumer that externally process
 *
 * @author psurti
 *
 * @param <K>
 * @param <V>
 */
public interface Consumer<K,V>	{
	/**
	 * accept the key and value
	 * @param k
	 * @param v
	 */
	void accept(K k, V v);

}