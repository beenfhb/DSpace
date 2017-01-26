package org.dspace.app.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.katharsis.resource.registry.ResourceRegistry;

/**
 * @author Andrea Bollini
 *
 */
@RestController
public class InfoController {
	@Autowired
	private ResourceRegistry resourceRegistry;

	@RequestMapping("/resourcesInfo")
	public Map<String, String> getResources() {
		Map<String, String> result = new HashMap<String, String>();
		// Add all resources (i.e. items, collections, etc.)
		for (Class<?> clazz : resourceRegistry.getResources().keySet()) {
			result.put(resourceRegistry.getResourceType(clazz), resourceRegistry.getResourceUrl(clazz));
		}
		return result;
	}

}
