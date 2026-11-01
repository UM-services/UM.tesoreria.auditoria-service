package um.tesoreria.aud.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TesoreriaAudApplication {

	public static void main(String[] args) {
		SpringApplication.run(TesoreriaAudApplication.class, args);
	}

}
