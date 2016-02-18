FROM gcr.io/google_appengine/jetty9
ADD target.war $JETTY_BASE/webapps/root.war
