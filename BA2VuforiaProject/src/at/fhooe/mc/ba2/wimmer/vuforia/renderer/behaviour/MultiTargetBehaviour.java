package at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.MyRenderer;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.CubeObject;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.SampleUtils;

import com.qualcomm.vuforia.CameraCalibration;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.MultiTargetResult;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;

public class MultiTargetBehaviour extends RenderingBehaviour {

	// Constants:
	final static float kCubeScaleX = 120.0f * 0.75f / 2.0f;
	final static float kCubeScaleY = 120.0f * 1.00f / 2.0f;
	final static float kCubeScaleZ = 120.0f * 0.50f / 2.0f;

	private CubeObject cubeObject;

	public MultiTargetBehaviour(Context context, MyRenderer renderer) {
		super(context, renderer);
		cubeObject = (CubeObject) renderer.getData(MyRenderer.KEY_CUBE);
	}

	@Override
	public void renderBehaviour(TrackableResult result, float[] modelViewMatrix) {
		if (result.isOfType(MultiTargetResult.getClassType())) {
			// from frame to frame
			CameraCalibration camCal = CameraDevice.getInstance()
					.getCameraCalibration();
			Matrix44F projectionMatrix = Tool.getProjectionGL(camCal, 10.0f,
					5000.0f);

			float[] modelViewProjection = new float[16];
			Matrix.scaleM(modelViewMatrix, 0, kCubeScaleX, kCubeScaleY,
					kCubeScaleZ);
			Matrix.multiplyMM(modelViewProjection, 0,
					projectionMatrix.getData(), 0, modelViewMatrix, 0);

			GLES20.glUseProgram(mRenderer.shaderProgramID);

			// Draw the cube:

			// We must detect if background reflection is active and adjust the
			// culling direction.
			// If the reflection is active, this means the post matrix has been
			// reflected as well, therefore standard counter clockwise face
			// culling will result in "inside out" models.
			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glCullFace(GLES20.GL_BACK);
			if (Renderer.getInstance().getVideoBackgroundConfig()
					.getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
				GLES20.glFrontFace(GLES20.GL_CW); // Front camera
			else
				GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

			GLES20.glVertexAttribPointer(mRenderer.vertexHandle, 3,
					GLES20.GL_FLOAT, false, 0, cubeObject.getVertices());
			GLES20.glVertexAttribPointer(mRenderer.normalHandle, 3,
					GLES20.GL_FLOAT, false, 0, cubeObject.getNormals());
			GLES20.glVertexAttribPointer(mRenderer.textureCoordHandle, 2,
					GLES20.GL_FLOAT, false, 0, cubeObject.getTexCoords());

			GLES20.glEnableVertexAttribArray(mRenderer.vertexHandle);
			GLES20.glEnableVertexAttribArray(mRenderer.normalHandle);
			GLES20.glEnableVertexAttribArray(mRenderer.textureCoordHandle);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					mRenderer.mTextures.get(2).mTextureID[0]);
			GLES20.glUniformMatrix4fv(mRenderer.mvpMatrixHandle, 1, false,
					modelViewProjection, 0);
			GLES20.glUniform1i(mRenderer.texSampler2DHandle, 0);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES,
					cubeObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
					cubeObject.getIndices());

			GLES20.glDisable(GLES20.GL_CULL_FACE);

			GLES20.glDisableVertexAttribArray(mRenderer.vertexHandle);
			GLES20.glDisableVertexAttribArray(mRenderer.normalHandle);
			GLES20.glDisableVertexAttribArray(mRenderer.textureCoordHandle);

			SampleUtils.checkGLError("MultiTargets renderFrame");
		}
	}
}
