package org.dspace.app.cris.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.eperson.EPerson;
import org.hibernate.annotations.Type;

import it.cilea.osd.common.model.Identifiable;

@Entity
@Table(name="cris_deduplication")
@NamedQueries({
    @NamedQuery(name = "CrisDeduplication.findAll", query = "from CrisDeduplication order by id"),
    @NamedQuery(name = "CrisDeduplication.count", query = "select count(*) from CrisDeduplication"),
    @NamedQuery(name = "CrisDeduplication.findByFirstAndSecond", query = "from CrisDeduplication where firstItemId = ? and secondItemId = ?"),
    @NamedQuery(name = "CrisDeduplication.uniqueByFirstAndSecond", query = "from CrisDeduplication where firstItemId = ? and secondItemId = ?")
})
public class CrisDeduplication implements Identifiable {
	/** DB Primary key */
	@Id
	@GeneratedValue(generator = "CRIS_DEDUPLICATION_SEQ")
	@SequenceGenerator(name = "CRIS_DEDUPLICATION_SEQ", sequenceName = "CRIS_DEDUPLICATION_SEQ", allocationSize = 1)
	private Integer id;

	@Column(name="first_item_id")
    private String firstItemId;
	@Column(name="second_item_id")
    private String secondItemId;
    
    private Integer resource_type_id;
    private boolean tofix;
    private boolean fake;
    
    private String reader_note;
    private UUID reader_id;
    private Date reader_time;    
    private String workflow_decision;
    private String submitter_decision;
    private String admin_decision;
    private UUID admin_id;
    private String note;
    
    private Date admin_time;
    private UUID eperson_id;
    private Date reject_time;
    
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getFirstItemId() {
		return firstItemId;
	}
	public void setFirstItemId(String first_item_id) {
		this.firstItemId = first_item_id;
	}
	public String getSecondItemId() {
		return secondItemId;
	}
	public void setSecondItemId(String second_item_id) {
		this.secondItemId = second_item_id;
	}
	public Integer getResource_type_id() {
		return resource_type_id;
	}
	public void setResource_type_id(Integer resource_type_id) {
		this.resource_type_id = resource_type_id;
	}
	public boolean isTofix() {
		return tofix;
	}
	public void setTofix(boolean tofix) {
		this.tofix = tofix;
	}
	public boolean isFake() {
		return fake;
	}
	public void setFake(boolean fake) {
		this.fake = fake;
	}
	public String getReader_note() {
		return reader_note;
	}
	public void setReader_note(String reader_note) {
		this.reader_note = reader_note;
	}
	public UUID getReader_id() {
		return reader_id;
	}
	public void setReader_id(UUID reader_id) {
		this.reader_id = reader_id;
	}
	public Date getReader_time() {
		return reader_time;
	}
	public void setReader_time(Date reader_time) {
		this.reader_time = reader_time;
	}
	public String getWorkflow_decision() {
		return workflow_decision;
	}
	public void setWorkflow_decision(String workflow_decision) {
		this.workflow_decision = workflow_decision;
	}
	public String getSubmitter_decision() {
		return submitter_decision;
	}
	public void setSubmitter_decision(String submitter_decision) {
		this.submitter_decision = submitter_decision;
	}
	public String getAdmin_decision() {
		return admin_decision;
	}
	public void setAdmin_decision(String admin_decision) {
		this.admin_decision = admin_decision;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public UUID getAdmin_id() {
		return admin_id;
	}
	public void setAdmin_id(UUID admin_id) {
		this.admin_id = admin_id;
	}
	public Date getAdmin_time() {
		return admin_time;
	}
	public void setAdmin_time(Date admin_time) {
		this.admin_time = admin_time;
	}
	public UUID getEperson_id() {
		return eperson_id;
	}
	public void setEperson_id(UUID eperson_id) {
		this.eperson_id = eperson_id;
	}
	public Date getReject_time() {
		return reject_time;
	}
	public void setReject_time(Date reject_time) {
		this.reject_time = reject_time;
	}
    
}
