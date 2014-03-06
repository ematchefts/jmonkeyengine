package com.jme3.gde.terraineditor;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author
 * jayfella
 */
public enum TerrainLightingAttribute
{
    // alpha maps
    AlphaMap_1("AlphaMap"),
    AlphaMap_2("AlphaMap_1"),
    AlphaMap_3("AlphaMap_2"),

    // diffuse maps
    DiffuseMap_1("DiffuseMap"),
    DiffuseMap_2("DiffuseMap_1");


    private static Map<String, TerrainLightingAttribute> mappings;

    private String attrName;

    private TerrainLightingAttribute(String attrName)
    {
        this.attrName = attrName;
    }

    public String getAttributeName() { return this.attrName; }

    public static TerrainLightingAttribute getAttribute(String attr)
    {
        if (mappings == null)
        {
            mappings = new HashMap<String, TerrainLightingAttribute>();

            for (TerrainLightingAttribute attribute : TerrainLightingAttribute.values())
                mappings.put(attribute.getAttributeName(), attribute);
        }

        return mappings.get(attr);
    }
}
