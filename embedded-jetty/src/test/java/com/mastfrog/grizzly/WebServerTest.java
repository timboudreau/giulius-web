package com.mastfrog.grizzly;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.streams.Streams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class WebServerTest {
    @Test
    public void testResourceDirectory() throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        File nue = new File(tmp, System.currentTimeMillis() + "_" + WebServerTest.class.getName());
        nue.mkdirs();
        Random r = new Random(System.currentTimeMillis());

        File data = new File(nue, "hello.txt");
        String text = "Hello from " + r.nextLong() + ":" + r.nextLong();
        PrintStream ps = new PrintStream(data);
        try {
            ps.print(text);
            ps.flush();
        } finally {
            ps.close();
        }
        WebServer svr = new WebServerBuilder(8237).add(X.class, "/foo").done().setWorkingDir(nue).build();
        svr.start();

        HttpGet get = new HttpGet("http://localhost:8237/hello.txt");
        HttpClient client = new DefaultHttpClient();
        HttpResponse resp = client.execute(get);
        assertEquals(resp.getStatusLine() + "", 200, resp.getStatusLine().getStatusCode());
        String recvd = Streams.readString(resp.getEntity().getContent()).trim();

        assertEquals(text, recvd);
    }

    @Test
    public void testBuilder() throws Exception {
        assertTrue(true);
        WebServer svr = new WebServerBuilder(8129).add(L.class).add(Resource.create("/thing.txt", "Hey now how")).add(new M()).add(TestServlet.class, "/*").withAttribute("foo", "bar").initializeWith("bar", "baz").build();
        assertEquals(8129, svr.getPort());
        svr.start();
        try {
            URL url = svr.getBaseURL();
            InputStream in = url.openStream();
            String s = Streams.readString(in);
            System.out.println(s);
            assertEquals("Hey There 0", s.trim());
            assertTrue(L.initialized);
        } finally {
            svr.stop();
            assertTrue(L.destroyed);
        }
    }
    @Singleton
    public static final class X extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(200);
            resp.getOutputStream().println("OK");
            resp.getOutputStream().close();
        }
    }
    @Singleton
    public static final class TestServlet extends HttpServlet {
        private final StringBuilder msg;
        private int count;

        @Inject
        public TestServlet(StringBuilder msg) {
            this.msg = msg;
            System.out.println("Created test servlet with " + msg);
        }

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            String val = config.getInitParameter("bar");
            assertEquals("baz", val);
            val = (String) config.getServletContext().getAttribute("foo");
            assertEquals("bar", val);

            URL url = null;
            try {
                url = config.getServletContext().getResource("/thing.txt");
            } catch (MalformedURLException ex) {
                Exceptions.chuck(ex);
            }
            System.out.println("url is " + url);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.addHeader("Content-Type", "text/plain");
            ServletOutputStream out = resp.getOutputStream();
            String message = "" + this.msg + " " + count;
            count++;
            out.println(message);
            out.close();
        }
    }
    public static final class M extends AbstractModule {
        {
            System.out.println("Created a module");
        }

        @Override
        protected void configure() {
            bind(StringBuilder.class).toInstance(new StringBuilder("Hey There"));
        }
    }
    public static final class L implements ServletContextListener {
        static boolean initialized;
        static boolean destroyed;

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            initialized = true;
            System.out.println("init");
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            destroyed = true;
            System.out.println("destroy");
        }
    }
}
