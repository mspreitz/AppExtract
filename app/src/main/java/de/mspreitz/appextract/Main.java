package de.mspreitz.appextract;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.stericson.RootTools.RootTools;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.InputStream;
import java.util.Locale;


public class Main extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab tab1 = ab.newTab().setText(R.string.main).setTabListener(
                new MyTabListener(this, MainFragment.class.getName()));
        ab.addTab(tab1);
        ActionBar.Tab tab2 = ab.newTab().setText(R.string.installed).setTabListener(
                new MyTabListener(this, ListInstalledApps.class.getName()));
        ab.addTab(tab2);
        ActionBar.Tab tab3 = ab.newTab().setText(R.string.running).setTabListener(
                new MyTabListener(this, ListRunningApps.class.getName()));
        ab.addTab(tab3);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = true;
        switch (item.getItemId()) {
            case R.id.menu_about: {
                displayAboutDialog();
                break;
            }
            case R.id.menu_send: {
                LoadData task = new LoadData();
                task.execute();
                break;
            }
            default: {
                result = super.onOptionsItemSelected(item);
                break;
            }
        }
        return result;
    }

    private void displayAboutDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about_title));
        builder.setMessage(getString(R.string.about_desc));
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static String calculateMD5(String apkFile) {
        MessageDigest digest;
        String output = "unknown";
        try{
            digest = MessageDigest.getInstance("MD5");
            try {
                InputStream is = new FileInputStream(apkFile);
                byte[] buffer = new byte[8192];
                int read;
                try {
                    while ((read = is.read(buffer)) > 0) {
                        digest.update(buffer, 0, read);
                    }
                    byte[] md5sum = digest.digest();
                    BigInteger bigInt = new BigInteger(1, md5sum);
                    output = bigInt.toString(16);
                    output = String.format("%32s", output).replace(' ', '0');
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to calculate MD5", e);
                }
            }catch (FileNotFoundException e){
                Log.e("MD5", "apk file not found", e);
            }
        }catch (NoSuchAlgorithmException e){
            Log.e("MD5", "No md5 algorithm available", e);
        }
        return output;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void sendEmailMessage(String body) {
        Intent mailIntent = new Intent();
        mailIntent.setAction(Intent.ACTION_SEND);
        mailIntent.setType("message/rfc822");
        mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {""});
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, "AppExtract for Android");
        mailIntent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(mailIntent);
    }

    public void sendMessage() {
        if (isExternalStorageWritable()) {
            try {
                File folder = new File(Environment.getExternalStorageDirectory() + "/AppExtract");
                if (!folder.exists()) folder.mkdir();
                StringBuilder eMailBody = new StringBuilder();
                final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                RootTools.debugMode = false;
                String isRooted = "not checked";
                if (RootTools.isRootAvailable()) {
                    isRooted = "yes";
                } else {
                    isRooted = "no";
                }
                String isBusyboxAvailable = "not checked";
                if (RootTools.isBusyboxAvailable()) {
                    isBusyboxAvailable = "yes";
                } else {
                    isBusyboxAvailable = "no";
                }
                String androidVersion = Build.VERSION.RELEASE;
                String androidModel = Build.MODEL;
                try{
                    FileWriter fw3 = new FileWriter(folder + "/Meta_" + String.valueOf(Calendar.getInstance().getTimeInMillis()) + ".txt");
                    String meta = "Android Device: " + androidModel + "\n" + "Android Version: " + androidVersion + "\n" + "Is Device rooted: " + isRooted + "\n" + "Is Busybox available: " + isBusyboxAvailable;
                    eMailBody.append(meta);
                    eMailBody.append("\n\n\n");
                    fw3.append(meta);
                    fw3.flush();
                    fw3.close();
                }catch (IOException e){
                    Log.e("Meta", "Error while gathering data", e);
                }
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                String filenameInstalledApps = folder + "/InstalledApps_" + String.valueOf(Calendar.getInstance().getTimeInMillis()) + ".csv";
                eMailBody.append("List of installed Applications:\n--------------------------------------------------------------\n");
                try{
                    FileWriter fw = new FileWriter(filenameInstalledApps);
                    fw.append("Type;App_Name;md5;TargetSdkVersion;Package_Name;Process_Name;APK_Location;Version_Code;Version_Name;Certificate_Info;Certificate_SN;InstallTime;LastModified;\n");
                    StringBuilder installedApps = new StringBuilder();
                    for(ApplicationInfo app : apps) {
                        installedApps.setLength(0);
                        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                            try{
                                String md5 = calculateMD5(app.sourceDir);
                                installedApps.append("SystemApp;")
                                        .append(pm.getApplicationLabel(app)).append(";")
                                        .append(md5).append(";")
                                        .append(app.targetSdkVersion).append(";")
                                        .append(app.packageName).append(";")
                                        .append(app.processName).append(";")
                                        .append(app.sourceDir).append(";")
                                        .append(pm.getPackageInfo(app.packageName, 0).versionCode).append(";")
                                        .append(pm.getPackageInfo(app.packageName, 0).versionName).append(";")
                                        .append(((X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures[0].toByteArray()))).getSubjectDN()).append(";")
                                        .append(((X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures[0].toByteArray()))).getSerialNumber()).append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append("\n");
                            }catch(Exception e){
                                Log.e("SystemApps", "Error while gathering data", e);
                                installedApps.append("SystemApp;")
                                        .append(pm.getApplicationLabel(app)).append(";")
                                        .append("unknown").append(";")
                                        .append(app.targetSdkVersion).append(";")
                                        .append(app.packageName).append(";")
                                        .append(app.processName).append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";\n");
                            }
                            eMailBody.append(installedApps);
                            fw.append(installedApps);
                        } else {
                            installedApps.setLength(0);
                            try{
                                String md5 = calculateMD5(app.sourceDir);
                                installedApps.append("UserApp;")
                                        .append(pm.getApplicationLabel(app)).append(";")
                                        .append(md5).append(";")
                                        .append(app.targetSdkVersion).append(";")
                                        .append(app.packageName).append(";")
                                        .append(app.processName).append(";")
                                        .append(app.sourceDir).append(";")
                                        .append(pm.getPackageInfo(app.packageName, 0).versionCode).append(";")
                                        .append(pm.getPackageInfo(app.packageName, 0).versionName).append(";")
                                        .append(((X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures[0].toByteArray()))).getSubjectDN()).append(";")
                                        .append(((X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures[0].toByteArray()))).getSerialNumber()).append(";")
                                        .append(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.GERMANY).format(new Date((pm.getPackageInfo(app.packageName, 0).firstInstallTime)))).append(";")
                                        .append(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.GERMANY).format(new Date((pm.getPackageInfo(app.packageName, 0).lastUpdateTime)))).append("\n");
                            }catch(Exception e){
                                Log.e("UserApps", "Error while gathering data", e);
                                installedApps.append("UserApp;")
                                        .append(pm.getApplicationLabel(app)).append(";")
                                        .append("unknown").append(";")
                                        .append(app.targetSdkVersion).append(";")
                                        .append(app.packageName).append(";")
                                        .append(app.processName).append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";")
                                        .append("unknown").append(";\n");
                            }
                            eMailBody.append(installedApps);
                            fw.append(installedApps);
                        }
                    }
                    eMailBody.append("--------------------------------------------------------------\n\n\n\n\n");
                    fw.flush();
                    fw.close();
                }catch (IOException e){
                    Log.e("InstalledApps", "Error while gathering data", e);
                }
                String filenameRunningApps = folder + "/RunningApps_" + String.valueOf(Calendar.getInstance().getTimeInMillis()) + ".csv";
                eMailBody.append("\n\n\n\nList of running Applications:\n--------------------------------------------------------------\n");
                try{
                    FileWriter fw2 = new FileWriter(filenameRunningApps);
                    ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
                    StringBuilder runningApps = new StringBuilder();
                    runningApps.append("Process_Name;Importance;PID;UID;\n");
                    List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
                    for(ActivityManager.RunningAppProcessInfo procInfo : procInfos) {
                        runningApps.setLength(0);
                        runningApps.append(procInfo.processName).append(";")
                                .append(procInfo.importance).append(";")
                                .append(procInfo.pid).append(";")
                                .append(procInfo.uid)
                                .append("\n");
                        fw2.append(runningApps);
                        eMailBody.append(runningApps);
                    }
                    fw2.flush();
                    fw2.close();
                }catch (IOException e){
                    Log.e("RunningApps", "Error while gathering data", e);
                }
                sendEmailMessage(eMailBody.toString());
            } catch (Exception e) {
                Log.e("SDcard", "No external storage available", e);
            }
        } else {
            Log.e("SDcard", "No external storage available");
        }
    }

    public class LoadData extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute()
        {
            progressDialog = ProgressDialog.show(Main.this, "AppExtract","gathering data and calculating md5 hashes for installed apps...", true);
        }
        @Override
        protected Void doInBackground(Void... params)
        {
            sendMessage();
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            progressDialog.dismiss();
        }
    }

    public void sendMessageButton(View view){
        LoadData task = new LoadData();
        task.execute();
    }
}
