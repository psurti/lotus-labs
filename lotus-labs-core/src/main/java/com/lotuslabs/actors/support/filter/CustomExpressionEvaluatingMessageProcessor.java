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

package com.lotuslabs.actors.support.filter;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * @author psurti
 *
 */
public class CustomExpressionEvaluatingMessageProcessor<T> extends ExpressionEvaluatingMessageProcessor<T> {

	public CustomExpressionEvaluatingMessageProcessor(Expression expression, Class<T> expectedType) {
		super(expression, expectedType);
	}

	public CustomExpressionEvaluatingMessageProcessor(Expression expression) {
		super(expression);
	}

	public CustomExpressionEvaluatingMessageProcessor(String expression, Class<T> expectedType) {
		super(expression, expectedType);
	}

	public CustomExpressionEvaluatingMessageProcessor(String expression) {
		super(expression);
	}


	@Override
	protected <T> T evaluateExpression(Expression expression, Object input, Class<T> expectedType) {
		T val = expression.getValue(this.getEvaluationContext(false), input, expectedType);
		logger.info(expression.getExpressionString() + " " + input + " " + val);
		return val;
	}
}
