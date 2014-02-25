package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mastfrog.util.Checks;
import com.mastfrog.util.ConfigurationError;
import com.mastfrog.util.Exceptions;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.File;
import java.io.IOException;

/**
 * Convenience implementation of EnumHtmlEmailTemplateProvider which takes a
 * folder on disk and an optional filename, and uses toString() on the passed
 * enum if no file name is passed to its constructor.
 *
 * @author Tim Boudreau
 * @param <T>
 */
@Singleton
public abstract class EnumHtmlTemplateProviderOverFiles<T extends Enum<T>> extends EnumHtmlEmailTemplateProvider<T> {

    private volatile Configuration config;
    private volatile long lastLoad;
    protected final File file;
    protected final String templateName;

    protected EnumHtmlTemplateProviderOverFiles(Class<T> type, File folder) throws IOException {
        this(type, folder, null);
    }

    protected EnumHtmlTemplateProviderOverFiles(Class<T> type, File folder, String fileName) throws IOException {
        super(type);
        Checks.notNull("folder", folder);
        file = folder;
        if (file != null && (!file.exists() || !file.isDirectory())) {
            throw new ConfigurationError("No such template dir - set " + folder);
        }
        if (fileName != null) {
            File f = new File(file, fileName);
            if (!f.exists() || !f.isFile()) {
                throw new ConfigurationError("No such template dir - set " + folder);
            } else if (f.exists() && !f.canRead()) {
                throw new ConfigurationError("Missing read permission on " + f);
            }
        }
        templateName = fileName;
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
                        config.setClassForTemplateLoading(EnumHtmlTemplateProviderOverFiles.class, "");
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
    public <T extends Enum<T>> Template template(T template) {
        if (template != null) {
            return findTemplate(type.cast(template));
        }
        try {
            return config().getTemplate("message-template.html");
        } catch (IOException ex) {
            return Exceptions.chuck(ex);
        }
    }

    @Override
    protected Template findTemplate(T template) {
        String fn = templateName;
        if (fn == null) {
            fn = template.toString();
        }
        try {
            return config().getTemplate(fn);
        } catch (IOException ex) {
            return Exceptions.chuck(ex);
        }
    }
}
