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

package jmetest.effects.water;

import com.jme.app.SimplePassGame;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.math.Plane;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.pass.RenderPass;
import com.jme.scene.*;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Torus;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.*;
import com.jme.util.TextureManager;
import com.jmex.effects.water.ProjectedGrid;
import com.jmex.effects.water.WaterHeightGenerator;
import com.jmex.effects.water.WaterRenderPass;

/**
 * <code>TestProjectedWater</code>
 * Test for the water effect pass.
 *
 * @author Rikard Herlitz (MrCoder)
 */
public class TestProjectedWater extends SimplePassGame {
	private WaterRenderPass waterEffectRenderPass;
	private Skybox skybox;
	private ProjectedGrid projectedGrid;
	private float farPlane = 10000.0f;

	//debug stuff
	private Node debugQuadsNode;

	public static void main( String[] args ) {
		TestProjectedWater app = new TestProjectedWater();
		app.setDialogBehaviour( ALWAYS_SHOW_PROPS_DIALOG );
		app.start();
	}

	protected void cleanup() {
		super.cleanup();
		waterEffectRenderPass.cleanup();
	}

	protected void simpleUpdate() {
		if( KeyBindingManager.getKeyBindingManager().isValidCommand( "f", false ) ) {
			projectedGrid.switchFreeze();
		}
		if( KeyBindingManager.getKeyBindingManager().isValidCommand( "e", false ) ) {
			switchShowDebug();
		}
		if( KeyBindingManager.getKeyBindingManager().isValidCommand( "g", false ) ) {
			waterEffectRenderPass.reloadShader();
		}

		skybox.getLocalTranslation().set( cam.getLocation() );
		cam.update();
	}

	protected void simpleInitGame() {
		display.setTitle( "Water Test" );
		cam.setFrustumPerspective( 45.0f, (float) display.getWidth() / (float) display.getHeight(), 1f, farPlane );
		cam.setLocation( new Vector3f( 100, 50, 100 ) );
		cam.lookAt( new Vector3f( 0, 0, 0 ), Vector3f.UNIT_Y );
		cam.update();

		setupKeyBindings();

		setupFog();

		Node reflectedNode = new Node( "reflectNode" );

		buildSkyBox();
		reflectedNode.attachChild( skybox );
		reflectedNode.attachChild( createObjects() );

		rootNode.attachChild( reflectedNode );

		waterEffectRenderPass = new WaterRenderPass( cam, 4, true, true );
		waterEffectRenderPass.setClipBias( 0.5f );
		waterEffectRenderPass.setWaterMaxAmplitude( 5.0f );
		//setting to default value just to show
		waterEffectRenderPass.setWaterPlane( new Plane( new Vector3f( 0.0f, 1.0f, 0.0f ), 0.0f ) );

		projectedGrid = new ProjectedGrid( "ProjectedGrid", cam, 100, 70, 0.01f, new WaterHeightGenerator() );
//or implement your own waves like this(or in a separate class)...
//		projectedGrid = new ProjectedGrid( "ProjectedGrid", cam, 50, 50, 0.01f, new HeightGenerator() {
//			public float getHeight( float x, float z, float time ) {
//				return FastMath.sin(x*0.05f+time*2.0f)+FastMath.cos(z*0.1f+time*4.0f)*2;
//			}
//		} );

		waterEffectRenderPass.setWaterEffectOnSpatial( projectedGrid );
		rootNode.attachChild( projectedGrid );

		createDebugQuads();
		rootNode.attachChild( debugQuadsNode );

		waterEffectRenderPass.setReflectedScene( reflectedNode );
		waterEffectRenderPass.setSkybox( skybox );
		pManager.add( waterEffectRenderPass );

		RenderPass rootPass = new RenderPass();
		rootPass.add( rootNode );
		pManager.add( rootPass );

		RenderPass fpsPass = new RenderPass();
		fpsPass.add( fpsNode );
		pManager.add( fpsPass );

		rootNode.setCullMode( Spatial.CULL_NEVER );
		rootNode.setRenderQueueMode( Renderer.QUEUE_OPAQUE );
		fpsNode.setRenderQueueMode( Renderer.QUEUE_OPAQUE );
	}

