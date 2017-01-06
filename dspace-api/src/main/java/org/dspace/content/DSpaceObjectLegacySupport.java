/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Database Object interface interface class that adds that getLegacyId method which returns the old integer based identifier
 * that was used to identify DSpaceObjects prior to DSpace 6.0
 *
 * @author kevinvandevelde at atmire.com
 */
public interface DSpaceObjectLegacySupport {

    /**
     * @return the old integer based legacy identifier
     */
	@JsonIgnore
    public Integer getLegacyId();

}
