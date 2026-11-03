package br.com.gdm.crossingblock.seeds;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends ComponentActivity {
    private static final int NAVY=Color.rgb(9,36,59),LIME=Color.rgb(180,189,0),MUTED=Color.rgb(89,107,121),LINE=Color.rgb(224,230,235),SOFT=Color.rgb(243,245,247);
    private static final int MAX_IMPORT_BYTES=32*1024*1024;
    private final ExecutorService io=Executors.newSingleThreadExecutor(),cameraExecutor=Executors.newSingleThreadExecutor();
    private RecordStore store;
    private UserStore users;
    private TextView userCaption;
    private ImageButton userButton;
    private Workbook base;
    private AtomicFile baseFile;
    private String currentQr="",digits="",pendingFileOp="",commentDraft="";
    private volatile boolean scanning=true,modalOpen=false,busy=false;
    private TextView todayCount,totalCount,todayMean,totalMean,status,message,earName,mother,father,duplicateNotice;
    private SeedCountView seedInput;
    private LinearLayout operations,ear;
    private TextView commentPreview;
    private EditText activeCommentEditor;
    private final List<Button> keypad=new ArrayList<>();
    private Button saveButton,rereadButton;
    private PreviewView preview;
    private ImageView frozen;
    private TextView cameraHint;
    private ProcessCameraProvider provider;
    private BarcodeScanner scanner;
    private final NumberFormat numberFormat=NumberFormat.getNumberInstance(new Locale("pt","BR"));
    private boolean destroyed=false;
    private int dialogCount=0;

    private final ActivityResultLauncher<String> cameraPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{if(granted)startCamera();else cameraFailure("Permita o uso da câmera para ler QR Codes. Toque em Reler para tentar novamente.");});
    private final ActivityResultLauncher<Intent> filePicker=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{
        String op=pendingFileOp;pendingFileOp="";
        if(result.getResultCode()!=RESULT_OK||result.getData()==null||result.getData().getData()==null){refresh();return;}
        Uri uri=result.getData().getData();
        if(op.equals("import"))importBase(uri);
        else if(op.equals("new"))writeNew(uri);
        else if(op.equals("existing"))prepareExisting(uri);
    });

    @Override public void onCreate(Bundle state){
        super.onCreate(state);numberFormat.setMaximumFractionDigits(1);
        store=new RecordStore(this);users=new UserStore(this);baseFile=new AtomicFile(new File(getFilesDir(),"input.xlsx"));
        if(state!=null){currentQr=state.getString("qr","");digits=state.getString("digits","");scanning=state.getBoolean("scanning",true);pendingFileOp=state.getString("pending","");commentDraft=state.getString("comment","");}
        createInterface();
        scanner=BarcodeScanning.getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());
        if(baseFile.getBaseFile().exists()){
            setBusy(true,"Carregando base de espigas…");io.execute(()->{try(InputStream in=baseFile.openRead()){Workbook loaded=ExcelFile.read(in);runOnUiThread(()->{if(destroyed)return;base=loaded;setBusy(false,"");refresh();});}catch(Exception e){runOnUiThread(()->{if(destroyed)return;setBusy(false,"");showError("A base não pôde ser carregada. Você pode coletar sem importar uma base.");});}});
        }
        refresh();ensureCameraPermission();if(users.current()==null)showUsersDialog();
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("qr",currentQr);state.putString("digits",digits);state.putBoolean("scanning",scanning);state.putString("pending",pendingFileOp);state.putString("comment",activeCommentEditor==null?commentDraft:activeCommentEditor.getText().toString());}
    @Override protected void onResume(){super.onResume();if(todayCount!=null)refreshSummary();}
    @Override protected void onDestroy(){destroyed=true;if(provider!=null)provider.unbindAll();if(scanner!=null)scanner.close();cameraExecutor.shutdown();io.shutdown();super.onDestroy();}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border)d.setStroke(dp(1),LINE);return d;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTag((float)size);t.setTextColor(color);return t;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void gap(LinearLayout l,int h){l.addView(new View(this),lp(1,h));}
    private Button button(String label,int fill,int color){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTag(14f);b.setTextColor(color);b.setTypeface(null,Typeface.NORMAL);b.setPadding(dp(8),0,dp(8),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setBackground(bg(fill,10,fill==Color.WHITE));return b;}
    private void createInterface(){
        if(users==null)users=new UserStore(this);
        LinearLayout header=row();header.setBackgroundColor(NAVY);header.setPadding(dp(16),dp(8),dp(10),dp(8));
        TextView title=text("Crossing Block - Seeds",16,Color.WHITE);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(2);title.setGravity(Gravity.CENTER_VERTICAL);header.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        userButton=new ImageButton(this);userButton.setImageDrawable(new UserIcon());userButton.setBackground(bg(NAVY,8,false));userButton.setPadding(dp(11),dp(11),dp(11),dp(11));userButton.setContentDescription("Escolher ou editar usuário");userButton.setOnClickListener(v->{if(!busy)showUsersDialog();});header.addView(userButton,lp(44,48));header.addView(iconButton(false,"Importar dados",this::showImportDialog),lp(44,48));header.addView(iconButton(true,"Exportar Excel",this::showExportDialog),lp(44,48));
        LinearLayout summary=row(),counts=vertical(),means=vertical();
        todayCount=text("",12,MUTED);totalCount=text("",12,MUTED);todayMean=text("",12,MUTED);totalMean=text("",12,MUTED);todayMean.setGravity(Gravity.END);totalMean.setGravity(Gravity.END);
        counts.addView(todayCount);gap(counts,3);counts.addView(totalCount);means.addView(todayMean);gap(means,3);means.addView(totalMean);summary.addView(counts,new LinearLayout.LayoutParams(0,-2,1));summary.addView(means,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout cameraBox=new FrameLayout(this);cameraBox.setBackground(bg(NAVY,12,false));cameraBox.setClipToOutline(true);cameraBox.setContentDescription("Imagem quadrada da câmera");
        preview=new PreviewView(this);preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);cameraBox.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        frozen=new ImageView(this);frozen.setScaleType(ImageView.ScaleType.CENTER_CROP);frozen.setVisibility(View.GONE);cameraBox.addView(frozen,new FrameLayout.LayoutParams(-1,-1));cameraBox.addView(new Reticle(this),new FrameLayout.LayoutParams(-1,-1));
        cameraHint=text("Enquadre o QR Code",11,Color.WHITE);cameraHint.setMaxLines(2);cameraHint.setGravity(Gravity.CENTER);cameraHint.setBackgroundColor(0x8809243B);cameraBox.addView(cameraHint,new FrameLayout.LayoutParams(-1,dp(30),Gravity.BOTTOM));
        rereadButton=button("Reler",SOFT,NAVY);rereadButton.setTag(12f);rereadButton.setMaxLines(1);rereadButton.setHorizontallyScrolling(false);rereadButton.setPadding(dp(4),0,dp(4),0);rereadButton.setOnClickListener(v->reread());status=text("Câmera ativa",11,MUTED);status.setMaxLines(2);status.setPadding(0,dp(4),0,dp(4));
        SquareCameraPanel cameraPanel=new SquareCameraPanel(this,cameraBox,rereadButton,status);
        ear=vertical();ear.setPadding(dp(12),dp(10),dp(12),dp(10));ear.setBackground(bg(Color.WHITE,12,true));ear.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout parents=row();earName=text("",18,NAVY);earName.setTypeface(null,Typeface.BOLD);earName.setMaxLines(1);parents.addView(earName,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout fraction=vertical();mother=parentLine(fraction,"Mãe");View rule=new View(this);rule.setBackgroundColor(NAVY);LinearLayout.LayoutParams line=lp(44,1);line.gravity=Gravity.END;fraction.addView(rule,line);father=parentLine(fraction,"Pai");parents.addView(fraction,lp(100,-2));ear.addView(parents);
        duplicateNotice=text("QR Code já coletado",12,Color.rgb(160,45,45));duplicateNotice.setTypeface(null,Typeface.BOLD);duplicateNotice.setPadding(0,dp(8),0,0);duplicateNotice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);duplicateNotice.setVisibility(View.GONE);ear.addView(duplicateNotice,lp(-1,-2));operations=vertical();ear.addView(operations);
        TextView commentLabel=text("Observação",13,NAVY);commentLabel.setGravity(Gravity.CENTER_VERTICAL);
        commentPreview=text("Adicionar observação…",14,MUTED);commentPreview.setPadding(dp(12),dp(6),dp(12),dp(6));commentPreview.setBackground(bg(SOFT,10,true));commentPreview.setMaxLines(2);commentPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);commentPreview.setGravity(Gravity.TOP);commentPreview.setClickable(true);commentPreview.setFocusable(true);commentPreview.setContentDescription("Editar observação da espiga");commentPreview.setOnClickListener(v->editComment());
        DetailsPanel detailsPanel=new DetailsPanel(this,ear,commentLabel,commentPreview);
        LinearLayout seedRow=row();seedRow.setGravity(Gravity.CENTER);LinearLayout seedBox=vertical();seedBox.setGravity(Gravity.CENTER);seedBox.setBackground(bg(SOFT,12,true));TextView seeds=text("Sementes",12,MUTED);seeds.setGravity(Gravity.CENTER);seeds.setMaxLines(1);seedBox.addView(seeds,lp(-1,20));seedInput=new SeedCountView(this);seedInput.setTag(32f);seedInput.setTextColor(NAVY);seedBox.addView(seedInput,new LinearLayout.LayoutParams(-1,0,1));seedRow.addView(seedBox,lp(180,-1));
        LinearLayout keys=vertical();String[] labels={"1","2","3","4","5","6","7","8","9","Limpar","0","⌫"};
        for(int r=0;r<4;r++){LinearLayout keysRow=row();for(int c=0;c<3;c++){String key=labels[r*3+c];Button b=button(key,SOFT,NAVY);b.setTag(key.equals("Limpar")?12f:23f);if(key.equals("⌫"))b.setContentDescription("Apagar último dígito");LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);if(c>0)p.leftMargin=dp(6);keysRow.addView(b,p);keypad.add(b);b.setOnClickListener(v->type(key));}LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,0,1);if(r<3)rp.bottomMargin=dp(6);keys.addView(keysRow,rp);}
        saveButton=button("Salvar",LIME,NAVY);saveButton.setTypeface(null,Typeface.BOLD);saveButton.setOnClickListener(v->save());message=text("",11,MUTED);message.setPadding(0,dp(3),0,0);message.setMinHeight(dp(20));message.setMaxLines(2);message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout feedback=vertical();userCaption=text("",11,MUTED);userCaption.setMaxLines(1);feedback.addView(userCaption);feedback.addView(message);refreshUserCaption();EntryPanel entry=new EntryPanel(this,seedRow,keys,saveButton,feedback);
        CaptureViewport viewport=new CaptureViewport(this,header,summary,cameraPanel,detailsPanel,entry);viewport.setBackgroundColor(Color.WHITE);
        viewport.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());}return insets;});setContentView(viewport);viewport.requestApplyInsets();
    }
    private void editComment(){
        if(scanning||busy||currentQr.isEmpty())return;
        final String qr=currentQr;LinearLayout body=dialogBody();EditText edit=new EditText(this);activeCommentEditor=edit;edit.setText(commentDraft);edit.setTextColor(NAVY);edit.setTextSize(16);edit.setGravity(Gravity.TOP);edit.setMinLines(3);edit.setMaxLines(6);edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32767)});edit.setHint("Adicionar observação…");edit.setContentDescription("Observação da espiga");body.addView(edit,lp(-1,-2));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Observação da espiga").setView(body).setNegativeButton("Cancelar",null).setPositiveButton("Concluir",(v,w)->{if(currentQr.equals(qr)){commentDraft=edit.getText().toString();refreshComment();}}).create();
        dialogCount++;modalOpen=true;d.setOnDismissListener(v->{dialogCount=Math.max(0,dialogCount-1);modalOpen=dialogCount>0;});d.show();d.setOnDismissListener(v->{activeCommentEditor=null;dialogCount=Math.max(0,dialogCount-1);modalOpen=dialogCount>0;});d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(NAVY);d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(NAVY);edit.requestFocus();d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void refreshUserCaption(){UserStore.User user=users.current();String label=user==null?"Escolher usuário":"Usuário · "+user.name;if(userCaption!=null)userCaption.setText(label);if(userButton!=null){userButton.setContentDescription("Escolher ou editar usuário. "+label);userButton.setTooltipText(label);}}
    private void showUsersDialog(){
        LinearLayout body=dialogBody();Spinner choices=new Spinner(this);ArrayAdapter<UserStore.User> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new ArrayList<>(users.all()));choices.setAdapter(adapter);choices.setContentDescription("Selecionar usuário");body.addView(choices,lp(-1,48));
        TextView empty=caption("Cadastre um usuário para identificar as próximas coletas.");body.addView(empty);
        Runnable reload=()->{adapter.clear();adapter.addAll(users.all());adapter.notifyDataSetChanged();UserStore.User current=users.current();for(int i=0;i<adapter.getCount();i++)if(current!=null&&adapter.getItem(i).id.equals(current.id))choices.setSelection(i);empty.setVisibility(adapter.isEmpty()?View.VISIBLE:View.GONE);};reload.run();
        action(body,"Editar nome",()->{UserStore.User chosen=(UserStore.User)choices.getSelectedItem();if(chosen!=null)editUser(chosen,id->{reload.run();for(int i=0;i<adapter.getCount();i++)if(adapter.getItem(i).id.equals(id))choices.setSelection(i);});});
        action(body,"Novo usuário",()->editUser(null,id->{reload.run();for(int i=0;i<adapter.getCount();i++)if(adapter.getItem(i).id.equals(id))choices.setSelection(i);}));
        TextView error=text("",12,Color.rgb(160,45,45));error.setVisibility(View.GONE);body.addView(error);
        AlertDialog d=showDialog("Usuário da coleta",body);d.getButton(AlertDialog.BUTTON_NEGATIVE).setText("Cancelar");Button use=button("Usar selecionado",LIME,NAVY);body.addView(use,lp(-1,48));use.setOnClickListener(v->{try{UserStore.User chosen=(UserStore.User)choices.getSelectedItem();if(chosen==null)throw new IllegalArgumentException("Cadastre e escolha um usuário.");users.select(chosen.id);refreshUserCaption();d.dismiss();}catch(Exception e){error.setVisibility(View.VISIBLE);error.setText(safe(e));}});
    }
    private void editUser(UserStore.User user,java.util.function.Consumer<String> changed){
        LinearLayout body=dialogBody();EditText name=new EditText(this);name.setSingleLine(true);name.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});name.setHint("Nome do usuário");name.setContentDescription("Nome do usuário");if(user!=null)name.setText(user.name);body.addView(name,lp(-1,-2));if(user!=null)body.addView(caption("O novo nome será usado nas próximas gravações. Coletas anteriores mantêm o nome registrado."));
        AlertDialog d=showDialog(user==null?"Novo usuário":"Editar usuário",body);d.getButton(AlertDialog.BUTTON_NEGATIVE).setText("Cancelar");Button save=button("Salvar usuário",LIME,NAVY);body.addView(save,lp(-1,48));save.setOnClickListener(v->{try{String id;if(user==null){id=users.add(name.getText().toString()).id;}else{users.rename(user.id,name.getText().toString());id=user.id;}changed.accept(id);refreshUserCaption();d.dismiss();}catch(Exception e){name.setError(safe(e));}});name.requestFocus();d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void refreshComment(){boolean enabled=!scanning&&!currentQr.isEmpty()&&!busy;commentPreview.setEnabled(enabled);commentPreview.setAlpha(enabled?1f:.45f);commentPreview.setText(commentDraft.isEmpty()?"Adicionar observação…":commentDraft);commentPreview.setTextColor(commentDraft.isEmpty()?MUTED:NAVY);commentPreview.setContentDescription("Editar observação da espiga"+(commentDraft.isEmpty()?"":": "+commentDraft));}
    private TextView parentLine(LinearLayout column,String label){LinearLayout r=row();TextView name=text(label,11,MUTED),value=text("",24,NAVY);r.addView(name,new LinearLayout.LayoutParams(0,-2,1));value.setGravity(Gravity.CENTER);value.setMaxLines(1);value.setHorizontallyScrolling(false);r.addView(value,lp(44,-2));column.addView(r);return value;}
    private ImageButton iconButton(boolean export,String description,Runnable action){ImageButton b=new ImageButton(this);b.setImageDrawable(new FileIcon(export));b.setBackground(bg(NAVY,8,false));b.setPadding(dp(11),dp(11),dp(11),dp(11));b.setContentDescription(description);b.setTooltipText(description);b.setOnClickListener(v->{if(!busy)action.run();});return b;}
    private void type(String key){if(scanning||busy||currentQr.isEmpty())return;if(key.equals("Limpar"))digits="";else if(key.equals("⌫"))digits=digits.isEmpty()?"":digits.substring(0,digits.length()-1);else if(digits.length()<2)digits=digits.equals("0")?key:digits+key;else return;seedInput.setText(digits);message.setText("");}
    private void refresh(){
        if(destroyed)return;
        refreshSummary();refreshComment();seedInput.setText(digits);boolean enabled=!scanning&&!currentQr.isEmpty()&&!busy;
        for(Button b:keypad){b.setEnabled(enabled);b.setAlpha(enabled?1f:.4f);}saveButton.setEnabled(enabled);saveButton.setAlpha(enabled?1f:.4f);rereadButton.setEnabled(!busy);
        earName.setText("");mother.setText("");father.setText("");operations.removeAllViews();
        try{duplicateNotice.setVisibility(!scanning&&!currentQr.isEmpty()&&store.find(currentQr)!=null?View.VISIBLE:View.GONE);}catch(Exception e){duplicateNotice.setVisibility(View.GONE);showError("Não foi possível consultar o histórico da espiga.");}
        boolean found=base!=null&&base.rows.containsKey(currentQr)&&!scanning;
        if(base!=null&&!found&&!scanning&&!currentQr.isEmpty())earName.setText("Sem registro");
        if(found){earName.setText(base.get(currentQr,"Lot_name"));String[] p=base.parents(currentQr);mother.setText(p[0]);father.setText(p[1]);addOperation("Emasculação",base.get(currentQr,"Emasculation date"),base.get(currentQr,"Emasculator1"));addOperation("Polinização",base.get(currentQr,"Pollination date"),base.get(currentQr,"Pollinator"));}
        mother.setVisibility(found?View.VISIBLE:View.INVISIBLE);father.setVisibility(found?View.VISIBLE:View.INVISIBLE);
        // Keep the whole parent area blank when there are no imported attributes.
        ((View)mother.getParent().getParent()).setVisibility(found?View.VISIBLE:View.GONE);
        status.setText(busy?"Processando…":scanning?"Câmera ativa · enquadre o próximo QR Code":found?"Espiga localizada":"QR Code lido");
        cameraHint.setText(scanning?"Enquadre o QR Code":"QR Code lido · leitura pausada");
        if(scanning)frozen.setVisibility(View.GONE);
    }
    private void addOperation(String label,String date,String person){if(date.trim().isEmpty()&&person.trim().isEmpty())return;if(operations.getChildCount()==0){gap(operations,8);View line=new View(this);line.setBackgroundColor(LINE);operations.addView(line,lp(-1,1));gap(operations,7);}TextView t=text(label+"   "+date+(date.trim().isEmpty()||person.trim().isEmpty()?"":" · ")+person,11,MUTED);t.setPadding(0,dp(2),0,dp(2));operations.addView(t);}
    private String mean(double value){return Double.isNaN(value)?"—":numberFormat.format(value);}
    private void refreshSummary(){try{Summary s=new Summary(store.all(),LocalDate.now(Workbook.ZONE));todayCount.setText("Hoje · "+s.todayCount+" registros");totalCount.setText("Total · "+s.totalCount+" registros");todayMean.setText("Média · "+mean(s.todayMean)+" sementes");totalMean.setText("Média total · "+mean(s.totalMean));}catch(Exception e){showError("Não foi possível ler o histórico local.");}}
    private void save(){
        if(scanning||busy||currentQr.isEmpty())return;if(digits.isEmpty()){showError("Digite a quantidade de sementes.");return;}
        UserStore.User user=users.current();if(user==null){showUsersDialog();return;}
        int n=Integer.parseInt(digits);try{store.save(currentQr,n,base,commentDraft,user.name);message.setTextColor(MUTED);message.setText(n+" sementes salvas.");currentQr="";digits="";commentDraft="";scanning=true;refresh();}catch(Exception e){showError("Falha ao salvar. A contagem continua na tela. "+safe(e));}
    }
    private void reread(){if(busy)return;Runnable resume=()->{currentQr="";digits="";commentDraft="";scanning=true;message.setText("");refresh();ensureCameraPermission();};if(!digits.isEmpty()||!commentDraft.isEmpty())dialog("Reler QR Code","A contagem ou o comentário ainda não foi salvo.","Descartar e reler",resume);else resume.run();}
    private void acceptQr(String raw){
        if(!scanning||modalOpen||busy||!pendingFileOp.isEmpty())return;
        String qr=raw.trim();if(qr.isEmpty()||qr.length()>2048){showError("QR Code vazio ou muito longo.");return;}
        scanning=false;currentQr=qr;
        Bitmap snapshot=preview.getBitmap();if(snapshot!=null){frozen.setImageBitmap(snapshot);frozen.setVisibility(View.VISIBLE);}
        try{SeedRecord previous=store.find(qr);digits=previous==null?"":Integer.toString(previous.seeds);commentDraft=previous==null?"":previous.comment;message.setTextColor(MUTED);message.setText("");refresh();}catch(Exception e){showError("Falha ao consultar o histórico.");}
    }
    private void ensureCameraPermission(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){if(provider==null)startCamera();}else cameraPermission.launch(Manifest.permission.CAMERA);}
    private void cameraFailure(String s){cameraHint.setText("Câmera indisponível");status.setText(s);}
    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{if(destroyed)return;try{provider=future.get();Preview p=new Preview.Builder().build();p.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis analysis=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(cameraExecutor,this::analyze);provider.unbindAll();provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,analysis);refresh();}catch(Exception e){provider=null;cameraFailure("Não foi possível abrir a câmera. Toque em Reler para tentar novamente.");}},ContextCompat.getMainExecutor(this));
    }
    @androidx.annotation.OptIn(markerClass=androidx.camera.core.ExperimentalGetImage.class)
    private void analyze(ImageProxy image){
        if(!scanning||modalOpen||busy||!pendingFileOp.isEmpty()||image.getImage()==null){image.close();return;}
        try{InputImage input=InputImage.fromMediaImage(image.getImage(),image.getImageInfo().getRotationDegrees());scanner.process(input).addOnSuccessListener(codes->{if(destroyed)return;for(Barcode code:codes){String raw=code.getRawValue();if(raw!=null){acceptQr(raw);break;}}}).addOnCompleteListener(task->image.close());}catch(Exception e){image.close();}
    }
    private LinearLayout dialogBody(){LinearLayout b=vertical();b.setPadding(dp(22),dp(10),dp(22),dp(18));return b;}
    private TextView caption(String value){TextView t=text(value,12,MUTED);t.setPadding(0,dp(5),0,dp(10));return t;}
    private void action(LinearLayout body,String label,Runnable click){Button b=button(label,SOFT,NAVY);LinearLayout.LayoutParams p=lp(-1,48);p.bottomMargin=dp(8);body.addView(b,p);b.setOnClickListener(v->click.run());}
    private AlertDialog showDialog(String title,LinearLayout body){AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(body).setNegativeButton("Fechar",null).create();dialogCount++;modalOpen=true;d.setOnDismissListener(v->{dialogCount=Math.max(0,dialogCount-1);modalOpen=dialogCount>0;});d.show();d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(NAVY);return d;}
    private void showImportDialog(){
        LinearLayout body=dialogBody();body.addView(caption("Importação opcional. Você pode registrar sementes sem planilha."));
        body.addView(caption(base==null?"Nenhuma base importada":getPreferences(0).getString("baseName","Base importada")+" · "+base.rows.size()+" espigas"));
        final AlertDialog[] d=new AlertDialog[1];
        action(body,"Escolher arquivo no celular",()->{d[0].dismiss();pick("import",false);});action(body,"Escolher arquivo no OneDrive",()->{d[0].dismiss();pick("import",true);});
        action(body,"Continuar sem importar",()->{d[0].dismiss();if(base==null)return;dialog("Continuar sem base?","As contagens coletadas serão preservadas. Os dados da espiga ficarão em branco.","Continuar sem base",()->{baseFile.delete();base=null;getPreferences(0).edit().remove("baseName").remove("baseUri").apply();refresh();});});
        d[0]=showDialog("Importar dados",body);
    }
    private void showExportDialog(){
        LinearLayout body=dialogBody();body.addView(caption("Somente espigas lidas e salvas, de todos os dias, inclusive zero."));body.addView(caption(store.all().size()+" registros · QR Code, sementes, observação, usuário e data/hora da coleta."));
        final AlertDialog[] d=new AlertDialog[1];action(body,"Salvar novo arquivo",()->{d[0].dismiss();pick("new",false);});action(body,"Escolher arquivo no OneDrive",()->{d[0].dismiss();pick("existing",true);});
        body.addView(caption("Os dados da espiga são incluídos quando disponíveis."));
        Button clear=button("Limpar dados coletados",Color.WHITE,Color.rgb(160,45,45));body.addView(clear,lp(-1,48));clear.setOnClickListener(v->{d[0].dismiss();confirmClear();});
        d[0]=showDialog("Exportar Excel",body);
    }
    private void confirmClear(){int count=store.all().size();dialog("Limpar dados coletados?","Serão apagadas todas as "+count+" contagens e suas datas do aplicativo. Esta ação não pode ser desfeita. A base importada e os arquivos Excel já salvos serão preservados.","Apagar contagens",()->{try{store.clear();currentQr="";digits="";commentDraft="";scanning=true;refresh();message.setText("Dados coletados apagados.");}catch(Exception e){showError("Não foi possível apagar os dados.");}});}
    private void dialog(String title,String msg,String positive,Runnable action){dialogCount++;modalOpen=true;AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setNegativeButton("Cancelar",null).setPositiveButton(positive,(v,w)->action.run()).create();d.setOnDismissListener(v->{dialogCount=Math.max(0,dialogCount-1);modalOpen=dialogCount>0;});d.show();d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(NAVY);d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(NAVY);}
    private void pick(String op,boolean cloud){
        if(!op.equals("import")&&store.all().isEmpty()){showError("Nenhum registro coletado para exportar.");return;}
        Intent intent=new Intent(op.equals("new")?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(ExcelFile.MIME);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(!op.equals("import"))intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(op.equals("new"))intent.putExtra(Intent.EXTRA_TITLE,"Crossing-Block-Seeds-"+LocalDate.now(Workbook.ZONE)+".xlsx");
        pendingFileOp=op;if(cloud)Toast.makeText(this,"No menu de locais, escolha OneDrive.",Toast.LENGTH_LONG).show();
        try{filePicker.launch(intent);}catch(ActivityNotFoundException e){pendingFileOp="";showError("Nenhum seletor de arquivos disponível neste aparelho.");}
    }
    private byte[] readBytes(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException("Não foi possível abrir o arquivo.");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_IMPORT_BYTES)throw new IOException("Use um arquivo de até 32 MB.");out.write(buffer,0,n);}return out.toByteArray();}}
    private String displayName(Uri uri){try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}return "Base importada.xlsx";}
    private void importBase(Uri uri){
        setBusy(true,"Importando base…");io.execute(()->{try{byte[] bytes=readBytes(uri);Workbook imported=ExcelFile.read(new ByteArrayInputStream(bytes));String name=displayName(uri);FileOutputStream stream=null;try{stream=baseFile.startWrite();stream.write(bytes);baseFile.finishWrite(stream);}catch(Exception e){if(stream!=null)baseFile.failWrite(stream);throw e;}
            getPreferences(0).edit().putString("baseName",name).putString("baseUri",uri.toString()).commit();
            runOnUiThread(()->{if(destroyed)return;base=imported;setBusy(false,"");refresh();message.setText("Base importada · "+base.rows.size()+" espigas. Contagens preservadas.");});
        }catch(Exception e){runOnUiThread(()->{if(destroyed)return;setBusy(false,"");showError("Importação não realizada. "+safe(e));});}});
    }
    private void writeNew(Uri uri){Workbook snapshot=base;List<SeedRecord> records=store.all();setBusy(true,"Exportando histórico…");io.execute(()->{try{byte[] bytes=ExcelFile.write(Workbook.export(snapshot,null,records));writeUri(uri,bytes);exportDone();}catch(Exception e){exportFailed(e);}});}
    private void prepareExisting(Uri uri){
        if(uri.toString().equals(getPreferences(0).getString("baseUri",""))){showError("Esse é o arquivo de entrada. Salve um novo arquivo para preservar a base.");return;}
        Workbook snapshot=base;List<SeedRecord> records=store.all();setBusy(true,"Lendo arquivo de saída…");io.execute(()->{try{byte[] original=readBytes(uri);Workbook existing=ExcelFile.read(new ByteArrayInputStream(original));if(existing.column(Workbook.SEEDS)<0)throw new IOException("O arquivo escolhido precisa conter Lot ID e Pega. Use Salvar novo arquivo.");byte[] bytes=ExcelFile.write(Workbook.export(snapshot,existing,records));
            runOnUiThread(()->{if(destroyed)return;setBusy(false,"");dialog("Atualizar arquivo?","As contagens deste aplicativo serão mescladas ao histórico do arquivo. Para o mesmo QR Code, a contagem e o comentário locais substituem os anteriores. O arquivo será regravado como tabela de dados.","Atualizar",()->{setBusy(true,"Atualizando arquivo…");io.execute(()->{try{if(!Arrays.equals(original,readBytes(uri)))throw new IOException("O arquivo foi alterado desde a seleção. Escolha-o novamente.");writeUri(uri,bytes);exportDone();}catch(Exception e){exportFailed(e);}});});});
        }catch(Exception e){exportFailed(e);}});
    }
    private void writeUri(Uri uri,byte[] bytes)throws Exception{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("O provedor não liberou a gravação.");out.write(bytes);out.flush();}}
    private void exportDone(){runOnUiThread(()->{if(destroyed)return;setBusy(false,"");message.setTextColor(MUTED);message.setText("Excel salvo com o histórico completo.");Toast.makeText(this,"Excel salvo",Toast.LENGTH_SHORT).show();});}
    private void exportFailed(Exception e){runOnUiThread(()->{if(destroyed)return;setBusy(false,"");showError("Falha ao exportar. O histórico local foi preservado. "+safe(e));});}
    private void setBusy(boolean value,String msg){busy=value;refresh();if(!msg.isEmpty())message.setText(msg);}
    private String safe(Exception e){String s=e.getMessage();return s==null?"Tente novamente.":s;}
    private void showError(String s){if(message==null)return;message.setTextColor(Color.rgb(160,45,45));message.setText(s);}
    private final class Reticle extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Reticle(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){float availableHeight=Math.max(0,getHeight()-dp(30)),side=Math.max(0,Math.min(getWidth()*.46f,availableHeight*.65f)),x=(getWidth()-side)/2,y=(availableHeight-side)/2,corner=Math.min(dp(24),side*.24f);paint.setColor(LIME);paint.setStrokeWidth(dp(2));for(int a=0;a<2;a++)for(int b=0;b<2;b++){float px=x+a*side,py=y+b*side;canvas.drawLine(px,py,px+(a==0?corner:-corner),py,paint);canvas.drawLine(px,py,px,py+(b==0?corner:-corner),paint);}}
    }
    private static final class FileIcon extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final boolean export;
        FileIcon(boolean export){this.export=export;}
        @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(LIME);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path doc=new Path();doc.moveTo(13,2);doc.lineTo(5,2);doc.lineTo(5,22);doc.lineTo(19,22);doc.lineTo(19,8);doc.lineTo(13,2);doc.lineTo(13,8);doc.lineTo(19,8);c.drawPath(doc,p);float start=export?10:1,end=export?23:14,y=14;c.drawLine(start,y,end,y,p);float head=export?end:end;c.drawLine(head-4,y-4,head,y,p);c.drawLine(head-4,y+4,head,y,p);c.restore();}
        @Override public void setAlpha(int alpha){p.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static final class UserIcon extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(LIME);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);c.drawCircle(12,7,4,p);c.drawArc(4,14,20,28,180,180,false,p);c.restore();}
        @Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
