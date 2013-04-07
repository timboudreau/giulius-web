/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.grizzly;

import com.mastfrog.util.Checks;
import com.mastfrog.util.Streams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A resource that should be visible from ServletContext.getResource()
 *
 * @author Tim Boudreau
 */
public abstract class Resource {
    protected final String path;

    protected Resource(String path) {
        Checks.notNull("path", path);
        this.path = path;
    }

    public static Resource create(String path, String body) {
        Checks.notNull("path", path);
        Checks.notNull("body", body);
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        return new StringResource(path, body);
    }

    protected abstract URL toURL() throws IOException;

    protected InputStream toStream() throws IOException {
        return toURL().openStream();
    }

    public boolean match(String path) {
        return this.path.equals(path);
    }

    @Override
    public String toString() {
        return path;
    }

    public String getPath() {
        return path;
    }

    String getRealPath() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Resource && ((Resource) o).getPath().equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode() * 127;
    }
    static class StringResource extends Resource {
        private volatile File file;
        private final String body;

        StringResource(String path, String body) {
            super(path);
            this.body = body;
        }

        private File createFile() throws IOException {
            File tmp = new File(System.getProperty("java.io.tmpdir"));
            String destName = path.replace('/', '_');
            File dest = new File(tmp, destName + "-" + System.currentTimeMillis());
            dest.createNewFile();
            Streams.writeString(body, dest);
            return dest;
        }

        @Override
        protected URL toURL() throws IOException {
            if (file == null) {
                synchronized (this) {
                    if (file == null) {
                        file = createFile();
                    }
                }
            }
            URL result = file.toURI().toURL();
            System.out.println("StringResource " + path + " gets " + result);
            return result;
        }

        protected String getRealPath() {
            try {
                return createFile().getAbsolutePath();
            } catch (IOException ex) {
                return null;
            }
        }
    }
}
