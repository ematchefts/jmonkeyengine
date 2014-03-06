/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.properties.TexturePropertyEditor;
import com.jme3.gde.core.properties.preview.DDSPreview;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import com.jme3.texture.Texture;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import jme3tools.converters.ImageToAwt;
import org.openide.util.ImageUtilities;

public abstract class CellRendererEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer
{
    private final TerrainEditorTopComponent topComponent;

    public CellRendererEditor(TerrainEditorTopComponent topComponent)
    {
        this.topComponent = topComponent;
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return getButton(value, row, column);

    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return getButton(value, row, column);
    }

    protected abstract void setTextureInModel(int row, String path);

    protected abstract void setTextureInModel(int row, Texture tex);

    protected abstract Texture getTextureFromModel(int index);

    protected abstract boolean supportsNullTexture();

    private JButton getButton(Object value, final int row, final int column)
    {
        Node terrain = topComponent.getTerrainEditorToolController().getTerrain(null);

        String buttonName = new StringBuilder()
                .append(terrain.getName())
                .append("-").append(row)
                .append("-").append(column)
                .toString();

        // JButton button = topComponent.getButtons().get(row + "-" + column);
        JButton button = topComponent.getButtons().get(buttonName);

        if (button == null)
        {
            final JButton lbl = new JButton();

            // topComponent.getButtons().put(row + "-" + column, lbl);
            topComponent.getButtons().put(buttonName, lbl);

            //TODO check if there is a normal or a texture here at this index
            if (value == null)
            {
                value = topComponent.getTerrainTextureController().getTableModel().getValueAt(row, column);
            }

            if (value != null)
            {
                int index = 0;

                // this is messy, fix it so we know what values are coming in from where:
                if (value instanceof String)
                {
                    index = new Float((String) value).intValue();
                }
                else if (value instanceof Float)
                {
                    index = ((Float) value).intValue();
                }
                else if (value instanceof Integer)
                {
                    index = (Integer) value;
                }

                Texture tex = getTextureFromModel(index); // delegate to sub-class

                //Texture tex = SceneApplication.getApplication().getAssetManager().loadTexture((String)value);

                if (tex != null)
                {
                    String selected = tex.getKey().getName();

                    if (selected.toLowerCase().endsWith(".dds"))
                    {
                        if (topComponent.getTerrainTextureController().ddsPreview == null)
                        {
                            topComponent.getTerrainTextureController().ddsPreview = new DDSPreview((ProjectAssetManager) SceneApplication.getApplication().getAssetManager());
                        }

                        topComponent.getTerrainTextureController().ddsPreview.requestPreview(selected, "", 80, 80, lbl, null);

                    }
                    else
                    {
                        Icon icon = ImageUtilities.image2Icon(ImageToAwt.convert(tex.getImage(), false, true, 0));
                        lbl.setIcon(icon);
                    }
                }
            }

            lbl.addActionListener(new ActionListener()
            {

                public void actionPerformed(ActionEvent e)
                {
                    if (topComponent.getTerrainTextureController().alreadyChoosing)
                        return;

                    topComponent.getTerrainTextureController().alreadyChoosing = true;

                    try
                    {
                        Texture selectedTex = getTextureFromModel(row); // delegates to sub class

                        if (selectedTex == null && !topComponent.getTerrainTextureController().availableNormalTextures) // bail if we are at our texture limit
                            return;

                        TexturePropertyEditor editor = new TexturePropertyEditor(selectedTex);
                        Component view = editor.getCustomEditor();
                        view.setVisible(true);
                        Texture tex = (Texture) editor.getValue();

                        if (editor.getValue() != null)
                        {
                            String selected = tex.getKey().getName();

                            if (selected.toLowerCase().endsWith(".dds"))
                            {
                                if (topComponent.getTerrainTextureController().ddsPreview == null)
                                {
                                    topComponent.getTerrainTextureController().ddsPreview = new DDSPreview((ProjectAssetManager) SceneApplication.getApplication().getAssetManager());
                                }

                                topComponent.getTerrainTextureController().ddsPreview.requestPreview(selected, "", 80, 80, lbl, null);

                            }
                            else
                            {
                                Icon newicon = ImageUtilities.image2Icon(ImageToAwt.convert(tex.getImage(), false, true, 0));
                                lbl.setIcon(newicon);
                            }
                        }
                        else if (supportsNullTexture())
                        {
                            lbl.setIcon(null);
                        }

                        setTextureInModel(row, tex);
                    }
                    finally
                    {
                        topComponent.getTerrainTextureController().alreadyChoosing = false;
                    }
                }
            });

            return lbl;
        }

        return button;
    }
}

