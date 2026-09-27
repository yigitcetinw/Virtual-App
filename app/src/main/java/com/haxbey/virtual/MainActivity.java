package com.haxbey.virtual;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnAddApp;
    private ListView listViewApps;
    private List<String> clonedAppsList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAddApp = findViewById(R.id.btnAddApp);
        listViewApps = findViewById(R.id.listViewApps);

        loadClonedApps();

        // "+" butonuna basıldığında telefonlardaki uygulamaları listeleyen pencere açılacak
        btnAddApp.setOnClickListener(v -> showInstalledAppsDialog());
    }

    private void loadClonedApps() {
        // android/data/com.haxbey.virtual/files/virtual_apps dizinini kontrol et
        File virtualDir = new File(getExternalFilesDir(null), "virtual_apps");
        if (!virtualDir.exists()) {
            virtualDir.mkdirs();
        }

        clonedAppsList.clear();
        File[] files = virtualDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clonedAppsList.add(file.getName());
                }
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, clonedAppsList);
        listViewApps.setAdapter(adapter);
    }

    private void showInstalledAppsDialog() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        List<String> appNames = new ArrayList<>();
        List<String> packageNames = new ArrayList<>();

        for (ApplicationInfo packageInfo : packages) {
            // Sadece üçüncü parti uygulamaları filtrelemek isteyebilirsin ama şimdilik hepsi gelsin
            appNames.add(pm.getApplicationLabel(packageInfo).toString());
            packageNames.add(packageInfo.packageName);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Uygulama Seç");

        View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        
        builder.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, appNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setText(appNames.get(position));
                return view;
            }
        }, (dialog, which) -> {
            String selectedPackage = packageNames.get(which);
            String selectedName = appNames.get(which);
            copyAppToVirtual(selectedPackage, selectedName);
        });

        builder.show();
    }

    // İstediğin Mantık: Seçilen uygulamanın tüm dosyalarını android/data/com.haxbey.virtual/... içine kopyalama
    private void copyAppToVirtual(String packageName, String appName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            File sourceFile = new File(appInfo.sourceDir); // base.apk yolu

            // Hedef Dizin: android/data/com.haxbey.virtual/files/virtual_apps/UygulamaAdi
            File targetDir = new File(getExternalFilesDir(null), "virtual_apps/" + appName);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            File destFile = new File(targetDir, "base.apk");

            // Kopyalama İşlemi
            InputStream in = new FileInputStream(sourceFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();

            Toast.makeText(this, appName + " Sanal alana kopyalandı!", Toast.LENGTH_SHORT).show();
            loadClonedApps();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
