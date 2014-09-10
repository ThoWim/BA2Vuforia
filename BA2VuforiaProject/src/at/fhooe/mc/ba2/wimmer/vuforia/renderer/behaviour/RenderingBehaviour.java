package at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour;

import android.content.Context;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.MyRenderer;

import com.qualcomm.vuforia.TrackableResult;

public abstract class RenderingBehaviour {

	public MyRenderer mRenderer;
	public Context mContext;

	public RenderingBehaviour(Context context, MyRenderer renderer) {
		mContext = context;
		mRenderer = renderer;
	}

	public abstract void renderBehaviour(TrackableResult result,
			float[] modelViewMatrix);
}
