package com.sdcard.createhook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_PATH = "/storage/emulated/0/Download/sdcard_create_hook.log";
    private TextView tvLog;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnOpenLog = findViewById(R.id.btnOpenLog);

        tvStatus.setText("模块已安装。请在 LSPosed 管理器中启用本模块，\n勾选作用域（建议勾选「系统框架」+ 需要监视的 App），然后重启手机。");

        btnRefresh.setOnClickListener(v -> loadLog());
        btnClear.setOnClickListener(v -> clearLog());
        btnOpenLog.setOnClickListener(v -> openLogWithOtherApp());

        loadLog();
    }

    private void loadLog() {
        File logFile = new File(LOG_PATH);
        if (!logFile.exists()) {
            tvLog.setText("日志文件尚不存在。\n请确认：\n1. 已在 LSPosed 中启用模块\n2. 已勾选作用域并重启\n3. 有 App 在根目录创建了文件/文件夹");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String line;
            // 只显示最后约 200 行，避免卡顿
            java.util.LinkedList<String> lines = new java.util.LinkedList<>();
            while ((line = br.readLine()) != null) {
                lines.add(line);
                if (lines.size() > 300) lines.removeFirst();
            }
            br.close();

            for (String l : lines) {
                sb.append(l).append("\n");
            }
            tvLog.setText(sb.length() > 0 ? sb.toString() : "日志为空");
        } catch (Exception e) {
            tvLog.setText("读取日志失败：\n" + e.getMessage());
        }
    }

    private void clearLog() {
        try {
            File logFile = new File(LOG_PATH);
            if (logFile.exists()) {
                new FileWriter(logFile, false).close(); // 清空
                Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
                loadLog();
            } else {
                Toast.makeText(this, "日志文件不存在", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "清空失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openLogWithOtherApp() {
        File logFile = new File(LOG_PATH);
        if (!logFile.exists()) {
            Toast.makeText(this, "日志文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(logFile);
            intent.setDataAndType(uri, "text/plain");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "打开日志"));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开：" + e.getMessage() + "\n请用文件管理器打开 Download 目录", Toast.LENGTH_LONG).show();
        }
    }
}
