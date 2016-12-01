FROM gcr.io/google_appengine/jetty
ADD target.war $JETTY_BASE/webapps/root.war
