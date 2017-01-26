package org.dspace.app.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
	@Value("${dspace.dir}")
	private String dspaceHome;
	
	public String getDspaceHome() {
		return dspaceHome;
	}
}
