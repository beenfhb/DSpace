package org.dspace.core;

import java.util.TimeZone;

import javax.servlet.Filter;

import org.dspace.app.util.DSpaceContextListener;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.servicemanager.servlet.DSpaceKernelServletContextListener;
import org.dspace.utils.servlet.DSpaceWebappServletFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, FlywayAutoConfiguration.class})
public class DSpace extends SpringBootServletInitializer {
	
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DSpace.class);
    }

	public static void main(String[] args) {
		SpringApplication.run(DSpace.class, args);		
	}
	

    /**
     * Register the "DSpaceKernelServletContextListener" so that it is loaded
     * for this Application.
     * @return DSpaceKernelServletContextListener
     */
    @Bean
    @Order(1)
    protected DSpaceKernelServletContextListener dspaceKernelListener() {
        // This registers our listener which starts the DSpace Kernel
        return new DSpaceKernelServletContextListener();
    }
    
    /**
     * Register the "DSpaceContextListener" so that it is loaded
     * for this Application.
     * @return DSpaceContextListener
     */
    @Bean
    @Order(2)
    protected DSpaceContextListener dspaceContextListener() {
        // This listener initializes the DSpace Context object
        // (and loads all DSpace configs)
        return new DSpaceContextListener();
    }
    
    /**
     * Register the DSpaceWebappServletFilter, which initializes the
     * DSpace RequestService / SessionService
     * 
     * @return DSpaceWebappServletFilter
     */
    @Bean
    @Order(1)
    protected Filter dspaceWebappServletFilter() {
        return new DSpaceWebappServletFilter();
    }
    

}
