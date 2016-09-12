package jp.techacademy.yuichi.otsuka.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ButtonBarLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class SearchResultActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.yuichi.otsuka.taskapp.TASK";

    private Realm mRealm2;
    private RealmResults<Task> mTaskRealmResults2;
    private RealmChangeListener mRealmListener2 = new RealmChangeListener() {
        @Override
        public void onChange() {
            reloadListView();
        }
    };

    private ListView mListView2;
    private TaskAdapter mTaskAdapter2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        Intent intentR = getIntent();
        String searchWord = intentR.getStringExtra("SEARCHWORD");
        //Log.d("taskappp", searchWord);

        // Realmの設定
        mRealm2 = Realm.getDefaultInstance();
        mTaskRealmResults2 = mRealm2.where(Task.class).contains("category", searchWord).findAll();
        mTaskRealmResults2.sort("date", Sort.DESCENDING);
        mRealm2.addChangeListener(mRealmListener2);

        // ListViewの設定
        mTaskAdapter2 = new TaskAdapter(SearchResultActivity.this);
        mListView2 = (ListView) findViewById(R.id.listView2);

        // ListViewをタップしたときの処理
        mListView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(SearchResultActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task);

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(SearchResultActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm2.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm2.beginTransaction();
                        results.clear();
                        mRealm2.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                SearchResultActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }

    private void reloadListView() {

        ArrayList<Task> taskArrayList = new ArrayList<>();

        for (int i = 0; i < mTaskRealmResults2.size(); i++) {
            Task task = new Task();

            task.setId(mTaskRealmResults2.get(i).getId());
            task.setTitle(mTaskRealmResults2.get(i).getTitle());
            task.setContents(mTaskRealmResults2.get(i).getContents());
            task.setDate(mTaskRealmResults2.get(i).getDate());
            task.setCategory(mTaskRealmResults2.get(i).getCategory());

            taskArrayList.add(task);
        }

        mTaskAdapter2.setTaskArrayList(taskArrayList);
        mListView2.setAdapter(mTaskAdapter2);
        mTaskAdapter2.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm2.close();
    }
}