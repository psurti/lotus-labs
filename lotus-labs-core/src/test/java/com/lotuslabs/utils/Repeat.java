package com.lotuslabs.utils;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * Repeat annotation to be applied at the test method level to indicate how many time the test method to be executed
 * repeatedly.
 * </p>
 * <p>
 * Usage:
 *
 * <pre>
 * &#64;Rule
 * public RepeatRule repeatRule = new RepeatRule();
 *
 * &#64;Test
 * &#64;Repeat(10)
 * public void testRepeatTenTimes()
 * {
 *     PrintStream err = System.err;
 *     err.println(Math.random());
 * }
 * </pre>
 * </p>
 *
 * @since 1.0
 **/
@Retention(RUNTIME)
@Target(METHOD)
public @interface Repeat
{
	/**
	 * <p>
	 * Indicate the number of time a test method should be repeated.
	 * </p>
	 *
	 * @return number of repeats
	 **/
	int value();
}
