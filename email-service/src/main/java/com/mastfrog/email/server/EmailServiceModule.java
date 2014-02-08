package com.mastfrog.email.server;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.Settings;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configures mail sending
 *
 * @author Tim Boudreau
 */
public final class EmailServiceModule extends AbstractModule {

    private final Settings settings;

    public EmailServiceModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        if (Dependencies.isProductionMode(settings)) {
            // fail fast
            System.out.println("Production mode - using real mail server");
            bind(EmailServerService.class).to(RealMailServer.class).asEagerSingleton();
        }
        bind(ExecutorService.class).annotatedWith(Names.named("mailqueue")).toInstance(Executors.newFixedThreadPool(2));
        if (Dependencies.isProductionMode(settings)) {
            bind(HtmlTemplateProvider.class).asEagerSingleton();
        }
    }
}
