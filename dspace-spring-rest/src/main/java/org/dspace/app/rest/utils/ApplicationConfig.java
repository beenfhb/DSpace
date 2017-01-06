package org.dspace.app.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("org.dspace.content.repository")
public class ApplicationConfig {
	@Value("${dspace.dir}")
	private String dspaceHome;
	
	public String getDspaceHome() {
		return dspaceHome;
	}
}
