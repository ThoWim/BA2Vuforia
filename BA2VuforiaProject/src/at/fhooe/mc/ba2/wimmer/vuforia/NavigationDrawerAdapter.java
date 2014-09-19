package at.fhooe.mc.ba2.wimmer.vuforia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NavigationDrawerAdapter extends ArrayAdapter<String> {
	public NavigationDrawerAdapter(Context context, int resource,
			String[] drawerItems) {
		super(context, resource, drawerItems);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		if (row == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			row = inflater.inflate(R.layout.drawer_item, null);
		}

		TextView txtItemName = (TextView) row.findViewById(R.id.item_text);
		ImageView itemColor = (ImageView) row.findViewById(R.id.item_color);

		txtItemName.setText(getItem(position));

		if ((position % 2) == 0) {
			itemColor.setBackgroundColor(getContext().getResources().getColor(
					R.color.color_navigation_drawer_item_ligth));
		} else {
			itemColor.setBackgroundColor(getContext().getResources().getColor(
					R.color.color_navigation_drawer_item_dark));
		}

		return row;
	}
}
