package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    String SERVER_IP = "192.168.1.33";
    int SERVER_PORT = 80;

    Socket socket;
    OutputStream outputStream;
    InputStream inputStream;

    Thread workerThread;
    volatile boolean stopWorker;

    EditText EditTextttt;
    TextView nowww, volttt, showww;
    ToggleButton connn;

    int tt = 0, vv = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button senddd = findViewById(R.id.sendd);
        connn    = findViewById(R.id.conn);
        showww   = findViewById(R.id.showww);
        nowww    = findViewById(R.id.noww);
        volttt   = findViewById(R.id.voltt);
        EditTextttt = findViewById(R.id.EditTextttt);

        senddd.setOnClickListener(v -> {

            if (socket == null || !socket.isConnected() || outputStream == null) {
                showww.setText("Not Connected ❌");
                return;
            }

            String msg = EditTextttt.getText().toString().trim();

            if (msg.isEmpty()) {
                showww.setText("Enter a value first ⚠️");
                return;
            }

            new Thread(() -> {
                try {
                    byte[] data = (msg + "\n").getBytes("UTF-8");
                    outputStream.write(data);
                    outputStream.flush();

                    runOnUiThread(() -> showww.setText("Sent: " + msg + " ✅"));

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> showww.setText("Send Error: " + e.getMessage() + " ⚠️"));
                }
            }).start();

        });
    }

    public void onToggleClicked_1(View v) {
        if (connn.isChecked()) {
            new Thread(() -> {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 3000);
                    outputStream = socket.getOutputStream();
                    inputStream  = socket.getInputStream();

                    runOnUiThread(() -> {
                        connn.setBackgroundResource(R.drawable.wifion);
                        showww.setText("Connected ✅");
                    });

                    beginListenForData();

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        connn.setChecked(false);
                        connn.setBackgroundResource(R.drawable.wifioff);
                        showww.setText("Connection Failed: " + e.getMessage());
                    });
                }
            }).start();

        } else {
            new Thread(() -> {
                try {
                    closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> {
                    showww.setText("Disconnected");
                    connn.setBackgroundResource(R.drawable.wifioff);
                });
            }).start();
        }
    }

    void beginListenForData() {
        Handler handler = new Handler(Looper.getMainLooper());
        stopWorker = false;

        workerThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));

                while (!stopWorker && socket != null && socket.isConnected()) {
                    String data = reader.readLine();
                    if (data == null) break;

                    String finalData = data.trim();

                    handler.post(() -> {
                        if (finalData.equals("V")) {
                            vv = 1;
                        } else if (finalData.equals("T")) {
                            tt = 1;
                        } else if (vv == 1) {
                            vv = 0;
                            volttt.setText(finalData);
                        } else if (tt == 1) {
                            tt = 0;
                            nowww.setText(finalData);
                        } else if (finalData.equals("a")) {
                            showww.setText("Water leak 🚨");
                        } else if (finalData.equals("c")) {
                            showww.setText("Low water level ⚠️");
                        } else if (finalData.equals("d")) {
                            showww.setText("---------------------");
                        }
                    });
                }

            } catch (IOException e) {
                runOnUiThread(() -> {
                    showww.setText("Disconnected ⚠️");
                    connn.setChecked(false);
                    connn.setBackgroundResource(R.drawable.wifioff);
                });
            }
        });

        workerThread.start();
    }

    void closeConnection() throws IOException {
        stopWorker = true;

        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
        try { if (inputStream  != null) inputStream.close();  } catch (IOException ignored) {}
        try { if (socket       != null) socket.close();       } catch (IOException ignored) {}

        outputStream = null;
        inputStream  = null;
        socket       = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(() -> {
            try { closeConnection(); } catch (IOException ignored) {}
        }).start();
    }
}
