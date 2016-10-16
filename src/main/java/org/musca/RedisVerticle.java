package org.musca;

import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.logging.Logger;

public class RedisVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(RedisVerticle.class.getName());
    static final String LOAD_PAYLOAD = "load.payload";

    private RedisClient client;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        client = RedisClient.create(vertx, new RedisOptions().setHost("127.0.0.1"));
        vertx.eventBus().localConsumer(LOAD_PAYLOAD, (Message<String> message) -> {
            final String uuid = message.body();

            client.get(uuid, (AsyncResult<String> reply) -> {
                if (reply.succeeded()) {
                    LOGGER.info("Loaded payload! " + reply.result());
                    vertx.eventBus().send(RabbitVerticle.PUBLISH_PAYLOAD, reply.result());
                } else {
                    LOGGER.warning("Could not load payload! " + reply.cause().getMessage());
                    LOGGER.warning("Not that bad, at least the message is saved, right?");
                }
            });
        });

        LOGGER.info("RedisVerticle deployed!");
        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }
}
