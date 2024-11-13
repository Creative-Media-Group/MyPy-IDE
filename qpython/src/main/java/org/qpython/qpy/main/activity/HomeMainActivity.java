package org.qpython.qpy.main.activity;

import static org.qpython.qpysdk.QPyConstants.PYTHON_2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.gyf.cactus.Cactus;
import com.quseit.util.FileUtils;
import com.quseit.util.NAction;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageHelper;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qpy.console.TermActivity;
import org.qpython.qpy.databinding.ActivityMainBinding;
import org.qpython.qpy.main.app.App;
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpy.main.utils.Bus;
import org.qpython.qpy.texteditor.EditorActivity;
import org.qpython.qpy.texteditor.TedLocalActivity;
import org.qpython.qpy.utils.BrandUtil;
import org.qpython.qpy.utils.JumpToUtils;
import org.qpython.qpy.utils.UpdateHelper;
import org.qpython.qpysdk.QPyConstants;
import org.qpython.qpysdk.QPySDK;
import org.qpython.qpysdk.utils.FileHelper;
import org.qpython.qsl4a.qsl4a.facade.AndroidFacade;
import org.qpython.qsl4a.qsl4a.facade.QPyInterfaceFacade;;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/***
 * Qpython主页
 */
public class HomeMainActivity extends BaseActivity {
    private static final String USER_NAME     = "username";
    private static final String TAG           = "HomeMainActivity";

    private static final int LOGIN_REQUEST_CODE = 136;

    private ActivityMainBinding binding;
    private SharedPreferences preferences;

    public static void start(Context context) {
        Intent starter = new Intent(context, HomeMainActivity.class);
        context.startActivity(starter);
    }

    public static void start(Context context, String userName) {
        Intent starter = new Intent(context, HomeMainActivity.class);
        starter.putExtra(USER_NAME, userName);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        //App.setActivity(this);
        startMain();
        handlePython3(getIntent());
        handleNotification(savedInstanceState);
        getIntentData(getIntent());
    }

    /**
     * 获取Intent数据，用于通知跳转
     * @param intent
     */
    private void getIntentData(Intent intent) {
        if (null != intent) {
            // 获取data里的值
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String action = null;
                String value = null;
                if(BrandUtil.isBrandHuawei()) {
                    //华为通知
                    action = bundle.getString(JumpToUtils.EXTRA_ACTION);
                    value = bundle.getString(JumpToUtils.EXTRA_VALUE);
                } else if(BrandUtil.isBrandXiaoMi()) {
                    if (bundle.getSerializable(PushMessageHelper.KEY_MESSAGE) != null) {
                        //小米通知
                        MiPushMessage message = (MiPushMessage) bundle.getSerializable(PushMessageHelper.KEY_MESSAGE);
                        Map<String, String> extra = message.getExtra();
                        action = extra.get(JumpToUtils.EXTRA_ACTION);
                        value = extra.get(JumpToUtils.EXTRA_VALUE);
                    }
                }

                if(!TextUtils.isEmpty(action) &&
                        !TextUtils.isEmpty(value)) {
                    delayNotifyJumpTo(action, value);
                }
            }
        }
    }

    /**
     * 延迟跳转页面
     * @param action
     * @param value
     */
    private void delayNotifyJumpTo(String action, String value) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                JumpToUtils.jumpTo(HomeMainActivity.this, action, value);
            }
        }, 1000);
    }

    private void initIcon() {
        //switch (NAction.getQPyInterpreter(this)) {
        //    case "3.x":
                binding.icon.setImageResource(R.drawable.img_home_logo_3);
        /*        break;
            case "2.x":
                binding.icon.setImageResource(R.drawable.img_home_logo);
                break;
            default:
                break;
        }*/
    }

    private void initUser() {
        if (App.getUser() == null) {
            binding.login.setVisibility(View.GONE);
        } else {
            binding.login.setText(Html.fromHtml(getString(R.string.welcome_s, App.getUser().getNick())));
        }
    }

    private void startMain() {
        initListener();
        setHandler();
//        startPyService();
        Bus.getDefault().register(this);
        openQpySDK();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            UpdateHelper.checkConfUpdate(this, QPyConstants.BASE_PATH);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initUser();
        initIcon();
        handleNotification();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handlePython3(intent);
        getIntentData(intent);
    }

    private void initListener() {
        binding.ivScan.setOnClickListener(v -> Bus.getDefault().post(new StartQrCodeActivityEvent()));
        binding.login.setOnClickListener(v -> {
            /*if (App.getUser() == null) {
                sendEvent(getString(R.string.event_login));
                startActivityForResult(new Intent(this, SignInActivity.class), LOGIN_REQUEST_CODE);
            } else {
                sendEvent(getString(R.string.event_me));
                UserActivity.start(this);
            }*/
        });

        binding.llTerminal.setOnClickListener(v -> {
            //openQpySDK(view -> {
                TermActivity.startActivity(HomeMainActivity.this);
                sendEvent(getString(R.string.event_term));
            //});
        });

        binding.llTerminal.setOnLongClickListener(v -> {
            CharSequence[] chars = new CharSequence[]{ this.getString(R.string.python_interpreter), this.getString(R.string.action_notebook), this.getString(R.string.shell_terminal)};
            new AlertDialog.Builder(this, R.style.MyDialog)
                    .setTitle(R.string.choose_action)
                    .setItems(chars, (dialog, which) -> {
                        switch (which) {
                            default:
                                break;
                            case 0: // Create Shortcut
                                TermActivity.startActivity(HomeMainActivity.this);
                                break;
                            case 1:
                                NotebookActivity.start(HomeMainActivity.this, null, false);
                                break;
                            case 2:
                                TermActivity.startShell(HomeMainActivity.this,"shell");
                                break;
                        }
                    }).setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            })
                    .show();

            return true;
        });
        binding.llEditor.setOnClickListener(v -> {
            EditorActivity.start(this);
            sendEvent(getString(R.string.event_editor));
        });
        binding.llLibrary.setOnClickListener(v -> {
            LibActivity.start(this);
            sendEvent(getString(R.string.event_qpypi));
        });
