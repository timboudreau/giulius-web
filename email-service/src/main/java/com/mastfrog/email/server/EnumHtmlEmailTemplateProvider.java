package com.mastfrog.email.server;

import com.mastfrog.util.Checks;
import freemarker.template.Template;

/**
 * Registered in a Guice Multibinder for EnumHtmlEmailTemplateProvider&lt;?&gt;, these can look up an email
 * template based on an enum constant as a lookup key
 *
 * @author Tim Boudreau
 * @param <T> An ad-hoc enum type
 */
public abstract class EnumHtmlEmailTemplateProvider<T extends Enum<T>> implements HtmlTemplateProvider {

    final Class<T> type;

    protected EnumHtmlEmailTemplateProvider(Class<T> type) {
        Checks.notNull("type", type);
        this.type = type;
    }

    <T extends Enum<T>> EnumHtmlEmailTemplateProvider<T> match(T enumValue) {
        if (enumValue != null && enumValue.getClass() == type) {
            return (EnumHtmlEmailTemplateProvider<T>) this;
        }
        return null;
    }

    @Override
    public <T extends Enum<T>> Template template(T template) {
        if (template != null) {
            return findTemplate(type.cast(template));
        }
        return null;
    }

    protected abstract Template findTemplate(T template);
}
