/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package at.fhooe.mc.ba2.wimmer.vuforia.renderer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import at.fhooe.mc.ba2.wimmer.vuforia.MainActivity;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour.RenderingBehaviour;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.CubeObject;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.CubeShaders;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.CylinderModel;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.SampleApplication3DModel;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.SampleUtils;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.Teapot;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.Texture;

import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vuforia;

// The renderer class for the ImageTargets sample. 
public class MyRenderer implements GLSurfaceView.Renderer {

	private MainActivity mActivity;

	private RenderingBehaviour mRenderingBehaviour;

	public Vector<Texture> mTextures;

	public int shaderProgramID;

	public int vertexHandle;

	public int normalHandle;

	public int textureCoordHandle;

	public int mvpMatrixHandle;

	public int texSampler2DHandle;

	public float kBuildingScale = 12.0f;

	private Renderer mRenderer;

	public boolean mIsActive = false;

	public static final float OBJECT_SCALE_FLOAT = 3.0f;

	public static final String KEY_TEAPOT = "Teapot";
	public static final String KEY_SPHERE = "Sphere";
	public static final String KEY_CYLINDER = "Cylinder";
	public static final String KEY_CUBE = "CUBE";

	private Map<String, Object> models = new HashMap<String, Object>();

	public MyRenderer(MainActivity activity) {
		mActivity = activity;
	}

	public void setBehaviour(
			Class<? extends RenderingBehaviour> renderingBehaviour) {
		try {
			// Call the constructor of the given class of type
			// RenderingBehaviour using reflections
			Constructor<?> constructor = renderingBehaviour.getConstructor(
					Context.class, MyRenderer.class);
			mRenderingBehaviour = (RenderingBehaviour) constructor.newInstance(
					mActivity, this);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	// Called to draw the current frame.
	@Override
	public void onDrawFrame(GL10 gl) {
		if (!mIsActive)
			return;

		// Call our function to render content
		renderFrame();
	}

	// Called when the surface is created or recreated.
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.d(this.getClass().getName(), "GLRenderer.onSurfaceCreated");

		initRendering();

		// Call Vuforia function to (re)initialize rendering after first use
		// or after OpenGL ES context was lost (e.g. after onPause/onResume):
		Vuforia.onSurfaceCreated();
	}

	// Called when the surface changed size.
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.d(this.getClass().getName(), "GLRenderer.onSurfaceChanged");

		// Call Vuforia function to handle render surface size changes:
		Vuforia.onSurfaceChanged(width, height);
	}

	// Function for initializing the renderer.
	private void initRendering() {
		mRenderer = Renderer.getInstance();

		// Define clear color
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
				: 1.0f);

		// Now generate the OpenGL texture objects and add settings
		for (Texture t : mTextures) {
			GLES20.glGenTextures(1, t.mTextureID, 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
					t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE, t.mData);
		}

		shaderProgramID = SampleUtils.createProgramFromShaderSrc(
				CubeShaders.CUBE_MESH_VERTEX_SHADER,
				CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

		vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
				"vertexPosition");
		normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
				"vertexNormal");
		textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
				"vertexTexCoord");
		mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
				"modelViewProjectionMatrix");
		texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
				"texSampler2D");

		Teapot teapot = new Teapot();
		CylinderModel cylinderModel = new CylinderModel(1f);
		SampleApplication3DModel sphereModel = new SampleApplication3DModel();

		try {
			sphereModel.loadModel(mActivity.getResources().getAssets(),
					"Sphere.txt");
		} catch (IOException e) {
			Log.e(this.getClass().getName(), "Unable to load soccer ball");
		}

		CubeObject cube = new CubeObject();

		// Add models to map
		models.put(KEY_TEAPOT, teapot);
		models.put(KEY_CYLINDER, cylinderModel);
		models.put(KEY_SPHERE, sphereModel);
		models.put(KEY_CUBE, cube);

		mActivity.showProgressDialog(false);
	}

	public Object getData(String key) {
		return models.get(key);
	}

	// The render function.
	private void renderFrame() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		State state = mRenderer.begin();
		mRenderer.drawVideoBackground();

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// handle face culling, we need to detect if we are using reflection
		// to determine the direction of the culling
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);
		if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
			GLES20.glFrontFace(GLES20.GL_CW); // Front camera
		else
			GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

		// did we find any trackables this frame?
		for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {

			TrackableResult result = state.getTrackableResult(tIdx);
			Trackable trackable = result.getTrackable();
			printUserData(trackable);
			Matrix44F modelViewMatrix_Vuforia = Tool
					.convertPose2GLMatrix(result.getPose());
			float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

			mRenderingBehaviour.renderBehaviour(result, modelViewMatrix);
		}

		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		mRenderer.end();
	}

	private void printUserData(Trackable trackable) {
		String userData = (String) trackable.getUserData();
		Log.d(this.getClass().getName(), "UserData:Retreived User Data	\""
				+ userData + "\"");
	}

	public void setTextures(Vector<Texture> textures) {
		mTextures = textures;

	}
}
