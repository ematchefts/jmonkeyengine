package com.jme3.gde.terraineditor;

import static com.jme3.gde.terraineditor.TerrainTextureController.DEFAULT_TEXTURE_SCALE;
import com.jme3.texture.Texture;
import javax.swing.table.DefaultTableModel;

/**
* Holds the table information and relays changes to that data to the actual
* terrain material. Info such as textures and texture scales.
*/
public class TextureTableModel extends DefaultTableModel
{
    private final TerrainEditorTopComponent topComponent;

    //private Material terrainMaterial;

    public TextureTableModel(TerrainEditorTopComponent topComponent)
    {
        super(new String[]{"", "Texture", "Normal", "Scale"}, 0);

        this.topComponent = topComponent;
    }

    public void initModel()
    {
        // empty the table
        while (getRowCount() > 0)
        {
            removeRow(0);
        }

        // fill the table with the proper data
        for (int i = 0; i < topComponent.getTerrainTextureController().MAX_TEXTURES; i++)
        {
            if (!topComponent.getTerrainTextureController().hasTextureAt(i))
            {
                continue;
            }

            Float scale = topComponent.getTerrainTextureController().getTextureScale(i);

            if (scale == null)
            {
                scale = DEFAULT_TEXTURE_SCALE;
            }

            addRow(new Object[]{"", i, i, scale});
        }
    }

    protected void updateScales() {
        for (int i = 0; i < topComponent.getTerrainTextureController().getNumUsedTextures(); i++) {
            float scale = topComponent.getTerrainTextureController().getTextureScale(i);
            setValueAt("" + scale, i, 3); // don't call this one's setValueAt, it will re-set the scales
        }
    }

    // it seems to keep the selection when we delete the row
    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (row < 0 || row > getRowCount() - 1) {
            return;
        }
        super.setValueAt(aValue, row, column);

        if (column == 3) {
            setTextureScale(row, new Float((String) aValue));
        }
    }

    protected void addNewTexture(int newIndex) {
        float scale = DEFAULT_TEXTURE_SCALE;

        // add it to the table model
        addRow(new Object[]{"", newIndex, null, scale}); // add to the table model

        // and add it to the actual material
        setTextureScale(newIndex, scale);
        setTexture(newIndex, (String) null);
        topComponent.getTerrainTextureController().enableTextureButtons();
    }

    protected void setTexture(final int index, final Texture texture) {
        setValueAt(index, index, 1);
        topComponent.getTerrainTextureController().setDiffuseTexture(index, texture);
    }

    protected void setTexture(final int index, final String texturePath) {
        setValueAt(index, index, 1);
        topComponent.getTerrainTextureController().setDiffuseTexture(index, texturePath);
    }

    protected void setNormal(final int index, final String texturePath) {
        setValueAt(index, index, 2);
        topComponent.getTerrainTextureController().setNormalMap(index, texturePath);
        topComponent.getTerrainTextureController().enableTextureButtons();
    }

    protected void setNormal(final int index, final Texture texture) {
        setValueAt(index, index, 2);
        topComponent.getTerrainTextureController().setNormalMap(index, texture);
        topComponent.getTerrainTextureController().enableTextureButtons();
    }

    protected void setTextureScale(int index, float scale) {
        // setTextureScale(index, scale);
        topComponent.getTerrainEditorToolController().setTextureScale(index, scale);
    }

    protected void removeTexture(final int index) {
        removeRow(index);
        topComponent.getTerrainTextureController().removeTextureLayer(index);
        topComponent.getTerrainTextureController().enableTextureButtons();
    }
}
