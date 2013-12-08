package com.jme3.material.util;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.FixedFuncBinding;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.Technique;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Debug;
import com.jme3.renderer.GL1Renderer;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.util.TempVars;

public class LightingUtil {
    
    private static final RenderState RS_ADDITIVE_LIGHT = new RenderState();
    
    static {
        RS_ADDITIVE_LIGHT.setBlendMode(BlendMode.AlphaAdditive);
        RS_ADDITIVE_LIGHT.setDepthWrite(false);
    }
    
    public static ColorRGBA getAmbientColor(LightList lightList, ColorRGBA store) {
        store.set(0, 0, 0, 1);
        for (int j = 0; j < lightList.size(); j++) {
            Light l = lightList.get(j);
            if (l instanceof AmbientLight) {
                store.addLocal(l.getColor());
            }
        }
        store.a = 1.0f;
        return store;
    }
    
    private static void preMaterialRenderFixedFunc(RenderManager renderManager, Material material, Technique technique) {
        // Apply material's render state.
        GL1Renderer renderer = (GL1Renderer) renderManager.getRenderer();
        renderer.applyRenderState(material.getMergedRenderState(renderManager));
        
        // Set fixed function bindings.
        for (MatParam param : material.getParams()) {
            FixedFuncBinding ffBinding = param.getFixedFuncBinding();
            if (ffBinding != null) {
                Object value = param.getValue();
                renderer.setFixedFuncBinding(ffBinding, value);
            }
            if (param.getVarType().isTextureType()) {
                MatParamTexture texParam = (MatParamTexture) param;
                renderer.setTexture(texParam.getUnit(), texParam.getTextureValue());
            }
        }
    }
    
    private static void preMaterialRender(RenderManager renderManager, Material material, Technique technique, Shader shader) {
        // Apply material's render state.
        Renderer renderer = renderManager.getRenderer();
        renderer.applyRenderState(material.getMergedRenderState(renderManager));
        
        // Reset unchanged uniform flag
        shader.clearUniformsSetByCurrentFlag();
        
        // Apply uniform bindings.
        renderManager.updateUniformBindings(shader.getUniformMap());
        
        // Assume that all uniforms bound to world parameters have been updated ..
        for (Uniform uniform : shader.getUniformMap().values()) {
            if (uniform.getLocation() >= 0 && uniform.getBinding() != null && !uniform.isSetByCurrentMaterial()) {
                throw new AssertionError();
            }
        }
        
        // Apply material parameters.
        for (MatParam param : material.getParams()) {
            Uniform uniform = shader.getUniform(param.getPrefixedName());
            if (param.getVarType().isTextureType()) {
                MatParamTexture texParam = (MatParamTexture) param;
                uniform.setValue(VarType.Int, texParam.getUnit());
                renderer.setTexture(texParam.getUnit(), texParam.getTextureValue());
            } else {
                uniform.setValue(param.getVarType(), param.getValue());
            }
        }
    }
  
    public static void renderFixedFunc(RenderManager renderManager, Material material, Technique technique, Geometry geometry, boolean renderLighting) {
        if (renderLighting) {
            Debug.printDebug(geometry.getName() + " (fixed pipeline - with lighting)");
        } else {
            Debug.printDebug(geometry.getName() + " (fixed pipeline - no lighting)");
        }
        
        Renderer renderer = renderManager.getRenderer();
        preMaterialRenderFixedFunc(renderManager, material, technique);
        if (renderLighting) {
            renderer.setLighting(geometry.getWorldLightList());
        } else {
            renderer.setLighting(null);
        }
        renderer.renderMesh(geometry.getMesh(), geometry.getLodLevel(), 1);
    }

    public static void renderNoLighting(RenderManager renderManager, Material material, Technique technique, Geometry geometry) {
        Debug.printDebug(geometry.getName() + " (no lighting)");
        
        Renderer renderer = renderManager.getRenderer();
        Shader shader = technique.acquireUnlitShader(material.getMaterialDef().getAssetManager());
        preMaterialRender(renderManager, material, technique, shader);
        shader.resetUniformsNotSetByCurrent();
        renderer.setShader(shader);
        renderer.renderMesh(geometry.getMesh(), geometry.getLodLevel(), 1);
    }
    
