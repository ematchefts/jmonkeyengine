package com.jme3.gde.terraineditor;

import com.jme3.gde.core.properties.preview.DDSPreview;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.terraineditor.tools.PaintTerrainToolAction;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.JTable;
import org.openide.util.Exceptions;

/**
 *
 * @author
 * jayfella
 */
public class TerrainTextureController
{
    private final TerrainEditorTopComponent topComponent;
    private final JTable textureTable;

    protected DDSPreview ddsPreview;
    protected boolean alreadyChoosing = false; // used for texture table selection
    protected boolean availableNormalTextures;

    protected boolean alphaLayersChanged = false;

    public static final String DEFAULT_TERRAIN_TEXTURE = "com/jme3/gde/terraineditor/dirt.jpg";
    public static final float DEFAULT_TEXTURE_SCALE = 16.0625f;
    public static final int NUM_ALPHA_TEXTURES = 3;
    protected final int MAX_TEXTURES = 16 - NUM_ALPHA_TEXTURES; // 16 max (diffuse and normal), minus the ones we are reserving
    protected final int MAX_DIFFUSE = 12;

    // used to remember the last texture used when the terrainquad was in focus.
    private Map<String, Integer> lastSelectedTexture;

    public TerrainTextureController(TerrainEditorTopComponent topComponent)
    {
        this.topComponent = topComponent;
        this.textureTable = topComponent.getTextureTable();

        lastSelectedTexture = new HashMap<String, Integer>();
    }

    // get the texture that was selected the last time the terrainquad was in focus.
    public int getSelectedTextureReminder(String terrainName)
    {
        Integer textureNum =  lastSelectedTexture.get(terrainName);

        return (textureNum == null) ? -1 : textureNum;
    }

    // set the texture that was last used when it was in focus.
    public void setSelectedTextureReminder(String terrainName, int texNum)
    {
        if (texNum < 0)
            return;

        lastSelectedTexture.put(terrainName, texNum);
    }

    public void clearTextureTable()
    {
        TextureCellRendererEditor rendererTexturer = new TextureCellRendererEditor(topComponent);

        textureTable.getColumnModel().getColumn(1).setCellRenderer(rendererTexturer); // diffuse
        textureTable.getColumnModel().getColumn(1).setCellEditor(rendererTexturer);

        NormalCellRendererEditor rendererNormal = new NormalCellRendererEditor(topComponent);
        textureTable.getColumnModel().getColumn(2).setCellRenderer(rendererNormal); // normal
        textureTable.getColumnModel().getColumn(2).setCellEditor(rendererNormal);

        // empty out the table
        while (textureTable.getModel().getRowCount() > 0)
        {
            ((TextureTableModel) textureTable.getModel()).removeRow(0);
        }
    }

    /**
     * re-initialize the texture rows in the texture table to match the given terrain.
     */
    protected void reinitTextureTable()
    {

        if (topComponent.getTerrainEditorToolController() == null)
            return; // we are not initialized yet


        clearTextureTable();
        getTableModel().initModel();

        if (textureTable.getRowCount() > 0)
        {
            topComponent.getTerrainToolController().setSelectedTextureIndex(0);
        }
        else
        {
            topComponent.getTerrainToolController().setSelectedTextureIndex(-1);
        }

        topComponent.getTerrainTextureController().enableTextureButtons();
        topComponent.getTriplanarCheckBox().setSelected(topComponent.getTerrainEditorToolController().isTriPlanarEnabled());
        topComponent.getShininessField().setText("" + topComponent.getTerrainEditorToolController().getShininess());

        // try to get the last selected texture used on this terrainquad
        // else just select first in the table if it exists.

        Node currentTerrain = topComponent.getTerrainEditorToolController().getTerrain(null);
        String terrainName = currentTerrain.getName();

        int lastSelectedRow = getSelectedTextureReminder(terrainName);

        if (lastSelectedRow == -1 || lastSelectedRow >= this.getTableModel().getRowCount())
            lastSelectedRow = 0;

        if (this.getTableModel().getRowCount() > 0)
        {
            textureTable.setRowSelectionInterval(lastSelectedRow, lastSelectedRow);
            topComponent.getTerrainToolController().setSelectedTextureIndex(lastSelectedRow);
        }
    }


