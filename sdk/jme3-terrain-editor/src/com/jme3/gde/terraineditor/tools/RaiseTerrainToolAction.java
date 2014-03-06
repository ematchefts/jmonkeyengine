/*
 * Copyright (c) 2009-2011 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.terraineditor.TerrainEditorController;
import com.jme3.gde.terraineditor.tools.TerrainTool.Meshes;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.NormalRecalcControl;
import java.util.ArrayList;
import java.util.List;

/**
 * Raise/lower the terrain, executed from the OpenGL thread.
 *
 * @author Brent Owens
 */
public class RaiseTerrainToolAction extends AbstractTerrainToolAction {

    private TerrainEditorController editorController;

    private Vector3f worldLoc;
    private float radius;
    private float height;
    private Meshes mesh;

    public RaiseTerrainToolAction(TerrainEditorController controller, Vector3f markerLocation, float radius, float height, Meshes mesh) {

        this.editorController = controller;
        this.worldLoc = markerLocation.clone();
        this.radius = radius;
        this.height = height;
        this.mesh = mesh;
        name = "Raise terrain";
    }

    public Object applyTool(AbstractSceneExplorerNode rootNode) {
        return doApplyTool(rootNode);
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        // Terrain terrain = getTerrain(rootNode.getLookup().lookup(Node.class));
        Terrain terrain = (Terrain)editorController.getTerrain(null);
        if (terrain == null)
            return null;
        modifyHeight(terrain, worldLoc, radius, height, mesh);
        return terrain;
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        if (undoObject == null)
            return;
        modifyHeight((Terrain)undoObject, worldLoc, radius, -height, mesh);
    }

    private void modifyHeight(Terrain terrain, Vector3f worldLoc, float radius, float heightDir, Meshes mesh) {

        int radiusStepsX = (int) (radius / ((Node)terrain).getWorldScale().x);
        int radiusStepsZ = (int) (radius / ((Node)terrain).getWorldScale().z);

        float xStepAmount = ((Node)terrain).getWorldScale().x;
        float zStepAmount = ((Node)terrain).getWorldScale().z;

        // Calculate the center of the terrain that the mouse is hovering over, instead of assuming
        // a singular position of Vector3f.ZERO
        Vector3f terrainPos = ((Node)terrain).getLocalTranslation();
        Vector3f workPos = worldLoc.subtractLocal(terrainPos);

        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();

        for (int z=-radiusStepsZ; z<radiusStepsZ; z++) {
            for (int x=-radiusStepsX; x<radiusStepsX; x++) {

                float locX = workPos.x + (x*xStepAmount);
                float locZ = workPos.z + (z*zStepAmount);

                // see if it is in the radius of the tool
                if (ToolUtils.isInMesh(locX-workPos.x,locZ-workPos.z,radius,mesh)) {
                    // adjust height based on radius of the tool
                    float h = ToolUtils.calculateHeight(radius, heightDir, locX-workPos.x, locZ-workPos.z);
                    // increase the height
                    locs.add(new Vector2f(locX, locZ));
                    heights.add(h);
                }
            }
        }

        // do the actual height adjustment
        terrain.adjustHeight(locs, heights);
        // terrain.setHeight(locs, heights);


        ((Node)terrain).updateModelBound(); // or else we won't collide with it where we just edited
    }



}
