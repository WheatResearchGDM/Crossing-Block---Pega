package br.com.gdm.crossingblock.seeds;

import android.content.*;
import org.json.*;
import java.util.*;

/** Local collector list. Records keep a name snapshot so renaming never rewrites history. */
public final class UserStore {
    public static final class User {
        public final String id,name;
        User(String id,String name){this.id=id;this.name=name;}
        @Override public String toString(){return name;}
    }
    private final SharedPreferences preferences;
    private final List<User> users=new ArrayList<>();
    private String selected;
    public UserStore(Context context){
        preferences=context.getSharedPreferences("collectors",Context.MODE_PRIVATE);
        try{JSONArray list=new JSONArray(preferences.getString("users","[]"));for(int i=0;i<list.length();i++){JSONObject u=list.getJSONObject(i);users.add(new User(u.getString("id"),u.getString("name")));}}
        catch(JSONException e){throw new IllegalStateException("Não foi possível carregar os usuários.",e);}
        selected=preferences.getString("selected","");
    }
    public List<User> all(){return Collections.unmodifiableList(new ArrayList<>(users));}
    public User current(){for(User u:users)if(u.id.equals(selected))return u;return null;}
    private String validName(String value,String except){String name=value==null?"":value.trim();if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException("Informe um nome de até 80 caracteres.");for(User u:users)if(!u.id.equals(except)&&u.name.equalsIgnoreCase(name))throw new IllegalArgumentException("Já existe um usuário com esse nome.");return name;}
    public User add(String name){User u=new User(UUID.randomUUID().toString(),validName(name,""));List<User> next=new ArrayList<>(users);next.add(u);persist(next,selected);return u;}
    public void rename(String id,String name){String clean=validName(name,id);List<User> next=new ArrayList<>(users);for(int i=0;i<next.size();i++)if(next.get(i).id.equals(id)){next.set(i,new User(id,clean));persist(next,selected);return;}throw new IllegalArgumentException("Usuário não encontrado.");}
    public void select(String id){for(User u:users)if(u.id.equals(id)){persist(users,id);return;}throw new IllegalArgumentException("Escolha um usuário.");}
    private void persist(List<User> next,String selectedId){
        JSONArray list=new JSONArray();try{for(User u:next){JSONObject obj=new JSONObject();obj.put("id",u.id);obj.put("name",u.name);list.put(obj);}}catch(JSONException e){throw new IllegalStateException(e);}
        if(!preferences.edit().putString("users",list.toString()).putString("selected",selectedId).commit())throw new IllegalStateException("Não foi possível salvar o usuário.");
        List<User> copy=new ArrayList<>(next);users.clear();users.addAll(copy);selected=selectedId;
    }
}
