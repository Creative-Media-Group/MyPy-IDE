/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.interpreter.html;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.qpython.qsl4a.qsl4a.util.FileUtils;
import org.qpython.qsl4a.qsl4a.LogUtil;
import org.qpython.qsl4a.qsl4a.SingleThreadExecutor;
import org.qpython.qsl4a.qsl4a.event.Event;
import org.qpython.qsl4a.qsl4a.facade.EventFacade;
import org.qpython.qsl4a.qsl4a.facade.ui.UiFacade;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTask;
import org.qpython.qsl4a.qsl4a.jsonrpc.JsonBuilder;
import org.qpython.qsl4a.qsl4a.jsonrpc.JsonRpcResult;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiverManager;
import org.qpython.qsl4a.qsl4a.rpc.MethodDescriptor;
import org.qpython.qsl4a.qsl4a.rpc.RpcError;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Alexey Reznichenko (alexey.reznichenko@gmail.com)
 */
public class HtmlActivityTask extends FutureActivityTask<Void> {

    private static final String TAG = "HtmlActivityTask";
  private static final String HTTP = "http";
  private static final String ANDROID_PROTOTYPE_JS =
      "Android.prototype.%1$s = function(var_args) { "
          + "return this._call(\"%1$s\", Array.prototype.slice.call(arguments)); };";

  private static final String PREFIX = "file://";
//  private static final String BASE_URL = PREFIX + InterpreterConstants.SCRIPTS_ROOT;

  private final RpcReceiverManager mReceiverManager;
  private final String mJsonSource;
  private final String mAndroidJsSource;
  private final String mTemplateSource;

  private final String mAPIWrapperSource;
  private final String mUrl;
  private final JavaScriptWrapper mWrapper;
  private final HtmlEventObserver mObserver;
  private final UiFacade mUiFacade;
  private ChromeClient mChromeClient;
  private WebView mView;
  private MyWebViewClient mWebViewClient;
  private static HtmlActivityTask reference;
  private boolean mDestroyManager;

  public HtmlActivityTask(RpcReceiverManager manager, String androidJsSource, String jsonSource,
      String templateSource, String url, boolean destroyManager) {
    reference = this;
    mReceiverManager = manager;
    mJsonSource = jsonSource;
    mAndroidJsSource = androidJsSource;
    mTemplateSource = templateSource;
    mAPIWrapperSource = generateAPIWrapper();
    mWrapper = new JavaScriptWrapper();
    mObserver = new HtmlEventObserver();
    mReceiverManager.getReceiver(EventFacade.class).addGlobalEventObserver(mObserver);
    mUiFacade = mReceiverManager.getReceiver(UiFacade.class);
    mUrl = url;
    mDestroyManager = destroyManager;
  }

  public RpcReceiverManager getRpcReceiverManager() {
    return mReceiverManager;
  }

