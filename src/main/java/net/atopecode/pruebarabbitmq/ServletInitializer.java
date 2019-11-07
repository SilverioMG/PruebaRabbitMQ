package net.atopecode.pruebarabbitmq;

import net.atopecode.pruebarabbitmq.utils.ResourcesFile;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);

		configLog4j();
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(PruebarabbitmqApplication.class);
	}

	private void configLog4j(){
		try{
			ResourcesFile resourcesFile = new ResourcesFile("log4j.properties");
			PropertyConfigurator.configure(resourcesFile.loadInputStreamFromResourceFile());

			logger.info("Server initialized...");
		}
		catch(Exception ex){
			System.out.println("No se ha podido configurar el Logger Log4j: " + ex.getMessage());
		}
	}
}