	private void setupFog() {
		FogState fogState = display.getRenderer().createFogState();
		fogState.setDensity( 1.0f );
		fogState.setEnabled( true );
		fogState.setColor( new ColorRGBA( 1.0f, 1.0f, 1.0f, 1.0f ) );
		fogState.setEnd( farPlane );
		fogState.setStart( farPlane / 10.0f );
		fogState.setDensityFunction( FogState.DF_LINEAR );
		fogState.setApplyFunction( FogState.AF_PER_VERTEX );
		rootNode.setRenderState( fogState );
	}

	private void buildSkyBox() {
		skybox = new Skybox( "skybox", 10, 10, 10 );

		String dir = "jmetest/effects/water/data/";
		Texture north = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						dir + "1.jpg" ),
				Texture.MM_LINEAR,
				Texture.FM_LINEAR );
		Texture south = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						dir + "3.jpg" ),
				Texture.MM_LINEAR,
				Texture.FM_LINEAR );
		Texture east = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						dir + "2.jpg" ),
				Texture.MM_LINEAR,
				Texture.FM_LINEAR );
		Texture west = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						dir + "4.jpg" ),
				Texture.MM_LINEAR,
				Texture.FM_LINEAR );
		Texture up = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						dir + "6.jpg" ),
				Texture.MM_LINEAR,
				Texture.FM_LINEAR );
		Texture down = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						dir + "5.jpg" ),
				Texture.MM_LINEAR,
				Texture.FM_LINEAR );

		skybox.setTexture( Skybox.NORTH, north );
		skybox.setTexture( Skybox.WEST, west );
		skybox.setTexture( Skybox.SOUTH, south );
		skybox.setTexture( Skybox.EAST, east );
		skybox.setTexture( Skybox.UP, up );
		skybox.setTexture( Skybox.DOWN, down );
		skybox.preloadTextures();

		CullState cullState = display.getRenderer().createCullState();
		cullState.setCullMode( CullState.CS_NONE );
		cullState.setEnabled( true );
		skybox.setRenderState( cullState );

		ZBufferState zState = display.getRenderer().createZBufferState();
		zState.setEnabled( false );
		skybox.setRenderState( zState );

		FogState fs = display.getRenderer().createFogState();
		fs.setEnabled( false );
		skybox.setRenderState( fs );

		skybox.setLightCombineMode( LightState.OFF );
		skybox.setCullMode( Spatial.CULL_NEVER );
		skybox.setTextureCombineMode( TextureState.REPLACE );
		skybox.updateRenderState();

		skybox.lockBounds();
		skybox.lockMeshes();
	}

	private Node createObjects() {
		Node objects = new Node( "objects" );

		Torus torus = new Torus( "Torus", 50, 50, 10, 20 );
		torus.setLocalTranslation( new Vector3f( 50, -5, 20 ) );
		TextureState ts = display.getRenderer().createTextureState();
		Texture t0 = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						"jmetest/data/images/Monkey.jpg" ),
				Texture.MM_LINEAR_LINEAR,
				Texture.FM_LINEAR );
		Texture t1 = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						"jmetest/data/texture/north.jpg" ),
				Texture.MM_LINEAR_LINEAR,
				Texture.FM_LINEAR );
		t1.setEnvironmentalMapMode( Texture.EM_SPHERE );
		ts.setTexture( t0, 0 );
		ts.setTexture( t1, 1 );
		ts.setEnabled( true );
		torus.setRenderState( ts );
		objects.attachChild( torus );

		ts = display.getRenderer().createTextureState();
		t0 = TextureManager.loadTexture(
				TestProjectedWater.class.getClassLoader().getResource(
						"jmetest/data/texture/wall.jpg" ),
				Texture.MM_LINEAR_LINEAR,
				Texture.FM_LINEAR );
		t0.setWrap( Texture.WM_WRAP_S_WRAP_T );
		ts.setTexture( t0 );

		Box box = new Box( "box1", new Vector3f( -10, -10, -10 ), new Vector3f( 10, 10, 10 ) );
		box.setLocalTranslation( new Vector3f( 0, -7, 0 ) );
		box.setRenderState( ts );
		objects.attachChild( box );

		box = new Box( "box2", new Vector3f( -5, -5, -5 ), new Vector3f( 5, 5, 5 ) );
		box.setLocalTranslation( new Vector3f( 15, 10, 0 ) );
		box.setRenderState( ts );
		objects.attachChild( box );

		box = new Box( "box3", new Vector3f( -5, -5, -5 ), new Vector3f( 5, 5, 5 ) );
		box.setLocalTranslation( new Vector3f( 0, -10, 15 ) );
		box.setRenderState( ts );
		objects.attachChild( box );

		box = new Box( "box4", new Vector3f( -5, -5, -5 ), new Vector3f( 5, 5, 5 ) );
		box.setLocalTranslation( new Vector3f( 20, 0, 0 ) );
		box.setRenderState( ts );
		objects.attachChild( box );

		ts = display.getRenderer().createTextureState();
		t0 = TextureManager.loadTexture(
				TestSimpleQuadWater.class.getClassLoader().getResource(
						"jmetest/data/images/Monkey.jpg" ),
				Texture.MM_LINEAR_LINEAR,
				Texture.FM_LINEAR );
		t0.setWrap( Texture.WM_WRAP_S_WRAP_T );
		ts.setTexture( t0 );

		box = new Box( "box5", new Vector3f( -50, -2, -50 ), new Vector3f( 50, 2, 50 ) );
		box.setLocalTranslation( new Vector3f( 0, -15, 0 ) );
		box.setRenderState( ts );
		box.setModelBound( new BoundingBox() );
		box.updateModelBound();
		objects.attachChild( box );

		return objects;
	}

	private void setupKeyBindings() {
		KeyBindingManager.getKeyBindingManager().set( "f", KeyInput.KEY_F );
		KeyBindingManager.getKeyBindingManager().set( "e", KeyInput.KEY_E );
		KeyBindingManager.getKeyBindingManager().set( "g", KeyInput.KEY_G );

		Text t = new Text( "Text", "F: switch freeze/unfreeze projected grid" );
		t.setRenderQueueMode( Renderer.QUEUE_ORTHO );
		t.setLightCombineMode( LightState.OFF );
		t.setLocalTranslation( new Vector3f( 0, 20, 1 ) );
		fpsNode.attachChild( t );

		t = new Text( "Text", "E: debug show/hide reflection and refraction textures" );
		t.setRenderQueueMode( Renderer.QUEUE_ORTHO );
		t.setLightCombineMode( LightState.OFF );
		t.setLocalTranslation( new Vector3f( 0, 40, 1 ) );
		fpsNode.attachChild( t );
	}

	private void switchShowDebug() {
		if( debugQuadsNode.getCullMode() == SceneElement.CULL_NEVER ) {
			debugQuadsNode.setCullMode( SceneElement.CULL_ALWAYS );
		}
		else {
			debugQuadsNode.setCullMode( SceneElement.CULL_NEVER );
		}
	}

	private void createDebugQuads() {
		debugQuadsNode = new Node( "quadNode" );
		debugQuadsNode.setCullMode( SceneElement.CULL_NEVER );

		float quadWidth = display.getWidth() / 8;
		float quadHeight = display.getWidth() / 8;
		Quad debugQuad = new Quad( "reflectionQuad", quadWidth, quadHeight );
		debugQuad.setRenderQueueMode( Renderer.QUEUE_ORTHO );
		debugQuad.setCullMode( SceneElement.CULL_NEVER );
		debugQuad.setLightCombineMode( LightState.OFF );
		TextureState ts = display.getRenderer().createTextureState();
		ts.setTexture( waterEffectRenderPass.getTextureReflect() );
		debugQuad.setRenderState( ts );
		debugQuad.updateRenderState();
		debugQuad.getLocalTranslation().set( quadWidth * 0.6f, quadHeight * 1.0f, 1.0f );
		debugQuadsNode.attachChild( debugQuad );

		if( waterEffectRenderPass.getTextureRefract() != null ) {
			debugQuad = new Quad( "refractionQuad", quadWidth, quadHeight );
			debugQuad.setRenderQueueMode( Renderer.QUEUE_ORTHO );
			debugQuad.setCullMode( SceneElement.CULL_NEVER );
			debugQuad.setLightCombineMode( LightState.OFF );
			ts = display.getRenderer().createTextureState();
			ts.setTexture( waterEffectRenderPass.getTextureRefract() );
			debugQuad.setRenderState( ts );
			debugQuad.updateRenderState();
			debugQuad.getLocalTranslation().set( quadWidth * 0.6f, quadHeight * 2.1f, 1.0f );
			debugQuadsNode.attachChild( debugQuad );
		}
	}
}