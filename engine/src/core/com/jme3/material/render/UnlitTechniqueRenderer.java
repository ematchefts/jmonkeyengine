package com.jme3.material.render;

import com.jme3.material.Material;
import com.jme3.material.Technique;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;

public class UnlitTechniqueRenderer extends AbstractTechniqueRenderer {

    public void postRender(Material material, Technique technique, Geometry geom, RenderManager renderManager) {
        Renderer renderer = renderManager.getRenderer();
        Shader shader = technique.getShader();

        // any unset uniforms will be set to 0
        resetUniformsNotSetByCurrent(shader);
        renderer.setShader(shader);
        renderer.renderMesh(geom.getMesh(), geom.getLodLevel(), 1);
    }
}
