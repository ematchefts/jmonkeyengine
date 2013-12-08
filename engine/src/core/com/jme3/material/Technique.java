/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.util.LightingConstants;
import com.jme3.shader.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Represents a technique instance.
 */
public class Technique /* implements Savable */ {

    private static final Logger logger = Logger.getLogger(Technique.class.getName());
    
    private TechniqueDef def;
    private Material owner;
//    private ArrayList<Uniform> worldBindUniforms;
    private DefineList allDefines;
    private Shader unlitShader;
    private Shader[] lightShaders;
    private boolean needReload = true;

    /**
     * Creates a new technique instance that implements the given
     * technique definition.
     * 
     * @param owner The material that will own this technique
     * @param def The technique definition being implemented.
     */
    public Technique(Material owner, TechniqueDef def) {
        this.owner = owner;
        this.def = def;
        if (def.isUsingShaders()) {
//            this.worldBindUniforms = new ArrayList<Uniform>();
            this.allDefines = new DefineList();
            if (def.getLightMode() != LightMode.Disable) {
                lightShaders = new Shader[LightingConstants.MAX_FLAGS];
            }
        }
    }

    /**
     * Serialization only. Do not use.
     */
    public Technique() {
    }

    /**
     * Returns the technique definition that is implemented by this technique
     * instance. 
     * 
     * @return the technique definition that is implemented by this technique
     * instance. 
     */
    public TechniqueDef getDef() {
        return def;
    }

    public Shader acquireUnlitShader(AssetManager assetManager) {
        if (unlitShader == null) {
            unlitShader = loadShader(assetManager);
            System.out.println("== Technique.acquireUnlitShader() - loading shader for " + owner + " ==");
            System.out.println("Technique: " + def.getName());
            System.out.println("Defines: " + allDefines);
        }
        return unlitShader;
    }
    
    /**
     * Returns the shader currently used by this technique instance.
     * <p>
     * Shaders are typically loaded dynamically when the technique is first
     * used, therefore, this variable will most likely be null most of the time.
     * 
     * @return the shader currently used by this technique instance.
     */
    @Deprecated
    public Shader getShader() {
        return null; //shader;
    }
    
    /**
     * @return The current defines that are used for the technique, based 
     * on the material's current parameters.
     * @deprecated Defines change depending on the light type or number of lights used..
     */
    @Deprecated
    public DefineList getAllDefines() {
        return allDefines;
    }
    
    public Shader acquireLightShader(AssetManager assetManager, int lightFlags) {
        Shader lightShader = lightShaders[lightFlags];
        if (lightShader == null) {
            if (def.getLightMode() == LightMode.MultiPass) {
                allDefines.set("NUMLIGHTS", VarType.Int, 1);
                allDefines.set("LIGHTTYPE", VarType.Int, lightFlags & LightingConstants.MASK_LIGHTTYPE);
                allDefines.set("ATTENUATION", VarType.Int, ((lightFlags & LightingConstants.MASK_ATTENUATION) != 0) ? 1 : 0);
            } else {
                allDefines.set("NUMLIGHTS", VarType.Int, LightingConstants.MAX_LIGHTS);
            }
            System.out.println("== Technique.acquireLightShader() - loading shader for " + owner + " ==");
            System.out.println("Technique: " + def.getName());
            System.out.println("Defines: " + allDefines);
            System.out.println("Flags: 0x" + Integer.toHexString(lightFlags));
            lightShader = loadShader(assetManager);
            lightShaders[lightFlags] = lightShader;
        }
        return lightShader;
    }

//    /**
//     * Returns a list of uniforms that implements the world parameters
//     * that were requested by the material definition.
//     * 
//     * @return a list of uniforms implementing the world parameters.
//     */
//    public List<Uniform> getWorldBindUniforms() {
//        return worldBindUniforms;
//    }

    /**
     * Called by the material to tell the technique a parameter was modified.
     * Specify <code>null</code> for value if the param is to be cleared.
     */
    void notifyParamChanged(String paramName, VarType type, Object value) {
        // Check if there's a define binding associated with this
        // parameter.
        String defineName = def.getShaderParamDefine(paramName);
        if (defineName != null) {
            // There is a define. Change it on the define list.
            // The "needReload" variable will determine
            // if the shader will be reloaded when the material
            // is rendered.
            
            if (value == null) {
                // Clear the define.
                needReload = allDefines.remove(defineName) || needReload;
            } else {
                // Set the define.
                needReload = allDefines.set(defineName, type, value) || needReload;
            }
        }
    }

    /**
     * Returns true if the technique must be reloaded.
     * <p>
     * If a technique needs to reload, then the {@link Material} should
     * call {@link #makeCurrent(com.jme3.asset.AssetManager) } on this
     * technique.
     * 
     * @return true if the technique must be reloaded.
     */
    public boolean isNeedReload() {
        return needReload;
    }

    /**
     * Prepares the technique for use by loading the shader and setting
     * the proper defines based on material parameters.
     * 
     * @param assetManager The asset manager to use for loading shaders.
     */
    public void makeCurrent(AssetManager assetManager, boolean techniqueSwitched) {
        if (!def.isUsingShaders()) {
            // No shaders are used, no processing is neccessary. 
            return;
        }
        
        if (techniqueSwitched) {
            // If the technique was switched, check if the define list changed
            // based on material parameters.
            DefineList newDefines = new DefineList();
            newDefines.addFrom(def.getShaderPresetDefines());
            Collection<MatParam> params = owner.getParams();
            for (MatParam param : params) {
                String defineName = def.getShaderParamDefine(param.getName());
                if (defineName != null) {
                    newDefines.set(defineName, param.getVarType(), param.getValue());
                }
            }
            
            // These are set dynamically (during lighting) and therefore do not apply
            // to the comparison.
            allDefines.remove("LIGHTTYPE");
            allDefines.remove("ATTENUATION");
            allDefines.remove("NUMLIGHTS");
            
            if (!allDefines.equals(newDefines)) {
                System.out.println(" == Technique.makeCurrent() - reloading shader for " + owner + " ==");
                System.out.println("Technique: " + def.getName());
                System.out.println("Old defines: " + allDefines + ", New defines: " + newDefines);
                // Defines were changed, update define list
                allDefines = newDefines;
                needReload = true;
            }
        }

        if (needReload) {
            if (lightShaders != null) {
                // We are using the light shaders.. Gotta reload them.
                Arrays.fill(lightShaders, null);
            } else {
                // We are using unlit shaders.. Gotta reload them.
                unlitShader = null;
            }
            needReload = false;
        }
    }

    private Shader loadShader(AssetManager assetManager) {
        // Load the shader from the cache.
        ShaderKey key = new ShaderKey(def.getVertexShaderName(),
                                      def.getFragmentShaderName(),
                                      allDefines.clone(),
                                      def.getVertexShaderLanguage(),
                                      def.getFragmentShaderLanguage());
        
        Shader loadedShader = assetManager.loadShader(key);

        // Register the world bound uniforms from the technique on the shader.
        // This allows the uniforms to be quickly set by
        // the uniform binding manager.
        // TODO: Maybe add this later??
        //ArrayList<Uniform> worldBindUniforms = loadedShader.getWorldBindUniforms();
        if (def.getWorldBindings() != null) {
            for (UniformBinding binding : def.getWorldBindings()) {
                Uniform uniform = loadedShader.getUniform("g_" + binding.name());
                uniform.setBinding(binding);
                //worldBindUniforms.add(uniform);
            }
        }
        
        return loadedShader;
    }
}
