/*
 * Copyright (c) 2003-2006 jMonkeyEngine
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

package com.jme.scene.shape;

import java.nio.FloatBuffer;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.TriMesh;
import com.jme.scene.batch.TriangleBatch;
import com.jme.util.geom.BufferUtils;

/**
 * <code>Sphere</code> represents a 3D object with all points equidistance from
 * a center point.
 * 
 * @author Joshua Slack
 * @version $Id: Sphere.java,v 1.16 2006-04-12 04:43:00 renanse Exp $
 */
public class Sphere extends TriMesh {
    private static final long serialVersionUID = 1L;

    public static final int TEX_ORIGINAL = 0;
    
    // Spherical projection mode, donated by Ogli from the jME forums.
	public static final int TEX_PROJECTED = 1;

    protected int zSamples;

    protected int radialSamples;

    /**the distance from the center point each point falls on*/
    public float radius;
    /**the center of the sphere*/
    public Vector3f center;

    private static Vector3f tempVa = new Vector3f();

    private static Vector3f tempVb = new Vector3f();

    private static Vector3f tempVc = new Vector3f();

	protected int textureMode = TEX_ORIGINAL;


    /**
     * Constructs a sphere. By default the Sphere has not geometry data or
     * center.
     * 
     * @param name
     *            The name of the sphere.
     */
    public Sphere(String name) {
        super(name);
    }

    /**
     * Constructs a sphere with center at the origin. For details, see the other
     * constructor.
     * 
     * @param name
     *            Name of sphere.
     * @param zSamples
     *            The samples along the Z.
     * @param radialSamples
     *            The samples along the radial.
     * @param radius
     *            Radius of the sphere.
     * @see #Sphere(java.lang.String, com.jme.math.Vector3f, int, int, float)
     */
    public Sphere(String name, int zSamples, int radialSamples, float radius) {
        this(name, new Vector3f(0, 0, 0), zSamples, radialSamples, radius);
    }

    /**
     * Constructs a sphere. All geometry data buffers are updated automatically.
     * Both zSamples and radialSamples increase the quality of the generated
     * sphere.
     * 
     * @param name
     *            Name of the sphere.
     * @param center
     *            Center of the sphere.
     * @param zSamples
     *            The number of samples along the Z.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the sphere.
     */
    public Sphere(String name, Vector3f center, int zSamples,
            int radialSamples, float radius) {
        super(name);
        setData(center, zSamples, radialSamples, radius);
    }

    /**
     * Changes the information of the sphere into the given values. 
     * 
     * @param center
     *            The new center of the sphere.
     * @param zSamples
     *            The new number of zSamples of the sphere.
     * @param radialSamples
     *            The new number of radial samples of the sphere.
     * @param radius
     *            The new radius of the sphere.
     */
    public void setData(Vector3f center, int zSamples, int radialSamples, float radius) {
        if (center != null)
            this.center = center;
        else
            this.center = new Vector3f(0, 0, 0);
        this.zSamples = zSamples;
        this.radialSamples = radialSamples;
        this.radius = radius;

        setGeometryData();
        setIndexData();
        setDefaultColor(ColorRGBA.white);
    }

