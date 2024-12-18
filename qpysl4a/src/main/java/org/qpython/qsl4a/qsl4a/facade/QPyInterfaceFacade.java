package org.qpython.qsl4a.qsl4a.facade;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qsl4a.qsl4a.util.FileUtils;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.util.SPFUtils;

import java.io.File;


/**
 * Wifi functions.
 */
public class QPyInterfaceFacade extends RpcReceiver {
    private final Service mService;
    private final AndroidFacade mAndroidFacade;
    private final Context context;
    public static Handler handler;
    private static JSONObject SharedVariable;

    public QPyInterfaceFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        context = mAndroidFacade.context;
    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub
    }

    @Rpc(description = "Execute QPython script throught SL4A", returns = "True if the operation succeeded.")
    public Boolean executeQPyAsSrv(@RpcParameter(name = "QPython script path") @RpcOptional String path) {
        Intent intent = new Intent();
        intent.setClassName(mService.getPackageName(), "org.qpython.qpylib.MPyService");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("org.qpython.qpylib.action.MPyApi");

        Bundle mBundle = new Bundle();
        mBundle.putString("app", SPFUtils.getCode(context));
        mBundle.putString("act", "onPyApi");
        mBundle.putString("flag", "api");
        mBundle.putString("param", "fileapi");
        mBundle.putString("pyfile", path);

        intent.putExtras(mBundle);

        context.startService(intent);

        return true;
    }


    @Rpc(description = "Execute QPython script throught SL4A", returns = "True if the operation succeeded.")
    public Boolean executeQPy(
            @RpcParameter(name = "QPython script path") @RpcDefault("") String path,
            @RpcParameter(name = "QPython script arguments") @RpcOptional String arg
                              ) {
        try {
            Message msg = new Message();
            msg.obj = new String[]{path,arg};
            handler.sendMessage(msg);
        } catch (Exception e) {
            Intent intent = new Intent();
            intent.setClassName(mService.getPackageName(), "org.qpython.qpylib.MPyApi");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("org.qpython.qpylib.action.MPyApi");

            Bundle mBundle = new Bundle();
            mBundle.putString("app", SPFUtils.getCode(context));
            mBundle.putString("act", "onPyApi");
            mBundle.putString("flag", "api");
            mBundle.putString("param", "fileapi");
            mBundle.putString("pyfile", path);
            mBundle.putString("pyarg",arg);

            intent.putExtras(mBundle);
            context.startActivity(intent);
        }

        return true;
    }

    @Rpc(description = "Execute QPython script throught SL4A", returns = "True if the operation succeeded.")
    public Boolean executeQPyCodeAsSrv(@RpcParameter(name = "QPython script code") @RpcOptional String code) {
        Intent intent = new Intent();
        intent.setClassName(mService.getPackageName(), "org.qpython.qpylib.MPyService");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("org.qpython.qpylib.action.MPyService");

        Bundle mBundle = new Bundle();
        mBundle.putString("app", SPFUtils.getCode(context));
        mBundle.putString("act", "onPyApi");
        mBundle.putString("flag", "api");
        mBundle.putString("param", "codeapi");
        mBundle.putString("pycode", code);

        intent.putExtras(mBundle);

        context.startService(intent);

        return true;
    }

    @Rpc(description = "Execute QPython script throught SL4A", returns = "True if the operation succeeded.")
    public Boolean executeQPyCode(@RpcParameter(name = "QPython script code") @RpcOptional String code) {
        Intent intent = new Intent();
        intent.setClassName(mService.getPackageName(), "org.qpython.qpylib.MPyApi");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("org.qpython.qpylib.action.MPyApi");

        Bundle mBundle = new Bundle();
        mBundle.putString("app", SPFUtils.getCode(context));
        mBundle.putString("act", "onPyApi");
        mBundle.putString("flag", "api");
        mBundle.putString("param", "codeapi");
        mBundle.putString("pycode", code);

        intent.putExtras(mBundle);

        context.startActivity(intent);

        return true;
    }

    @Rpc(description = "Get last QPython execute log", returns = "LogUtil string")
    public String getLastLog(
            @RpcParameter(name = "QPython log name") @RpcDefault("last.log") String logName) {
        /*String content;
        content = FileUtils.getFileContents(path, 64);
        boolean isQApp = content.contains("#qpy:qpyapp");
        boolean isWeb = content.contains("#qpy:webapp");
        //boolean isKivy = content.contains("#qpy:kivy");

        /*if (isKivy) {
            File script = new File(path);
            String log = script.getParentFile().getAbsolutePath()+"/.run.log";
            File lf = new File(log);
            if (lf.exists()) {
                return "# QPython:getLastLog(ok)\n"
                        +"# RunTime:"+ SPFUtils.getDateTime(lf.lastModified())+"\n"
                        +"# LogFile:"+ log+"\n"
                        +FileUtils.getFileContents(log);
            } else {
                return "# QPython:getLastLog(-1)"
                        +"# LogFile:"+ log;

            }

        } else if (isWeb || isQApp) {
            //File script = new File(path);
            */
        String log = context.getExternalFilesDir("").getParent() + "/log/" + logName;
        File lf = new File(log);
        if (lf.exists()) {
            return "# QPython:getLastLog(ok)\n"
                    + "# RunTime:" + SPFUtils.getDateTime(lf.lastModified()) + "\n"
                    + "# LogFile:" + log + "\n"
                    + FileUtils.getFileContents(log);
        } else {
            return "# QPython:getLastLog(-1)\n"
                    + "# LogFile:" + log
                    ;

        }


    } /*else {
            return "";

        }*/

    @Rpc(description = "set Java Shared Variable .")
    public void sharedVariableSet(
            @RpcParameter(name = "key") String key,
            @RpcParameter(name = "value") String value
    ) throws JSONException {
        if(SharedVariable==null)
            SharedVariable=new JSONObject();
        SharedVariable.put(key,value);
    }

    @Rpc(description = "get Java Shared Variable .")
    public String sharedVariableGet(
            @RpcParameter(name = "key") String key
    ) throws JSONException {
        return SharedVariable.getString(key);
    }

    @Rpc(description = "remove Java Shared Variable .")
    public void sharedVariableRemove(
            @RpcParameter(name = "key") String key
    ) throws JSONException {
        SharedVariable.remove(key);
        if(SharedVariable.length()==0)
            SharedVariable=null;
    }
}
