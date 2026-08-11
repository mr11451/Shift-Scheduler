FROM node:20-slim AS frontend-builder

WORKDIR /web

COPY frontend/package.json ./
RUN npm install

COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-builder

WORKDIR /build

COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV CLIENT_DIST_DIR=/app/client/dist
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

COPY --from=backend-builder /build/target/server-1.0.0.jar ./server.jar
COPY --from=frontend-builder /web/dist ./client/dist

EXPOSE 8080 5005

CMD ["java", "-jar", "server.jar"]
