package br.com.gdm.crossingblock.seeds;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** A single worksheet, preserving source columns, blank values and numeric/date types. */
public final class Workbook {
    public static final String ID="Lot ID", SEEDS="Pega", TIME="Data/hora da coleta", UPDATED="Data/hora da alteração";
    public static final List<String> STANDARD = Arrays.asList(ID,"Lot_name","Female origin","Male origin","Sentido","Emasculation date","Emasculator1","Pollination date","Pollinator");
    public static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
    public static final class Cell {
        public final String value;
        public final boolean number, date;
        public Cell(String value, boolean number, boolean date) { this.value=value==null?"":value; this.number=number;this.date=date; }
        public static Cell text(String s){return new Cell(s,false,false);}
        public String display(){
            if(date && number) try {return LocalDate.of(1899,12,30).plusDays((long)Double.parseDouble(value)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}catch(Exception ignored){}
            return value;
        }
    }
    public final List<String> headers=new ArrayList<>();
    public final LinkedHashMap<String,LinkedHashMap<String,Cell>> rows=new LinkedHashMap<>();
    public String sheetName="Coletas";
    public int column(String name){for(int i=0;i<headers.size();i++)if(headers.get(i).equalsIgnoreCase(name))return i;return -1;}
    public String get(String qr,String name){Map<String,Cell> row=rows.get(qr);if(row==null)return "";for(String h:headers)if(h.equalsIgnoreCase(name)){Cell c=row.get(h);return c==null?"":c.display();}return "";}
    public String[] parents(String qr){
        String mother=get(qr,"Female origin"),father=get(qr,"Male origin");
        String direction=get(qr,"Sentido").trim();
        if(direction.equalsIgnoreCase("Inverso")){String swap=mother;mother=father;father=swap;}
        return new String[]{mother,father};
    }
    private static void addHeader(Workbook out,String h){if(out.column(h)<0)out.headers.add(h);}
    private static void copyRow(Workbook out,String qr,Map<String,Cell> values){
        if(values==null)return;
        LinkedHashMap<String,Cell> dst=out.rows.computeIfAbsent(qr,key->new LinkedHashMap<>());
        for(Map.Entry<String,Cell> c:values.entrySet()){
            addHeader(out,c.getKey());String h=out.headers.get(out.column(c.getKey()));
            if(!dst.containsKey(h)||dst.get(h).value.isEmpty())dst.put(h,c.getValue());
        }
    }
    public static Workbook export(Workbook base, Workbook existing,List<SeedRecord> records){
        Workbook out=new Workbook();
        if(existing!=null)for(String h:existing.headers)addHeader(out,h);
        if(base!=null)for(String h:base.headers)addHeader(out,h);
        if(out.headers.isEmpty())out.headers.addAll(STANDARD);
        addHeader(out,ID);addHeader(out,SEEDS);addHeader(out,TIME);addHeader(out,UPDATED);
        if(existing!=null)for(String qr:existing.rows.keySet()){
            String saved=existing.get(qr,SEEDS).trim();
            if(saved.isEmpty())continue;
            int count;
            try{count=Integer.parseInt(saved);if(count<0||count>99)throw new NumberFormatException();}
            catch(NumberFormatException e){throw new IllegalArgumentException("Pega inválida no arquivo de saída para Lot ID "+qr+".");}
            copyRow(out,qr,existing.rows.get(qr));
            out.rows.get(qr).put(out.headers.get(out.column(SEEDS)),new Cell(Integer.toString(count),true,false));
        }
        for(SeedRecord r:records){
            // A base supplies attributes for collected IDs only; it never creates output rows.
            if(existing!=null)copyRow(out,r.qr,existing.rows.get(r.qr));
            if(base!=null)copyRow(out,r.qr,base.rows.get(r.qr));
            LinkedHashMap<String,Cell> row=out.rows.computeIfAbsent(r.qr,key->new LinkedHashMap<>());
            for(Map.Entry<String,Cell> attr:r.attributes.entrySet()){addHeader(out,attr.getKey());row.put(out.headers.get(out.column(attr.getKey())),attr.getValue());}
            row.put(out.headers.get(out.column(ID)),Cell.text(r.qr));
            row.put(out.headers.get(out.column(SEEDS)),new Cell(Integer.toString(r.seeds),true,false));
            row.put(out.headers.get(out.column(TIME)),Cell.text(formatInstant(r.collectedAt)));
            row.put(out.headers.get(out.column(UPDATED)),Cell.text(r.updatedAt.equals(r.collectedAt)?"":formatInstant(r.updatedAt)));
        }
        return out;
    }
    public static String formatInstant(String iso){return Instant.parse(iso).atZone(ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX"));}
}