    /**
     * builds the vertices based on the radius, center and radial and zSamples.
     *
     */
    private void setGeometryData() {

        // allocate vertices
        batch.setVertQuantity((zSamples - 2) * (radialSamples + 1) + 2);
        batch.setVertBuf(BufferUtils.createVector3Buffer(batch.getVertBuf(), batch.getVertQuantity()));

        // allocate normals if requested
        batch.setNormBuf(BufferUtils.createVector3Buffer(batch.getNormBuf(), batch.getVertQuantity()));

        // allocate texture coordinates
        batch.getTexBuf().set(0, BufferUtils.createVector2Buffer((FloatBuffer)batch.getTexBuf().get(0), batch.getVertQuantity()));

        // generate geometry
        float fInvRS = 1.0f / (float) radialSamples;
        float fZFactor = 2.0f / (float) (zSamples - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        float[] afSin = new float[(radialSamples + 1)];
        float[] afCos = new float[(radialSamples + 1)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle);
            afSin[iR] = FastMath.sin(fAngle);
        }
        afSin[radialSamples] = afSin[0];
        afCos[radialSamples] = afCos[0];

        // generate the sphere itself
        int i = 0;
        for (int iZ = 1; iZ < (zSamples - 1); iZ++) {
            float fZFraction = -1.0f + fZFactor * iZ; // in (-1,1)
            float fZ = radius * fZFraction;

            // compute center of slice
            Vector3f kSliceCenter = tempVb.set(center);
            kSliceCenter.z += fZ;

            // compute radius of slice
            float fSliceRadius = FastMath.sqrt(FastMath.abs(radius * radius
                    - fZ * fZ));

            // compute slice vertices with duplication at end point
            Vector3f kNormal;
            int iSave = i;
            for (int iR = 0; iR < radialSamples; iR++) {
                float fRadialFraction = iR * fInvRS; // in [0,1)
                Vector3f kRadial = tempVc.set(afCos[iR], afSin[iR], 0);
                kRadial.mult(fSliceRadius, tempVa);
                batch.getVertBuf().put(kSliceCenter.x + tempVa.x).put(kSliceCenter.y + tempVa.y).put(kSliceCenter.z + tempVa.z);
                
                BufferUtils.populateFromBuffer(tempVa, batch.getVertBuf(), i);
                kNormal = tempVa.subtractLocal(center);
                kNormal.normalizeLocal();
                if (true) // later we may allow interior texture vs. exterior
                    batch.getNormBuf().put(kNormal.x).put(kNormal.y).put(kNormal.z);
                else 
                	batch.getNormBuf().put(-kNormal.x).put(-kNormal.y).put(-kNormal.z);

                if (textureMode == TEX_ORIGINAL)
                	((FloatBuffer)batch.getTexBuf().get(0)).put(fRadialFraction).put(0.5f * (fZFraction + 1.0f));
                else if (textureMode == TEX_PROJECTED)
                	((FloatBuffer)batch.getTexBuf().get(0)).put(fRadialFraction).put(FastMath.INV_PI * (FastMath.HALF_PI + FastMath.asin(fZFraction)));
                
                i++;
            }

            BufferUtils.copyInternalVector3(batch.getVertBuf(), iSave, i);
            BufferUtils.copyInternalVector3(batch.getNormBuf(), iSave, i);

            if (textureMode == TEX_ORIGINAL)
            	((FloatBuffer)batch.getTexBuf().get(0)).put(1.0f).put(0.5f * (fZFraction + 1.0f));
            else if (textureMode == TEX_PROJECTED)
            	((FloatBuffer)batch.getTexBuf().get(0)).put(1.0f).put(FastMath.INV_PI * (FastMath.HALF_PI + FastMath.asin(fZFraction)));

            i++;
        }

        // south pole
        batch.getVertBuf().position(i*3);
        batch.getVertBuf().put(center.x).put(center.y).put(center.z-radius);

        batch.getNormBuf().position(i * 3);        
        if (true) batch.getNormBuf().put(0).put(0).put(-1); // allow for inner texture orientation later.
        else batch.getNormBuf().put(0).put(0).put(1);

        ((FloatBuffer)batch.getTexBuf().get(0)).position(i*2);
        ((FloatBuffer)batch.getTexBuf().get(0)).put(0.5f).put(0.0f);

        i++;

        // north pole
        batch.getVertBuf().put(center.x).put(center.y).put(center.z+radius);
        
        if (true) batch.getNormBuf().put(0).put(0).put(1);
        else batch.getNormBuf().put(0).put(0).put(-1);

        ((FloatBuffer)batch.getTexBuf().get(0)).put(0.5f).put(1.0f);
    }

