package br.com.gdm.crossingblock.seeds;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.*;

public class WorkbookTest {
    private SeedRecord record(String qr,int seeds,String at){return new SeedRecord(qr,seeds,at,at);}
    private Workbook base(){Workbook b=new Workbook();b.headers.addAll(Workbook.STANDARD);return b;}
    private void row(Workbook b,String id,String female,String male,String direction){LinkedHashMap<String,Workbook.Cell> r=new LinkedHashMap<>();r.put("Lot ID",Workbook.Cell.text(id));r.put("Female origin",Workbook.Cell.text(female));r.put("Male origin",Workbook.Cell.text(male));r.put("Sentido",Workbook.Cell.text(direction));b.rows.put(id,r);}
    @Test public void directInverseAndBlankDirection(){Workbook b=base();row(b,"a","6","10","Inverso");row(b,"b","3","4","");row(b,"c","3","4","Direto");assertArrayEquals(new String[]{"10","6"},b.parents("a"));assertArrayEquals(new String[]{"3","4"},b.parents("b"));assertArrayEquals(new String[]{"3","4"},b.parents("c"));}
    @Test public void noBasePreservesQrZeroAndTimestamp()throws Exception{
        String at="2026-11-02T02:14:54Z";Workbook output=Workbook.export(null,null,Arrays.asList(record("000123",0,at),record("999999",99,at)));
        Workbook loaded=ExcelFile.read(new ByteArrayInputStream(ExcelFile.write(output)));
        assertEquals(2,loaded.rows.size());assertEquals("0",loaded.get("000123","Pega"));assertEquals("",loaded.get("000123","Female origin"));assertEquals("01/11/2026 23:14:54 -03:00",loaded.get("000123",Workbook.TIME));assertEquals("99",loaded.get("999999","Pega"));assertTrue(loaded.rows.get("000123").get("Pega").number);assertFalse(loaded.rows.get("000123").get("Lot ID").number);
    }
    @Test public void exportIncludesCollectedZeroAndUnknownQrButExcludesUncollectedBaseRows()throws Exception{
        Workbook b=base();row(b,"1","3","4","");row(b,"2","6","10","Inverso");Workbook output=Workbook.export(b,null,List.of(record("1",0,"2026-11-01T03:00:00Z"),record("unknown",5,"2026-11-02T03:00:00Z")));
        Workbook loaded=ExcelFile.read(new ByteArrayInputStream(ExcelFile.write(output)));
        assertEquals(2,loaded.rows.size());assertEquals("0",loaded.get("1","Pega"));assertEquals("3",loaded.get("1","Female origin"));assertFalse(loaded.rows.containsKey("2"));assertEquals("5",loaded.get("unknown","Pega"));assertEquals("",loaded.get("unknown","Female origin"));
    }
    @Test public void mergingOldOutputOmitsBlankRowsAndKeepsCollectedZero(){Workbook old=base();old.headers.add(Workbook.SEEDS);row(old,"empty","3","4","");row(old,"zero","6","10","");old.rows.get("zero").put(Workbook.SEEDS,Workbook.Cell.text("0"));Workbook output=Workbook.export(base(),old,List.of(record("new",2,"2026-11-02T03:00:00Z")));assertEquals(2,output.rows.size());assertFalse(output.rows.containsKey("empty"));assertEquals("0",output.get("zero",Workbook.SEEDS));}
    @Test public void loadedBaseDoesNotCountAsCollectedHistory(){Workbook b=base();row(b,"not-collected","3","4","");assertTrue(Workbook.export(b,null,List.of()).rows.isEmpty());}
    @Test public void allDaysAndZeroCountsContributeToRightSummary(){
        List<SeedRecord> records=Arrays.asList(record("a",0,"2026-11-02T02:59:59Z"),record("b",10,"2026-11-02T03:00:00Z"),record("c",20,"2026-11-02T04:00:00Z"));Summary s=new Summary(records,LocalDate.of(2026,11,2));assertEquals(2,s.todayCount);assertEquals(3,s.totalCount);assertEquals(15,s.todayMean,0);assertEquals(10,s.totalMean,0);
        Summary empty=new Summary(List.of(),LocalDate.of(2026,11,2));assertTrue(Double.isNaN(empty.todayMean));assertTrue(Double.isNaN(empty.totalMean));
    }
    @Test public void existingOutputRetainsOtherDaysAndLocalOverridesCurrentQr(){
        Workbook b=base();row(b,"1","3","4","");Workbook prior=Workbook.export(b,null,List.of(record("old",7,"2026-10-30T12:00:00Z"),record("1",1,"2026-10-30T12:00:00Z")));
        Workbook merged=Workbook.export(b,prior,List.of(record("1",9,"2026-11-02T03:00:00Z")));assertEquals("7",merged.get("old","Pega"));assertEquals("9",merged.get("1","Pega"));assertEquals(2,merged.rows.size());
    }
    @Test public void metadataSurvivesChangingOrRemovingBase(){
        Workbook b=base();row(b,"1","3","4","");SeedRecord record=new SeedRecord("1",2,"2026-11-02T03:00:00Z","2026-11-02T04:00:00Z",b.rows.get("1"));Workbook output=Workbook.export(null,null,List.of(record));assertEquals("3",output.get("1","Female origin"));assertEquals("02/11/2026 01:00:00 -03:00",output.get("1",Workbook.UPDATED));
    }
    @Test public void datesAndFormulaLikeTextRoundTripAsData()throws Exception{
        Workbook b=base();row(b,"=2+2","3","4","");b.rows.get("=2+2").put("Lot_name",Workbook.Cell.text("<Espiga> & \"teste\""));b.rows.get("=2+2").put("Emasculation date",new Workbook.Cell("46314",true,true));Workbook loaded=ExcelFile.read(new ByteArrayInputStream(ExcelFile.write(b)));assertTrue(loaded.rows.containsKey("=2+2"));assertEquals("<Espiga> & \"teste\"",loaded.get("=2+2","Lot_name"));assertEquals("46314",loaded.rows.get("=2+2").get("Emasculation date").value);assertTrue(loaded.rows.get("=2+2").get("Emasculation date").date);
    }
    private byte[] duplicateZip()throws Exception{
        Workbook b=base();row(b,"1","3","4","");byte[] input=ExcelFile.write(b);ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(input));ZipOutputStream zip=new ZipOutputStream(out)){ZipEntry e;while((e=in.getNextEntry())!=null){byte[] data=in.readAllBytes();if(e.getName().endsWith("sheet1.xml")){String s=new String(data,StandardCharsets.UTF_8);String row=s.substring(s.indexOf("<row r=\"2\""),s.indexOf("</row>",s.indexOf("<row r=\"2\""))+6);s=s.replace("</sheetData>",row+"</sheetData>");data=s.getBytes(StandardCharsets.UTF_8);}zip.putNextEntry(new ZipEntry(e.getName()));zip.write(data);zip.closeEntry();}}return out.toByteArray();
    }
    @Test public void duplicateInputIdsRejected()throws Exception{try{ExcelFile.read(new ByteArrayInputStream(duplicateZip()));fail("Should reject duplicates");}catch(IOException e){assertTrue(e.getMessage().contains("duplicado"));}}
    @Test public void invalidQuantitiesRejected(){for(int n:new int[]{-1,100}){try{record("x",n,"2026-11-02T00:00:00Z");fail();}catch(IllegalArgumentException expected){}}}
    @Test public void commentsExportAsLiteralMultilineTextAndRemainAssociated()throws Exception{
        String at="2026-11-02T03:00:00Z",note="=Espiga <boa> & germinação\nSegunda linha.";
        SeedRecord a=new SeedRecord("0001",0,at,at,Map.of(),note),b=new SeedRecord("0002",99,at,at,Map.of(),"Outra observação");
        Workbook out=Workbook.export(null,null,List.of(a,b));Workbook loaded=ExcelFile.read(new ByteArrayInputStream(ExcelFile.write(out)));
        assertEquals(note,loaded.get("0001",Workbook.COMMENT));assertEquals("Outra observação",loaded.get("0002",Workbook.COMMENT));assertFalse(loaded.rows.get("0001").get(Workbook.COMMENT).number);assertEquals(2,loaded.rows.size());
        Workbook merged=Workbook.export(null,loaded,List.of(new SeedRecord("0001",2,at,at,Map.of(),"")));assertEquals("",merged.get("0001",Workbook.COMMENT));assertEquals("Outra observação",merged.get("0002",Workbook.COMMENT));
    }
    @Test public void userAndObservationRoundTripAndOldCommentHeaderMerges()throws Exception{
        String at="2026-11-02T03:00:00Z";Workbook old=base();old.headers.addAll(List.of(Workbook.SEEDS,"Comentário"));row(old,"old","3","4","");old.rows.get("old").put(Workbook.SEEDS,Workbook.Cell.text("0"));old.rows.get("old").put("Comentário",Workbook.Cell.text("Nota legada"));
        Workbook out=Workbook.export(null,old,List.of(new SeedRecord("new",12,at,at,Map.of(),"Observação atual","Natália")));Workbook read=ExcelFile.read(new ByteArrayInputStream(ExcelFile.write(out)));assertEquals("Natália",read.get("new",Workbook.USER));assertEquals("Observação atual",read.get("new",Workbook.COMMENT));assertEquals("Nota legada",read.get("old",Workbook.COMMENT));assertEquals(-1,read.column("Comentário"));assertEquals("",read.get("old",Workbook.USER));assertEquals(List.of(Workbook.ID,"Lot_name",Workbook.SEEDS,Workbook.COMMENT,Workbook.USER,Workbook.TIME,Workbook.UPDATED),read.headers.subList(0,7));assertEquals(2,read.rows.size());
    }
}
