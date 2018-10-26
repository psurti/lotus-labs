package com.lotuslabs.actors.support;

import org.springframework.integration.router.ErrorMessageExceptionTypeRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.router.PayloadTypeRouter;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

import com.lotuslabs.actors.support.filter.CustomExpressionEvaluatingSelector;

/**
 * Available Routers
 *
 * @author psurti
 *
 */
public class Routers {

	private final Actors actors;
	private final DestinationResolver<MessageChannel> defaultResolver;

	public Routers(Actors actors) {
		super();
		this.actors = actors;
		this.defaultResolver =  name -> actors.getSubscriberChannel(name);
	}

	static class ChannelMapping<K,V> {
		K mappingType;
		V channelName;
		public ChannelMapping(K mappingType, V channelName) {
			this.mappingType = mappingType;
			this.channelName = channelName;
		}
	}

	@SuppressWarnings("unchecked")
	protected RecipientListRouter router(ChannelMapping<String, String>... mappings) {
		RecipientListRouter router = new RecipientListRouter();
		router.setBeanFactory(actors.getBeanFactory());
		router.setComponentName("multi-channel-router");
		router.setDefaultOutputChannel(this.actors.getDefaultSubscriberChannel());
		router.setChannelResolver(defaultResolver);

		for (ChannelMapping<String,String> mapping : mappings) {
			CustomExpressionEvaluatingSelector expressionEvaluatingSelector =
					new CustomExpressionEvaluatingSelector(mapping.mappingType);
			router.addRecipient(mapping.channelName, expressionEvaluatingSelector);
		}
		return router;
	}

	protected RecipientListRouter router(String... channelNames) {
		RecipientListRouter router = new RecipientListRouter();
		router.setBeanFactory(actors.getBeanFactory());
		router.setComponentName("multi-channel-router");
		router.setDefaultOutputChannel(this.actors.getDefaultSubscriberChannel());
		router.setChannelResolver(defaultResolver);
		for (String channelName : channelNames) {
			router.addRecipient(channelName);
		}
		return router;
	}

	@SuppressWarnings("unchecked")
	protected ExpressionEvaluatingRouter exprEvalRouter(String expressionString, ChannelMapping<String,String>... mappings) {
		ExpressionEvaluatingRouter router = new ExpressionEvaluatingRouter(expressionString);
		router.setBeanFactory(actors.getBeanFactory());
		router.setDefaultOutputChannel(this.actors.getDefaultSubscriberChannel());
		router.setComponentName("expression-eval-router");
		router.setChannelResolver(defaultResolver);
		for (ChannelMapping<String,String> mapping : mappings) {
			router.setChannelMapping(mapping.mappingType, mapping.channelName);
		}
		return router;
	}

	@SuppressWarnings("unchecked")
	protected PayloadTypeRouter payloadTypeRouter(ChannelMapping<String,String>... mappings) {
		PayloadTypeRouter router = new PayloadTypeRouter();
		router.setBeanFactory(actors.getBeanFactory());
		router.setComponentName("payload-type-router");
		router.setDefaultOutputChannel(this.actors.getDefaultSubscriberChannel());
		router.setChannelResolver(defaultResolver);
		for (ChannelMapping<String,String> mapping : mappings) {
			router.setChannelMapping(mapping.mappingType, mapping.channelName);
		}
		return router;
	}

	@SuppressWarnings("unchecked")
	protected ErrorMessageExceptionTypeRouter exceptionTypeRouter(ChannelMapping<String,String>... mappings) {
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(actors.getBeanFactory());
		router.setChannelResolver(defaultResolver);
		router.setDefaultOutputChannel(this.actors.getDefaultSubscriberChannel());
		for (ChannelMapping<String,String> mapping : mappings) {
			router.setChannelMapping(mapping.mappingType, mapping.channelName);
		}
		return router;
	}

	@SuppressWarnings("unchecked")
	protected HeaderValueRouter headerValueRouter(String headerName, ChannelMapping<String,String>... mappings) {
		HeaderValueRouter router = new HeaderValueRouter(headerName);
		router.setBeanFactory(actors.getBeanFactory());
		router.setChannelResolver(defaultResolver);
		for (ChannelMapping<String,String> mapping : mappings) {
			router.setChannelMapping(mapping.mappingType, mapping.channelName);
		}
		return router;
	}
}
