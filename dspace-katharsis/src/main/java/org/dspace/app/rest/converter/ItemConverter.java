package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemConverter extends DSpaceObjectConverter<org.dspace.content.Item, org.dspace.app.rest.model.Item>{
	@Autowired
	private CollectionConverter collectionConverter;
	
	@Override
	public Item fromModel(org.dspace.content.Item obj) {
		Item item = super.fromModel(obj);
		item.setOwningCollection(collectionConverter.fromModel(obj.getOwningCollection()));
		return item;
	}

	@Override
	public org.dspace.content.Item toModel(Item obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Item newInstance() {
		return new Item();
	}
}
