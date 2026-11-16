package pl.omega.swarm;

import android.app.Activity;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.*;
import java.io.*;

public final class MainActivity extends Activity {
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private HardwareSigner signer;
 private EditText endpoint,node,voltage,frequency,pool;
 private TextView status,summary;
 private volatile boolean connected=false,busy=false;
 private static final int GOLD=Color.rgb(212,175,55),BG=Color.rgb(7,8,10);
 private final Runnable poll=new Runnable(){public void run(){if(!isFinishing()){if(connected)fetch();handler.postDelayed(this,3000);}}};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);signer=new HardwareSigner(this);
  ScrollView scroll=new ScrollView(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);
  panel.setPadding(28,24,28,32);panel.setBackgroundColor(BG);scroll.addView(panel);setContentView(scroll);
  label(panel,"OMEGA / SOVEREIGN SWARM",22,GOLD,true);
  status=label(panel,"TRYB DEMO · BRAK POTWIERDZONEJ BRAMY",14,GOLD,true);
  endpoint=field(panel,"Brama HTTPS",getPreferences(0).getString("endpoint","https://192.168.1.10:8443"));
  node=field(panel,"Identyfikator koparki",getPreferences(0).getString("node","asic-01"));
  Button connect=button(panel,"POŁĄCZ Z BRAMĄ",GOLD);
  connect.setOnClickListener(v->{connected=false;getPreferences(0).edit().putString("endpoint",endpoint.getText().toString().trim()).putString("node",node.getText().toString().trim()).apply();fetch();});
  summary=label(panel,"DEMO · telemetry data unavailable",17,Color.WHITE,true);
  voltage=field(panel,"Napięcie profilu [mV]","335");
  frequency=field(panel,"Częstotliwość profilu [MHz]","560");
  pool=field(panel,"Identyfikator puli","sv2-pool-primary");
  button(panel,"AWARYJNY STOP",Color.rgb(180,55,62)).setOnClickListener(v->authenticate("STOP"));
  button(panel,"ZMIANA PULI",GOLD).setOnClickListener(v->authenticate("POOL"));
  button(panel,"ZASTOSUJ PROFIL DVFS",GOLD).setOnClickListener(v->authenticate("DVFS"));
  button(panel,"KLUCZ PUBLICZNY STRONGBOX",Color.LTGRAY).setOnClickListener(v->io.execute(()->{
   try{String p=Base64.getEncoder().encodeToString(signer.publicKey());runOnUiThread(()->new android.app.AlertDialog.Builder(this).setTitle("P-256 SPKI DER / Base64")
    .setMessage(p).setPositiveButton("Kopiuj",(d,w)->{android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cb.setPrimaryClip(android.content.ClipData.newPlainText("SPKI",p));}).setNegativeButton("Zamknij",null).show());
   }catch(Exception e){error(e.getMessage());}
  }));
  label(panel,"Polecenia wymagają StrongBox, biometrii, prawidłowego TLS i ACK bramy. Brak ACK nie oznacza wykonania. Urządzenia fizyczne muszą mieć własne blokady bezpieczeństwa.",12,Color.LTGRAY,false);
  handler.postDelayed(poll,3000);
 }
 private TextView label(LinearLayout parent,String value,int size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);
  if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  v.setPadding(4,9,4,13);parent.addView(v);return v;
 }
 private EditText field(LinearLayout p,String title,String value){
  label(p,title,12,Color.LTGRAY,false);EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setTextColor(Color.WHITE);p.addView(e);return e;
 }
 private Button button(LinearLayout p,String title,int color){
  Button b=new Button(this);b.setText(title);b.setTextColor(Color.BLACK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));p.addView(b);return b;
 }
 private void error(String message){runOnUiThread(()->{status.setText("BŁĄD: "+message);status.setTextColor(Color.RED);});}
 private String gateway(){
  String s=endpoint.getText().toString().trim();
  if(!s.startsWith("https://"))throw new IllegalArgumentException("Wymagany HTTPS");
  return s.replaceAll("/+$","");
 }
 private String nodeId(){
  String s=node.getText().toString().trim();
  if(!s.matches("[a-zA-Z0-9_-]{1,64}"))throw new IllegalArgumentException("Invalid node ID");
  return s;
 }
 private void fetch(){
  final String base,id;
  try{base=gateway();id=nodeId();}catch(Exception e){connected=false;error(e.getMessage());return;}
  io.execute(()->{
   try{
    HttpURLConnection c=(HttpURLConnection)new URL(base+"/v1/telemetry?node="+URLEncoder.encode(id,"UTF-8")).openConnection();
    c.setConnectTimeout(3000);c.setReadTimeout(3000);
    if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
    JSONObject j=new JSONObject(new String(c.getInputStream().readAllBytes(),StandardCharsets.UTF_8));c.disconnect();
    runOnUiThread(()->{
     connected=true;status.setText("BRAMA ZWERYFIKOWANA · "+id);status.setTextColor(GOLD);
     summary.setText(String.format(java.util.Locale.US,"HASHRATE %.2f TH/s\nTEMPERATURA %.1f °C\nMOC %.0f W\nPULA %s\nSTAN %s",
      j.optDouble("hashrate_ths"),j.optDouble("temperature_c"),j.optDouble("power_w"),j.optString("pool"),j.optString("state")));
    });
   }catch(Exception e){connected=false;error("Brama niedostępna: "+e.getMessage());}
  });
 }
 private void authenticate(String action){
  if(!connected||busy){Toast.makeText(this,"Brak aktywnej bramy lub trwa operacja",Toast.LENGTH_SHORT).show();return;}
  final String base,id,poolId;final double volt,freq;
  try{
   base=gateway();id=nodeId();poolId=pool.getText().toString().trim();
   volt=Double.parseDouble(voltage.getText().toString());freq=Double.parseDouble(frequency.getText().toString());
   if(!Double.isFinite(volt)||!Double.isFinite(freq))throw new IllegalArgumentException("Invalid numbers");
  }catch(Exception e){error(e.getMessage());return;}
  new android.app.AlertDialog.Builder(this).setTitle("Potwierdź "+action)
   .setMessage("Węzeł "+id+" · "+action+". Wymagana autoryzacja biometryczna.")
   .setNegativeButton("Anuluj",null).setPositiveButton("Autoryzuj",(d,w)->{
    try{
     Signature prepared=signer.prepare();
     BiometricPrompt prompt=new BiometricPrompt.Builder(this).setTitle("OMEGA / "+action)
      .setSubtitle("Sprzętowy podpis StrongBox")
      .setNegativeButton("Anuluj",getMainExecutor(),(dialog,which)->{}).build();
     prompt.authenticate(new BiometricPrompt.CryptoObject(prepared),new CancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
      @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
       Signature s=result.getCryptoObject()==null?null:result.getCryptoObject().getSignature();
       if(s==null){error("Brak podpisu po biometrii");return;}
       dispatch(action,id,base,volt,freq,poolId,s);
      }
      @Override public void onAuthenticationError(int code,CharSequence err){error(err.toString());}
     });
    }catch(Exception e){error(e.getMessage());}
   }).show();
 }
 private void dispatch(String action,String id,String base,double volt,double freq,String poolId,Signature signature){
  if(busy)return;busy=true;status.setText("WYSYŁANIE · OCZEKIWANIE NA ACK");
  io.execute(()->{
   try{
    byte[] canonical=CommandCodec.canonical(id,System.currentTimeMillis(),UUID.randomUUID().toString(),action,volt,freq,action.equals("POOL")?poolId:"");
    signature.update(canonical);byte[] signed=signature.sign();
    JSONObject cmd=new JSONObject();cmd.put("payload",Base64.getEncoder().encodeToString(canonical));
    cmd.put("signature",Base64.getEncoder().encodeToString(signed));
    cmd.put("public_key",Base64.getEncoder().encodeToString(signer.publicKey()));
    HttpURLConnection c=(HttpURLConnection)new URL(base+"/v1/commands").openConnection();
    c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(5000);c.setReadTimeout(5000);
    c.setRequestProperty("Content-Type","application/json");
    try(OutputStream out=c.getOutputStream()){out.write(cmd.toString().getBytes(StandardCharsets.UTF_8));}
    int code=c.getResponseCode();
    InputStream stream=code<400?c.getInputStream():c.getErrorStream();
    JSONObject ack=new JSONObject(new String(stream.readAllBytes(),StandardCharsets.UTF_8));c.disconnect();
    if(code!=200||!ack.optBoolean("applied",false))throw new IOException("Command not confirmed");
    runOnUiThread(()->{status.setText("ACK · ZASTOSOWANO "+action);status.setTextColor(GOLD);});
   }catch(Exception e){error(e.getMessage());}finally{busy=false;}
  });
 }
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
