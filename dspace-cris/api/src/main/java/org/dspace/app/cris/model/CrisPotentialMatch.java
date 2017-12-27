package org.dspace.app.cris.model;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import it.cilea.osd.common.model.Identifiable;

@Entity
@Table(name="cris_potentialmatches")
public class CrisPotentialMatch implements Identifiable {
	 /** DB Primary key */
     @Id
     @GeneratedValue(generator = "CRIS_POTENTIALMATCHES_SEQ")
     @SequenceGenerator(name = "CRIS_POTENTIALMATCHES_SEQ", sequenceName = "CRIS_POTENTIALMATCHES_SEQ", allocationSize = 1)     
	 private Integer id;
	 
	 private UUID item_id;
	 
	 private String rp;
	 
	 private Integer pending;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public UUID getItem_id() {
		return item_id;
	}

	public void setItem_id(UUID item_id) {
		this.item_id = item_id;
	}

	public String getRp() {
		return rp;
	}

	public void setRp(String rp) {
		this.rp = rp;
	}

	public Integer getPending() {
		return pending;
	}

	public void setPending(Integer pending) {
		this.pending = pending;
	}
}
