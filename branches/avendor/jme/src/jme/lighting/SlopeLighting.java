/*
 * Copyright (c) 2003, jMonkeyEngine - Mojo Monkey Coding
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer. 
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution. 
 * 
 * Neither the name of the Mojo Monkey Coding, jME, jMonkey Engine, nor the 
 * names of its contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission. 
 * 
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
 *
 */

package jme.lighting;


import jme.exception.MonkeyRuntimeException;
import jme.locale.external.data.AbstractHeightMap;
import jme.math.Vector;

/**
 * <code>SlopeLighting</code> creates a light map based on a given heightmap.
 * This is based on Charlie Van Noland's algorithm. Slope lighting simply 
 * takes the difference between two vertices (heighthwise) and determines 
 * a lighting value. Direction for comparision is important and direction 
 * can only be an increment of 45 degrees. Thus, the direction parameters 
 * can be combinations of 1 and -2. I.e. 1,1 1,-1 -1,-1 -1,1.
 * 
 * @author Mark Powell
 * @version $Id: SlopeLighting.java,v 1.1.1.1 2003-10-29 10:58:11 Anakan Exp $
 */
public class SlopeLighting extends AbstractLightMap {

	//attributes.
	private int softness;
	private float maxBright;
	private float minBright;
	private int dirZ;
	private int dirX;

	//data source
	private AbstractHeightMap heightMap;

	/**
	 * Constructor sets the lighting attributes and calls 
	 * <code>createLighting</code> initializing the light map.
	 * 
	 * @param heightMap the height map to use for the slopes.
	 * @param dirX the x direction of the light source, 1 or -1
	 * @param dirZ the z direction of the light source, 1 or -1
	 * @param minBright the minimum brightness 0 to 1.
	 * @param maxBright the maximum brightness 0 to 1
	 * @param softness how much to blend light values, higher 
	 * 		value more soft.
	 * @throws MonkeyRuntimeException if either direction is anything 
	 * 		other than 1 or -1, either brightness is not between 0 and 1, or
	 * 		heightmap is null.
	 */
	public SlopeLighting(
		AbstractHeightMap heightMap,
		int dirX,
		int dirZ,
		float minBright,
		float maxBright,
		int softness) {

		if (null == heightMap) {
			throw new MonkeyRuntimeException("Heightmap cannot be null");
		} else if ((dirX != 1 && dirX != -1) || (dirZ != 1 && dirZ != -1)) {
			throw new MonkeyRuntimeException("Directions must be 1 or -1");
		} else if (
			(maxBright < 0 || maxBright > 1)
				|| (minBright < 0 || minBright > 1)) {
			throw new MonkeyRuntimeException("Brightness values must be between 0 and 1");
		}

		this.dirX = dirX;
		this.dirZ = dirZ;
		this.minBright = minBright;
		this.maxBright = maxBright;
		this.softness = softness;
		this.heightMap = heightMap;

		color = new Vector(1.0f, 1.0f, 1.0f);

		createLighting();
	}

	/**
	 * <code>createLighting</code> generates the latest lightmap
	 * from the available data set. A call to <code>createLighting</code>
	 * is required to generate lighting for any changes of attributes, during
	 * construction this is called to insure a valid lightmap is available.
	 *
	 */
	public void createLighting() {
		float shade;
		int size = heightMap.getSize();
		lightMap = new float[size][size];

		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				if (x >= dirX
					&& z >= dirZ
					&& (x - dirX) < size
					&& (z - dirZ) < size) {
					shade =
						(1.0f - ((heightMap
								.getTrueHeightAtPoint(x - dirX, z - dirZ)
								- (float) heightMap.getTrueHeightAtPoint(x, z))
								/ softness));
					if (shade > maxBright) {
						shade = maxBright;
					} else if (shade < minBright) {
						shade = minBright;
					}
				} else {
					shade = maxBright;
				}

				lightMap[x][z] = shade;
			}
		}
	}

	/**
	 * <code>setDirX</code> sets the direction of the light on the X axis.
	 * This value must be either 1 or -1.
	 * @param dirX direction of the light on the x axis.
	 * @throws MonkeyRuntimeException if dirX is not 1 or -1.
	 */
	public void setDirX(int dirX) {
		if(dirX != 1 && dirX != -1) {
			throw new MonkeyRuntimeException("Direction must be either 1 or -1");
		}
		this.dirX = dirX;
	}

	/**
	 * <code>setDirZ</code> sets the direction of the light on the Z axis.
	 * This value must be either 1 or -1.
	 * @param dirZ direction of the light on the z axis.
	 * @throws MonkeyRuntimeException if dirZ is not 1 or -1.
	 */
	public void setDirZ(int dirZ) {
		if(dirZ != 1 && dirZ != -1) {
			throw new MonkeyRuntimeException("Direction must be either 1 or -1");
		}
		this.dirZ = dirZ;
	}

	/**
	 * <code>setMaxBright</code> sets the maximum brightness value for any
	 * given point. 
	 * @param maxBright the maximum brightness for a point.
	 * @throws MonkeyRuntimeException if maxBright is not between 0 and 1.
	 */
	public void setMaxBright(float maxBright) {
		if(maxBright > 1 || maxBright < 0) {
			throw new MonkeyRuntimeException("Brightness value must be " +
				"between 0 and 1");
		}
		this.maxBright = maxBright;
	}

	/**
	 * <code>setMinBright</code> sets the minimum brightness value for any
	 * given point. 
	 * @param minBright the minimum brightness for a point.
	 * @throws MonkeyRuntimeException if minBright is not between 0 and 1.
	 */
	public void setMinBright(float minBright) {
		if(minBright > 1 || minBright < 0) {
			throw new MonkeyRuntimeException("Brightness value must be " +
				"between 0 and 1");
		}
		this.minBright = minBright;
	}

	/**
	 * <code>setSoftness</code> sets the amount to blend shadows. The
	 * higher the number the more blending will be done. It is recommend
	 * to keep the number between 10 and 40.
	 * @param softness the amount to blend shadows.
	 */
	public void setSoftness(int softness) {
		this.softness = softness;
	}

	/**
	 * <code>setHeightMap</code> sets the heightmap for the slope calculations.
	 * 
	 * @param map the heightmap for slope calculations.
	 * @throws MonkeyRuntimeException if map is null.
	 */
	public void setHeightMap(AbstractHeightMap map) {
		if(null == map) {
			throw new MonkeyRuntimeException("The height map cannot be null");
		}
		heightMap = map;
	}

}