/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to convert the SearchFilter object into a SearchResultsRest.AppliedFilter object
 */
@Component
public class SearchFilterToAppliedFilterConverter {

//    @Autowired
//    private AuthorityValueService authorityValueService;

    public SearchResultsRest.AppliedFilter convertSearchFilter(Context context, SearchFilter searchFilter) {
//        TODO         
//        AuthorityValue authorityValue = null;
//        if(searchFilter.hasAuthorityOperator()) {
//            authorityValue = authorityValueService.findByUID(context, searchFilter.getValue());
//        }

        SearchResultsRest.AppliedFilter appliedFilter;
//        if (authorityValue == null) {
            appliedFilter = new SearchResultsRest.AppliedFilter(searchFilter.getName(), searchFilter.getOperator(),
                    searchFilter.getValue(), searchFilter.getValue());
//        } else {
//            appliedFilter = new SearchResultsRest.AppliedFilter(searchFilter.getName(), searchFilter.getOperator(),
//                    searchFilter.getValue(), authorityValue.getValue());
//        }

        return appliedFilter;
    }
}
