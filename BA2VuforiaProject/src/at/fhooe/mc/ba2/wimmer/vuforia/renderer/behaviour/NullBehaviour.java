package at.fhooe.mc.ba2.wimmer.vuforia.renderer.behaviour;

import com.qualcomm.vuforia.TrackableResult;

import android.content.Context;
import at.fhooe.mc.ba2.wimmer.vuforia.renderer.MyRenderer;

public class NullBehaviour extends RenderingBehaviour {

	public NullBehaviour(Context context, MyRenderer renderer) {
		super(context, renderer);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void renderBehaviour(TrackableResult result, float[] modelViewMatrix) {
		// TODO Auto-generated method stub

	}

}
