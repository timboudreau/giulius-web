package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mastfrog.settings.Settings;
import com.mastfrog.util.Exceptions;
import com.mastfrog.util.Streams;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

@Singleton
final class DefaultHtmlMessageFormatter implements HtmlMessageFormatter {

    private final HtmlTemplateProvider provider;
    private final boolean escape;
    @Inject
    DefaultHtmlMessageFormatter(HtmlTemplateProvider prov, Settings settings) {
        this.provider = prov;
        escape = settings.getBoolean("email.escape.html", true);
    }

    private String escape(String s) {
        if (!escape) {
            return s;
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "</p><p>")
                .replace("@", "&#064;");
    }

    @Override
    public String format(EmailAddress sender, String subject, String body, Map<String, Object> injected) {
        Template tpl = provider.template(null);
        Map<String, Object> model = new HashMap<>(injected);
        model.put("subject", escape(subject));
        model.put("message", escape(body));
        model.put("from", sender.toString());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            tpl.process(model, new OutputStreamWriter(out));
            String result = Streams.readString(new ByteArrayInputStream(out.toByteArray()), 256);
            return result;
        } catch (TemplateException | IOException ex) {
            return Exceptions.chuck(ex);
        }
    }

    @Override
    public <T extends Enum<T>> String format(T template, EmailAddress sender, String subject, String body, Map<String, Object> injected) {
        Template tpl = provider.template(template);
        Map<String, Object> model = new HashMap<>(injected);
        model.put("subject", escape(subject));
        model.put("message", escape(body));
        model.put("from", sender.toString());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            tpl.process(model, new OutputStreamWriter(out));
            String result = Streams.readString(new ByteArrayInputStream(out.toByteArray()), 256);
            return result;
        } catch (TemplateException | IOException ex) {
            return Exceptions.chuck(ex);
        }
    }
}
