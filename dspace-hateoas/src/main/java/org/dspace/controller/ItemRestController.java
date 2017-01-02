package org.dspace.controller;

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/item")
public class ItemRestController {

	@RequestMapping(value="test", method = RequestMethod.GET)
	public boolean test() {

		return true;
	}
	
	@RequestMapping(value="find/{uuid}", method = RequestMethod.GET)
	public Item find(@PathVariable String uuid) {
		ItemService itemService = ContentServiceFactory.getInstance().getItemService();
		Context context = null;
		try {
			context = new Context();
			return itemService.find(context, UUID.fromString(uuid));
		}
		catch(Exception ex) {
			
		}
		finally {
			if(context!=null && context.isValid()) {
				context.abort();
			}
		}
		return null;
	}


	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<?> add(@PathVariable String uuid, @RequestBody Item item) {
		return null;
	}

	@RequestMapping(value="bulk", method = RequestMethod.POST)
	public ResponseEntity<?> bulk(@PathVariable String uuid, @RequestBody java.util.Collection<Item> items) {
		return null;
	}
	
}
