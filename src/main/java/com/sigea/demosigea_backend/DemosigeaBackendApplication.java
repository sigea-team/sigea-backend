package com.sigea.demosigea_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación SIGEA Backend.
 * <p>
 * Punto de entrada de la aplicación Spring Boot que inicializa el contexto de la aplicación,
 * la configuración automática y el escaneo de componentes del paquete base
 * {@code com.sigea.demosigea_backend}.
 * </p>
 *
 * @author SIGEA Team
 * @version 1.0
 */
@SpringBootApplication
public class DemosigeaBackendApplication {

	/**
	 * Método principal que arranca la aplicación Spring Boot.
	 *
	 * @param args argumentos de línea de comandos pasados al iniciar la aplicación
	 */
	public static void main(String[] args) {
		SpringApplication.run(DemosigeaBackendApplication.class, args);
	}
}
