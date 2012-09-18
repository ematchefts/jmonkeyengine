/*
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * PssmShadow renderer use Parrallel Split Shadow Mapping technique (pssm)<br>
 * It splits the view frustum in several parts and compute a shadow map for each
 * one.<br> splits are distributed so that the closer they are from the camera,
 * the smaller they are to maximize the resolution used of the shadow map.<br>
 * This result in a better quality shadow than standard shadow mapping.<br> for
 * more informations on this read this
 * <a href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a><br>
 * <p/>
 * @author Rémy Bouquet aka Nehon
 */
public class PssmShadowRenderer implements SceneProcessor {

    /**
     * <code>FilterMode</code> specifies how shadows are filtered
     */
    public enum FilterMode {

        /**
         * Shadows are not filtered. Nearest sample is used, causing in blocky
         * shadows.
         */
        Nearest,
        /**
         * Bilinear filtering is used. Has the potential of being hardware
         * accelerated on some GPUs
         */
        Bilinear,
        /**
         * Dither-based sampling is used, very cheap but can look bad
         * at low resolutions.
         */
        Dither,
        /**
         * 4x4 percentage-closer filtering is used. Shadows will be smoother
         * at the cost of performance
         */
        PCF4,
        /**
         * 8x8 percentage-closer  filtering is used. Shadows will be smoother
         * at the cost of performance
         */
        PCFPOISSON,        
        /**
         * 8x8 percentage-closer  filtering is used. Shadows will be smoother
         * at the cost of performance
         */
        PCF8
        
    }

    /**
     * Specifies the shadow comparison mode 
     */
    public enum CompareMode {

        /**
         * Shadow depth comparisons are done by using shader code
         */
        Software,
        /**
         * Shadow depth comparisons are done by using the GPU's dedicated
         * shadowing pipeline.
         */
        Hardware;
    }
    private int nbSplits = 3;
    private float shadowMapSize;
    private float lambda = 0.65f;
    private float shadowIntensity = 0.7f;
    private float zFarOverride = 0;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private FrameBuffer[] shadowFB;
    private Texture2D[] shadowMaps;
    private Texture2D dummyTex;
    private Camera shadowCam;
    private Material preshadowMat;
    private Material postshadowMat;
    private GeometryList splitOccluders = new GeometryList(new OpaqueComparator());
    private Matrix4f[] lightViewProjectionsMatrices;
    private ColorRGBA splits;
    private float[] splitsArray;
    private boolean noOccluders = false;
    private Vector3f direction = new Vector3f();
    private AssetManager assetManager;
    private boolean debug = false;
    private float edgesThickness = 1.0f;
    private FilterMode filterMode;
    private CompareMode compareMode;
    private Picture[] dispPic;
    private Vector3f[] points = new Vector3f[8];
    private boolean flushQueues = true;
    // define if the fallback material should be used.
    private boolean needsfallBackMaterial = false;
    //Name of the post material technique
    private String postTechniqueName = "PostShadow";

    /**
     * Create a PSSM Shadow Renderer 
     * More info on the technique at <a href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     * @param manager the application asset manager
     * @param size the size of the rendered shadowmaps (512,1024,2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps). 
     *  @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps). 
     */
    public PssmShadowRenderer(AssetManager manager, int size, int nbSplits) {
        this(manager, size, nbSplits, new Material(manager, "Common/MatDefs/Shadow/PostShadowPSSM.j3md"));

    }

    /**
     * Create a PSSM Shadow Renderer 
     * More info on the technique at <a href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     * @param manager the application asset manager
     * @param size the size of the rendered shadowmaps (512,1024,2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps). 
     * @param postShadowMat the material used for post shadows if you need to override it      * 
     */
    //TODO remove the postShadowMat when we have shader injection....or remove this todo if we are in 2020.
    public PssmShadowRenderer(AssetManager manager, int size, int nbSplits, Material postShadowMat) {
        assetManager = manager;
        nbSplits = Math.max(Math.min(nbSplits, 4), 1);
        this.nbSplits = nbSplits;
        shadowMapSize = size;

        shadowFB = new FrameBuffer[nbSplits];
        shadowMaps = new Texture2D[nbSplits];
        dispPic = new Picture[nbSplits];
        lightViewProjectionsMatrices = new Matrix4f[nbSplits];
        splits = new ColorRGBA();
        splitsArray = new float[nbSplits + 1];

        //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
        dummyTex = new Texture2D(size, size, Format.RGBA8);

        preshadowMat = new Material(manager, "Common/MatDefs/Shadow/PreShadow.j3md");
        this.postshadowMat = postShadowMat;
        postshadowMat.setFloat("ShadowMapSize", size);       
        
        for (int i = 0; i < nbSplits; i++) {
            lightViewProjectionsMatrices[i] = new Matrix4f();
            shadowFB[i] = new FrameBuffer(size, size, 1);
            shadowMaps[i] = new Texture2D(size, size, Format.Depth);

            shadowFB[i].setDepthTexture(shadowMaps[i]);

            //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
            shadowFB[i].setColorTexture(dummyTex);

            postshadowMat.setTexture("ShadowMap" + i, shadowMaps[i]);

            //quads for debuging purpose
            dispPic[i] = new Picture("Picture" + i);
            dispPic[i].setTexture(manager, shadowMaps[i], false);
        }

        setCompareMode(CompareMode.Hardware);
        setFilterMode(FilterMode.Bilinear);
        setShadowIntensity(0.7f);

        shadowCam = new Camera(size, size);
        shadowCam.setParallelProjection(true);

        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

    }

