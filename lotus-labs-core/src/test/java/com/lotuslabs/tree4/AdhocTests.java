package com.lotuslabs.tree4;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "example", mixinStandardHelpOptions = true, version = "Picocli example 3.0")
public class AdhocTests implements Runnable {
	@Option(names = { "-v", "--verbose" }, description = "Verbose mode. Helpful for troubleshooting. " +
			"Multiple -v options increase the verbosity.")
	private boolean[] verbose = new boolean[0];

	@Parameters(arity = "1..*", paramLabel = "FILE", description = "File(s) to process.")
	private File[] inputFiles;

	@Override
	public void run() {
		if (verbose.length > 0) {
			System.out.println(inputFiles.length + " files to process...");
		}
		if (verbose.length > 1) {
			for (File f : inputFiles) {
				System.out.println(f.getAbsolutePath());
			}
		}
	}

	public static void main(String[] args) {
		Random rdm = new Random();
		Map<Integer,AtomicInteger> dist = new TreeMap<>();
		for (int i = 0; i < 1000; i++) {
			int val = rdm.nextInt(1);
			if (val == 2) val = 500;
			if (val == 1 || val == 3 ) val = 200;
			if (val == 0 || val == 4 ) val = 0;
			AtomicInteger atom = dist.get(val);
			if (atom == null) {
				atom = new AtomicInteger(1);
				dist.put(val, atom);
			} else {
				atom.incrementAndGet();
			}
		}
		for ( Map.Entry<Integer,AtomicInteger> entry : dist.entrySet() ) {
			System.out.println( entry.getKey() + ":" + entry.getValue());
		}
		System.exit(0);
		int[] a = new int[] { 10, 3, 70, 4, 1, 2, 20};
		for (int i = 0; i < a.length; i++)
			System.out.print(a[i] + ",");
		System.out.println();
		parallelMergeSort(a, 2);
		for (int i = 0; i < a.length; i++)
			System.out.print(a[i] + ",");
		System.out.println();
		//		CommandLine.run(new AdhocTests(), System.out, args);
	}

	public static void parallelMergeSort(int[] a, int NUM_THREADS)
	{
		if(NUM_THREADS <= 1)
		{
			mergeSort(a);
			return;
		}

		int mid = a.length / 2;

		int[] left = Arrays.copyOfRange(a, 0, mid);
		int[] right = Arrays.copyOfRange(a, mid, a.length);

		Thread leftSorter = mergeSortThread(left, NUM_THREADS);
		Thread rightSorter = mergeSortThread(right, NUM_THREADS);

		leftSorter.start();
		rightSorter.start();

		try {
			leftSorter.join();
			rightSorter.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		merge(left, right, a);
	}

	private static Thread mergeSortThread(int[] a, int NUM_THREADS)
	{
		return new Thread()
		{
			@Override
			public void run()
			{
				parallelMergeSort(a, NUM_THREADS / 2);
			}
		};
	}

	public static void mergeSort(int[] a)
	{
		if(a.length <= 1) return;

		int mid = a.length / 2;

		int[] left = Arrays.copyOfRange(a, 0, mid);
		int[] right = Arrays.copyOfRange(a, mid, a.length);

		mergeSort(left);
		mergeSort(right);

		merge(left, right, a);
	}


	private static void merge(int[] a, int[] b, int[] r)
	{
		int i = 0, j = 0, k = 0;
		while(i < a.length && j < b.length)
		{
			if(a[i] < b[j])
				r[k++] = a[i++];
			else
				r[k++] = b[j++];
		}

		while(i < a.length)
			r[k++] = a[i++];

		while(j < b.length)
			r[k++] = b[j++];
	}}
