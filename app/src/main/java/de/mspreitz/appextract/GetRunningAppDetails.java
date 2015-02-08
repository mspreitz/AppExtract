package de.mspreitz.appextract;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class GetRunningAppDetails extends ArrayAdapter<ActivityManager.RunningAppProcessInfo> {
    private List<ActivityManager.RunningAppProcessInfo> appsList = null;
    private Context context;
    private PackageManager packageManager;

    public GetRunningAppDetails(Context context, int textViewResourceId, List<ActivityManager.RunningAppProcessInfo> appsList) {
        super(context, textViewResourceId, appsList);
        this.context = context;
        this.appsList = appsList;
        packageManager = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return ((null != appsList) ? appsList.size() : 0);
    }

    @Override
    public ActivityManager.RunningAppProcessInfo getItem(int position) {
        return ((null != appsList) ? appsList.get(position) : null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.list2, null);
        }

        ActivityManager.RunningAppProcessInfo data = appsList.get(position);
        if (null != data) {
            TextView processName = (TextView) view.findViewById(R.id.process_name);
            processName.setText(data.processName);
            TextView appName = (TextView) view.findViewById(R.id.app_name);
            ImageView iconView = (ImageView) view.findViewById(R.id.process_icon);
            try{
                appName.setText(packageManager.getPackageInfo(data.processName, 0).applicationInfo.loadLabel(packageManager));
                iconView.setImageDrawable(packageManager.getPackageInfo(data.processName, 0).applicationInfo.loadIcon(packageManager));
            }catch(Exception e){
                appName.setText(data.processName);
            }
        }
        return view;
    }
}