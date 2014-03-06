/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor;

import com.jme3.texture.Texture;

public class NormalCellRendererEditor extends CellRendererEditor
{
    private final TerrainEditorTopComponent topComponent;

    public NormalCellRendererEditor(TerrainEditorTopComponent topComponent)
    {
        super(topComponent);

        this.topComponent = topComponent;
    }

    @Override
    public Object getCellEditorValue()
    {
        int row = topComponent.getTextureTable().getSelectedRow();
        if (row < 0) {
            return null;
        }
        return topComponent.getTerrainTextureController().getTableModel().getValueAt(row, 2);
    }

    @Override
    protected void setTextureInModel(int row, String path) {
        topComponent.getTerrainTextureController().getTableModel().setNormal(row, path);
    }

    @Override
    protected void setTextureInModel(int row, Texture tex) {
        topComponent.getTerrainTextureController().getTableModel().setNormal(row, tex);
    }

    @Override
    protected Texture getTextureFromModel(int index) {

        return topComponent.getTerrainTextureController().getNormalMap(index);
    }

    @Override
    protected boolean supportsNullTexture() {
        return true;
    }
}
