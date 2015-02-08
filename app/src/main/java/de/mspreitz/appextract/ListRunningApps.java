package de.mspreitz.appextract;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ListRunningApps extends ListFragment {
    private GetRunningAppDetails listadaptor = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        new LoadApplications().execute();
        return inflater.inflate(R.layout.running, container, false);
    }

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<ActivityManager.RunningAppProcessInfo> applist = new ArrayList<ActivityManager.RunningAppProcessInfo>();
            ActivityManager actvityManager = (ActivityManager) ListRunningApps.this.getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> procInfos = actvityManager.getRunningAppProcesses();
            for(ActivityManager.RunningAppProcessInfo procInfo : procInfos) {
                applist.add(procInfo);
            }
            listadaptor = new GetRunningAppDetails(ListRunningApps.this.getActivity(), R.layout.list, applist);
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(listadaptor);
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ListRunningApps.this.getActivity(), null, "Loading applications...");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }
}