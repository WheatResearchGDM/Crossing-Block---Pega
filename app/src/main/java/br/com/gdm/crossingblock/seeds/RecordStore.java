package br.com.gdm.crossingblock.seeds;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.Instant;
import java.util.*;
import org.json.*;

/** SQLite transactions survive process death; export never removes local history. */
public final class RecordStore extends SQLiteOpenHelper {
    public RecordStore(Context c){super(c,"crossing-block.db",null,2);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE records(qr TEXT PRIMARY KEY NOT NULL,seeds INTEGER NOT NULL CHECK(seeds BETWEEN 0 AND 99),collected_at TEXT NOT NULL,updated_at TEXT NOT NULL,attributes TEXT NOT NULL DEFAULT '{}',comment TEXT NOT NULL DEFAULT '')");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2)db.execSQL("ALTER TABLE records ADD COLUMN comment TEXT NOT NULL DEFAULT ''");}
    public synchronized SeedRecord find(String qr){try(Cursor c=getReadableDatabase().query("records",null,"qr=?",new String[]{qr},null,null,null)){return c.moveToFirst()?from(c):null;}}
    private SeedRecord from(Cursor c){
        Map<String,Workbook.Cell> attrs=new LinkedHashMap<>();
        try{JSONObject obj=new JSONObject(c.getString(c.getColumnIndexOrThrow("attributes")));Iterator<String> keys=obj.keys();while(keys.hasNext()){String h=keys.next();JSONObject cell=obj.getJSONObject(h);attrs.put(h,new Workbook.Cell(cell.getString("v"),cell.getBoolean("n"),cell.getBoolean("d")));}}catch(JSONException e){throw new SQLiteException("Atributos do registro inválidos.");}
        return new SeedRecord(c.getString(c.getColumnIndexOrThrow("qr")),c.getInt(c.getColumnIndexOrThrow("seeds")),c.getString(c.getColumnIndexOrThrow("collected_at")),c.getString(c.getColumnIndexOrThrow("updated_at")),attrs,c.getString(c.getColumnIndexOrThrow("comment")));
    }
    public synchronized void save(String qr,int count,Workbook base){
        SeedRecord previous=find(qr);save(qr,count,base,previous==null?"":previous.comment);
    }
    public synchronized void save(String qr,int count,Workbook base,String comment){
        SeedRecord previous=find(qr);String now=Instant.now().toString();
        Map<String,Workbook.Cell> attrs=base!=null&&base.rows.containsKey(qr)?base.rows.get(qr):previous==null?Collections.emptyMap():previous.attributes;
        SeedRecord r=new SeedRecord(qr,count,previous==null?now:previous.collectedAt,now,attrs,comment);
        ContentValues values=new ContentValues();values.put("qr",r.qr);values.put("seeds",r.seeds);values.put("collected_at",r.collectedAt);values.put("updated_at",r.updatedAt);
        values.put("comment",r.comment);
        try{JSONObject obj=new JSONObject();for(Map.Entry<String,Workbook.Cell> entry:attrs.entrySet()){Workbook.Cell cell=entry.getValue();JSONObject v=new JSONObject();v.put("v",cell.value);v.put("n",cell.number);v.put("d",cell.date);obj.put(entry.getKey(),v);}values.put("attributes",obj.toString());}catch(JSONException e){throw new SQLiteException("Não foi possível preparar o registro.");}
        if(getWritableDatabase().insertWithOnConflict("records",null,values,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new SQLiteException("Não foi possível salvar a contagem.");
    }
    public synchronized List<SeedRecord> all(){List<SeedRecord> list=new ArrayList<>();try(Cursor c=getReadableDatabase().query("records",null,null,null,null,null,"collected_at ASC")){while(c.moveToNext())list.add(from(c));}return list;}
    public synchronized void clear(){getWritableDatabase().delete("records",null,null);}
}
