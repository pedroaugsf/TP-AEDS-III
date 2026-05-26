# ----- build stage -----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir -p out && \
    find src -name "*.java" > sources.txt && \
    javac -d out @sources.txt

# ----- runtime stage -----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY web ./web

# Render fornece PORT via env var; default 8080 para rodar local
ENV PORT=8080
EXPOSE 8080

# dados/ é criado em runtime; persistente se houver volume montado
CMD ["java", "-cp", "out", "app.Servidor"]
