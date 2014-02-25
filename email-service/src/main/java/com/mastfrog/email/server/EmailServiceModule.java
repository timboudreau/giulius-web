package com.mastfrog.email.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.settings.Settings;
import freemarker.template.Template;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openide.util.Exceptions;

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
            bind(HtmlTemplateProvider.class).asEagerSingleton();
        } else {
            System.out.println("Development mode - will not send real emails");
        }
        bind(ExecutorService.class).annotatedWith(Names.named("mailqueue")).toInstance(Executors.newFixedThreadPool(2));
        // Guice requires this even if none get bound elsewhere
        Multibinder<EnumHtmlEmailTemplateProvider<?>> placeholder
                = Multibinder.newSetBinder(binder(), ENUM_EMAIL_TEMPLATE_PROVIDER_LITERAL);
        bind(ShutdownMailqueue.class).asEagerSingleton();
    }
    public static TypeLiteral<EnumHtmlEmailTemplateProvider<?>> ENUM_EMAIL_TEMPLATE_PROVIDER_LITERAL = new TL();

    private static class TL extends TypeLiteral<EnumHtmlEmailTemplateProvider<?>> {

    }

    static class ShutdownMailqueue implements Runnable {

        private final ExecutorService svc;

        @Inject
        ShutdownMailqueue(@Named("mailqueue") ExecutorService svc, ShutdownHookRegistry reg) {
            this.svc = svc;
            reg.add(this);
        }

        @Override
        public void run() {
            svc.shutdown();
            System.out.println("Waiting for mail queue to empty");
            try {
                svc.awaitTermination(2, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
