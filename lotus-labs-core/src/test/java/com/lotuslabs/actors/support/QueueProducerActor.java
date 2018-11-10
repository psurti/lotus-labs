package com.lotuslabs.actors.support;

import static com.lotuslabs.actors.support.MessageChannels.ROUTER1_CHANNEL;
import static com.lotuslabs.actors.support.MessageChannels.ROUTER2_CHANNEL;
import static com.lotuslabs.actors.support.MessageChannels.ROUTER3_CHANNEL;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.PayloadTypeRouter;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.messaging.support.GenericMessage;

import com.lotuslabs.actors.support.Routers.ChannelMapping;
import com.lotuslabs.eip.processor.resequencer.ResequencerQueueChannel;
import com.lotuslabs.eip.processor.resequencer.SequenceSupplier;

/**
 * Data Producer
 *
 * @author psurti
 *
 */
public class QueueProducerActor extends AbstractActor<String,Void> {

	private static final Logger logger = LoggerFactory.getLogger(QueueProducerActor.class);
	private final RecipientListRouter router;
	private final RecipientListRouter router1;
	private PayloadTypeRouter router2;
	private ExpressionEvaluatingRouter router3;

	@SuppressWarnings("unchecked")
	public QueueProducerActor(Director director) {
		super(director);

		router = routers().router(ROUTER1_CHANNEL,ROUTER2_CHANNEL);

		router1 = routers().router(
				new ChannelMapping<>("payload.matches('[A-M]17:ROUTER')", ROUTER1_CHANNEL),
				new ChannelMapping<>("payload.matches('[N-Z]17:ROUTER')", ROUTER2_CHANNEL)
				);

		router2 = routers().payloadTypeRouter(
				new ChannelMapping<>(Integer.class.getName(), ROUTER1_CHANNEL),
				new ChannelMapping<>(String.class.getName(), ROUTER2_CHANNEL));

		router3 = routers().exprEvalRouter("payload",
				new ChannelMapping<>("payload.matches('[A-M]17:ROUTER')", ROUTER1_CHANNEL),
				new ChannelMapping<>("payload.matches('[N-Z]17:ROUTER')", ROUTER2_CHANNEL));

		director.registerPollableChannel(ROUTER3_CHANNEL, new ResequencerQueueChannel(100, 0,
				new SequenceSupplier<Long>() {

			@Override
			public Long get(Long current) {
				if (current == null )
					return Long.valueOf(0);
				return Long.valueOf(current.longValue()+1);
			}
		}));

	}

	@Override
	public Void execute(String item) {
		logger.info("Executing {}" , QueueProducerActor.class.getSimpleName());
		char start = 'A';
		char end = 'Z';
		for (int i = start; i <= end; i++) {
			String v = (char)i + String.valueOf("17");
			Map<String,Object> headers = new HashMap<>();
			headers.put("SID", Long.valueOf(i));
			GenericMessage<String> msg = new GenericMessage<>(v, headers);

			logger.info("send message: {}", v);
			//			send(ROUTER1_CHANNEL, new GenericMessage<>(v + ":ROUTER1"));
			//			send(ROUTER2_CHANNEL, new GenericMessage<>(v + ":ROUTER2"));
			send(router1, new GenericMessage<>(v+":ROUTER"), 0);
			//			put(msg);
			put(ROUTER3_CHANNEL,new GenericMessage<>(v, headers));

			/*
			MessageFilter filter = new MessageFilter(message ->
			message.getPayload().toString().charAt(0) == 'P');
			filter.setOutputChannel(null);
			filter.handleMessage(new GenericMessage<>(v));
			 */
		}
		send(EOS_EVENT); //broadcast
		return null;
	}

}