    /**
     * sets the indices for rendering the sphere.
     *
     */
    private void setIndexData() {

        // allocate connectivity
        ((TriangleBatch)batch).setTriangleQuantity(2 * (zSamples - 2) * radialSamples);
        ((TriangleBatch)batch).setIndexBuffer(BufferUtils.createIntBuffer(3*((TriangleBatch)batch).getTriangleQuantity()));

        // generate connectivity
        int index = 0;
        for (int iZ = 0, iZStart = 0; iZ < (zSamples - 3); iZ++) {
            int i0 = iZStart;
            int i1 = i0 + 1;
            iZStart += (radialSamples + 1);
            int i2 = iZStart;
            int i3 = i2 + 1;
            for (int i = 0; i < radialSamples; i++, index += 6) {
                if (true) {
                	((TriangleBatch)batch).getIndexBuffer().put(i0++);
                	((TriangleBatch)batch).getIndexBuffer().put(i1);
                	((TriangleBatch)batch).getIndexBuffer().put(i2);
                	((TriangleBatch)batch).getIndexBuffer().put(i1++);
                	((TriangleBatch)batch).getIndexBuffer().put(i3++);
                	((TriangleBatch)batch).getIndexBuffer().put(i2++);
                } else // inside view
                {
                	((TriangleBatch)batch).getIndexBuffer().put(i0++);
                	((TriangleBatch)batch).getIndexBuffer().put(i2);
                	((TriangleBatch)batch).getIndexBuffer().put(i1);
                	((TriangleBatch)batch).getIndexBuffer().put(i1++);
                	((TriangleBatch)batch).getIndexBuffer().put(i2++);
                	((TriangleBatch)batch).getIndexBuffer().put(i3++);
                }
            }
        }

        // south pole triangles
        for (int i = 0; i < radialSamples; i++, index += 3) {
            if (true) {
            	((TriangleBatch)batch).getIndexBuffer().put(i);
            	((TriangleBatch)batch).getIndexBuffer().put(batch.getVertQuantity() - 2);
            	((TriangleBatch)batch).getIndexBuffer().put(i + 1);
            } else // inside view
            {
            	((TriangleBatch)batch).getIndexBuffer().put(i);
            	((TriangleBatch)batch).getIndexBuffer().put(i + 1);
            	((TriangleBatch)batch).getIndexBuffer().put(batch.getVertQuantity() - 2);
            }
        }

        // north pole triangles
        int iOffset = (zSamples - 3) * (radialSamples + 1);
        for (int i = 0; i < radialSamples; i++, index += 3) {
            if (true) {
            	((TriangleBatch)batch).getIndexBuffer().put(i + iOffset);
            	((TriangleBatch)batch).getIndexBuffer().put(i + 1 + iOffset);
            	((TriangleBatch)batch).getIndexBuffer().put(batch.getVertQuantity() - 1);
            } else // inside view
            {
            	((TriangleBatch)batch).getIndexBuffer().put(i + iOffset);
            	((TriangleBatch)batch).getIndexBuffer().put(batch.getVertQuantity() - 1);
            	((TriangleBatch)batch).getIndexBuffer().put(i + 1 + iOffset);
            }
        }
    }

    /**
     * Returns the center of this sphere.
     * 
     * @return The sphere's center.
     */
    public Vector3f getCenter() {
        return center;
    }

    /**
     * Sets the center of this sphere. Note that other information (such as
     * geometry buffers and actual vertex information) is not changed. In most
     * cases, you'll want to use setData()
     * 
     * @param aCenter
     *            The new center.
     * @see #setData(com.jme.math.Vector3f, int, int, float, boolean)
     */
    public void setCenter(Vector3f aCenter) {
        center = aCenter;
    }
	/**
	 * @return Returns the textureMode.
	 */
	public int getTextureMode() {
		return textureMode;
	}
	/**
	 * @param textureMode The textureMode to set.
	 */
	public void setTextureMode(int textureMode) {
		this.textureMode = textureMode;
		setGeometryData();
	}
}