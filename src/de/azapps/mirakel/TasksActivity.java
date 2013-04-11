package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class TasksActivity extends Activity {
	private static final String TAG = "TasksActivity";
	private List_mirakle list;
	private TasksDataSource datasource;
	private ListsDataSource datasource_lists;
	private TaskAdapter adapter;
	private NumberPicker picker;
	private TasksActivity main;
	private String server_url;
	private String Email;
	private String Password;
	protected static final int RESULT_SPEECH = 2;
	protected static final int RESULT_LIST=1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasks);
		main = this;
		datasource = new TasksDataSource(this);
		datasource.open();
		datasource_lists = new ListsDataSource(this);
		datasource_lists.open();
		this.list = datasource_lists.getList(this.getIntent().getIntExtra("listId",
				0));
		Log.v(TAG, "Start list" + list.getId());
		server_url = this.getIntent().getStringExtra("url");
		if (server_url != null) {
			Email = this.getIntent().getStringExtra("email");
			Password = this.getIntent().getStringExtra("password");
			datasource_lists.sync_lists(Email, Password, server_url);
			datasource.sync_tasks(Email, Password, server_url);
		}
		getResources().getString(R.string.action_settings);
		ListView listView = (ListView) findViewById(R.id.tasks_list);

		Map<SwipeListener.Direction, SwipeCommand> commands = new HashMap<SwipeListener.Direction, SwipeCommand>();
		commands.put(SwipeListener.Direction.LEFT, new SwipeCommand() {
			@Override
			public void runCommand(View v, MotionEvent event) {
				Intent list = new Intent(v.getContext(), ListActivity.class);
				datasource.close();
				datasource_lists.close();
				startActivityForResult(list, 1);
			}
		});
		listView.setOnTouchListener(new SwipeListener(false, commands));

		// Events
		final EditText newTask = (EditText) findViewById(R.id.tasks_new);
		newTask.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					Log.v(TAG, "New Task");
					long id = list.getId();
					Log.v(TAG, "Create in " + id);
					if (id <= 0) {
						try {
							id = datasource_lists.getFirstList().getId();
						} catch (NullPointerException e) {
							Toast.makeText(getApplicationContext(),
									R.string.no_lists, Toast.LENGTH_LONG)
									.show();
							return false;
						}
					}
					Task task = datasource.createTask(v.getText().toString(),
							id);
					v.setText(null);
					adapter.add(task);
					adapter.notifyDataSetChanged();
					// adapter.swapCursor(updateListCursor());
					return true;
				}
				return false;
			}
		});
		ImageButton btnSpeak = (ImageButton) findViewById(R.id.btnSpeak_tasks);
		//txtText = newTask;


		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(
						RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, main.getString(R.string.speak_lang_code));

				try {
					startActivityForResult(intent, RESULT_SPEECH);
					newTask.setText("");
				} catch (ActivityNotFoundException a) {
					Toast t = Toast.makeText(getApplicationContext(),
							"Opps! Your device doesn't support Speech to Text",
							Toast.LENGTH_SHORT);
					t.show();
				}
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
			case RESULT_LIST: 
				if (resultCode == RESULT_OK) {
					Log.v(TAG, "Change List");
					int listId = data.getIntExtra("listId", Mirakel.LIST_ALL);
					list = datasource_lists.getList(listId);
					datasource_lists.open();
					datasource.open();
					load_tasks();
				}
				break;
			case RESULT_SPEECH:
				if (resultCode == RESULT_OK && null != data) {
					ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					((EditText)main.findViewById(R.id.tasks_new)).setText(text.get(0));
				}
				break;
		}	
	}

	private void load_tasks() {
		Log.v(TAG, "loading...");
		if(list==null) return;
		Log.v(TAG, "loading..." + list.getId());
		final List<Task> values = datasource.getTasks(list, list.getSortBy());
		adapter = new TaskAdapter(this, R.layout.tasks_row, values,
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Task task = (Task) cb.getTag();
						task.toggleDone();
						datasource.saveTask(task);
						load_tasks();
					}
				}, new OnClickListener() {
					@Override
					public void onClick(final View v) {

						picker = new NumberPicker(main);
						picker.setMaxValue(4);
						picker.setMinValue(0);
						String[] t = { "-2", "-1", "0", "1", "2" };
						picker.setDisplayedValues(t);
						picker.setWrapSelectorWheel(false);
						picker.setValue(((Task) v.getTag()).getPriority() + 2);
						new AlertDialog.Builder(main)
								.setTitle(
										main.getString(R.string.task_change_prio_title))
								.setMessage(
										main.getString(R.string.task_change_prio_cont))
								.setView(picker)
								.setPositiveButton(main.getString(R.string.OK),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												Task task = (Task) v.getTag();
												task.setPriority((picker
														.getValue() - 2));
												datasource.saveTask(task);
												load_tasks();
											}

										})
								.setNegativeButton(
										main.getString(R.string.Cancel),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												// Do nothing.
											}
										}).show();

					}
				});
		ListView listView = (ListView) findViewById(R.id.tasks_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				Task t = values.get((int) id);
				Log.v(TAG, "Switch to Task " + t.getId());
				Intent task = new Intent(item.getContext(), TaskActivity.class);
				task.putExtra("id", t.getId());
				startActivity(task);
			}
		});
		switch (list.getId()) {
		case Mirakel.LIST_ALL:
			this.setTitle(this.getString(R.string.list_all));
			break;
		case Mirakel.LIST_DAILY:
			this.setTitle(this.getString(R.string.list_today));
			break;
		case Mirakel.LIST_WEEKLY:
			this.setTitle(this.getString(R.string.list_week));
			break;
		default:
			this.setTitle(list.getName());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		datasource.open();
		datasource_lists.open();
		load_tasks();

	}

	@Override
	protected void onPause() {
		datasource.close();
		datasource_lists.close();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tasks, menu);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		return true;
	}

	@Override
	public void onStart() {
		super.onStart();
		datasource.open();
		datasource_lists.open();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		datasource.open();
		datasource_lists.open();
	}

	@Override
	public void onStop() {
		datasource.close();
		datasource_lists.close();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		datasource.close();
		datasource_lists.close();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.list_delete:
			long listId = list.getId();
			if (listId == Mirakel.LIST_ALL || listId == Mirakel.LIST_DAILY
					|| listId == Mirakel.LIST_WEEKLY)
				return true;
			new AlertDialog.Builder(this)
					.setTitle(this.getString(R.string.list_delete_title))
					.setMessage(this.getString(R.string.list_delete_content))
					.setPositiveButton(this.getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									datasource_lists.deleteList(list);
									list = datasource_lists
											.getList(Mirakel.LIST_ALL);
									load_tasks();
								}
							})
					.setNegativeButton(this.getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
			return true;
		case R.id.task_sorting:
			final CharSequence[] items = getResources().getStringArray(
					R.array.task_sorting_items);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(this.getString(R.string.task_sorting_title));
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Log.v(TAG,"selected " + item);
					switch (item) {
					case 0:
						list.setSortBy(Mirakel.SORT_BY_OPT);
						break;
					case 1:
						list.setSortBy(Mirakel.SORT_BY_DUE);
						break;
					case 2:
						list.setSortBy(Mirakel.SORT_BY_PRIO);
						break;
					default:
						list.setSortBy(Mirakel.SORT_BY_ID);
						break;
					}
					datasource_lists.saveList(list);
					Log.e(TAG, "sorting: " + list.getSortBy());
					load_tasks();
					Toast.makeText(getApplicationContext(), items[item],
							Toast.LENGTH_SHORT).show();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		case android.R.id.home:
			Intent list = new Intent(this.getApplicationContext(),
					ListActivity.class);
			datasource.close();
			datasource_lists.close();
			startActivityForResult(list, 1);
			return true;
		default:
			return super.onOptionsItemSelected(item);

		}
	}
}
