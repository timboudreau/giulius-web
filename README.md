giulius-web
===========

A few related projects for traditional Java web development with Guice and using embedded Jetty,
specifically:

  * embedded-jetty - a builder for embedded web servers and a simple way to wrap embedded Jetty around Guice servlets and launch it
  * jackson - a simple extensible way to bind Jackson and allow looking up of things which need to configure Jackson to serialize things on the classpath
  * giulius-servlet - simplified Guice servlets
  * statistics - Ability to annotate methods on Guice-created objects in order to automatically generate JMX Mbeans and optionally publish timing and access statistics via UDP packets
  * statsd-aop - Uses Guice-AOP to allow guice-instantiated objects to be annotated with, e.g. `@Metric(value = "failedLoginAttempts", type = Metric.Types.INCREMENT)` and calls to that method will be sent to a statsd server.

Builds and a Maven repository containing this project can be <a href="https://timboudreau.com/builds/">found on timboudreau.com</a>.

