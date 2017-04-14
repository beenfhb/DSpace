/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;

import java.util.Collection;

/**
 * Represents an interface for the mapping of the MetadataValue fields
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */

public interface MetadataFieldMapping<RecordType,QueryType> {

    /**
     * @param field MetadataFieldConfig representing what to map the value to
     * @param value The value to map to a MetadataValueDTO
     * @return A MetadataValueDTO created from the field and value
     */
    public MetadataValueDTO toDCValue(MetadataFieldConfig field, String value);


    /**
     * Create a collection of MetadataValueDTO retrieved from a given RecordType
     * @param record Used to retrieve the MetadataValueDTO
     * @return Collection of MetadataValueDTO
     */
    public Collection<MetadataValueDTO> resultToDCValueMapping(RecordType record);



}
