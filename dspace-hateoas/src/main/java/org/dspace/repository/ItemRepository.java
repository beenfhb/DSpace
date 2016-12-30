package org.dspace.repository;

import java.util.Optional;
import java.util.UUID;

import org.dspace.content.Item;

public interface ItemRepository extends DSORepository<Item, UUID> {	
	Optional<Item> findByLegacyId(Integer id);
}
