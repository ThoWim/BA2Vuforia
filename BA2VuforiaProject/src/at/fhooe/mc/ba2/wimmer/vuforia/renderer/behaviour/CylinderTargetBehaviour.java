package at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.MyRenderer;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.CylinderModel;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.SampleApplication3DModel;
import at.fhooe.mc.ba2.wimmer.vuforia.utils.SampleUtils;

import com.qualcomm.vuforia.CameraCalibration;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.CylinderTargetResult;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.TrackableResult;

public class CylinderTargetBehaviour extends RenderingBehaviour {

	// dimensions of the cylinder (as set in the TMS tool)
	private float kCylinderHeight = 85.0f;
	private float kCylinderDiameter = 91.0f;

	// the height of the tea pot
	private float kObjectHeight = 1.0f;

	// we want the object to be the 1/3 of the height of the cylinder
	private float kRatioCylinderObjectHeight = 3.0f;

	private float rotateBallAngle;
	private double prevTime;

	// Scaling of the object to match the ratio we want
	private float kObjectScale = kCylinderHeight
			/ (kRatioCylinderObjectHeight * kObjectHeight);

	// scaling of the cylinder model to fit the actual cylinder
	private float kCylinderScaleX = kCylinderDiameter / 2.0f;
	private float kCylinderScaleY = kCylinderDiameter / 2.0f;
	private float kCylinderScaleZ = kCylinderHeight;

	private CylinderModel mCylinderModel;
	private SampleApplication3DModel mSphereModel;

	public CylinderTargetBehaviour(Context context, MyRenderer renderer) {
		super(context, renderer);
		prevTime = System.currentTimeMillis();
		rotateBallAngle = 0;
		mCylinderModel = (CylinderModel) renderer
				.getData(MyRenderer.KEY_CYLINDER);
		mSphereModel = (SampleApplication3DModel) renderer
				.getData(MyRenderer.KEY_SPHERE);
	}

	@Override
	public void renderBehaviour(TrackableResult result, float[] modelViewMatrix) {
		if (result.isOfType(CylinderTargetResult.getClassType())) {
			float[] modelViewProjection = new float[16];

			Matrix.scaleM(modelViewMatrix, 0, kCylinderScaleX, kCylinderScaleY,
					kCylinderScaleZ);
			CameraCalibration camCal = CameraDevice.getInstance()
					.getCameraCalibration();
			Matrix44F projectionMatrix = Tool.getProjectionGL(camCal, 10.0f,
					5000.0f);
			Matrix.multiplyMM(modelViewProjection, 0,
					projectionMatrix.getData(), 0, modelViewMatrix, 0);
			SampleUtils.checkGLError("CylinderTargets prepareCylinder");

			GLES20.glUseProgram(mRenderer.shaderProgramID);

			// Draw the cylinder:

			// We must detect if background reflection is active and adjust the
			// culling direction.
			// If the reflection is active, this means the post matrix has been
			// reflected as well,
			// therefore standard counter clockwise face culling will result in
			// "inside out" models.
			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glCullFace(GLES20.GL_BACK);
			GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

			GLES20.glVertexAttribPointer(mRenderer.vertexHandle, 3,
					GLES20.GL_FLOAT, false, 0, mCylinderModel.getVertices());
			GLES20.glVertexAttribPointer(mRenderer.normalHandle, 3,
					GLES20.GL_FLOAT, false, 0, mCylinderModel.getNormals());
			GLES20.glVertexAttribPointer(mRenderer.textureCoordHandle, 2,
					GLES20.GL_FLOAT, false, 0, mCylinderModel.getTexCoords());

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
					mCylinderModel.getNumObjectIndex(),
					GLES20.GL_UNSIGNED_SHORT, mCylinderModel.getIndices());

			GLES20.glDisable(GLES20.GL_CULL_FACE);
			SampleUtils.checkGLError("CylinderTargets drawCylinder");

			// prepare the object
			modelViewMatrix = Tool.convertPose2GLMatrix(result.getPose())
					.getData();

			// draw the anchored object
			animateObject(modelViewMatrix);

			// we move away the object from the target
			Matrix.translateM(modelViewMatrix, 0, 1.0f * kCylinderDiameter,
					0.0f, kObjectScale);
			Matrix.scaleM(modelViewMatrix, 0, kObjectScale, kObjectScale,
					kObjectScale);

			Matrix.multiplyMM(modelViewProjection, 0,
					projectionMatrix.getData(), 0, modelViewMatrix, 0);

			GLES20.glUseProgram(mRenderer.shaderProgramID);

			GLES20.glVertexAttribPointer(mRenderer.vertexHandle, 3,
					GLES20.GL_FLOAT, false, 0, mSphereModel.getVertices());
			GLES20.glVertexAttribPointer(mRenderer.normalHandle, 3,
					GLES20.GL_FLOAT, false, 0, mSphereModel.getNormals());
			GLES20.glVertexAttribPointer(mRenderer.textureCoordHandle, 2,
					GLES20.GL_FLOAT, false, 0, mSphereModel.getTexCoords());

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					mRenderer.mTextures.get(3).mTextureID[0]);
			GLES20.glUniform1i(mRenderer.texSampler2DHandle, 0);
			GLES20.glUniformMatrix4fv(mRenderer.mvpMatrixHandle, 1, false,
					modelViewProjection, 0);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
					mSphereModel.getNumObjectVertex());

			GLES20.glDisableVertexAttribArray(mRenderer.vertexHandle);
			GLES20.glDisableVertexAttribArray(mRenderer.normalHandle);
			GLES20.glDisableVertexAttribArray(mRenderer.textureCoordHandle);

			SampleUtils.checkGLError("CylinderTargets renderFrame");
		}
	}

	private void animateObject(float[] modelViewMatrix) {
		double time = System.currentTimeMillis(); // Get real time difference
		float dt = (float) (time - prevTime) / 1000; // from frame to frame

		rotateBallAngle += dt * 180.0f / 3.1415f; // Animate angle based on time
		rotateBallAngle %= 360;

		Matrix.rotateM(modelViewMatrix, 0, rotateBallAngle, 0.0f, 0.0f, 1.0f);

		prevTime = time;
	}
}
