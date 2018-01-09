/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.components;

import java.util.List;
import java.util.Map;

public class ExploreMapProcessors {
	private Map<String, List<ExploreProcessor>> processorsMap;
	
	public void setProcessorsMap(Map<String, List<ExploreProcessor>> processorsMap) {
		this.processorsMap = processorsMap;
	}
	
	public Map<String, List<ExploreProcessor>> getProcessorsMap() {
		return processorsMap;
	}
}