    /**
     * Sets the filtering mode for shadow edges see {@link FilterMode} for more info
     * @param filterMode 
     */
    final public void setFilterMode(FilterMode filterMode) {
        if (filterMode == null) {
            throw new NullPointerException();
        }

        if (this.filterMode == filterMode) {
            return;
        }

        this.filterMode = filterMode;
        postshadowMat.setInt("FilterMode", filterMode.ordinal());
        postshadowMat.setFloat("PCFEdge", edgesThickness);
        if (compareMode == CompareMode.Hardware) {
            for (Texture2D shadowMap : shadowMaps) {
                if (filterMode == FilterMode.Bilinear) {
                    shadowMap.setMagFilter(MagFilter.Bilinear);
                    shadowMap.setMinFilter(MinFilter.BilinearNoMipMaps);
                } else {
                    shadowMap.setMagFilter(MagFilter.Nearest);
                    shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
                }
            }
        }
    }

    /**
     * sets the shadow compare mode see {@link CompareMode} for more info
     * @param compareMode 
     */
    final public void setCompareMode(CompareMode compareMode) {
        if (compareMode == null) {
            throw new NullPointerException();
        }

        if (this.compareMode == compareMode) {
            return;
        }

        this.compareMode = compareMode;
        for (Texture2D shadowMap : shadowMaps) {
            if (compareMode == CompareMode.Hardware) {
                shadowMap.setShadowCompareMode(ShadowCompareMode.LessOrEqual);
                if (filterMode == FilterMode.Bilinear) {
                    shadowMap.setMagFilter(MagFilter.Bilinear);
                    shadowMap.setMinFilter(MinFilter.BilinearNoMipMaps);
                } else {
                    shadowMap.setMagFilter(MagFilter.Nearest);
                    shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
                }
            } else {
                shadowMap.setShadowCompareMode(ShadowCompareMode.Off);
                shadowMap.setMagFilter(MagFilter.Nearest);
                shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
            }
        }
        postshadowMat.setBoolean("HardwareShadows", compareMode == CompareMode.Hardware);
    }

