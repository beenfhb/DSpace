package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.MetadataEntry;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;

public abstract class DSpaceObjectConverter <M extends DSpaceObject, R extends org.dspace.app.rest.model.DSpaceObject> extends DSpaceConverter<M, R> {
	
	@Override
	public R fromModel(M obj) {
		R resource = newInstance();
		resource.setHandle(obj.getHandle());
		if (obj.getID() != null) {
			resource.setUuid(obj.getID().toString());
		}
		resource.setName(obj.getName());
		List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
		for (MetadataValue mv : obj.getMetadata()) {
			MetadataEntry me = new MetadataEntry();
			me.setKey(mv.getMetadataField().toString('.'));
			me.setValue(mv.getValue());
			me.setLanguage(mv.getLanguage());
			metadata.add(me);
		}
		resource.setMetadata(metadata);
		return resource;
	}
	
	@Override
	public M toModel(R obj) {
		return null;
	}

	protected abstract R newInstance();
}