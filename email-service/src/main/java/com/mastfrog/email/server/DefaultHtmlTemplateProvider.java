package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mastfrog.settings.Settings;
import com.mastfrog.util.ConfigurationError;
import com.mastfrog.util.Exceptions;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.File;
import java.io.IOException;

@Singleton
class DefaultHtmlTemplateProvider implements HtmlTemplateProvider {

    private volatile Configuration config;
    private volatile long lastLoad;
    private final File file;

    @Inject
    DefaultHtmlTemplateProvider(Settings settings) throws IOException {
        String path = settings.getString(SETTINGS_KEY_EMAIL_TEMPLATE);
        file = path == null ? null : new File(path);
        if (file != null && (!file.exists() || !file.isDirectory())) {
            throw new ConfigurationError("No such template dir - set " + SETTINGS_KEY_EMAIL_TEMPLATE + ": " + file);
        }
    }
    
    private long lastModified() {
        // XXX expensive check - can configuration be forced to reload
        // if the file is modified?
        long lastModified = 0;
        for (File f : file.listFiles()) {
            lastModified = Math.max(lastModified, f.lastModified());
        }
        return lastModified;
    }

    private Configuration config() throws IOException {
        if (config == null || (file != null && lastModified() > lastLoad)) {
            synchronized (this) {
                if (config == null || (file != null && file.lastModified() > lastLoad)) {
                    config = new Configuration();
                    config.setDefaultEncoding("UTF-8");
                    config.setIncompatibleImprovements(new Version(2, 3, 20));
                    config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                    if (file == null) {
                        config.setClassForTemplateLoading(DefaultHtmlTemplateProvider.class, "");
                    } else {
                        config.setDirectoryForTemplateLoading(file);
                        lastLoad = lastModified();
                    }
                }
            }
        }
        return config;
    }

    @Override
    public Template template() {
        try {
            return config().getTemplate("message-template.html");
        } catch (IOException ex) {
            return Exceptions.chuck(ex);
        }
    }
}
