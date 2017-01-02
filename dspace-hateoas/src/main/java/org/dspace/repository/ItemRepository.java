package org.dspace.repository;

import java.util.Optional;
import java.util.UUID;

import org.dspace.content.Item;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "item", path = "item")
public interface ItemRepository extends DSORepository<Item, UUID> {	
	Optional<Item> findByLegacyId(Integer id);
}
