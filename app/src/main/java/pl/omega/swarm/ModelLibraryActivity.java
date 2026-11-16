package pl.omega.swarm;
import android.app.*;import android.content.*;import android.database.Cursor;import android.graphics.Color;
import android.graphics.Typeface;import android.net.Uri;import android.os.Bundle;import android.provider.OpenableColumns;
import android.text.InputType;import android.view.WindowManager;import android.widget.*;
import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** GGUF library; no automatic privileges to control mining equipment. */
public final class ModelLibraryActivity extends Activity {
 private static final int OPEN=1001,EXPORT=1002,GOLD=0xffd4af37,BG=0xff07080a;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final AtomicBoolean cancel=new AtomicBoolean(),busy=new AtomicBoolean();
 private TextView status,selected;private EditText repo,revision,path,sha,token;
 private LinearLayout items;private ModelFiles store;private File exportFile;private long lastUpdate;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  try{File external=getExternalFilesDir("models");store=new ModelFiles(external!=null?external:new File(getFilesDir(),"models"));}
  catch(IOException e){new AlertDialog.Builder(this).setMessage(e.getMessage()).setPositiveButton("OK",(d,w)->finish()).show();return;}
  ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);
  root.setBackgroundColor(BG);root.setPadding(28,24,28,32);sc.addView(root);setContentView(sc);
  text(root,"OMEGA / GGUF MODEL VAULT",22,GOLD,true);
  status=text(root,"Tryb biblioteki modeli · odizolowany od komend koparek",13,GOLD,false);
  selected=text(root,"Brak wybranego modelu",15,Color.WHITE,true);
  button(root,"IMPORTUJ GGUF Z TELEFONU",GOLD).setOnClickListener(v->{
   Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);
   in.setType("*/*");in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(in,OPEN);
  });
  text(root,"HUGGING FACE",18,GOLD,true);
  repo=field(root,"Repozytorium owner/repo","Qwen/Qwen3-0.6B-GGUF");
  revision=field(root,"Rewizja (main lub SHA)","main");
  path=field(root,"Nazwa pliku GGUF","");
  sha=field(root,"Oczekiwany SHA-256 (opcjonalnie)","");
  token=field(root,"Token HF tylko na tę sesję (opcjonalnie)","");
  token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  button(root,"POKAŻ PLIKI GGUF",GOLD).setOnClickListener(v->catalog());
  button(root,"POBIERZ GGUF",GOLD).setOnClickListener(v->download());
  button(root,"ANULUJ TRANSFER",Color.LTGRAY).setOnClickListener(v->{cancel.set(true);setStatus("Żądanie anulowania");});
  text(root,"MOJE MODELE",18,GOLD,true);items=new LinearLayout(this);items.setOrientation(1);root.addView(items);
  button(root,"ODŚWIEŻ LISTĘ",Color.LTGRAY).setOnClickListener(v->refresh());
  button(root,"CZAT LOCAL LLAMA-SERVER",GOLD).setOnClickListener(v->startActivity(new Intent(this,LocalChatActivity.class)));
  text(root,"Import sprawdza nagłówek GGUF i SHA-256, nie uruchamia inferencji. Czat wymaga osobnego llama-server na 127.0.0.1:8080. Modele wieloczęściowe wymagają wszystkich plików. Pobieranie działa podczas aktywności ekranu; przerwane pobieranie z przypiętej rewizji można wznowić.",12,Color.LTGRAY,false);
  refresh();
 }
 private TextView text(LinearLayout p,String value,int size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(4,10,4,11);
  if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);p.addView(v);return v;
 }
 private Button button(LinearLayout p,String title,int color){Button b=new Button(this);b.setText(title);b.setTextColor(Color.BLACK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));p.addView(b);return b;}
 private EditText field(LinearLayout p,String label,String defaultValue){text(p,label,12,Color.LTGRAY,false);EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setText(defaultValue);p.addView(e);return e;}
 private String val(EditText e){return e.getText().toString().trim();}
 private void setStatus(String s){runOnUiThread(()->{if(!isFinishing())status.setText(s);});}
 private boolean acquire(){if(!busy.compareAndSet(false,true)){setStatus("Transfer już trwa");return false;}cancel.set(false);lastUpdate=0;return true;}
 private void finishTransfer(){busy.set(false);runOnUiThread(this::refresh);}
 private void progress(long done,long total){long now=System.currentTimeMillis();if(now-lastUpdate<500)return;lastUpdate=now;
  setStatus(String.format(Locale.US,"%.1f MiB%s",done/1048576.0,total>0?String.format(Locale.US," / %.1f MiB",total/1048576.0):""));
 }
 private void catalog(){
  if(!acquire())return;String repoId=val(repo),auth=val(token);
  io.execute(()->{
   try{HuggingFaceCatalog.Catalog cat=HuggingFaceCatalog.get(repoId,auth);
    if(cat.ggufFiles.isEmpty()){setStatus("Brak plików GGUF");return;}
    runOnUiThread(()->new AlertDialog.Builder(this).setTitle(repoId).setItems(cat.ggufFiles.toArray(new String[0]),(d,w)->{
     path.setText(cat.ggufFiles.get(w));revision.setText(cat.revisionSha);
     setStatus("Wybrano: "+cat.ggufFiles.get(w));
    }).setNegativeButton("Anuluj",null).show());
   }catch(Exception e){setStatus("Błąd HF: "+e.getMessage());}finally{finishTransfer();}
  });
 }
 private void download(){
  if(!acquire())return;String r=val(repo),rev=val(revision),file=val(path),expected=val(sha),auth=val(token);
  io.execute(()->{
   try{ModelFiles.Entry e=new HuggingFaceDownloader(store).download(r,rev,file,auth,expected,cancel,this::progress);
    setStatus("Pobrano: "+e.file.getName()+" · SHA-256 "+e.sha256);
   }catch(Exception e){setStatus("Pobieranie: "+e.getMessage());}finally{finishTransfer();}
  });
 }
 private String fileName(Uri uri){
  try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
   if(c!=null&&c.moveToFirst()){String n=c.getString(0);if(n!=null)return n;}
  }catch(Exception ignored){}
  return uri.getLastPathSegment()==null?"model.gguf":uri.getLastPathSegment();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
  if(request==OPEN){if(!acquire())return;String name=fileName(uri);
   io.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){
    if(in==null)throw new IOException("Cannot open selected document");
    ModelFiles.Entry e=store.importFile(name,in,cancel,n->progress(n,-1));setStatus("Imported "+e.file.getName()+" · "+e.sha256);
   }catch(Exception e){setStatus("Import: "+e.getMessage());}finally{finishTransfer();}});
  }else if(request==EXPORT){if(!acquire())return;File source=exportFile;
   io.execute(()->{try(InputStream in=new BufferedInputStream(new FileInputStream(source));OutputStream out=getContentResolver().openOutputStream(uri,"w")){
    if(out==null)throw new IOException("Cannot export document");byte[] b=new byte[262144];long done=0;int n;
    while((n=in.read(b))!=-1){if(cancel.get())throw new IOException("Cancelled");out.write(b,0,n);done+=n;progress(done,source.length());}
    out.flush();setStatus("Eksport zakończony");
   }catch(Exception e){setStatus("Eksport: "+e.getMessage());}finally{finishTransfer();}});
  }
 }
 private void refresh(){
  if(store==null||items==null)return;items.removeAllViews();
  String active=getPreferences(0).getString("selected_model","");
  List<ModelFiles.Entry> entries=store.list();if(entries.isEmpty())text(items,"Brak modeli",13,Color.LTGRAY,false);
  for(ModelFiles.Entry e:entries){
   String display=e.file.getName().replaceFirst("^[0-9a-f-]{36}--","");
   text(items,(active.equals(e.file.getName())?"● ":"○ ")+display+String.format(Locale.US," · %.2f GiB",e.bytes/1073741824.0),14,Color.WHITE,true);
   text(items,"SHA-256: "+e.sha256,11,Color.LTGRAY,false);
   button(items,"WYBIERZ",GOLD).setOnClickListener(v->{getPreferences(0).edit().putString("selected_model",e.file.getName()).apply();refresh();});
   button(items,"EKSPORTUJ",Color.LTGRAY).setOnClickListener(v->{exportFile=e.file;Intent in=new Intent(Intent.ACTION_CREATE_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);in.setType("application/octet-stream");in.putExtra(Intent.EXTRA_TITLE,display);startActivityForResult(in,EXPORT);});
   button(items,"USUŃ",0xffb8333a).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Usuń model?").setMessage(display).setNegativeButton("Nie",null).setPositiveButton("Usuń",(d,w)->{
    try{store.delete(e.file.getName());if(active.equals(e.file.getName()))getPreferences(0).edit().remove("selected_model").apply();refresh();}
    catch(IOException err){setStatus(err.getMessage());}
   }).show());
  }
  selected.setText(active.isEmpty()?"Model nie został wybrany":"Wybrany: "+active.replaceFirst("^[0-9a-f-]{36}--",""));
 }
 @Override protected void onDestroy(){cancel.set(true);io.shutdownNow();super.onDestroy();}
}
