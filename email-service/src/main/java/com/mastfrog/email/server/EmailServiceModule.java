package com.mastfrog.email.server;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.Settings;
import freemarker.template.Template;
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
        // Guice requires this even if none get bound elsewhere
        Multibinder<EnumHtmlEmailTemplateProvider<?>> placeholder
                = Multibinder.newSetBinder(binder(), ENUM_EMAIL_TEMPLATE_PROVIDER_LITERAL);
    }
    public static TypeLiteral<EnumHtmlEmailTemplateProvider<?>> ENUM_EMAIL_TEMPLATE_PROVIDER_LITERAL = new TL();

    private static class TL extends TypeLiteral<EnumHtmlEmailTemplateProvider<?>> {

    }
}
