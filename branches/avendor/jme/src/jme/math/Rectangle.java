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
package jme.math;

/**
 * <code>Rectangle</code> defines a finite plane with an origin point and
 * two edge directions that defines it's extents. 
 * @author Mark Powell
 * @version $Id: Rectangle.java,v 1.1.1.1 2003-10-29 10:58:52 Anakan Exp $
 */
public class Rectangle {
    private Vector origin;
    private Vector firstEdge;
    private Vector secondEdge;
    
    /**
     * Constructor instantiates a new <code>Rectangle</code> object. All values
     * origin, firstEdge and secondEdge are (0, 0, 0).
     *
     */
    public Rectangle() {
        origin = new Vector();
        firstEdge = new Vector();
        secondEdge = new Vector();
    }

    /**
     * Constructor instantiates a new <code>Rectangle</code> object. The 
     * attributes of the rectangle are defined during construction.
     * @param origin the point defining the least point of the rectange.
     * @param firstEdge the first extent.
     * @param secondEdge the second extent.
     */
    public Rectangle(Vector origin, Vector firstEdge, Vector secondEdge) {
        this.origin = origin;
        this.firstEdge = firstEdge;
        this.secondEdge = secondEdge;
    }
    
    /**
     * <code>getFirstEdge</code> returns the first extent vector.
     * @return the first vector edge.
     */
    public Vector getFirstEdge() {
        return firstEdge;
    }

    /**
     * <code>setFirstEdge</code> sets the first extent vector.
     * @param firstEdge the new first vector edge.
     */
    public void setFirstEdge(Vector firstEdge) {
        this.firstEdge = firstEdge;
    }

    /**
     * <code>getOrigin</code> returns the origin point of the rectangle.
     * @return the origin of the rectangle.
     */
    public Vector getOrigin() {
        return origin;
    }

    /**
     * <code>setOrigin</code> sets the origin point of the rectangle.
     * @param origin the new origin of the rectangle.
     */
    public void setOrigin(Vector origin) {
        this.origin = origin;
    }

    /**
     * <code>getSecondEdge</code> returns the second extent vector.
     * @return the second edge vector.
     */
    public Vector getSecondEdge() {
        return secondEdge;
    }

    /**
     * <code>setSecondEdge</code> sets the second edge vector.
     * @param secondEdge the new second edge vector.
     */
    public void setSecondEdge(Vector secondEdge) {
        this.secondEdge = secondEdge;
    }

    
    
    

}