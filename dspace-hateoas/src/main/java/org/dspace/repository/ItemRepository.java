package org.dspace.repository;

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;

public interface ItemRepository extends DSORepository<Item, UUID>, ItemDAO {
}
