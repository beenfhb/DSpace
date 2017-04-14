/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadataValueDTO;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper class used to accommodate for the possibility of correlations between multiple MetadataValueContributor objects
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class CombinedMetadataValueContributor<T> implements MetadataContributor<T> {
    private MetadataFieldConfig field;

    private LinkedList<MetadataContributor> MetadataValueContributors;

    private String separator;

    private MetadataFieldMapping<T,MetadataContributor<T>> metadataFieldMapping;

    /**
     * Initialize an empty CombinedMetadataValueContributor object
     */
    public CombinedMetadataValueContributor() {
    }

    /**
     *
     * @param field {@link org.dspace.importer.external.metadatamapping.MetadataFieldConfig} used in mapping
     * @param MetadataValueContributors A list of MetadataContributor
     * @param separator A separator used to differentiate between different values
     */
    public CombinedMetadataValueContributor(MetadataFieldConfig field, List<MetadataContributor> MetadataValueContributors, String separator) {
        this.field = field;
        this.MetadataValueContributors = (LinkedList<MetadataContributor>) MetadataValueContributors;
        this.separator = separator;
    }

    /**
     * Set the metadatafieldMapping used in the transforming of a record to actual metadata
     * @param metadataFieldMapping
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;

        for (MetadataContributor MetadataValueContributor : MetadataValueContributors) {
            MetadataValueContributor.setMetadataFieldMapping(metadataFieldMapping);
        }
    }

    /**
     * a separate MetadataValue object is created for each index of MetadataValue returned from the calls to
     * MetadataValueContributor.contributeMetadata(t) for each MetadataValueContributor in the MetadataValueContributors list.
     * We assume that each contributor returns the same amount of MetadataValue objects
     * @param t the object we are trying to translate
     * @return a collection of metadata composed by each MetadataContributor
     */
    @Override
    public Collection<MetadataValueDTO> contributeMetadata(T t) {
        List<MetadataValueDTO> values=new LinkedList<>();

        LinkedList<LinkedList<MetadataValueDTO>> MetadataValueLists = new LinkedList<>();

        for (MetadataContributor MetadataValueContributor : MetadataValueContributors) {
            LinkedList<MetadataValueDTO> MetadataValues = (LinkedList<MetadataValueDTO>) MetadataValueContributor.contributeMetadata(t);
            MetadataValueLists.add(MetadataValues);
        }

        for (int i = 0; i<MetadataValueLists.getFirst().size();i++) {

            StringBuilder value = new StringBuilder();

            for (LinkedList<MetadataValueDTO> MetadataValues : MetadataValueLists) {
                value.append(MetadataValues.get(i).getValue());

                if(!MetadataValues.equals(MetadataValueLists.getLast())) {
                    value.append(separator);
                }
            }
            values.add(metadataFieldMapping.toDCValue(field, value.toString()));
        }

        return values;
    }

    /**
     * Return the MetadataFieldConfig used while retrieving MetadataValueDTO
     * @return MetadataFieldConfig
     */
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     * @param field MetadataFieldConfig used while retrieving MetadataValueDTO
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Return the List of MetadataContributor objects set to this class
     * @return MetadataValueContributors, list of MetadataContributor
     */
    public LinkedList<MetadataContributor> getMetadataValueContributors() {
        return MetadataValueContributors;
    }

    /**
     * Set the List of MetadataContributor objects set to this class
     * @param MetadataValueContributors A list of MetadataValueContributor classes
     */
    public void setMetadataValueContributors(LinkedList<MetadataContributor> MetadataValueContributors) {
        this.MetadataValueContributors = MetadataValueContributors;
    }

    /**
     * Return the separator used to differentiate between distinct values
     * @return the separator used to differentiate between distinct values
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Set the separator used to differentiate between distinct values
     * @param separator
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
