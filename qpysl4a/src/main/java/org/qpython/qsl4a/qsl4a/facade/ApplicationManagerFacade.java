package org.qpython.qsl4a.qsl4a.facade;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;


import org.json.JSONObject;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for managing Applications.
 * 
 */
public class ApplicationManagerFacade extends RpcReceiver {

  private final AndroidFacade mAndroidFacade;
  private final ActivityManager mActivityManager;
  private final PackageManager mPackageManager;
  private final Context context;

  public ApplicationManagerFacade(FacadeManager manager) {
    super(manager);
    Service service = manager.getService();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    mActivityManager = (ActivityManager) service.getSystemService(Context.ACTIVITY_SERVICE);
    mPackageManager = service.getPackageManager();
    context = mAndroidFacade.context;
  }

  @Rpc(description = "Returns a list of all launchable packages with class name and application name .")
  public Map<String, String> getLaunchablePackages(
          @RpcParameter(name = "need class name") @RpcDefault("false") Boolean needClassName) {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, 0);
    Map<String, String> applications = new HashMap<String, String>();
    if (needClassName){
      for (ResolveInfo info : resolveInfos) {
      applications.put(info.activityInfo.packageName, info.activityInfo.name+"|"+info.loadLabel(mPackageManager).toString());
    }}
    else {
    for (ResolveInfo info : resolveInfos) {
      applications.put(info.activityInfo.packageName, info.loadLabel(mPackageManager).toString());
    }}
    return applications;
  }

  @Rpc(description = "get Application Info")
  public JSONObject getApplicationInfo(
          @RpcParameter(name = "package name") @RpcOptional String packageName) throws Exception {
    if(packageName == null)
      packageName = context.getPackageName();
    ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
    JSONObject result = new JSONObject();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
      result.put("compileSdkVersion",appInfo.compileSdkVersion);
    result.put("targetSdkVersion",appInfo.targetSdkVersion);
    result.put("minSdkVersion",appInfo.minSdkVersion);
    result.put("className",appInfo.className);
    result.put("uid",appInfo.uid);
    result.put("dataDir",appInfo.dataDir);
    result.put("nativeLibraryDir",appInfo.nativeLibraryDir);
    result.put("sourceDir",appInfo.sourceDir);
    result.put("publicSourceDir",appInfo.publicSourceDir);
    result.put("deviceProtectedDataDir",appInfo.deviceProtectedDataDir);
    result.put("label",appInfo.loadLabel(mPackageManager));
    return result;
  }

  @Rpc(description = "Start activity with the given classname and/or packagename .")
  public void launch(@RpcParameter(name = "classname") @RpcOptional final String classname,
                     @RpcParameter(name = "packagename") @RpcOptional String packagename,
                     @RpcParameter(name = "wait") @RpcDefault("true") final Boolean wait)
          throws Exception {
    Intent intent;
    if (classname == null) {
      intent=context.getPackageManager().getLaunchIntentForPackage(packagename);
    } else {
      intent = new Intent(Intent.ACTION_MAIN);
      if (packagename == null) {
        packagename = classname.substring(0, classname.lastIndexOf("."));
      }
      intent.setClassName(packagename, classname);
    }
    mAndroidFacade.doStartActivity(intent,wait);
  }

  @Rpc(description = "Returns a list of packages running activities or services.", returns = "List of packages running activities.")
  public Set<String> getRunningPackages() {
    Set<String> runningPackages = new HashSet<>();
    List<ActivityManager.RunningAppProcessInfo> appProcesses =
        mActivityManager.getRunningAppProcesses();
    for (ActivityManager.RunningAppProcessInfo info : appProcesses) {
      runningPackages.addAll(Arrays.asList(info.pkgList));
    }
    List<ActivityManager.RunningServiceInfo> serviceProcesses =
        mActivityManager.getRunningServices(Integer.MAX_VALUE);
    for (ActivityManager.RunningServiceInfo info : serviceProcesses) {
      runningPackages.add(info.service.getPackageName());
    }
    return runningPackages;
  }

  @Rpc(description = "get installed packages")
  public Map<String,String> getInstalledPackages(
          @RpcParameter(name = "flag") @RpcDefault("4") final Integer flag
  ) {
    Map<String, String> packages = new HashMap<>();
    List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
    boolean allow;
    for (PackageInfo info : packageInfos) {
      switch (flag) {
        case 5://所有应用
          allow = true;
          break;
        case 4://用户应用
          allow = ( info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM ) == 0;
          break;
        case 3://系统应用
          allow = ( info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM ) != 0;
          break;
        case 2://系统已更新应用
          allow = ( info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP ) != 0;
          break;
        case 1://系统未更新应用
          allow = ( info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM ) != 0 && ( info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP ) == 0;
          break;
        default:
          allow = false;
      }
      if(allow)
          packages.put(info.packageName,  info.applicationInfo.loadLabel(mPackageManager).toString());
    }
    return packages;
  }

  @SuppressWarnings("deprecation")
@Rpc(description = "Force stops a package.")
  public void forceStopPackage(
      @RpcParameter(name = "packageName", description = "name of package") final String packageName) {
    mActivityManager.restartPackage(packageName);
  }

  @Override
  public void shutdown() {
  }
}
