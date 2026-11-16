package pl.omega.swarm;
import android.app.Activity;import android.os.Bundle;import android.graphics.Color;import android.widget.*;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.concurrent.*;
/** Optional localhost-only OpenAI-compatible bridge to a user-started llama-server. */
public final class LocalChatActivity extends Activity {
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private EditText prompt;private TextView result;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  ScrollView sc=new ScrollView(this);LinearLayout box=new LinearLayout(this);
  box.setOrientation(1);box.setPadding(24,24,24,24);box.setBackgroundColor(0xff07080a);sc.addView(box);setContentView(sc);
  TextView title=new TextView(this);title.setText("OMEGA / LOCAL LLAMA.CPP");title.setTextColor(0xffd4af37);title.setTextSize(20);box.addView(title);
  TextView note=new TextView(this);note.setText("Uruchom llama-server osobno na 127.0.0.1:8080. Wyeksportuj GGUF do pamięci dostępnej dla Termux. AI nigdy nie steruje koparkami automatycznie.");note.setTextColor(Color.LTGRAY);note.setPadding(0,16,0,16);box.addView(note);
  prompt=new EditText(this);prompt.setHint("Pytanie do lokalnego modelu");prompt.setHintTextColor(Color.GRAY);prompt.setTextColor(Color.WHITE);prompt.setMinLines(3);box.addView(prompt);
  Button send=new Button(this);send.setText("ZAPYTAJ MODEL");box.addView(send);
  result=new TextView(this);result.setTextColor(Color.WHITE);result.setTextSize(15);box.addView(result);
  send.setOnClickListener(v->{
   String question=prompt.getText().toString().trim();
   if(question.isEmpty()||question.length()>8192){result.setText("Limit 1–8192 znaków");return;}
   send.setEnabled(false);result.setText("Oczekiwanie...");
   io.execute(()->{
    try{
     JSONObject body=new JSONObject();body.put("stream",false);body.put("max_tokens",320);
     JSONArray messages=new JSONArray();messages.put(new JSONObject().put("role","user").put("content",question));body.put("messages",messages);
     HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8080/v1/chat/completions").openConnection();
     c.setConnectTimeout(5000);c.setReadTimeout(120000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
     try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
     if(c.getResponseCode()!=200)throw new IOException("llama-server HTTP "+c.getResponseCode());
     ByteArrayOutputStream resultBytes=new ByteArrayOutputStream();
     try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;
      while((n=in.read(b))!=-1){if(resultBytes.size()+n>2*1024*1024)throw new IOException("Reply too large");resultBytes.write(b,0,n);}}
     c.disconnect();
     JSONObject decoded=new JSONObject(resultBytes.toString(StandardCharsets.UTF_8));
     String answer=decoded.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
     runOnUiThread(()->result.setText(answer));
    }catch(Exception e){runOnUiThread(()->result.setText("Błąd lokalnego serwera: "+e.getMessage()));}
    finally{runOnUiThread(()->send.setEnabled(true));}
   });
  });
 }
 @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
