package org.dspace.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.ItemDAO;
import org.dspace.repository.CollectionRepository;
import org.dspace.repository.ItemRepository;
import org.dspace.validator.DSONotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/core/item")
public class ItemRestController {
	
	private final ItemRepository itemRepository;
	private final CollectionRepository collectionRepository;
	
	@Autowired
	public ItemRestController(ItemRepository itemRepository, CollectionRepository collectionRepository) {
		this.itemRepository = itemRepository;
		this.collectionRepository = collectionRepository;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public Item find(@PathVariable String itemUUIDorId) {
		this.validateItem(itemUUIDorId);
		if(StringUtils.isNumeric(itemUUIDorId)) {
			return this.itemRepository.findByLegacyId(Integer.parseInt(itemUUIDorId)).get();	
		} else {
			return this.itemRepository.findById(UUID.fromString(itemUUIDorId)).get();			
		}
	}


	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<?> add(@PathVariable String uuid, @RequestBody Item item) {
		validateCollection(uuid);
		Collection collection = this.collectionRepository.findById(UUID.fromString(uuid)).get();
		item.setOwningCollection(collection);
		return ResponseEntity.ok(itemRepository.save(item));
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<?> bulk(@PathVariable String uuid, @RequestBody java.util.Collection<Item> items) {
		validateCollection(uuid);
		Collection collection = this.collectionRepository.findById(UUID.fromString(uuid)).get();
		List<String> locations = new ArrayList<>();
		for(Item item : items) {
			item.setOwningCollection(collection);
			Item dspaceItem = itemRepository.save(item);
			
			URI location = ServletUriComponentsBuilder
					.fromCurrentRequest().path("/{id}")
					.buildAndExpand(dspaceItem.getID()).toUri();
			locations.add(location.toString());
		}
		return ResponseEntity.accepted().varyBy(locations.toArray(new String[locations.size()])).build();
	}
	
	private void validateItem(String itemUUIDorId) {		
		if(StringUtils.isNumeric(itemUUIDorId)) {
			this.itemRepository.findByLegacyId(Integer.parseInt(itemUUIDorId)).orElseThrow(() -> new DSONotFoundException(itemUUIDorId));	
		} else {
			this.itemRepository.findById(UUID.fromString(itemUUIDorId)).orElseThrow(() -> new DSONotFoundException(itemUUIDorId));			
		}
	}
	
	private void validateCollection(String uuid) {		
		this.collectionRepository.findById(UUID.fromString(uuid)).orElseThrow(() -> new DSONotFoundException(uuid));			
	}
}
