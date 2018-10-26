/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lotuslabs.actors.support.filter;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.AbstractMessageProcessingSelector;

/**
 * A {@link MessageSelector} implementation that evaluates a SpEL expression.
 * The evaluation result of the expression must be a boolean value.
 *
 * @author Mark Fisher
 * @author Liujiong
 * @since 2.0
 */
public class CustomExpressionEvaluatingSelector extends AbstractMessageProcessingSelector {

	private static final ExpressionParser expressionParser =
			new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final String expressionString;

	public CustomExpressionEvaluatingSelector(String expressionString) {
		super(new CustomExpressionEvaluatingMessageProcessor<>(expressionParser.parseExpression(expressionString),
				Boolean.class));
		this.expressionString = expressionString;
	}

	public CustomExpressionEvaluatingSelector(Expression expression) {
		super(new CustomExpressionEvaluatingMessageProcessor<>(expression, Boolean.class));
		this.expressionString = expression.getExpressionString();
	}

	public String getExpressionString() {
		return this.expressionString;
	}

	@Override
	public String toString() {
		return "ExpressionEvaluatingSelector for: [" + this.expressionString + "]";
	}


}