//        binding.llCommunity.setOnClickListener(v -> {
//            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_COMMUNITY)));
//            sendEvent(getString(R.string.event_commu));
//        });
//        binding.llGist.setOnClickListener(view -> GistActivity.startCommunity(HomeMainActivity.this)
//        );
        binding.llSetting.setOnClickListener(v -> {
            SettingActivity.startActivity(this);
            sendEvent(getString(R.string.event_setting));
        });
        binding.llFile.setOnClickListener(v -> {
            TedLocalActivity.start(this, TedLocalActivity.REQUEST_HOME_PAGE);
            sendEvent(getString(R.string.event_file));
        });
        binding.llQpyApp.setOnClickListener(v -> {
            //openQpySDK(view -> {
                AppListActivity.start(HomeMainActivity.this, AppListActivity.TYPE_SCRIPT);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                sendEvent(getString(R.string.event_top));
            //});
        });

//        binding.llCourse.setOnClickListener(v -> {
//            CourseActivity.start(HomeMainActivity.this);
//            sendEvent(getString(R.string.event_course));
//        });
        String courseUrl = getString(R.string.url_course);
        binding.llCourse.setOnClickListener(v ->
                QWebViewActivity.start(HomeMainActivity.this, getString(R.string.course), courseUrl));

//        initCourseListener();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.getDefault().unregister(this);
        boolean isKeepAlive = preferences.getBoolean(getString(R.string.key_alive), false);
        if (!isKeepAlive){
            return;
        }
        Cactus.getInstance().unregister(this);
    }

    private void handlePython3(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(getString(R.string.action_from_python_three))
                && NAction.getQPyInterpreter(this).equals(PYTHON_2)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.py2_now)
                    .setMessage(R.string.switch_py3_hint)
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(R.string.goto_setting, (dialog, which) -> SettingActivity.startActivity(this))
                    .create()
                    .show();
        }
    }

    private void handleNotification(Bundle bundle) {
        if (bundle == null) {return;}
        if (!bundle.getBoolean("force") && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_hide_push), true)) {
            return;
        }
        String type = bundle.getString("type", "");
        if (!type.equals("")) {
            String link = bundle.getString("link", "");
            String title = bundle.getString("title", "");

            switch (type) {
                case "in":
                    QWebViewActivity.start(this, title, link);
                    break;
                case "ext":
                    Intent starter = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(starter);
                    break;
                default:break;
            }
        }
    }

    private void handleNotification() {
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_hide_push), true)) {
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences(CONF.NOTIFICATION_SP_NAME, MODE_PRIVATE);
        try {
            String notifString = sharedPreferences.getString(CONF.NOTIFICATION_SP_OBJ, "");
            if ("".equals(notifString)) {
                return;
            }
            JSONObject extra = new JSONObject(notifString);
            String type = extra.getString("type");
            String link = extra.getString("link");
            String title = extra.getString("title");
            switch (type) {
                case "in":
                    QWebViewActivity.start(this, title, link);
                    break;
                case "ext":
                    Intent starter = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(starter);
                    break;
                default:break;
            }
            sharedPreferences.edit().clear().apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void openQpySDK() {
        /*Log.d("HomeMainActivity", "openQpySDK");
        String[] permssions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        checkPermissionDo(permssions, new BaseActivity.PermissionAction() {
            @Override
            public void onGrant() {*/
                //这里只执行一次做为初始化
                if (!NAction.isQPyInterpreterSet(HomeMainActivity.this)) {
                    initQpySDK3();
                }
            /*}

            @Override
            public void onDeny() {
                Toast.makeText(HomeMainActivity.this,  getString(R.string.grant_storage_hint), Toast.LENGTH_SHORT).show();
            }
        });*/
        try {
            CONF.NATIVE_LIBRARY = this.getPackageManager().getApplicationInfo(
                    this.getPackageName(),PackageManager.GET_UNINSTALLED_PACKAGES).nativeLibraryDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在工作线程中作初始化
     */
    private void initQpySDK3() {
        long t1 = SystemClock.elapsedRealtimeNanos();
        NAction.setQPyInterpreter(HomeMainActivity.this, "3.x");
        initQPy();
        initIcon();
        long t2 = SystemClock.elapsedRealtimeNanos();
        int t = (int) Math.round((t2-t1)*0.025);
        File f = new File(getFilesDir()+"/resource3.version");
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(t);
        progressDialog.setTitle(R.string.initial_qpython);
        progressDialog.setCancelable(false);
        progressDialog.show();
        final boolean[] exist = {false};
        (new CountDownTimer(t,1000){
            @Override
            public void onTick(long l) {
                int c = t - (int) l;
             progressDialog.setProgress(c);
             if(c*4<t && !exist[0]) {
                 if(f.exists())
                     exist[0] = true;
             } else {
                 if(!f.exists()){
                     this.cancel();
                     onFinish();
                 }
             }
             }
            @Override
            public void onFinish() {
                progressDialog.dismiss();
            }
        }).start();
    }

    private void initQPy() {
        new Thread(() -> {
            QPySDK qpysdk = new QPySDK(HomeMainActivity.this, HomeMainActivity.this);
            File externalStorage = FileUtils.getPath(App.getContext());
            qpysdk.extractRes("resource3", HomeMainActivity.this.getFilesDir(),true);
            TermActivity.startShell(HomeMainActivity.this,"setup");
            FileHelper.createDirIfNExists(externalStorage + "/cache");
            FileHelper.createDirIfNExists(externalStorage + "/log");
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case LOGIN_REQUEST_CODE:
                    binding.login.setText(Html.fromHtml(getString(R.string.welcome_s, App.getUser().getNick())));
                    break;
            }
        }
    }

    @Subscribe
    public void startQrCodeActivity(StartQrCodeActivityEvent event) {
        String[] permissions = {Manifest.permission.CAMERA};

        checkPermissionDo(permissions, new BaseActivity.PermissionAction() {
            @Override
            public void onGrant() {
                QrCodeActivity.start(HomeMainActivity.this);
            }

            @Override
            public void onDeny() {
                Toast.makeText(HomeMainActivity.this, getString(R.string.no_camera), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class StartQrCodeActivityEvent {

    }

    private void sendEvent(String evenName) {

    }

    @SuppressLint("HandlerLeak")
    private void setHandler(){
        QPyInterfaceFacade.handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                String[] string = (String[]) msg.obj;
                ScriptExec.getInstance().playScript(
                        HomeMainActivity.this,
                        string[0],string[1]
                );
            }
        };
        AndroidFacade.handler = QPyInterfaceFacade.handler;
    }

}
