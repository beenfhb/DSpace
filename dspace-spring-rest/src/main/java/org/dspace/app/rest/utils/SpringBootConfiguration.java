package org.dspace.app.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpringBootConfiguration {
	@Value("${dspace.dir}")
	private String dspaceHome;
	
	public String getDspaceHome() {
		return dspaceHome;
	}
}