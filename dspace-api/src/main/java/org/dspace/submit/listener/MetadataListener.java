package org.dspace.submit.listener;

import java.util.Map;

import org.dspace.services.ConfigurationService;

import gr.ekt.bte.core.DataLoader;

public class MetadataListener {

	private Map<String, String> metadata;
	
	private ConfigurationService configurationService;

	private Map<String, DataLoader> dataloadersMap;
	
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public Map<String, DataLoader> getDataloadersMap() {
		return dataloadersMap;
	}

	public void setDataloadersMap(Map<String, DataLoader> dataloadersMap) {
		this.dataloadersMap = dataloadersMap;
	}
	
}
