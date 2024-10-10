FROM amd64/amazoncorretto:17

WORKDIR /app

COPY ./build/libs/goready-0.0.1-SNAPSHOT.jar /app/goready.jar

CMD ["java", "-Duser.timezone=Asia/Seoul", "-jar", "-Dspring.profiles.active=dev", "/app/goready.jar"]