    public static void renderSinglePassLighting(RenderManager renderManager, Material material, Technique technique, Geometry geometry) {
        Debug.printDebug(geometry.getName() + " (singlepass lighting)");

        Renderer renderer = renderManager.getRenderer();
        AssetManager assetManager = material.getMaterialDef().getAssetManager();
        
        // Find which shader to use.
        Shader lightShader = technique.acquireLightShader(assetManager, 0);
        
        preMaterialRender(renderManager, material, technique, lightShader);
        
        Uniform numLights = lightShader.getUniform("g_NumLights");
        Uniform lightColor = lightShader.getUniform("g_LightColor");
        Uniform lightPos = lightShader.getUniform("g_LightPosition");
        Uniform lightDir = lightShader.getUniform("g_LightDirection");
        
        lightColor.setVector4Length(LightingConstants.MAX_LIGHTS);
        lightPos.setVector4Length(LightingConstants.MAX_LIGHTS);
        lightDir.setVector4Length(LightingConstants.MAX_LIGHTS);

        LightList lightList = geometry.getWorldLightList();
        
        TempVars vars = TempVars.get();
        ColorRGBA ambientColor = vars.color;
        Uniform ambientColorUniform = lightShader.getUniform("g_AmbientLightColor");
        ambientColorUniform.setValue(VarType.Vector4, getAmbientColor(lightList, ambientColor));

        int lightIndex = 0;
        for (int i = 0; i < lightList.size(); i++) {
            if (lightIndex >= LightingConstants.MAX_LIGHTS) {
                break;
            }
            
            Light light = lightList.get(i); // Index in LightList!
            ColorRGBA color = light.getColor();
            lightColor.setVector4InArray(color.getRed(),
                                         color.getGreen(),
                                         color.getBlue(),
                                         light.getType().getId(),
                                         lightIndex); // Index in uniform!

            switch (light.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) light;
                    Vector3f dir = dl.getDirection();
                    lightPos.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), 0, lightIndex);
                    break;
                case Point:
                    PointLight pl = (PointLight) light;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();
                    lightPos.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, lightIndex);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) light;
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
                    throw new UnsupportedOperationException("Unknown type of light: " + light.getType());
            }
            lightIndex++;
        }
        for (int i = lightIndex; i < LightingConstants.MAX_LIGHTS; i++) {
            lightColor.setVector4InArray(0f, 0f, 0f, 0f, i);
            lightPos.setVector4InArray(0f, 0f, 0f, 0f, i);
        }
        
        numLights.setValue(VarType.Int, lightIndex);
        
        lightShader.resetUniformsNotSetByCurrent();
        
        renderer.setShader(lightShader);
        renderer.renderMesh(geometry.getMesh(), geometry.getLodLevel(), 1);
        
        vars.release();
    }
    
    public static void renderMultiPassLighting(RenderManager renderManager, Material material, Technique technique, Geometry geometry, boolean useDefines) {
        Debug.printDebug(geometry.getName() + " (multipass lighting)");
        
        LightList lightList = geometry.getWorldLightList();
        int lightIndex = 0;

        for (int i = 0; i < lightList.size(); i++) {
            Light light = lightList.get(i);
            if (light instanceof AmbientLight) {
                continue;
            }

            renderOneLight(renderManager, material, technique, geometry, light, lightIndex, useDefines);
            lightIndex++;
        }

        if (lightIndex == 0) {
            // There are only ambient lights in the scene. Render
            // a dummy "normal light" so we can see the ambient
            renderAmbientOnly(renderManager, material, technique, geometry, useDefines);
        }
    }
    
    public static void renderAmbientOnly(RenderManager renderManager, Material material, Technique technique, Geometry geometry, boolean useDefines) {
        Renderer renderer = renderManager.getRenderer();
        
        // Choose the right shader
        AssetManager assetManager = material.getMaterialDef().getAssetManager();

        // Ambient Color
        TempVars vars = TempVars.get();
        ColorRGBA ambientColor = vars.color;
        
        Debug.printDebug(geometry.getName() + " (ambient pass only)");
            
        // Ambient Only
        Shader lightShader;
        if (useDefines) {
            // If defines are used, get the multipass shader that handles 
            // ambient lights only.
            lightShader = technique.acquireLightShader(assetManager, LightingConstants.FLAG_LIGHTTYPE_AMBIENT);
        } else {
            // Defines are not used so the regular lighting shader is used.
            // Ensure the lighting computation results is nullified.
            lightShader = technique.acquireUnlitShader(assetManager);
            
            Uniform lightColorUniform = lightShader.getUniform("g_LightColor");
            Uniform lightPosUniform = lightShader.getUniform("g_LightPosition");
            lightColorUniform.setValue(VarType.Vector4, Vector3f.ZERO);
            lightPosUniform.setValue(VarType.Vector4, Vector3f.ZERO);
        }
        
        // Prepare material for rendering!
        preMaterialRender(renderManager, material, technique, lightShader);
        
        Uniform ambientColorUniform = lightShader.getUniform("g_AmbientLightColor");
        
        ambientColorUniform.setValue(VarType.Vector4, getAmbientColor(geometry.getWorldLightList(), ambientColor));
        
        lightShader.resetUniformsNotSetByCurrent();
        
        renderer.setShader(lightShader);
        renderer.renderMesh(geometry.getMesh(), geometry.getLodLevel(), 1);
        
        vars.release();
    }
    
    public static void renderOneLight(RenderManager renderManager, Material material, Technique technique, Geometry geometry, Light light, int lightNumber, boolean useDefines) {
        Renderer renderer = renderManager.getRenderer();
        
        // Choose the right shader
        AssetManager assetManager = material.getMaterialDef().getAssetManager();
        int flags = 0;
        
        // Lights
        DirectionalLight directionalLight = null;
        PointLight pointLight = null;
        SpotLight spotLight = null;
        
        // Ambient Color
        TempVars vars = TempVars.get();
        ColorRGBA ambientColor = vars.color;
        
        // Find the shader with the neccessary defines ..
        Debug.printDebug(geometry.getName() + " (pass " + (lightNumber + 1) + ", type=" + light.getType() + ")");

        switch (light.getType()) {
            case Directional:
                directionalLight = (DirectionalLight) light;
                flags = LightingConstants.FLAG_LIGHTTYPE_DIRECTIONAL;
                break;
            case Point:
                pointLight = (PointLight) light;
                flags = LightingConstants.FLAG_LIGHTTYPE_POINT
                        | (pointLight.getRadius() != 0f ? LightingConstants.FLAG_ATTENUATION : 0);
                break;
            case Spot:
                spotLight = (SpotLight) light;
                flags = LightingConstants.FLAG_LIGHTTYPE_SPOT
                        | (spotLight.getSpotRange() != 0f ? LightingConstants.FLAG_ATTENUATION : 0);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported light type: " + light.getType());
        }
        
        Shader lightShader;
        if (useDefines) {
            lightShader = technique.acquireLightShader(assetManager, flags);
        } else {
            lightShader = technique.acquireUnlitShader(assetManager);
        }
        
        // Prepare material for rendering!
        preMaterialRender(renderManager, material, technique, lightShader);
        
        Uniform ambientColorUniform = lightShader.getUniform("g_AmbientLightColor");
        
        if (lightNumber == 0) {
            // This is the first light, let's set the ambient light color.
            ambientColorUniform.setValue(VarType.Vector4, getAmbientColor(geometry.getWorldLightList(), ambientColor));
        } else if (lightNumber > 0) {
            ambientColorUniform.setValue(VarType.Vector4, ColorRGBA.Black);
            
            // Apply additive blending starting at 2nd pass
            renderer.applyRenderState(RS_ADDITIVE_LIGHT);
        }
        
        Uniform lightDirUniform = lightShader.getUniform("g_LightDirection");
        Uniform lightColorUniform = lightShader.getUniform("g_LightColor");
        Uniform lightPosUniform = lightShader.getUniform("g_LightPosition");

        Quaternion tmpLightDirection = vars.quat1;
        Quaternion tmpLightPosition = vars.quat2;
        ColorRGBA tmpLightColor = vars.color2;
        Vector4f tmpVec = vars.vect4f;

        ColorRGBA color = light.getColor();
        tmpLightColor.set(color);
        tmpLightColor.a = light.getType().getId();
        lightColorUniform.setValue(VarType.Vector4, tmpLightColor);

        switch (light.getType()) {
            case Directional:
                Vector3f dir = directionalLight.getDirection();
                tmpLightPosition.set(dir.getX(), dir.getY(), dir.getZ(), -1);
                lightPosUniform.setValue(VarType.Vector4, tmpLightPosition);
                tmpLightDirection.set(0, 0, 0, 0);
                lightDirUniform.setValue(VarType.Vector4, tmpLightDirection);
                break;
            case Point:
                Vector3f pos = pointLight.getPosition();
                float invRadius = pointLight.getInvRadius();

                tmpLightPosition.set(pos.getX(), pos.getY(), pos.getZ(), invRadius);
                lightPosUniform.setValue(VarType.Vector4, tmpLightPosition);
                tmpLightDirection.set(0, 0, 0, 0);
                lightDirUniform.setValue(VarType.Vector4, tmpLightDirection);
                break;
            case Spot:
                SpotLight sl = (SpotLight) spotLight;
                Vector3f pos2 = sl.getPosition();
                Vector3f dir2 = sl.getDirection();
                float invRange = sl.getInvSpotRange();
                float spotAngleCos = sl.getPackedAngleCos();

                tmpLightPosition.set(pos2.getX(), pos2.getY(), pos2.getZ(), invRange);
                lightPosUniform.setValue(VarType.Vector4, tmpLightPosition);

                // We transform the spot direction in view space here to save 5 varying later in the lighting shader
                // one vec4 less and a vec4 that becomes a vec3
                // the downside is that spotAngleCos decoding happen now in the frag shader.
                tmpVec.set(dir2.getX(), dir2.getY(), dir2.getZ(), 0);
                renderManager.getCurrentCamera().getViewMatrix().mult(tmpVec, tmpVec);
                tmpLightDirection.set(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), spotAngleCos);

                lightDirUniform.setValue(VarType.Vector4, tmpLightDirection);

                break;
            default:
                throw new UnsupportedOperationException("Unknown type of light: " + light.getType());
        }
        
        lightShader.resetUniformsNotSetByCurrent();
        
        renderer.setShader(lightShader);
        renderer.renderMesh(geometry.getMesh(), geometry.getLodLevel(), 1);
        
        vars.release();
    }
}
