package com.jaycee88.socketdemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TCPClientActivity";
    public static final int MSG_RECEIVED = 0x10;
    public static final int MSG_READY = 0x11;

    private EditText editText;
    private TextView textView;
    private Button sendBtn;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;
    private StringBuilder stringBuilder;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_READY:
                    sendBtn.setEnabled(true);
                    break;
                case MSG_RECEIVED:
                    stringBuilder.append(msg.obj).append("\n");
                    textView.setText(stringBuilder.toString());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.displayTextView);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(this);
        sendBtn.setEnabled(false);

        stringBuilder = new StringBuilder();

        startService();

        new Thread() {
            @Override
            public void run() {
                connectTcpServer();
            }
        }.start();
    }

    private void startService() {
        Intent intent = new Intent(MainActivity.this, TCPServerService.class);
        startService(intent);
    }

    private String formatDateTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    private void connectTcpServer() {
        Socket socket = null;
        while (socket == null) {
            try {
                socket = new Socket("localhost", 8888);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                mHandler.sendEmptyMessage(MSG_READY);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // receive message
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!isFinishing()) {
            try {
                String msg = bufferedReader.readLine();
                if (msg != null) {
                    String time = formatDateTime(System.currentTimeMillis());
                    String showedMsg = "server " + time + ":" + msg + "\n";
                    mHandler.obtainMessage(MSG_RECEIVED, showedMsg).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }
    }

    private void sendToServer(final String msg) {
        new Thread() {
            @Override
            public void run() {
                mPrintWriter.println(msg);
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendBtn:
                if (mPrintWriter != null) {
                    String msg = editText.getText().toString();
                    sendToServer(msg);
                    editText.setText("");
                    String time = formatDateTime(System.currentTimeMillis());
                    String showedMsg = "self " + time + ":" + msg + "\n";
                    stringBuilder.append(showedMsg);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
