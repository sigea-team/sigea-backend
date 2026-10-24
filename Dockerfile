# ===============================================
# Etapa 1: Compilar la aplicación Spring Boot
# ===============================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar configuración del proyecto y código fuente
COPY pom.xml .
COPY src ./src

# Compilar y empaquetar el ejecutable jar (omitimos tests para un despliegue más rápido)
RUN mvn clean package -DskipTests

# ===============================================
# Etapa 2: Imagen liviana de ejecución
# ===============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Render asigna automáticamente un puerto mediante la variable de entorno $PORT (por defecto 10000 u 8080)
EXPOSE 8080

# Parámetros JVM optimizados para Render (limita memoria RAM al 75% del contenedor para evitar OOM Killer en planes gratuitos/starter)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Ejecución de la aplicación enlazando el puerto dinámico de Render
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
