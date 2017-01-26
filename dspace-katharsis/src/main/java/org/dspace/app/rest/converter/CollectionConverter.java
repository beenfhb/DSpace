package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.Collection;
import org.springframework.stereotype.Component;

@Component
public class CollectionConverter
		extends DSpaceObjectConverter<org.dspace.content.Collection, org.dspace.app.rest.model.Collection> {
	@Override
	public org.dspace.content.Collection toModel(org.dspace.app.rest.model.Collection obj) {
		return (org.dspace.content.Collection) super.toModel(obj);
	}

	@Override
	public Collection fromModel(org.dspace.content.Collection obj) {
		return (Collection) super.fromModel(obj);
	}
	
	@Override
	protected Collection newInstance() {
		return new Collection();
	}
}
