/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.gde.core.sceneexplorer.SceneExplorerTopComponent;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;

/**
 *
 * @author Brent Owens
 */
public class SelectTool extends SceneEditTool {

    protected Spatial selected;
    private boolean wasDragging = false;

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, final JmeNode rootNode, DataObject dataObject) {
        if (!pressed && !wasDragging) {
            // mouse released and wasn't dragging, select a new spatial
            final Spatial result = pickWorldSpatial(getCamera(), screenCoord, rootNode);

            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    if (result != null) {
//                        System.out.println(rootNode.getChild(result).getName());
                        SceneViewerTopComponent.findInstance().setActivatedNodes(new org.openide.nodes.Node[]{rootNode.getChild(result)});
                        SceneExplorerTopComponent.findInstance().setSelectedNode(rootNode.getChild(result));

                    } else {
                        SceneViewerTopComponent.findInstance().setActivatedNodes(new org.openide.nodes.Node[]{rootNode});
                        SceneExplorerTopComponent.findInstance().setSelectedNode(rootNode);
                    }
                }
            });

            if (result != null) {
                doUpdateToolsTransformation();
            }
        }

        if (!pressed) {
            wasDragging = false;
        }
    }

    @Override
    public void actionSecondary(final Vector2f screenCoord, boolean pressed, final JmeNode rootNode, DataObject dataObject) {
        if (!pressed && !wasDragging) {
            final Vector3f result = pickWorldLocation(getCamera(), screenCoord, rootNode);
            if (result != null) {
                toolController.doSetCursorLocation(result);
            }
        }
        if (!pressed) {
            wasDragging = false;
        }
    }

    @Override
    public void mouseMoved(Vector2f screenCoord) {
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        wasDragging = pressed;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        wasDragging = pressed;
    }
}
