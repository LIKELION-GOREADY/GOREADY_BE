FROM amd64/amazoncorretto:17

WORKDIR /app

COPY ./build/libs/lazyweather-0.0.1-SNAPSHOT.jar /app/lazyweather.jar

CMD ["java", "-Duser.timezone=Asia/Seoul", "-jar", "-Dspring.profiles.active=dev", "/app/lazyweather.jar"]
