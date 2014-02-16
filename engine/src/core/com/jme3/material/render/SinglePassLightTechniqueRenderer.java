package com.jme3.material.render;

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import static com.jme3.light.Light.Type.Ambient;
import static com.jme3.light.Light.Type.Directional;
import static com.jme3.light.Light.Type.Point;
import static com.jme3.light.Light.Type.Spot;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.Technique;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;

public class SinglePassLightTechniqueRenderer extends AbstractTechniqueRenderer {

    /**
     * Uploads the lights in the light list as two uniform arrays.
     * <p>
     * <code>uniform vec4 g_LightColor[numLights];</code><br/>
     * g_LightColor.rgb is the diffuse/specular color of the light.<br/>
     * g_Lightcolor.a is the type of light, 0 = Directional, 1 = Point, 2 = Spot.<br/><br/>
     * <code>uniform vec4 g_LightPosition[numLights];</code><br/>
     * g_LightPosition.xyz is the position of the light (for point lights) 
     * or the direction of the light (for directional lights).<br/>
     * g_LightPosition.w is the inverse radius (1/r) of the light (for
     * attenuation)<br/>
     * </p>
     * 
     * @param shader The shader on which the lightlist uniforms are to be set.
     * @param g The geometry from where to retrieve the light list.
     * @param numLights The size of the light arrays in the shader.
     */
    protected void updateLightListUniforms(Shader shader, Geometry g, int numLights) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return;
        }

        LightList lightList = g.getWorldLightList();
        Uniform lightColor = shader.getUniform("g_LightColor");
        Uniform lightPos = shader.getUniform("g_LightPosition");
        Uniform lightDir = shader.getUniform("g_LightDirection");
        
        lightColor.setVector4Length(numLights);
        lightPos.setVector4Length(numLights);
        lightDir.setVector4Length(numLights);

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");
        ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList));

        int lightIndex = 0;

        for (int i = 0; i < numLights; i++) {
            if (lightList.size() <= i) {
                lightColor.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);
                lightPos.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);
            } else {
                Light l = lightList.get(i);
                ColorRGBA color = l.getColor();
                lightColor.setVector4InArray(color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        l.getType().getId(),
                        i);

                switch (l.getType()) {
                    case Directional:
                        DirectionalLight dl = (DirectionalLight) l;
                        Vector3f dir = dl.getDirection();
                        lightPos.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, lightIndex);
                        break;
                    case Point:
                        PointLight pl = (PointLight) l;
                        Vector3f pos = pl.getPosition();
                        float invRadius = pl.getInvRadius();
                        lightPos.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, lightIndex);
                        break;
                    case Spot:
                        SpotLight sl = (SpotLight) l;
                        Vector3f pos2 = sl.getPosition();
                        Vector3f dir2 = sl.getDirection();
                        float invRange = sl.getInvSpotRange();
                        float spotAngleCos = sl.getPackedAngleCos();

                        lightPos.setVector4InArray(pos2.getX(), pos2.getY(), pos2.getZ(), invRange, lightIndex);
                        lightDir.setVector4InArray(dir2.getX(), dir2.getY(), dir2.getZ(), spotAngleCos, lightIndex);
                        break;
                    case Ambient:
                        // skip this light. Does not increase lightIndex
                        continue;
                    default:
                        throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
                }
            }

            lightIndex++;
        }

        while (lightIndex < numLights) {
            lightColor.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);
            lightPos.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);

            lightIndex++;
        }
    }
    
    public void postRender(Material material, Technique technique, Geometry geom, RenderManager renderManager) {
        Renderer renderer = renderManager.getRenderer();
        Shader shader = technique.getShader();

        updateLightListUniforms(technique.getShader(), geom, 4);
        resetUniformsNotSetByCurrent(shader);
        renderer.setShader(shader);
        renderer.renderMesh(geom.getMesh(), geom.getLodLevel(), 1, null);
    }
    
}
