package org.musca;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.UUID;
import java.util.logging.Logger;

public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(ServerVerticle.class.getName());

    private RedisClient client;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        client = RedisClient.create(vertx, new RedisOptions().setHost("127.0.0.1"));
        final Router router = createRouter();

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, (AsyncResult<HttpServer> result) -> {
                    if (result.succeeded()) {
                        LOGGER.info("Started server on port 8080");
                        startFuture.complete();
                    } else {
                        LOGGER.warning("Failed to start server on port 8080");
                        startFuture.fail(result.cause());
                    }
                });
    }

    private Router createRouter() {
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route(HttpMethod.POST, "/data")
                .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
                .handler((RoutingContext context) -> {
                    JsonObject payload = context.getBodyAsJson();
                    LOGGER.info("Got payload: " + payload.encodePrettily());
                    context.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                    String uuid = UUID.randomUUID().toString();

                    client.set(uuid, payload.encode(), (AsyncResult<Void> result) -> {
                        if (result.succeeded()) {
                            LOGGER.info("Saved json to redis!");
                            vertx.eventBus().send(RedisVerticle.LOAD_PAYLOAD, uuid);
                        } else {
                            LOGGER.warning("Could not save to redis! " + result.cause().getMessage());
                            LOGGER.warning("What should we do with the payload?");
                        }
                    });
                });
        return router;
    }
}
