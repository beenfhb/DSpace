/**
 * Copyright 2012 Lyncode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lyncode.xoai.dataprovider.core;

import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.data.internal.MetadataFormatSuper;
import com.lyncode.xoai.dataprovider.data.internal.MetadataTransformer;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import com.lyncode.xoai.dataprovider.sets.StaticSet;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**

 * @version 3.1.0
 */
public class XOAIContext {
    private static Logger log = LogManager.getLogger(XOAIContext.class);
    private String baseUrl;
    private String name;
    private String description;
    private Condition condition;
    private Map<String, StaticSet> sets;
    private MetadataTransformer transformer;
    private Map<String, MetadataFormatSuper> formats;

    public XOAIContext(String baseUrl, String name, String description, MetadataTransformer transformer,
                       List<MetadataFormatSuper> formats, List<StaticSet> sets) {
        this.baseUrl = baseUrl;
        this.name = name;
        this.description = description;
        this.transformer = transformer;
        this.formats = new HashMap<String, MetadataFormatSuper>();
        for (MetadataFormatSuper mdf : formats)
            this.formats.put(mdf.getPrefix(), mdf);
        this.sets = new HashMap<String, StaticSet>();
        for (StaticSet s : sets)
            this.sets.put(s.getSetSpec(), s);
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public MetadataTransformer getTransformer() {
        return transformer;
    }

    private List<StaticSet> cachedSets = null;

    public List<StaticSet> getStaticSets() {
        if (cachedSets == null) {
            log.debug("{ XOAI } Static Sets for this ContextConfiguration: "
                    + sets.values().size());
            cachedSets = new ArrayList<StaticSet>(sets.values());
        }
        return cachedSets;
    }

    public Condition getSetFilter(String setID) {
        log.debug("{ XOAI } Getting StaticSet filters");
        return sets.get(setID).getCondition();
    }

    public MetadataFormatSuper getFormatByPrefix(String prefix)
            throws CannotDisseminateFormatException {
        for (MetadataFormatSuper format : this.formats.values())
            if (format.getPrefix().equals(prefix))
                return format;
        throw new CannotDisseminateFormatException(prefix);
    }

    public List<MetadataFormatSuper> getFormats() {
        return new ArrayList<MetadataFormatSuper>(formats.values());
    }

    public List<MetadataFormatSuper> getFormats(Item item) {
        List<MetadataFormatSuper> formats = new ArrayList<MetadataFormatSuper>();
        if (this.isItemShown(item)) {
            for (MetadataFormatSuper format : this.formats.values())
                if (item.isDeleted() || format.isApplicable(item))
                    formats.add(format);
        }
        return formats;
    }

    public boolean isItemShown(ItemIdentifier item) {
        if (hasCondition())
            return this.condition.getFilter().isItemShown(item);
        else
            return true;
    }

    public boolean isStaticSet(String setSpec) {
        for (StaticSet s : this.getStaticSets())
            if (s.getSetSpec().equals(setSpec))
                return true;
        return false;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        if (name == null) return this.baseUrl;
        return name;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}
