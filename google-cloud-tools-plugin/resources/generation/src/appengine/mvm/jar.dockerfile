FROM gcr.io/google_appengine/openjdk8
ADD target.jar /app/
ENTRYPOINT ["/docker-entrypoint.bash"]
CMD ["java","-jar","/app/target.jar"]