    protected TextureTableModel getTableModel()
    {
        return (TextureTableModel) textureTable.getModel();
    }

    /**
     * Get the diffuse texture at the specified layer.
     * Blocks on the GL thread!
     */
    public Texture getDiffuseTexture(final int layer)
    {
        if (SceneApplication.getApplication().isOgl())
        {
            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return null;

            MatParam matParam = null;

            if (layer == 0)
                matParam = terrain.getMaterial().getParam("DiffuseMap");
            else
                matParam = terrain.getMaterial().getParam("DiffuseMap_" + layer);

            if (matParam == null || matParam.getValue() == null)
            {
                return null;
            }

            Texture tex = (Texture) matParam.getValue();
            return tex;
        }
        else
        {
            try
            {

                Callable<Texture> callable = new Callable<Texture>()
                {
                    @Override
                    public Texture call() throws Exception
                    {
                        return getDiffuseTexture(layer);
                    }
                };

                Texture tex = SceneApplication.getApplication().enqueue(callable).get();
                return tex;

            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            return null;
        }
    }

    /**
     * Get the normal map texture at the specified layer.
     * Run this on the GL thread!
     */
    public Texture getNormalMap(final int layer)
    {
        if (SceneApplication.getApplication().isOgl())
        {
            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return null;

            MatParam matParam = null;

            if (layer == 0)
                matParam = terrain.getMaterial().getParam("NormalMap");
            else
                matParam = terrain.getMaterial().getParam("NormalMap_"+layer);

            if (matParam == null || matParam.getValue() == null)
            {
                return null;
            }

            Texture tex = (Texture) matParam.getValue();
            return tex;
        }
        else
        {
            try
            {
                Callable<Texture> callable = new Callable<Texture>()
                {
                    public Texture call() throws Exception
                    {
                        return getNormalMap(layer);
                    }
                };

                Texture tex = SceneApplication.getApplication().enqueue(callable).get();
                return tex;
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }

        return null;
    }

    /**
     * Is there a texture at the specified layer?
     * Blocks on ogl thread
     */
    public boolean hasTextureAt(final int i)
    {
        if (SceneApplication.getApplication().isOgl())
        {
            Texture tex = getDiffuseTexture(i);
            return tex != null;
        }
        else
        {
            try
            {
                Callable<Boolean> callable = new Callable<Boolean>()
                {
                    public Boolean call() throws Exception
                    {
                        return hasTextureAt(i);
                    }
                };

                Boolean result = SceneApplication.getApplication().enqueue(callable).get();
                return result;
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            return false;
        }
    }

    /**
     * Get the scale of the texture at the specified layer.
     * Blocks on the OGL thread
     */
    public Float getTextureScale(final int layer)
    {
        if (SceneApplication.getApplication().isOgl())
        {
            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return 1f;

            MatParam matParam = null;
            matParam = terrain.getMaterial().getParam("DiffuseMap_"+layer+"_scale");

            if (matParam == null)
                return -1f;

            return (Float) matParam.getValue();
        }
        else
        {
            try
            {
                Callable<Float> callable = new Callable<Float>()
                {
                    public Float call() throws Exception
                    {
                        return getTextureScale(layer);
                    }
                };

                Float scale = SceneApplication.getApplication().enqueue(callable).get();
                return scale;
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }

        return null;
    }

    /**
     * How many textures are currently being used.
     * Blocking call on GL thread
     */
    protected int getNumUsedTextures()
    {
        if (SceneApplication.getApplication().isOgl())
        {
            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return 0;

            int count = 0;

            for (int i=0; i<MAX_TEXTURES; i++)
            {
                Texture tex = getDiffuseTexture(i);

                if (tex != null)
                    count++;

                tex = getNormalMap(i);

                if (tex != null)
                    count++;
            }

            return count;
        }
        else
        {
            try
            {
                // Integer count =
                  // SceneApplication.getApplication().enqueue(new Callable<Integer>() {

                Callable<Integer> callable = new Callable<Integer>()
                {
                    public Integer call() throws Exception
                    {
                        return getNumUsedTextures();
                    }
                };

                Integer count = SceneApplication.getApplication().enqueue(callable).get();
                return count;
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            return -1;
        }
    }

    /**
     * Enable/disable the add and remove texture buttons based
     * on how many textures are currently being used.
     */
    protected void enableTextureButtons()
    {
        final int numAvailable = MAX_TEXTURES-getNumUsedTextures();
        final boolean add = getNumDiffuseTextures() < MAX_DIFFUSE && numAvailable > 0;
        final boolean remove = getNumDiffuseTextures() > 1;

        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                topComponent.enableAddTextureButton(add);
                topComponent.enableRemoveTextureButton(remove);
                topComponent.updateTextureCountLabel(numAvailable);
                topComponent.setAddNormalTextureEnabled(numAvailable>0);
            }
        });
    }

    /**
     * Set the diffuse texture at the specified layer.
     * Blocks on the GL thread
     * @param layer number to set the texture
     * @param texturePath if null, the default texture will be used
     */
    public void setDiffuseTexture(final int layer, final String texturePath)
    {
        String path = texturePath;
        if (texturePath == null || texturePath.equals(""))
            path = DEFAULT_TERRAIN_TEXTURE;

        Texture tex = SceneApplication.getApplication().getAssetManager().loadTexture(path);
        setDiffuseTexture(layer, tex);
    }

    /**
     * Set the diffuse texture at the specified layer.
     * Blocks on the GL thread
     * @param layer number to set the texture
     */
    public void setDiffuseTexture(final int layer, final Texture texture)
    {
        if (SceneApplication.getApplication().isOgl())
        {
            texture.setWrap(Texture.WrapMode.Repeat);

            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return;

            if (layer == 0)
                terrain.getMaterial().setTexture("DiffuseMap", texture);
            else
                terrain.getMaterial().setTexture("DiffuseMap_"+ layer, texture);

            topComponent.getTerrainEditorToolController().setNeedsSave(true);
        }
        else
        {
            try
            {
                SceneApplication.getApplication().enqueue(new Callable<Object>()
                {
                    public Object call() throws Exception
                    {
                        setDiffuseTexture(layer, texture);
                        return null;
                    }
                }).get();
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * Set the normal map at the specified layer.
     * Blocks on the GL thread
     */
    public void setNormalMap(final int layer, final String texturePath)
    {
        if (texturePath != null)
        {
            Texture tex = SceneApplication.getApplication().getAssetManager().loadTexture(texturePath);
            setNormalMap(layer, tex);
        }
        else
        {
            setNormalMap(layer, (Texture)null);
        }
    }

    /**
     * Set the normal map texture at the specified layer
     */
    public void setNormalMap(final int layer, final Texture texture)
    {
        if (SceneApplication.getApplication().isOgl())
        {
            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return;

            if (texture == null)
            {
                // remove the texture if it is null
                if (layer == 0)
                    terrain.getMaterial().clearParam("NormalMap");
                else
                    terrain.getMaterial().clearParam("NormalMap_"+layer);
                return;
            }

            texture.setWrap(Texture.WrapMode.Repeat);

            if (layer == 0)
                terrain.getMaterial().setTexture("NormalMap", texture);
            else
                terrain.getMaterial().setTexture("NormalMap_"+layer, texture);

            topComponent.getTerrainEditorToolController().setNeedsSave(true);
        }
        else
        {
            try
            {
                SceneApplication.getApplication().enqueue(new Callable<Object>()
                {
                    public Object call() throws Exception
                    {
                        setNormalMap(layer, texture);
                        return null;
                    }
                }).get();
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * Remove a whole texture layer: diffuse and normal map
     * @param layer
     * @param texturePath
     */
    public void removeTextureLayer(final int layer) {
        if (SceneApplication.getApplication().isOgl()) {
            doRemoveDiffuseTexture(layer);
            doRemoveNormalMap(layer);
            doClearAlphaMap(layer);
        } else {
            try {
                SceneApplication.getApplication().enqueue(new Callable<Object>()
                {
                    public Object call() throws Exception {
                        removeTextureLayer(layer);
                        return null;
                    }
                }).get();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void doRemoveDiffuseTexture(int layer)
    {
        // Terrain terrain = (Terrain) getTerrain(null);
        Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

        if (terrain == null)
            return;

        if (layer == 0)
            terrain.getMaterial().clearParam("DiffuseMap");
        else
            terrain.getMaterial().clearParam("DiffuseMap_"+layer);

        topComponent.getTerrainEditorToolController().setNeedsSave(true);
    }

    private void doRemoveNormalMap(int layer)
    {
        // Terrain terrain = (Terrain) getTerrain(null);
        Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

        if (terrain == null)
            return;

        if (layer == 0)
            terrain.getMaterial().clearParam("NormalMap");
        else
            terrain.getMaterial().clearParam("NormalMap_"+layer);

        topComponent.getTerrainEditorToolController().setNeedsSave(true);
    }

    private void doClearAlphaMap(int selectedTextureIndex)
    {
        // Terrain terrain = (Terrain) getTerrain(null);
        Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

        if (terrain == null)
            return;

        int alphaIdx = selectedTextureIndex/4; // 4 = rgba = 4 textures
        int texIndex = selectedTextureIndex - ((selectedTextureIndex/4)*4); // selectedTextureIndex/4 is an int floor
        //selectedTextureIndex - (alphaIdx * 4)
        Texture tex = doGetAlphaTexture(terrain, alphaIdx);
        Image image = tex.getImage();

        PaintTerrainToolAction paint = new PaintTerrainToolAction();

        ColorRGBA color = ColorRGBA.Black;
        for (int y=0; y<image.getHeight(); y++) {
            for (int x=0; x<image.getWidth(); x++) {

                paint.manipulatePixel(image, x, y, color, false); // gets the color at that location (false means don't write to the buffer)
                switch (texIndex) {
                    case 0:
                        color.r = 0; break;
                    case 1:
                        color.g = 0; break;
                    case 2:
                        color.b = 0; break;
                    case 3:
                        color.a = 0; break;
                }
                color.clamp();
                paint.manipulatePixel(image, x, y, color, true); // set the new color
            }
        }
        image.getData(0).rewind();
        tex.getImage().setUpdateNeeded();
        topComponent.getTerrainEditorToolController().setNeedsSave(true);
        alphaLayersChanged();
    }

    protected Texture doGetAlphaTexture(Terrain terrain, int alphaLayer)
    {
        if (terrain == null)
            return null;

        MatParam matParam = null;

        if (alphaLayer == 0)
            matParam = terrain.getMaterial().getParam("AlphaMap");
        else if(alphaLayer == 1)
            matParam = terrain.getMaterial().getParam("AlphaMap_1");
        else if(alphaLayer == 2)
            matParam = terrain.getMaterial().getParam("AlphaMap_2");

        if (matParam == null || matParam.getValue() == null)
        {
            return null;
        }

        Texture tex = (Texture) matParam.getValue();
        return tex;
    }

    /**
     * Painting happened and the alpha maps need saving.
     */
    public void alphaLayersChanged()
    {
        alphaLayersChanged = true;
    }

    /**
     * How many diffuse textures are being used.
     * Blocking call on GL thread
     */
    protected int getNumDiffuseTextures()
    {
        if (SceneApplication.getApplication().isOgl())
        {
            // Terrain terrain = (Terrain) getTerrain(null);
            Terrain terrain = (Terrain)topComponent.getTerrainEditorToolController().getTerrain(null);

            if (terrain == null)
                return 0;

            int count = 0;

            for (int i=0; i<MAX_TEXTURES; i++)
            {
                Texture tex = getDiffuseTexture(i);

                if (tex != null)
                    count++;
            }

            return count;
        }
        else
        {
            try
            {
                // Integer count =
                  // SceneApplication.getApplication().enqueue(new Callable<Integer>() {
                Callable<Integer> callable = new Callable<Integer>()
                {
                    public Integer call() throws Exception
                    {
                        return getNumDiffuseTextures();
                    }
                };

                Integer count = SceneApplication.getApplication().enqueue(callable).get();
                return count;
            }
            catch (InterruptedException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            catch (ExecutionException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            return -1;
        }
    }

    public void cleanup()
    {
        lastSelectedTexture.clear();
    }

}
