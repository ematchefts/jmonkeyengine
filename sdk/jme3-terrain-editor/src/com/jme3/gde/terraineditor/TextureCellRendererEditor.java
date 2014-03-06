package com.jme3.gde.terraineditor;

import com.jme3.texture.Texture;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

public class TextureCellRendererEditor extends CellRendererEditor
{
    private final TerrainEditorTopComponent topComponent;

    public TextureCellRendererEditor(TerrainEditorTopComponent topComponent)
    {
        super(topComponent);

        this.topComponent = topComponent;
    }


    @Override
    public Object getCellEditorValue() {
        int row = topComponent.getTextureTable().getSelectedRow();
        if (row < 0) {
            return null;
        }
        return topComponent.getTerrainTextureController().getTableModel().getValueAt(row, 1);
    }

    @Override
    protected void setTextureInModel(int row, String path) {
        if (path != null) {
            topComponent.getTerrainTextureController().getTableModel().setTexture(row, path);
        }
    }

    @Override
    protected void setTextureInModel(int row, Texture tex) {
        if (tex != null) {
            topComponent.getTerrainTextureController().getTableModel().setTexture(row, tex);
        }
    }

    @Override
    protected Texture getTextureFromModel(int index) {
        return topComponent.getTerrainTextureController().getDiffuseTexture(index);
    }

    @Override
    protected boolean supportsNullTexture() {
        return false;
    }
}