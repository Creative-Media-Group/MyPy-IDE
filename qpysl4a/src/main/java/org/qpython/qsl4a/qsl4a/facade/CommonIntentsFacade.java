package org.qpython.qsl4a.qsl4a.facade;

import android.app.SearchManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Contacts.People;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;


import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A selection of commonly used intents. <br>
 * <br>
 * These can be used to trigger some common tasks.
 * 
 */
@SuppressWarnings("deprecation")
public class CommonIntentsFacade extends RpcReceiver {

  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private final String qpyProvider;
  private final Service mService;

  public CommonIntentsFacade(FacadeManager manager) {
    super(manager);
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    qpyProvider = mAndroidFacade.qpyProvider;
    mService = manager.getService();
  }

  @Override
  public void shutdown() {
  }

  @Rpc(description = "Display content to be picked by URI (e.g. contacts)", returns = "A map of result values.")
  public Intent pick(@RpcParameter(name = "uri") String uri) throws JSONException {
    return mAndroidFacade.startActivityForResult(Intent.ACTION_PICK, uri, null, null, null, null);
  }

  @Rpc(description = "Starts the barcode scanner.", returns = "Scan Result String .")
  public String scanBarcode(
          @RpcParameter(name = "title") @RpcOptional String title
  ) throws Exception {
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),"org.qpython.qpy.main.activity.QrCodeActivityRstOnly");
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra("title",title);
    intent = mAndroidFacade.startActivityForResult(intent);
    try {
      return intent.getStringExtra("result"); }
    catch (NullPointerException e) {
      return null;
    }
  }

  private void view(Uri uri, String type) throws Exception {

    Intent intent = new Intent();
    intent.setClassName(this.mAndroidFacade.getmService().getApplicationContext(),"org.qpython.qpy.main.QWebViewActivity");
    intent.putExtra("com.quseit.common.extra.CONTENT_URL1", "main");
    intent.putExtra("com.quseit.common.extra.CONTENT_URL2", "QPyWebApp");
    //intent.putExtra("com.quseit.common.extra.CONTENT_URL6", "drawer");
    intent.setDataAndType(uri, type);
    mAndroidFacade.startActivity(intent);
  }

  @Rpc(description = "Start activity with view action by URI (i.e. browser, contacts, etc.).")
  public void view(
      @RpcParameter(name = "uri") String uri,
      @RpcParameter(name = "type", description = "MIME type/subtype of the URI") @RpcOptional String type,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent") @RpcOptional JSONObject extras)
      throws Exception {
    mAndroidFacade.startActivity(Intent.ACTION_VIEW, uri, type, extras, true, null, null);
  }

  @Rpc(description = "Opens a map search for query (e.g. pizza, 123 My Street).")
  public void viewMap(@RpcParameter(name = "query, e.g. pizza, 123 My Street") String query)
      throws Exception {
    view("geo:0,0?q=" + query, null, null);
  }

  @Rpc(description = "Opens the list of contacts.")
  public void viewContacts() throws Exception {
    view(People.CONTENT_URI, null);
  }

  @Rpc(description = "Opens the browser to display a local HTML/text/audio/video File or http(s) Website .")
  public void viewHtml(
          @RpcParameter(name = "path", description = "the path to the local HTML/text/audio/video File or http(s) Website") String path,
          @RpcParameter(name = "title") @RpcOptional String title,
          @RpcParameter(name = "wait") @RpcDefault("true") Boolean wait)
          throws Exception {
    Uri uri;
    Intent intent = new Intent();
    if (path.contains("://")) {
      uri=Uri.parse(path);
      intent.putExtra("src",path);
    } else {
      uri=Uri.fromFile(new File(path));
      intent.putExtra("LOG_PATH",path);
    }
    intent.setClassName(context,"org.qpython.qpy.main.activity.QWebViewActivity");
    intent.setDataAndType(uri, "text/html");
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("title",title);
    mAndroidFacade.doStartActivity(intent,wait);
  }

  @Rpc(description = "Starts a search for the given query.")
  public void search(@RpcParameter(name = "query") String query) throws Exception {
    Intent intent = new Intent(Intent.ACTION_SEARCH);
    intent.putExtra(SearchManager.QUERY, query);
    mAndroidFacade.startActivity(intent);
  }

  @Rpc(description = "Convert normal path to content:// or file:// .")
  public String pathToUri(
          @RpcParameter(name = "path") String path) {
    File file = new File(path);
    Uri uri;
    if (Build.VERSION.SDK_INT>=24) {
    uri = FileProvider.getUriForFile(context,qpyProvider,file);
    } else {
       uri = Uri.fromFile(file);
    }
    return uri.toString();
  }

  @Rpc(description = "Open a file with path")
  public void openFile(
          @RpcParameter(name = "path") String path,
          @RpcParameter(name = "type", description = "a MIME type of a file") @RpcOptional String type,
          @RpcParameter(name = "wait") @RpcDefault("true") Boolean wait)
          throws Exception {
    MimeTypeMap mime = MimeTypeMap.getSingleton();
    if (type == null) {
      /* 获取文件的后缀名 */
      int dotIndex = path.lastIndexOf(".");
      if (dotIndex < 0) {
        type = "*/*";  //找不到扩展名
      } else {
        try {
          type = mime.getMimeTypeFromExtension( path.substring( dotIndex + 1 ).toLowerCase() );
          if (type == null) {
            type = "*/*";  //找不到打开方式
          }
        } catch (Exception e) {
          type="*/*";  //出现错误
        }
      }}
    Intent intent = new Intent();
    intent.setAction(android.content.Intent.ACTION_VIEW);
    File file = new File(path);
    Uri uri;
    uri = FileProvider.getUriForFile(context,qpyProvider,file);
    intent.setDataAndType(uri, type);
    try {
      mAndroidFacade.doStartActivity(intent,wait);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
