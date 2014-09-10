package at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.MyRenderer;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.SampleUtils;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.Teapot;

import com.qualcomm.vuforia.CameraCalibration;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.TrackableResult;

public class CloudRecognitionBehaviour extends RenderingBehaviour {

	private Teapot mTeapot;

	public CloudRecognitionBehaviour(Context context, MyRenderer renderer) {
		super(context, renderer);
		mTeapot = (Teapot) renderer.getData(MyRenderer.KEY_TEAPOT);
	}

	@Override
	public void renderBehaviour(TrackableResult result, float[] modelViewMatrix) {
		Trackable trackable = result.getTrackable();

		Log.d("TRACKABLE NAME", trackable.getName());
		if (!trackable.getName().equalsIgnoreCase("android")) {
			// TODO
			return;
		}

		int textureIndex = 0;
		Log.e(this.getClass().getName(), "TextureIndex: " + textureIndex);

		// deal with the modelview and projection matrices
		float[] modelViewProjection = new float[16];

		Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
				MyRenderer.OBJECT_SCALE_FLOAT);
		Matrix.scaleM(modelViewMatrix, 0, MyRenderer.OBJECT_SCALE_FLOAT,
				MyRenderer.OBJECT_SCALE_FLOAT, MyRenderer.OBJECT_SCALE_FLOAT);

		CameraCalibration camCal = CameraDevice.getInstance()
				.getCameraCalibration();
		Matrix44F projectionMatrix = Tool.getProjectionGL(camCal, 10.0f,
				5000.0f);

		Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix.getData(),
				0, modelViewMatrix, 0);

		// activate the shader program and bind the vertex/normal/tex coords
		GLES20.glUseProgram(mRenderer.shaderProgramID);

		GLES20.glVertexAttribPointer(mRenderer.vertexHandle, 3,
				GLES20.GL_FLOAT, false, 0, mTeapot.getVertices());
		GLES20.glVertexAttribPointer(mRenderer.normalHandle, 3,
				GLES20.GL_FLOAT, false, 0, mTeapot.getNormals());
		GLES20.glVertexAttribPointer(mRenderer.textureCoordHandle, 2,
				GLES20.GL_FLOAT, false, 0, mTeapot.getTexCoords());

		GLES20.glEnableVertexAttribArray(mRenderer.vertexHandle);
		GLES20.glEnableVertexAttribArray(mRenderer.normalHandle);
		GLES20.glEnableVertexAttribArray(mRenderer.textureCoordHandle);

		// activate texture 0, bind it, and pass to shader
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
				mRenderer.mTextures.get(textureIndex).mTextureID[0]);
		GLES20.glUniform1i(mRenderer.texSampler2DHandle, 0);

		// pass the model view matrix to the shader
		GLES20.glUniformMatrix4fv(mRenderer.mvpMatrixHandle, 1, false,
				modelViewProjection, 0);

		// finally draw the teapot
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTeapot.getNumObjectIndex(),
				GLES20.GL_UNSIGNED_SHORT, mTeapot.getIndices());

		// disable the enabled arrays
		GLES20.glDisableVertexAttribArray(mRenderer.vertexHandle);
		GLES20.glDisableVertexAttribArray(mRenderer.normalHandle);
		GLES20.glDisableVertexAttribArray(mRenderer.textureCoordHandle);

		SampleUtils.checkGLError("Render Frame");
	}
}
