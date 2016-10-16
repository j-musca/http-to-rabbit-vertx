package org.musca;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.rabbitmq.RabbitMQClient;
import rx.Observable;

import java.util.logging.Logger;

public class RabbitVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(RabbitVerticle.class.getName());

    static final String PUBLISH_PAYLOAD = "publish.payload";

    private RabbitMQClient client;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        createRabbitClient();

        final Observable<Void> startObservable = client.startObservable();
        final Observable<Void> exchangeObservable = client.exchangeDeclareObservable("incoming_payloads", "fanout", true, false);
        final Observable<JsonObject> queueObservable = client.queueDeclareObservable("payloads_for_trips", true, false, false);
        final Observable<Void> bindObservable = client.queueBindObservable("payloads_for_trips", "incoming_payloads", "");

        Observable
            .concat(startObservable, exchangeObservable, queueObservable, bindObservable)
            .doOnError((Throwable error) -> {
                LOGGER.warning("Could not start rabbit mq: " + error.getMessage());
                startFuture.fail(error);
            })
            .doOnCompleted(() -> {
                registerRabbitPublisher();
                startFuture.complete();
            })
            .subscribe();
    }

    private void createRabbitClient() {
        JsonObject config = new JsonObject();
        config.put("user", "guest");
        config.put("password", "guest");
        client = RabbitMQClient.create(vertx, config);
    }

    private void registerRabbitPublisher() {
        vertx.eventBus().localConsumer(PUBLISH_PAYLOAD, (Message<String> message) -> {
            LOGGER.info("Got cool message payload: " + message.body());
            final JsonObject rabbitMessage = createRabbitMessage(message);

            client.basicPublish("incoming_payloads", "", rabbitMessage, (AsyncResult<Void> publishResult) -> {
                if (publishResult.succeeded()) {
                    LOGGER.info("Published message!");
                } else {
                    LOGGER.warning("Could not publish message: " + publishResult.cause());
                }
            });
        });
    }

    private JsonObject createRabbitMessage(Message<String> message) {
        JsonObject rabbitMessage = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.put("contentType", "application/json");
        properties.put("contentEncoding", "UTF-8");
        JsonObject body = new JsonObject(message.body());

        rabbitMessage.put("properties", properties);
        rabbitMessage.put("body", body);
        return rabbitMessage;
    }
}
