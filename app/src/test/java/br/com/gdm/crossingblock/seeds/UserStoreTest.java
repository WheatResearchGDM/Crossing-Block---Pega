package br.com.gdm.crossingblock.seeds;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class UserStoreTest {
    private Context context;
    @Before public void reset(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("collectors",0).edit().clear().commit();}
    @Test public void addChooseRenameAndReopen(){UserStore users=new UserStore(context);assertNull(users.current());UserStore.User a=users.add(" Natália "),b=users.add("Carlos");users.select(a.id);users.rename(a.id,"Natália Silva");users=new UserStore(context);assertEquals(a.id,users.current().id);assertEquals("Natália Silva",users.current().name);users.select(b.id);assertEquals("Carlos",new UserStore(context).current().name);assertEquals(2,users.all().size());}
    @Test public void duplicateAndEmptyNamesAreRejectedWithoutChangingSelection(){UserStore users=new UserStore(context);UserStore.User a=users.add("Natália");users.select(a.id);for(String name:new String[]{"","  ","natália","x".repeat(81)}){try{users.add(name);fail();}catch(IllegalArgumentException expected){}}assertEquals(1,new UserStore(context).all().size());assertEquals(a.id,new UserStore(context).current().id);}
}
