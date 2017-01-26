/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import io.katharsis.resource.annotations.JsonApiResource;
import io.katharsis.resource.annotations.JsonApiToMany;
import io.katharsis.resource.annotations.JsonApiToOne;

@JsonApiResource(type = "collections")
public class Collection extends DSpaceObject {
    //Relationships
	@JsonApiToOne
    private Bitstream logo;

    @JsonApiToOne
    private Item templateItem;
    
    @JsonApiToMany(opposite="true")
    private List<Item> items;

    //Calculated
    private Integer numberItems;

	public Bitstream getLogo() {
		return logo;
	}

	public void setLogo(Bitstream logo) {
		this.logo = logo;
	}

	public Item getTemplateItem() {
		return templateItem;
	}

	public void setTemplateItem(Item templateItem) {
		this.templateItem = templateItem;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public Integer getNumberItems() {
		return numberItems;
	}

	public void setNumberItems(Integer numberItems) {
		this.numberItems = numberItems;
	}
}
