package com.myubuntu.example.glenn.holepunchingclient;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class HolePunchMainActivity extends Activity {

    private String userId;
    private DatagramSocket socket;
    private final HashMap<String, SocketInfo> clients = new HashMap<String, SocketInfo>();
    private TextView textView2;
    private TextView editText;
    private GetPunch punchStart;

    public void setSocket() {

        try {
            //if (socket == null) {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                //socket.setBroadcast(true);
                socket.bind(new InetSocketAddress(38888));

        } catch (SocketException e) {
            Log.e("SETSOCKET","ERROR TO NEW SOCKET");
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e("onStart:","onstart");
    }

    @Override
    public void onResume() {
        super.onResume();
        setSocket();
        Log.e("onResume:","onresume");
    }

    @Override
    public void onStop() {
        Log.e("onStop:","onstop");
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("onPause:","onpause");
        socket.close();

        if (punchStart.getStatus() == AsyncTask.Status.RUNNING)
        {
            punchStart.cancel(true);
            Log.e("onPause:","AYSNCTASK CANCEL");
        }
    }

    private class SocketInfo {

        private InetAddress IP;
        private int PORT;
        private int PUNCHED; // 2: receive 2nd reply , 0: initial, 1: receive first reply

        SocketInfo(InetAddress IP, int PORT, int PUNCHED) {
            this.IP = IP;
            this.PORT = PORT;
            this.PUNCHED = PUNCHED;
        }

        public InetAddress getIP() { return IP;}
        public int getPORT() { return PORT; }
        public int getPUNCHED() { return PUNCHED; }

        public void setPUNCHED(int PUNCHED) {
            this.PUNCHED = PUNCHED;
        }
    }

    private class Hearbeat extends AsyncTask<Void, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            socket.close();
            setSocket();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            textView2.setText(values[0]);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = "";
            try {

                byte infobuffer[];

                InetAddress address = InetAddress.getByName("zzozeap2.iptime.org");

                userId = editText.getText().toString();
                infobuffer = userId.getBytes();

                DatagramPacket packet = new DatagramPacket(infobuffer, infobuffer.length, address, 7777);


                socket.send(packet);

                receiverRoutine();
                //while (!socket.isClosed()) {
                //result = receiverRoutine();
                //publishProgress(result);
                //}

                /*
                Log.e("getAddress:", address.getAddress().toString());
                Log.e("getHostAddress:",new String(address.getHostAddress()));
                Log.e("getHostAddressName:",new String(address.getHostName()));
                Log.e("getCanonicalHAddrName:",new String(address.getCanonicalHostName()));

                Log.e("PgetAddress:", packet.getAddress().getAddress().toString());
                Log.e("PgetHostAddress:",new String(packet.getAddress().getHostAddress()));
                Log.e("PgetHostAddressName:",new String(packet.getAddress().getHostName()));
                Log.e("PgetCanonicalHAddrName:",new String(packet.getAddress().getCanonicalHostName()));

                Log.e("PgetSocketAddress:", packet.getSocketAddress().toString() );
                */

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e("SENDER_SERVER","SocketException OCCURED..");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("SENDER_SERVER","END!"+result);
        }
    }

    private class GetPunch extends AsyncTask< Void, String, String > {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            socket.close();
            setSocket();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            textView2.setText(values[0]);
            Log.e("SENDER_GAME_ONPROGRESS",values[0]);
        }

        @Override
        protected String doInBackground(Void... voids) {

            String result="";
            boolean getHolePunch = true;
            while (getHolePunch) {

                try {
                    Log.e("SENDER_GAME","SEND START");

                    byte sendbuffer[];
                    String game;

                    Iterator it = clients.keySet().iterator();
                    while(it.hasNext()) {

                        String partnerId = (String)(it.next());
                        SocketInfo toRecvs = clients.get(partnerId);
                        int punched = toRecvs.getPUNCHED();
                        game = userId + "##";

                        if( punched == 0) {
                            sendbuffer = game.getBytes();
                            socket.send(new DatagramPacket(sendbuffer, sendbuffer.length, toRecvs.getIP(), toRecvs.getPORT()));
                        } else if( punched == 1) { //reply
                            game += toRecvs + "##";
                            sendbuffer = game.getBytes();
                            socket.send(new DatagramPacket(sendbuffer, sendbuffer.length, toRecvs.getIP(), toRecvs.getPORT()));
                        }

                        Log.e("SENDER_GAME", "send_to_CLIENT:" + toRecvs);
                    }
                    if(clients.size() > 0) {
                        socket.setSoTimeout(3000);
                        receiverRoutine();
                        //publishProgress(result);
                    }


                    // check all punched connect
                    getHolePunch = false;
                    it = clients.keySet().iterator();
                    while(it.hasNext())
                    {
                        String partner = (String)(it.next());
                        result += partner + " # ";
                        if ( clients.get(partner).getPUNCHED() != 2 ) {
                            getHolePunch = true;
                            result = "";
                            break;
                        }
                    }

                    socket.setSoTimeout(0);
                    Log.e("SENDER_GAME","punch Success:" + getHolePunch);

                } catch (SocketTimeoutException e) {
                    //getHolePunch = true;
                    Log.e("SENDER_GAME","SocketTimeout..");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    Log.e("SENDER_GAME", "SocketException");
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return result + "\n";
        }
        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);
            Log.e("SENDER_GAME","END! + RESULT: " + result );

            textView2.setText(result);
        }
    }

    private void receiverRoutine() throws SocketTimeoutException, SocketException {

        byte fromserverbuffer[] = new byte[512];
        DatagramPacket packet = new DatagramPacket(fromserverbuffer, fromserverbuffer.length);

           try {

                   socket.receive(packet);

                if(!socket.isClosed()) {
                    String receiveStr = new String(packet.getData(), 0 ,packet.getLength()); // here unicode � ? ---> .trim()

                    Log.e("RECEIVER", "receiveStr:"+receiveStr);
                    Log.e("RECEIVER", "receiveStr_len:"+receiveStr.length());

                    String[] tokens = receiveStr.split("##");
                    Log.e("RECEIVER", "tokenLen"+Integer.toString(tokens.length));

                    if (tokens.length > 0) {
                        String fromWhom = tokens[0];

                        if (fromWhom.equals("SERVER")) {

                            for (int i = 1; i < tokens.length; i++) {
                                String[] socketinfo = tokens[i].split("#");

                                Log.e("RECEIVER", "Client ID: " + socketinfo[0]);
                                if (socketinfo.length == 3 && !(socketinfo[0].equals(userId))) {
                                    clients.put(socketinfo[0], new SocketInfo(InetAddress.getByName(socketinfo[1]), Integer.parseInt(socketinfo[2]),0));
                                    Log.e("RECEIVER", "ADDed Client ID: " + socketinfo[0]);
                                }
                            }
                            Log.e("RECEIVER", "ADDED Client size: " + clients.size());
                        } else {
                            Log.e("RECEIVER", "GAMESENDER" + receiveStr);

                            if( tokens.length == 2 ) { // receive 2nd
                                clients.get(fromWhom).setPUNCHED(2);
                                Log.e("RECEIVER", "GAMESENDER get 2nd from " + fromWhom );
                            } else if( tokens.length == 1) { // receive 1st
                                clients.get(fromWhom).setPUNCHED(1);
                                Log.e("RECEIVER", "GAMESENDER get 1st from " + fromWhom );
                            }
                        }
                    }
                }

            } catch (SocketTimeoutException e) {
               Log.e("RECEIVER","SocketTimeout..");
               throw e;
            } catch (SocketException e){
               Log.e("RECEIVER","SocketException OCCURED...");
               e.printStackTrace();
               throw e;
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        return;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hole_punch_main);

        Collections.synchronizedMap(clients);

        editText = (TextView) findViewById(R.id.editText); // 'final' to be accessed from inner class
        textView2 = (TextView) findViewById(R.id.textView2);

        Button buttonHosts = (Button) findViewById(R.id.button);
        Button buttonServer = (Button) findViewById(R.id.button2);

        buttonServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Hearbeat().execute();
            }
        });

        buttonHosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                punchStart = new GetPunch();
                punchStart.execute();
            }
        });
    }
}


