package com.haxbey.virtual;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import androidx.core.content.FileProvider;

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

        // Listeden tıklayınca yönetim menüsü açılır
        listViewApps.setOnItemClickListener((parent, view, position, id) -> {
            String appName = clonedAppsList.get(position);
            
            // Doğrudan istediğin ana dizin yolu
            File baseVirtualDir = new File(Environment.getExternalStorageDirectory(), "Android/data/com.haxbey.virtual");
            File virtualAppRoot = new File(baseVirtualDir, "virtual_apps/" + appName);
            File apkFile = new File(virtualAppRoot, "apk/base.apk");
            
            showVirtualAppOptions(virtualAppRoot, apkFile, appName);
        });
    }

    private void loadClonedApps() {
        // Doğrudan ana Android/data/com.haxbey.virtual/virtual_apps dizinini okuyoruz
        File baseVirtualDir = new File(Environment.getExternalStorageDirectory(), "Android/data/com.haxbey.virtual");
        File virtualDir = new File(baseVirtualDir, "virtual_apps");
        
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

    private void copyVirtualEnvironmentStructure(String packageName, String appName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            File sourceFile = new File(appInfo.sourceDir);

            // İstediğin Tam Kök Dizin: /storage/emulated/0/Android/data/com.haxbey.virtual/
            File baseVirtualDir = new File(Environment.getExternalStorageDirectory(), "Android/data/com.haxbey.virtual");
            File virtualAppRoot = new File(baseVirtualDir, "virtual_apps/" + appName);
            
            // İstediğin yol şeması:
            // /Android/data/com.haxbey.virtual/Android/data/paket.adi
            File apkDir = new File(virtualAppRoot, "apk");
            File androidDataDir = new File(baseVirtualDir, "Android/data/" + packageName);
            File androidObbDir = new File(baseVirtualDir, "Android/obb/" + packageName);

            if (!apkDir.exists()) apkDir.mkdirs();
            if (!androidDataDir.exists()) androidDataDir.mkdirs();
            if (!androidObbDir.exists()) androidObbDir.mkdirs();

            // 1. base.apk kopyala
            File destFile = new File(apkDir, "base.apk");
            copyFile(sourceFile, destFile);

            // 2. Harici data klasörünü kopyala
            File externalData = new File(Environment.getExternalStorageDirectory(), "Android/data/" + packageName);
            if (externalData.exists() && externalData.isDirectory()) {
                copyDirectory(externalData, androidDataDir);
            }

            // 3. Harici obb klasörünü kopyala
            File externalObb = new File(Environment.getExternalStorageDirectory(), "Android/obb/" + packageName);
            if (externalObb.exists() && externalObb.isDirectory()) {
                copyDirectory(externalObb, androidObbDir);
            }

            Toast.makeText(this, appName + " tam şema ile klonlandı!", Toast.LENGTH_SHORT).show();
            loadClonedApps();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Kopyalama Hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showVirtualAppOptions(File rootDir, File apkFile, String appName) {
        CharSequence[] options = {"Sanal APK'yı Çalıştır / Yükle", "Klasör Yolunu Göster"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(appName + " Sanal Yönetimi");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (apkFile.exists()) {
                    runClonedApk(apkFile);
                } else {
                    Toast.makeText(this, "base.apk bulunamadı!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }

    private void runClonedApk(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Çalıştırma Hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

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
