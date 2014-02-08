package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;
import java.util.Map;

/**
 * Formats messages as HTML
 *
 * @author Tim Boudreau
 */
@ImplementedBy(DefaultHtmlMessageFormatter.class)
public interface HtmlMessageFormatter {

    String format(EmailAddress sender, String subject, String body, Map<String, Object> injected);
}
