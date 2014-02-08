package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;
import freemarker.template.Template;

/**
 *
 * @author Tim Boudreau
 */
@ImplementedBy(DefaultHtmlTemplateProvider.class)
public interface HtmlTemplateProvider {

    public static final String SETTINGS_KEY_EMAIL_TEMPLATE = "email.template.dir";

    public Template template();
}
