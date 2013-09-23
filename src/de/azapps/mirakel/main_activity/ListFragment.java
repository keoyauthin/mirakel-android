/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.larswerkman.colorpicker.ColorPicker;

import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

public class ListFragment extends Fragment {
	// private static final String TAG = "ListsActivity";
	private ListAdapter adapter;
	protected MainActivity main;
	protected EditText input;
	private View view;
	protected boolean EditName;
	private boolean created = false;
	private DragNDropListView listView;
	private static final int LIST_COLOR=0, LIST_RENAME = 1, LIST_DESTROY = 2;
	protected static final String TAG = "ListFragment";
	private boolean enableDrag;

	public void setActivity(MainActivity activity) {
		main = activity;
	}
	
	private static ListFragment me=null;
	
	private static void setSingleton(ListFragment me){
		ListFragment.me=me;
	}
	public static ListFragment getSingleton() {
		return me;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setSingleton(this);
		main = (MainActivity) getActivity();
		EditName = false;
		enableDrag = false;
		view = inflater.inflate(R.layout.activity_list, container, false);
		if(PreferenceManager.getDefaultSharedPreferences(main).getBoolean("DarkTheme", false)){
			view.findViewById(R.id.lists_list).setBackgroundColor(getResources().getColor(R.color.background_dark_transparent));
		}else{
			view.findViewById(R.id.lists_list).setBackgroundColor(getResources().getColor(android.R.color.background_light));
		}
		// Inflate the layout for this fragment
		created = true;
		update();
		return view;
	}

	public void enable_drop(boolean drag) {
		enableDrag = drag;
		update();
	}

	public void update() {
		if (!created)
			return;
		final List<ListMirakel> values = ListMirakel.all();
		main.updateLists();

		main.showMessageFromSync();

		if (adapter != null && enableDrag == adapter.isDropEnabled()) {
			adapter.changeData(values);
			adapter.notifyDataSetChanged();
			return;
		}

		adapter = new ListAdapter(this.getActivity(), R.layout.lists_row,
				values, enableDrag);
		listView = (DragNDropListView) view.findViewById(R.id.lists_list);
		listView.setEnableDrag(enableDrag);
		listView.setItemsCanFocus(true);
		listView.setAdapter(adapter);
		listView.requestFocus();
		listView.setDragListener(new DragListener() {

			@Override
			public void onStopDrag(View itemView) {
				itemView.setVisibility(View.VISIBLE);

			}

			@Override
			public void onStartDrag(View itemView) {
				itemView.setVisibility(View.INVISIBLE);

			}

			@Override
			public void onDrag(int x, int y, ListView listView) {
				// Nothing
			}
		});
		listView.setDropListener(new DropListener() {

			@Override
			public void onDrop(int from, int to) {
				if (from != to) {
					adapter.onDrop(from, to);
					listView.requestLayout();
				}
				Log.e(TAG, "Drop from:" + from + " to:" + to);

			}
		});

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				if (EditName) {
					EditName = false;
					return;
				}

				ListMirakel list = values.get((int) id);
				main.setCurrentList(list, item);
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View item,
					int position, final long id) {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				ListMirakel list = values.get((int) id);
				builder.setTitle(list.getName());
				List<CharSequence> items = new ArrayList<CharSequence>(Arrays
						.asList(getActivity().getResources().getStringArray(
								R.array.list_actions_items)));

				builder.setItems(items.toArray(new CharSequence[items.size()]),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								ListMirakel list = values.get((int) id);
								switch (item) {
								case LIST_COLOR:
									editColor(list);
									break;
								case LIST_RENAME:
									editList(list);
									break;
								case LIST_DESTROY:
									main.handleDestroyList(list);
									break;
								}
							}
						});

				AlertDialog dialog = builder.create();
				dialog.show();

				return false;
			}
		});
	}

	public ListAdapter getAdapter() {
		return adapter;
	}


	void editColor(final ListMirakel list) {
		final ColorPicker cp=new ColorPicker(main);
		cp.setColor(list.getColor());
		Log.e("Blubb","Color: "+list.getColor());

		new AlertDialog.Builder(main)
				.setTitle(main.getString(R.string.list_change_color))
				.setPositiveButton(R.string.set_color, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						list.setColor(cp.getColor());
						list.save();
						main.getListFragment().update();
						
					}
				})
				.setNegativeButton(R.string.unset_color, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						list.setColor(0);
						list.save();
						main.getListFragment().update();
					}
				})
				.setView(cp).show();
	}
	/**
	 * Edit the name of the List
	 * 
	 * @param list
	 */
	void editList(final ListMirakel list) {

		input = new EditText(main);
		input.setText(list == null ? getString(R.string.list_menu_new_list)
				: list.getName());
		input.setTag(main);
		input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		new AlertDialog.Builder(main)
				.setTitle(main.getString(R.string.list_change_name_title))
				.setMessage(main.getString(R.string.list_change_name_cont))
				.setView(input)
				.setPositiveButton(main.getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// List_mirakle list = values.get((int) id);
								ListMirakel l = list;
								if (list == null)
									l = ListMirakel.newList(input.getText()
											.toString());
								else
									l.setName(input.getText().toString());
								l.save(list != null);
								update();
							}
						})
				.setNegativeButton(main.getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
						}).show();
	}

}
