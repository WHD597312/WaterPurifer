package com.peihou.waterpurifer.device.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.peihou.waterpurifer.R;
import com.peihou.waterpurifer.esptouch.EspWifiAdminSimple;
import com.peihou.waterpurifer.esptouch.EsptouchTask;
import com.peihou.waterpurifer.esptouch.IEsptouchListener;
import com.peihou.waterpurifer.esptouch.IEsptouchResult;
import com.peihou.waterpurifer.esptouch.IEsptouchTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class AddDeviceActivity extends AppCompatActivity {

    Unbinder unbinder;
    @BindView(R.id.et_ssid) EditText et_ssid;//wifi名称
    @BindView(R.id.et_pswd) EditText et_pswd;//wifi密码
    private EspWifiAdminSimple mWifiAdmin;
    private ProgressDialog mProgressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        //绑定注解
        unbinder=ButterKnife.bind(this);
        mWifiAdmin = new EspWifiAdminSimple(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String ssid=mWifiAdmin.getWifiConnectedSsid();
        et_ssid.setText(ssid);
    }

    @OnClick({R.id.btn_ensure})
    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_ensure:
                String ssid=et_ssid.getText().toString();
                String apBssid=mWifiAdmin.getWifiConnectedBssid();
                String apPassword=et_pswd.getText().toString();
                String taskResultCountStr = "1";
                new EsptouchAsyncTask3().execute(ssid, apBssid, apPassword, taskResultCountStr);
                break;
        }
    }
    private IEsptouchTask mEsptouchTask;

    private class EsptouchAsyncTask3 extends AsyncTask<String, Void, List<IEsptouchResult>> {


        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
//            popupWindow();
//            addDeviceDialog=new AddDeviceDialog(AddDeviceActivity.this);
//            addDeviceDialog.setCanceledOnTouchOutside(false);
//            addDeviceDialog.show();
            mProgressDialog = new ProgressDialog(AddDeviceActivity.this);
            mProgressDialog.setMessage("正在配置, 请耐心等待...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
//            CountTimer countTimer = new CountTimer(30000, 1000);
//            countTimer.start();
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                // !!!NOTICE
                String apSsid = params[0];
                String apBssid = params[1];
                String apPassword = params[2];
                String taskResultCountStr = params[3];
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, AddDeviceActivity.this);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    try {
                        Thread.sleep(300);
                        Log.i("IEsptouchResult", "-->" + result.size());
                        for (IEsptouchResult resultInList : result) {
                            //                String ssid=et_ssid.getText().toString();
                            String ssid = resultInList.getBssid();
                            sb.append("配置成功" + ssid);
                            if (!TextUtils.isEmpty(ssid)) {
                                mProgressDialog.dismiss();
                                Toast.makeText(AddDeviceActivity.this, "配置成功,ssid=" + ssid, Toast.LENGTH_LONG).show();
                                break;
                            }
                            count++;
                            if (count >= maxDisplayCount) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (count < result.size()) {
                        sb.append("\nthere's " + (result.size() - count)
                                + " more result(s) without showing\n");
                    }
                } else {
                    mProgressDialog.dismiss();
                    Toast.makeText(AddDeviceActivity.this, "配置失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
//                Toast.makeText(AddDeviceActivity.this, text,
//                        Toast.LENGTH_LONG).show();
            }

        });
    }

    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(unbinder!=null)
            unbinder.unbind();//解绑注解
    }
}
