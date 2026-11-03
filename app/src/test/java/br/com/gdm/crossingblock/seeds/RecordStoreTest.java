package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class RecordStoreTest {
    private Context context;
    @Before public void reset(){context=RuntimeEnvironment.getApplication();context.deleteDatabase("crossing-block.db");}
    @Test public void upgradeKeepsOldCountsDatesAndAttributes(){
        SQLiteDatabase old=context.openOrCreateDatabase("crossing-block.db",0,null);
        old.execSQL("CREATE TABLE records(qr TEXT PRIMARY KEY,seeds INTEGER,collected_at TEXT,updated_at TEXT,attributes TEXT)");
        String at="2026-11-02T03:14:15Z";
        old.execSQL("INSERT INTO records VALUES(?,?,?,?,?)",new Object[]{"0007",0,at,at,"{\"Female origin\":{\"v\":\"3\",\"n\":true,\"d\":false}}"});old.setVersion(1);old.close();
        RecordStore store=new RecordStore(context);
        SeedRecord migrated=store.find("0007");assertEquals(0,migrated.seeds);assertEquals(at,migrated.collectedAt);assertEquals("3",migrated.attributes.get("Female origin").value);assertEquals("",migrated.comment);
        store.save("0007",8,null,"Comentário após atualização.");store.close();
        store=new RecordStore(context);SeedRecord saved=store.find("0007");assertEquals(8,saved.seeds);assertEquals(at,saved.collectedAt);assertEquals("Comentário após atualização.",saved.comment);assertEquals("3",saved.attributes.get("Female origin").value);store.close();
    }
    @Test public void commentsAreIndependentForEachQrAndSurviveReopening(){
        RecordStore store=new RecordStore(context);store.save("a",0,null,"Primeira espiga\nCom acento: germinação.");store.save("b",99,null,"Segunda espiga");String collectedAt=store.find("a").collectedAt;store.close();
        store=new RecordStore(context);assertEquals("Primeira espiga\nCom acento: germinação.",store.find("a").comment);assertEquals("Segunda espiga",store.find("b").comment);
        store.save("a",1,null,"");assertEquals("",store.find("a").comment);assertEquals(collectedAt,store.find("a").collectedAt);assertEquals("Segunda espiga",store.find("b").comment);assertEquals(2,store.all().size());store.close();
    }
    @Test public void optionalCommentsAndLegacySaveDoNotEraseNotes(){RecordStore store=new RecordStore(context);store.save("a",5,null,"Nota");store.save("a",6,null);store.save("b",0,null);assertEquals("Nota",store.find("a").comment);assertEquals("",store.find("b").comment);store.close();}
}