  /*
   * New WebviewClient
   */
  private class MyWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      /*
       * if (Uri.parse(url).getHost().equals("www.example.com")) { // This is my web site, so do not
       * override; let my WebView load the page return false; } // Otherwise, the link is not for a
       * page on my site, so launch another Activity that handles URLs Intent intent = new
       * Intent(Intent.ACTION_VIEW, Uri.parse(url)); startActivity(intent);
       */
      if (!HTTP.equals(Uri.parse(url).getScheme())) {
        String source = null;
        try {
          source = FileUtils.readToString(new File(Uri.parse(url).getPath()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        if (!source.contains("</html>")) {  // if doesn't contain html tag, then use builtin template
          source = mTemplateSource.replace("{{WEBAPPCONTENT}}", source);
        }

        source = source.replace("{{QPYBUILTIN}}","<script>" + mJsonSource + "</script>" + "<script>" + mAndroidJsSource + "</script>"
                + "<script>" + mAPIWrapperSource + "</script>");

        mView.loadDataWithBaseURL(getBaseUrl(mView.getContext().getApplicationContext()), source, "text/html", "utf-8", null);

        Log.d("MyWebViewClient", source);

      } else {
        mView.loadUrl(url);
      }
      return true;
    }
  }

  private String getBaseUrl(Context context){
    return PREFIX + com.quseit.util.FileUtils.getScriptsRootPath(context);
  }

  @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
  @Override
  public void onCreate() {
    mView = new WebView(getActivity());
    mView.setId(View.NO_ID);
    mView.getSettings().setJavaScriptEnabled(true);
    mView.addJavascriptInterface(mWrapper, "_rpc_wrapper");
    mView.addJavascriptInterface(new Object() {

      @SuppressWarnings("unused")
      public void register(String event, int id) {
        mObserver.register(event, id);
      }
    }, "_callback_wrapper");

    getActivity().setContentView(mView);

    mView.setOnCreateContextMenuListener(getActivity());
    mChromeClient = new ChromeClient(getActivity());
    mWebViewClient = new MyWebViewClient();
    mView.setWebChromeClient(mChromeClient);
    mView.setWebViewClient(mWebViewClient);
    mView.loadUrl("javascript:" + mJsonSource);
    mView.loadUrl("javascript:" + mAndroidJsSource);
    mView.loadUrl("javascript:" + mAPIWrapperSource);
    load();
  }

  private void load() {
      Log.d(TAG, "load:"+mUrl);
    if (!HTTP.equals(Uri.parse(mUrl).getScheme())) {
      String source = null;
      try {

        source = FileUtils.readToString(new File(Uri.parse(mUrl).getPath()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
        if (!source.contains("</html>")) {  // if doesn't contain html tag, then use builtin template
            source = mTemplateSource.replace("{{WEBAPPCONTENT}}", source);
        }

        source = source.replace("{{QPYBUILTIN}}","<script>" + mJsonSource + "</script>" + "<script>" + mAndroidJsSource + "</script>"
              + "<script>" + mAPIWrapperSource + "</script>");

        //Log.d(TAG, "source:"+source);
      mView.loadDataWithBaseURL(getBaseUrl(mView.getContext().getApplicationContext()), source, "text/html", "utf-8", null);
    } else {
      mView.loadUrl(mUrl);
    }
  }

  @Override
  public void onDestroy() {
    mReceiverManager.getReceiver(EventFacade.class).removeEventObserver(mObserver);
    if (mDestroyManager) {
      mReceiverManager.shutdown();
    }
    mView.destroy();
    mView = null;
    reference = null;
    setResult(null);
  }

  public static void shutdown() {
    if (HtmlActivityTask.reference != null) {
      HtmlActivityTask.reference.finish();
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    mUiFacade.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    return mUiFacade.onPrepareOptionsMenu(menu);
  }

  private String generateAPIWrapper() {
    StringBuilder wrapper = new StringBuilder();
    for (Class<? extends RpcReceiver> clazz : mReceiverManager.getRpcReceiverClasses()) {
      for (MethodDescriptor rpc : MethodDescriptor.collectFrom(clazz)) {
        wrapper.append(String.format(ANDROID_PROTOTYPE_JS, rpc.getName()));
      }
    }
    return wrapper.toString();
  }

  private class JavaScriptWrapper {
    @SuppressWarnings("unused")
    @JavascriptInterface
    public String call(String data) throws JSONException {
      LogUtil.v("Received: " + data);
      JSONObject request = new JSONObject(data);
      int id = request.getInt("id");
      String method = request.getString("method");
      JSONArray params = request.getJSONArray("params");
      MethodDescriptor rpc = mReceiverManager.getMethodDescriptor(method);
      if (rpc == null) {
        return JsonRpcResult.error(id, new RpcError("Unknown RPC:"+method)).toString();
      }
      try {
        return JsonRpcResult.result(id, rpc.invoke(mReceiverManager, params)).toString();
      } catch (Throwable t) {
        LogUtil.e("Invocation error.", t);
        return JsonRpcResult.error(id, t).toString();
      }
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void dismiss() {
        Log.d(TAG, "dismiss");
      Activity parent = getActivity();
      parent.finish();
    }
  }

  private class HtmlEventObserver implements EventFacade.EventObserver {
    private Map<String, Set<Integer>> mEventMap = new HashMap<String, Set<Integer>>();

    public void register(String eventName, Integer id) {
      if (mEventMap.containsKey(eventName)) {
        mEventMap.get(eventName).add(id);
      } else {
        Set<Integer> idSet = new HashSet<Integer>();
        idSet.add(id);
        mEventMap.put(eventName, idSet);
      }
    }

    @Override
    public void onEventReceived(Event event) {
      final JSONObject json = new JSONObject();
      try {
        json.put("data", JsonBuilder.build(event.getData()));
      } catch (JSONException e) {
        LogUtil.e(e);
      }
      if (mEventMap.containsKey(event.getName())) {
        for (final Integer id : mEventMap.get(event.getName())) {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mView.loadUrl(String.format("javascript:droid._callback(%d, %s);", id, json));
            }
          });
        }
      }
    }

    @SuppressWarnings("unused")
    public void dismiss() {
      Activity parent = getActivity();
      parent.finish();
    }
  }

  private class ChromeClient extends WebChromeClient {
    private final static String JS_TITLE = "JavaScript Dialog";

    private final Activity mActivity;
    private final Resources mResources;
    private final ExecutorService mmExecutor;

    public ChromeClient(Activity activity) {
      mActivity = activity;
      mResources = mActivity.getResources();
      mmExecutor = new SingleThreadExecutor();
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
      mActivity.setTitle(title);
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
      mActivity.getWindow().requestFeature(Window.FEATURE_RIGHT_ICON);
      mActivity.getWindow().setFeatureDrawable(Window.FEATURE_RIGHT_ICON, new BitmapDrawable(icon));
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
      final UiFacade uiFacade = mReceiverManager.getReceiver(UiFacade.class);
      uiFacade.dialogCreateAlert(JS_TITLE, message);
      uiFacade.dialogSetPositiveButtonText(mResources.getString(android.R.string.ok));

      mmExecutor.execute(new Runnable() {

        @Override
        public void run() {
          try {
            uiFacade.dialogShow();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          uiFacade.dialogGetResponse();
          result.confirm();
        }
      });
      return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
      final UiFacade uiFacade = mReceiverManager.getReceiver(UiFacade.class);
      uiFacade.dialogCreateAlert(JS_TITLE, message);
      uiFacade.dialogSetPositiveButtonText(mResources.getString(android.R.string.ok));
      uiFacade.dialogSetNegativeButtonText(mResources.getString(android.R.string.cancel));

      mmExecutor.execute(new Runnable() {

        @Override
        public void run() {
          try {
            uiFacade.dialogShow();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          Map<String, Object> mResultMap = (Map<String, Object>) uiFacade.dialogGetResponse();
          if ("positive".equals(mResultMap.get("which"))) {
            result.confirm();
          } else {
            result.cancel();
          }
        }
      });

      return true;
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, final String message,
        final String defaultValue, final JsPromptResult result) {
      final UiFacade uiFacade = mReceiverManager.getReceiver(UiFacade.class);
      mmExecutor.execute(new Runnable() {
        @Override
        public void run() {
          String value = null;
          try {
            value = uiFacade.dialogGetInput(JS_TITLE, message, defaultValue);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          if (value != null) {
            result.confirm(value);
          } else {
            result.cancel();
          }
        }
      });
      return true;
    }
  }
}
