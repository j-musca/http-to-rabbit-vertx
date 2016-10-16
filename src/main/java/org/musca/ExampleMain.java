package org.musca;

import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import java.util.logging.Logger;

public class ExampleMain {

    private static final Logger LOGGER = Logger.getLogger(ExampleMain.class.getName());

    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();
        final Observable<String> redisStartup = vertx.deployVerticleObservable(RedisVerticle.class.getName());
        final Observable<String> rabbitStartup = vertx.deployVerticleObservable(RabbitVerticle.class.getName());
        final Observable<String> necessaryStartups = Observable.merge(redisStartup, rabbitStartup);

        necessaryStartups
            .doOnError(ExampleMain::handleError)
            .doOnCompleted(() -> {
                vertx.deployVerticleObservable(ServerVerticle.class.getName())
                .doOnError(ExampleMain::handleError)
                .doOnCompleted(() -> {
                    LOGGER.info("Started app!");
                })
                .subscribe();
            })
            .subscribe();
    }

    private static void handleError(Throwable error) {
        LOGGER.warning("Got start up problems! " + error.getMessage());
    }
}
