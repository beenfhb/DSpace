/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datamodel;

import org.dspace.importer.external.metadatamapping.MetadataValueDTO;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class contains all MetadataValueDTO objects from an imported item
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class ImportRecord {
    private List<MetadataValueDTO> valueList = null;

    /**
     * Retrieve an unmodifiableList of MetadataValueDTO
     * @return List of MetadataValueDTO
     */
    public List<MetadataValueDTO> getValueList() {
        return Collections.unmodifiableList(valueList);
    }

    /**
     * Create an ImportRecord instance initialized with a List of MetadataValueDTO objects
     * @param valueList
     */
    public ImportRecord(List<MetadataValueDTO> valueList) {
        //don't want to alter the original list. Also now I can control the type of list
        this.valueList = new LinkedList<>(valueList);
    }

    /**
     * Build a string based on the values in the valueList object
     * The syntax will be
     * Record{valueList={"schema"; "element" ; "qualifier"; "value"}}
     *
     * @return a concatenated string containing all values of the MetadataValueDTO objects in valueList
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{valueList=");
        for(MetadataValueDTO val:valueList){
            sb.append("{");
            sb.append(val.getSchema());
            sb.append("; ");
            sb.append(val.getElement());
            sb.append("; ");

            sb.append(val.getQualifier());
            sb.append("; ");

            sb.append(val.getValue());
            sb.append("; ");
            sb.append("}\n");

        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Return the MetadataValueDTO's that are related to a given schema/element/qualifier pair/triplet
     * @param schema
     * @param element
     * @param qualifier
     * @return the MetadataValueDTO's that are related to a given schema/element/qualifier pair/triplet
     */
    public Collection<MetadataValueDTO> getValue(String schema, String element, String qualifier){
        List<MetadataValueDTO> values=new LinkedList<MetadataValueDTO>();
        for(MetadataValueDTO value:valueList){
            if(value.getSchema().equals(schema)&&value.getElement().equals(element)){
               if(qualifier==null&&value.getQualifier()==null){
                   values.add(value);
               } else if (value.getQualifier()!=null&&value.getQualifier().equals(qualifier)) {
                   values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * Add a value to the valueList
     * @param value The MetadataValueDTO to add to the valueList
     */
    public void addValue(MetadataValueDTO value){
        this.valueList.add(value);
    }
}
