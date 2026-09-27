package com.haxbey.virtual;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
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

        // "+" butonuna basıldığında yüklü uygulamaları listele
        btnAddApp.setOnClickListener(v -> showInstalledAppsDialog());

        // Listeden tıklayınca sanal kopyanın klasör içeriğini ve boyutunu göster
        listViewApps.setOnItemClickListener((parent, view, position, id) -> {
            String appName = clonedAppsList.get(position);
            File virtualAppDir = new File(getExternalFilesDir(null), "virtual_apps/" + appName);
            Toast.makeText(this, "Sanal Dizin: " + virtualAppDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadClonedApps() {
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
            appNames.add(pm.getApplicationLabel(packageInfo).toString());
            packageNames.add(packageInfo.packageName);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sanal Alana Eklenecek Uygulamayı Seç");

        builder.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, appNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setText(appNames.get(position));
                return view;
            }
        }, (dialog, which) -> {
            String selectedPackage = packageNames.get(which);
            String selectedName = appNames.get(which);
            copyVirtualEnvironmentStructure(selectedPackage, selectedName);
        });

        builder.show();
    }

    // İstediğin Mantık: Tam bir sanal ortam klasör yapısı oluşturup dosyaları oraya kopyalamak
    private void copyVirtualEnvironmentStructure(String packageName, String appName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            File sourceFile = new File(appInfo.sourceDir); // APK kaynağı

            // Kök Sanal Dizin: android/data/com.haxbey.virtual/files/virtual_apps/UygulamaAdi/
            File virtualAppRoot = new File(getExternalFilesDir(null), "virtual_apps/" + appName);
            
            // İstediğin alt dizin yapıları:
            // 1. App binary (apk)
            File apkDir = new File(virtualAppRoot, "apk");
            // 2. Android/data simülasyonu
            File androidDataDir = new File(virtualAppRoot, "Android/data/" + packageName);
            // 3. Android/obb simülasyonu
            File androidObbDir = new File(virtualAppRoot, "Android/obb/" + packageName);

            if (!apkDir.exists()) apkDir.mkdirs();
            if (!androidDataDir.exists()) androidDataDir.mkdirs();
            if (!androidObbDir.exists()) androidObbDir.mkdirs();

            // 1. base.apk dosyasını kopyala
            File destFile = new File(apkDir, "base.apk");
            copyFile(sourceFile, destFile);

            // 2. Eğer telefonun harici hafazasında bu uygulamanın data klasörü varsa oradakileri de kopyala
            File externalData = new File(Environment.getExternalStorageDirectory(), "Android/data/" + packageName);
            if (externalData.exists() && externalData.isDirectory()) {
                copyDirectory(externalData, androidDataDir);
            }

            // 3. Eğer obb dosyaları varsa onları da kopyala
            File externalObb = new File(Environment.getExternalStorageDirectory(), "Android/obb/" + packageName);
            if (externalObb.exists() && externalObb.isDirectory()) {
                copyDirectory(externalObb, androidObbDir);
            }

            Toast.makeText(this, appName + " tüm sanal yapısıyla kopyalandı!", Toast.LENGTH_SHORT).show();
            loadClonedApps();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Kopyalama Hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Dosya kopyalama yardımcısı
    private void copyFile(File source, File dest) {
        try {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Klasör kopyalama yardımcısı (obb ve data dosyaları için)
    private void copyDirectory(File sourceDir, File destDir) {
        try {
            File[] files = sourceDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    File destFile = new File(destDir, file.getName());
                    if (file.isDirectory()) {
                        destFile.mkdirs();
                        copyDirectory(file, destFile);
                    } else {
                        copyFile(file, destFile);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
