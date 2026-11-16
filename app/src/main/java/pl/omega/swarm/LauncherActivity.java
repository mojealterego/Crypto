package pl.omega.swarm;
import android.app.Activity;import android.os.Bundle;import android.content.Intent;
import android.graphics.Color;import android.graphics.Typeface;import android.widget.*;
/** Navigation root: mining control and local AI are intentionally separate. */
public final class LauncherActivity extends Activity {
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(28,48,28,28);box.setBackgroundColor(0xff07080a);setContentView(box);
  TextView title=new TextView(this);title.setText("OMEGA / SOVEREIGN SWARM");
  title.setTextColor(0xffd4af37);title.setTextSize(23);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(title);
  TextView info=new TextView(this);info.setText("Autonomiczne wydobycie i lokalne modele AI. AI nie posiada bezpośrednich uprawnień sterowania sprzętem.");
  info.setTextColor(Color.LTGRAY);info.setPadding(0,12,0,25);box.addView(info);
  Button mining=new Button(this);mining.setText("KOPARKI · TELEMETRIA I STEROWANIE");box.addView(mining);
  mining.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
  Button gguf=new Button(this);gguf.setText("GGUF · HUGGING FACE · MODELE");box.addView(gguf);
  gguf.setOnClickListener(v->startActivity(new Intent(this,ModelLibraryActivity.class)));
  Button chat=new Button(this);chat.setText("CZAT LOKALNY · LLAMA.CPP");box.addView(chat);
  chat.setOnClickListener(v->startActivity(new Intent(this,LocalChatActivity.class)));
  TextView disclaimer=new TextView(this);disclaimer.setText("Wymagany bezpieczny gateway do sterowania sprzętem. Modele GGUF nie uruchamiają się bez silnika inference (llama.cpp).");
  disclaimer.setTextColor(Color.LTGRAY);disclaimer.setPadding(0,24,0,0);box.addView(disclaimer);
 }
}
