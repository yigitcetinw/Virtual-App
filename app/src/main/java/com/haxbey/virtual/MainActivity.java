package com.haxbey.virtual;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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

        // "+" butonuna basıldığında telefonlardaki uygulamaları listele
        btnAddApp.setOnClickListener(v -> showInstalledAppsDialog());

        // Listeden bir klonlanmış uygulamaya tıklandığında
        listViewApps.setOnItemClickListener((parent, view, position, id) -> {
            String appName = clonedAppsList.get(position);
            File apkFile = new File(getExternalFilesDir(null), "virtual_apps/" + appName + "/base.apk");
            
            if (apkFile.exists()) {
                showAppOptionsDialog(apkFile, appName);
            } else {
                Toast.makeText(this, "Hata: APK dosyası bulunamadı!", Toast.LENGTH_SHORT).show();
            }
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
            // Sadece sistem dışı kullanıcı uygulamalarını filtrelemek isteyebilirsin
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
            copyAppToVirtual(selectedPackage, selectedName);
        });

        builder.show();
    }

    private void copyAppToVirtual(String packageName, String appName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            File sourceFile = new File(appInfo.sourceDir);

            // İstediğin Dizin: android/data/com.haxbey.virtual/files/virtual_apps/UygulamaAdi/base.apk
            File targetDir = new File(getExternalFilesDir(null), "virtual_apps/" + appName);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            File destFile = new File(targetDir, "base.apk");

            InputStream in = new FileInputStream(sourceFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();

            Toast.makeText(this, appName + " başarıyla sanal alana kopyalandı!", Toast.LENGTH_SHORT).show();
            loadClonedApps();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Kopyalama Hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Tıklayınca seçenek sunan menü
    private void showAppOptionsDialog(File apkFile, String appName) {
        CharSequence[] options = {"Uygulamayı Başlat (Install/Open)", "Dosya Konumunu Göster"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(appName + " Seçenekleri");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                installAndOpenClonedApp(apkFile);
            } else {
                Toast.makeText(this, "Konum: " + apkFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }
        // Kopyalanan APK'yı güvenli geçici dizine alıp yüöneticisine (PackageInstaller) gönderen gelişmiş metod
    private void installAndOpenClonedApp(File apkFile) {
        try {
            // Android veri klasöründeki dosya doğrudan okunamadığı için, öncelikle 
            // uygulamanın cache (önbellek) klasörüne kopyalıyoruz
            File cacheFile = new File(getCacheDir(), "temp_cloned.apk");
            
            InputStream in = new FileInputStream(apkFile);
            OutputStream out = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();

            // Şimdi yükleme yöneticisini tetikle
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", cacheFile);
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            startActivity(intent);
            Toast.makeText(this, "Yükleme ekranı açılıyor...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Çalıştırma Hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