    //debug function that create a displayable frustrum
    private Geometry createFrustum(Vector3f[] pts, int i) {
        WireFrustum frustum = new WireFrustum(pts);
        Geometry frustumMdl = new Geometry("f", frustum);
        frustumMdl.setCullHint(Spatial.CullHint.Never);
        frustumMdl.setShadowMode(ShadowMode.Off);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        frustumMdl.setMaterial(mat);
        switch (i) {
            case 0:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Pink);
                break;
            case 1:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Red);
                break;
            case 2:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Green);
                break;
            case 3:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Blue);
                break;
            default:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.White);
                break;
        }

        frustumMdl.updateGeometricState();
        return frustumMdl;
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        viewPort = vp;
        //checking for caps to chosse the appropriate post material technique
        if (renderManager.getRenderer().getCaps().contains(Caps.GLSL150)) {
            postTechniqueName = "PostShadow15";
        } else {
            postTechniqueName = "PostShadow";
        }
    }

    public boolean isInitialized() {
        return viewPort != null;
    }

    /**
     * returns the light direction used by the processor
     * @return 
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Sets the light direction to use to compute shadows
     * @param direction 
     */
    public void setDirection(Vector3f direction) {
        this.direction.set(direction).normalizeLocal();
    }

    @SuppressWarnings("fallthrough")
    public void postQueue(RenderQueue rq) {
        GeometryList occluders = rq.getShadowQueueContent(ShadowMode.Cast);
        if (occluders.size() == 0) {
            return;
        }

        GeometryList receivers = rq.getShadowQueueContent(ShadowMode.Receive);
        if (receivers.size() == 0) {
            return;
        }

        Camera viewCam = viewPort.getCamera();

        float zFar = zFarOverride;
        if (zFar == 0) {
            zFar = viewCam.getFrustumFar();
        }

        //We prevent computing the frustum points and splits with zeroed or negative near clip value
        float frustumNear = Math.max(viewCam.getFrustumNear(), 0.001f);
        ShadowUtil.updateFrustumPoints(viewCam, frustumNear, zFar, 1.0f, points);

        //shadowCam.setDirection(direction);
        shadowCam.getRotation().lookAt(direction, shadowCam.getUp());
        shadowCam.update();
        shadowCam.updateViewProjection();

        PssmShadowUtil.updateFrustumSplits(splitsArray, frustumNear, zFar, lambda);


        switch (splitsArray.length) {
            case 5:
                splits.a = splitsArray[4];
            case 4:
                splits.b = splitsArray[3];
            case 3:
                splits.g = splitsArray[2];
            case 2:
            case 1:
                splits.r = splitsArray[1];
                break;
        }

        Renderer r = renderManager.getRenderer();
        renderManager.setForcedMaterial(preshadowMat);
        renderManager.setForcedTechnique("PreShadow");

        for (int i = 0; i < nbSplits; i++) {

            // update frustum points based on current camera and split
            ShadowUtil.updateFrustumPoints(viewCam, splitsArray[i], splitsArray[i + 1], 1.0f, points);

            //Updating shadow cam with curent split frustra
            ShadowUtil.updateShadowCamera(occluders, receivers, shadowCam, points, splitOccluders);

            //saving light view projection matrix for this split
            lightViewProjectionsMatrices[i] = shadowCam.getViewProjectionMatrix().clone();
            renderManager.setCamera(shadowCam, false);

            r.setFrameBuffer(shadowFB[i]);
            r.clearBuffers(false, true, false);

            // render shadow casters to shadow map
            viewPort.getQueue().renderShadowQueue(splitOccluders, renderManager, shadowCam, true);
        }
        if (flushQueues) {
            occluders.clear();
        }
        //restore setting for future rendering
        r.setFrameBuffer(viewPort.getOutputFrameBuffer());
        renderManager.setForcedMaterial(null);
        renderManager.setForcedTechnique(null);
        renderManager.setCamera(viewCam, false);

    }

    //debug only : displays depth shadow maps
    private void displayShadowMap(Renderer r) {
        Camera cam = viewPort.getCamera();
        renderManager.setCamera(cam, true);
        int h = cam.getHeight();
        for (int i = 0; i < dispPic.length; i++) {
            dispPic[i].setPosition((128 * i) +(150 + 64 * (i + 1) ), h / 20f);
            dispPic[i].setWidth(128);
            dispPic[i].setHeight(128);
            dispPic[i].updateGeometricState();
            renderManager.renderGeometry(dispPic[i]);
        }
        renderManager.setCamera(cam, false);
    }

    /**For dubuging purpose
     * Allow to "snapshot" the current frustrum to the scene
     */
    public void displayDebug() {
        debug = true;
    }

    public void postFrame(FrameBuffer out) {
        Camera cam = viewPort.getCamera();
        if (!noOccluders) {
            //setting params to recieving geometry list
            setMatParams();
            //some materials in the scene does not have a post shadow technique so we're using the fall back material
            if (needsfallBackMaterial) {
                renderManager.setForcedMaterial(postshadowMat);
            }

            //forcing the post shadow technique and render state
            renderManager.setForcedTechnique(postTechniqueName);

            //rendering the post shadow pass
            viewPort.getQueue().renderShadowQueue(ShadowMode.Receive, renderManager, cam, flushQueues);

            //resetting renderManager settings
            renderManager.setForcedTechnique(null);
            renderManager.setForcedMaterial(null);
            renderManager.setCamera(cam, false);

        }
        if (debug) {
            displayShadowMap(renderManager.getRenderer());
        }
    }

    private void setMatParams() {

        GeometryList l = viewPort.getQueue().getShadowQueueContent(ShadowMode.Receive);

        //iteratin throught all the geometries of the list to set the material params
        for (int i = 0; i < l.size(); i++) {
            Material mat = l.get(i).getMaterial();
            //checking if the material has the post technique and setting the params.
            if (mat.getMaterialDef().getTechniqueDef(postTechniqueName) != null) {
                mat.setColor("Splits", splits);
                postshadowMat.setColor("Splits", splits);
                for (int j = 0; j < nbSplits; j++) {
                    mat.setMatrix4("LightViewProjectionMatrix" + j, lightViewProjectionsMatrices[j]);
                    mat.setTexture("ShadowMap" + j, shadowMaps[j]);
                }
                mat.setBoolean("HardwareShadows", compareMode == CompareMode.Hardware);
                mat.setInt("FilterMode", filterMode.ordinal());
                mat.setFloat("PCFEdge", edgesThickness);
                mat.setFloat("ShadowIntensity", shadowIntensity);
                if(mat.getParam("ShadowMapSize") == null){
                    mat.setFloat("ShadowMapSize", shadowMapSize);
                }
            } else {
                needsfallBackMaterial = true;
            }
        }


        //At least one material of the receiving geoms does not support the post shadow techniques
        //so we fall back to the forced material solution (transparent shadows won't be supported for these objects)
        if (needsfallBackMaterial) {
            postshadowMat.setColor("Splits", splits);
            for (int j = 0; j < nbSplits; j++) {
                postshadowMat.setMatrix4("LightViewProjectionMatrix" + j, lightViewProjectionsMatrices[j]);
            }
        }

    }

    public void preFrame(float tpf) {
    }

    public void cleanup() {
    }

    public void reshape(ViewPort vp, int w, int h) {
    }

    /**
     * returns the labda parameter<br>
     * see {@link setLambda(float lambda)}
     * @return lambda
     */
    public float getLambda() {
        return lambda;
    }

    /*
     * Adjust the repartition of the different shadow maps in the shadow extend
     * usualy goes from 0.0 to 1.0
     * a low value give a more linear repartition resulting in a constant quality in the shadow over the extends, but near shadows could look very jagged
     * a high value give a more logarithmic repartition resulting in a high quality for near shadows, but the quality quickly decrease over the extend.
     * the default value is set to 0.65f (theoric optimal value).
     * @param lambda the lambda value.
     */
    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    /**
     * How far the shadows are rendered in the view
     * see {@link setShadowZExtend(float zFar)}
     * @return shadowZExtend
     */
    public float getShadowZExtend() {
        return zFarOverride;
    }

    /**
     * Set the distance from the eye where the shadows will be rendered
     * default value is dynamicaly computed to the shadow casters/receivers union bound zFar, capped to view frustum far value.
     * @param zFar the zFar values that override the computed one
     */
    public void setShadowZExtend(float zFar) {
        this.zFarOverride = zFar;
    }

    /**
     * returns the shdaow intensity<br>
     * see {@link setShadowIntensity(float shadowIntensity)}
     * @return shadowIntensity
     */
    public float getShadowIntensity() {
        return shadowIntensity;
    }

    /**
     * Set the shadowIntensity, the value should be between 0 and 1,
     * a 0 value gives a bright and invisilble shadow,
     * a 1 value gives a pitch black shadow,
     * default is 0.7
     * @param shadowIntensity the darkness of the shadow
     */
    final public void setShadowIntensity(float shadowIntensity) {
        this.shadowIntensity = shadowIntensity;
        postshadowMat.setFloat("ShadowIntensity", shadowIntensity);
    }

    /**
     * returns the edges thickness <br>
     * see {@link setEdgesThickness(int edgesThickness)}
     * @return edgesThickness
     */
    public int getEdgesThickness() {
        return (int) (edgesThickness * 10);
    }

    /**
     * Sets the shadow edges thickness. default is 1, setting it to lower values can help to reduce the jagged effect of the shadow edges
     * @param edgesThickness 
     */
    public void setEdgesThickness(int edgesThickness) {
        this.edgesThickness = Math.max(1, Math.min(edgesThickness, 10));
        this.edgesThickness *= 0.1f;
        postshadowMat.setFloat("PCFEdge", edgesThickness);
    }

    /**
     * returns true if the PssmRenderer flushed the shadow queues
     * @return flushQueues
     */
    public boolean isFlushQueues() {
        return flushQueues;
    }

    /**
     * Set this to false if you want to use several PssmRederers to have multiple shadows cast by multiple light sources.
     * Make sure the last PssmRenderer in the stack DO flush the queues, but not the others
     * @param flushQueues 
     */
    public void setFlushQueues(boolean flushQueues) {
        this.flushQueues = flushQueues;
    }
}
