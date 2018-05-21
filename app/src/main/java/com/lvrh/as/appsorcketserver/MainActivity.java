package com.lvrh.as.appsorcketserver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ServerThread";
    EditText txtMsg;
    private Button btn_sms;
    private Button btn_phone;
    public static String SMS_SEND_ACTIOIN = "SMS_SEND_ACTIOIN";
    public static String SMS_DELIVERED_ACTION = "SMS_DELIVERED_ACTION";

    ServerThread serverThread;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), msg.getData().getString("MSG", "Toast"), Toast.LENGTH_SHORT).show();
            txtMsg.setText(txtMsg.getText()+"\n"+ msg.getData().getString("MSG", "Toast"));
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS} , 0);
        txtMsg=(EditText)findViewById(R.id.msg);
        btn_sms=(Button)findViewById(R.id.sms);btn_phone=(Button)findViewById(R.id.getPhone);
        btn_sms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msgBody="调度命令 \n2018年03月17日02时30分  第13361号\n受令处所:\n柳州,拉堡线路所,进德I场\n" +
                        "抄送:进德Ⅰ场(抄送:施工负责人)\n"+
                        "调度员姓名:胡凯波\n"+
                        "内容:\n自接令时起,准许柳州站至拉堡线路所(全所,含进行德联络线上行0km000m至1km929m,进" +
                        "德联络线下行0km000m至0km175m)至进德一场间上下线进行120分钟维修作业。\n"+
                        "受令车站:进德I场\n"+"车站值班员:张庚清";
                //SendMessage("14986939016",msgBody);
                String phone="14986939016";
                sendMessageByInterface2(phone,msgBody);
            }
        });
        btn_phone.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v){
                txtMsg.setText(getPhoneNumber());
            }
        });

        serverThread = new ServerThread();
        serverThread.start();
    }
    private String getPhoneNumber(){
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager)  getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serverThread.setIsLoop(false);
    }
    //发长短信
    public void sendMessageByInterface2( String phoneNumber,String message) {
         /* 建立SmsManager对象 */
        SmsManager sms = SmsManager.getDefault();

        Intent sentIntent = new Intent(SMS_SEND_ACTIOIN);
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, 0);

        Intent deliverIntent = new Intent(SMS_DELIVERED_ACTION);
        PendingIntent deliverPI = PendingIntent.getBroadcast(this, 0, deliverIntent, 0);

        if (message.length() > 70) {
            ArrayList<String> msgs = sms.divideMessage(message);//拆分
            ArrayList<PendingIntent> sentIntents =  new ArrayList<PendingIntent>();

            for(int i = 0;i<msgs.size();i++){
                sentIntents.add(sentPI);
            }
            sms.sendMultipartTextMessage(phoneNumber, null, msgs, sentIntents, null);//发送
        } else {
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliverPI);
        }


    }
    public void SendMessage(String strDestAddress,String strMessage){
    /* 建立SmsManager对象 */
        SmsManager smsManager = SmsManager.getDefault();
        try {
     /* 建立自定义Action常数的Intent(给PendingIntent参数之用) */
            Intent itSend = new Intent(SMS_SEND_ACTIOIN);
            Intent itDeliver = new Intent(SMS_DELIVERED_ACTION);

     /* sentIntent参数为传送后接受的广播信息PendingIntent */
            PendingIntent mSendPI = PendingIntent.getBroadcast(this, 0, itSend, 0);

     /* deliveryIntent参数为送达后接受的广播信息PendingIntent */
            PendingIntent mDeliverPI = PendingIntent.getBroadcast(this, 0, itDeliver, 0);
            List<String> divideContents = smsManager.divideMessage(strMessage);
            for (String text:divideContents) {
         /* 发送SMS短信，注意倒数的两个PendingIntent参数 */
                smsManager.sendTextMessage(strDestAddress, null, text, mSendPI, mDeliverPI);
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    class ServerThread extends Thread {
        boolean isLoop = true;
        public void setIsLoop(boolean isLoop) {
            this.isLoop = isLoop;
        }
        @Override
        public void run() {
            Log.d(TAG, "running");
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(9000);
                while (isLoop) {
                    Socket socket = serverSocket.accept();
                    Log.d(TAG, "accept");
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    byte[] bt=new byte[1024];
                    int n=inputStream.available();

                    inputStream.read(bt,1,n-1);
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                    String msg =new String(bt,"UTF-8");// df.format(new Date());// inputStream.readUTF();
                    String msg2=inputStream.readUTF();
                    Message message = Message.obtain();
                    Bundle bundle = new Bundle();
                    bundle.putString("MSG", msg);
                    message.setData(bundle);
                    handler.sendMessage(message);
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.d(TAG, "destory");
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
