package br.com.gdm.crossingblock.seeds;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import static br.com.gdm.crossingblock.seeds.Workbook.*;

/** Small OOXML reader/writer for tabular .xlsx files. No Excel automation or network. */
public final class ExcelFile {
    public static final String MIME="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int MAX_XML=32*1024*1024,MAX_TOTAL=96*1024*1024,MAX_ROWS=100000,MAX_COLUMNS=256;
    private static Document xml(byte[] bytes)throws Exception{
        String rawXml=new String(bytes,StandardCharsets.UTF_8);
        if(rawXml.indexOf('\0')>=0||rawXml.contains("<!DOCTYPE"))throw new IOException("Use XML UTF-8 sem definições DTD na planilha.");
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
        for(String feature:new String[]{"http://apache.org/xml/features/disallow-doctype-decl","http://xml.org/sax/features/external-general-entities","http://xml.org/sax/features/external-parameter-entities"}){
            try{f.setFeature(feature,feature.endsWith("disallow-doctype-decl"));}catch(ParserConfigurationException ignored){/* Android parser rejects unknown features; DTDs are rejected above. */}
        }
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
    private static NodeList tags(Element e,String name){return e.getElementsByTagNameNS("*",name);}
    private static String text(Element e,String name){NodeList list=tags(e,name);return list.getLength()==0?"":list.item(0).getTextContent();}
    private static String richText(Element e){StringBuilder b=new StringBuilder();NodeList list=tags(e,"t");for(int i=0;i<list.getLength();i++)b.append(list.item(i).getTextContent());return b.toString();}
    private static Map<String,byte[]> unzip(InputStream stream)throws IOException{
        Map<String,byte[]> entries=new HashMap<>();int total=0,count=0;
        try(ZipInputStream zip=new ZipInputStream(stream)){
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){
                if(++count>10000)throw new IOException("Arquivo Excel possui entradas demais.");
                String n=entry.getName();
                boolean keep=n.equals("xl/workbook.xml")||n.equals("xl/_rels/workbook.xml.rels")||n.equals("xl/sharedStrings.xml")||n.equals("xl/styles.xml")||n.matches("xl/worksheets/[^/]+\\.xml");
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int read;
                int entryBytes=0;while((read=zip.read(buf))!=-1){total+=read;entryBytes+=read;if(total>MAX_TOTAL||entryBytes>MAX_XML)throw new IOException("Planilha muito grande. Limite de 32 MB por folha.");if(keep)out.write(buf,0,read);}
                if(keep)entries.put(n,out.toByteArray());
            }
        }
        return entries;
    }
    public static Workbook read(InputStream stream)throws Exception{
        Map<String,byte[]> files=unzip(stream);
        if(!files.containsKey("xl/workbook.xml"))throw new IOException("Escolha um arquivo Excel .xlsx válido.");
        Element book=xml(files.get("xl/workbook.xml")).getDocumentElement();
        NodeList sheets=tags(book,"sheet");
        if(sheets.getLength()!=1)throw new IOException("Use uma planilha com uma única aba de dados. Nenhuma aba foi alterada.");
        Element sheet=(Element)sheets.item(0);String sheetPath="xl/worksheets/sheet1.xml";
        if(files.containsKey("xl/_rels/workbook.xml.rels")){
            NodeList rels=tags(xml(files.get("xl/_rels/workbook.xml.rels")).getDocumentElement(),"Relationship");
            String id=sheet.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");
            for(int i=0;i<rels.getLength();i++){Element r=(Element)rels.item(i);if(r.getAttribute("Id").equals(id)){String p=r.getAttribute("Target");sheetPath=p.startsWith("/")?p.substring(1):"xl/"+p;}}
        }
        if(!files.containsKey(sheetPath))throw new IOException("A aba de dados não foi encontrada.");
        boolean date1904=false;NodeList props=tags(book,"workbookPr");
        if(props.getLength()>0){String v=((Element)props.item(0)).getAttribute("date1904");date1904=v.equals("1")||v.equals("true");}
        List<String> strings=new ArrayList<>();
        if(files.containsKey("xl/sharedStrings.xml")){NodeList si=tags(xml(files.get("xl/sharedStrings.xml")).getDocumentElement(),"si");for(int i=0;i<si.getLength();i++)strings.add(richText((Element)si.item(i)));}
        List<Boolean> dates=new ArrayList<>();
        if(files.containsKey("xl/styles.xml")){
            Element styles=xml(files.get("xl/styles.xml")).getDocumentElement();Map<String,String> formats=new HashMap<>();
            NodeList nums=tags(styles,"numFmt");for(int i=0;i<nums.getLength();i++){Element n=(Element)nums.item(i);formats.put(n.getAttribute("numFmtId"),n.getAttribute("formatCode"));}
            NodeList cellXfs=tags(styles,"cellXfs");if(cellXfs.getLength()>0){NodeList xfs=tags((Element)cellXfs.item(0),"xf");for(int i=0;i<xfs.getLength();i++){
                String fmt=((Element)xfs.item(i)).getAttribute("numFmtId");int n=fmt.isEmpty()?0:Integer.parseInt(fmt);
                String code=formats.getOrDefault(fmt,"").replaceAll("\"[^\"]*\"|\\\\.|\\[[^\\]]*\\]","").toLowerCase(Locale.ROOT);
                dates.add(n>=14&&n<=22||n>=45&&n<=47||code.matches(".*[yd].*"));
            }}
        }
        Workbook result=new Workbook();result.sheetName=sheet.getAttribute("name");
        Element data=xml(files.get(sheetPath)).getDocumentElement();NodeList rows=tags(data,"row");
        if(rows.getLength()>MAX_ROWS+1)throw new IOException("Limite de 100.000 linhas excedido.");
        int idCol=-1;
        for(int ri=0;ri<rows.getLength();ri++){
            Element row=(Element)rows.item(ri);TreeMap<Integer,Cell> cells=new TreeMap<>();NodeList cs=tags(row,"c");int next=0;
            for(int ci=0;ci<cs.getLength();ci++){
                Element c=(Element)cs.item(ci);String ref=c.getAttribute("r");int col=ref.isEmpty()?next:columnIndex(ref);next=col+1;if(col>=MAX_COLUMNS)throw new IOException("Limite de 256 colunas excedido.");
                String type=c.getAttribute("t"),v=text(c,"v");boolean number=false,date=false;
                if(type.equals("s")){int index=Integer.parseInt(v);v=strings.get(index);}
                else if(type.equals("inlineStr"))v=richText(c);
                else if(type.equals("b"))v=v.equals("1")?"TRUE":"FALSE";
                else if(type.equals("e"))throw new IOException("Há uma célula com erro na linha "+(ri+1)+". Corrija no Excel e importe novamente.");
                else if(!type.equals("str")&&!type.equals("d")&&!v.isEmpty()){
                    try{v=new BigDecimal(v).stripTrailingZeros().toPlainString();number=true;}catch(NumberFormatException ignored){}
                    String s=c.getAttribute("s");int style=s.isEmpty()?0:Integer.parseInt(s);date=number&&style<dates.size()&&dates.get(style);
                    if(date&&date1904)v=new BigDecimal(v).add(new BigDecimal(1462)).toPlainString();
                }
                if(v.length()>32767)throw new IOException("Célula com texto muito longo.");
                cells.put(col,new Cell(v,number,date));
            }
            if(cells.values().stream().allMatch(v->v.value.trim().isEmpty()))continue;
            if(result.headers.isEmpty()){
                int last=cells.lastKey();Set<String> seen=new HashSet<>();
                for(int i=0;i<=last;i++){String h=cells.getOrDefault(i,Cell.text("")).value.replace("\uFEFF","").trim();if(h.isEmpty())h="Coluna "+(i+1);if(!seen.add(h.toLowerCase(Locale.ROOT)))throw new IOException("Cabeçalho duplicado: "+h);result.headers.add(h);}
                idCol=result.column(ID);if(idCol<0)throw new IOException("A primeira linha precisa conter a coluna Lot ID.");continue;
            }
            if(!cells.isEmpty()&&cells.lastKey()>=result.headers.size())throw new IOException("Há dados sem cabeçalho na linha "+(ri+1)+".");
            String key=cells.getOrDefault(idCol,Cell.text("")).value.trim();
            if(key.isEmpty())throw new IOException("Lot ID vazio na linha "+(ri+1)+".");
            if(result.rows.containsKey(key))throw new IOException("Lot ID duplicado: "+key+". Corrija a base antes de importar.");
            LinkedHashMap<String,Cell> mapped=new LinkedHashMap<>();for(int i=0;i<result.headers.size();i++)mapped.put(result.headers.get(i),cells.getOrDefault(i,Cell.text("")));
            mapped.put(result.headers.get(idCol),Cell.text(key));result.rows.put(key,mapped);
        }
        if(idCol<0)throw new IOException("Planilha sem cabeçalho Lot ID.");
        return result;
    }
    private static int columnIndex(String ref){int v=0;for(char c:ref.toUpperCase(Locale.ROOT).toCharArray()){if(c<'A'||c>'Z')break;v=v*26+c-'A'+1;}return v-1;}
    private static String columnName(int index){StringBuilder b=new StringBuilder();for(int n=index+1;n>0;n=(n-1)/26)b.insert(0,(char)('A'+(n-1)%26));return b.toString();}
    private static String escape(String s){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<32&&c!='\n'&&c!='\r'&&c!='\t')continue;switch(c){case '&':b.append("&amp;");break;case '<':b.append("&lt;");break;case '>':b.append("&gt;");break;case '"':b.append("&quot;");break;default:b.append(c);}}return b.toString();}
    private static void entry(ZipOutputStream zip,String name,String content)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    public static byte[] write(Workbook book)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){
            entry(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Coletas\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(zip,"xl/styles.xml","<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/></numFmts><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF09243B\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"4\"><xf fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/><xf fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/><xf fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment wrapText=\"1\" vertical=\"top\"/></xf></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
            StringBuilder s=new StringBuilder("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols>");
            for(int i=0;i<book.headers.size();i++)s.append("<col min=\"").append(i+1).append("\" max=\"").append(i+1).append("\" width=\"").append(book.headers.get(i).equalsIgnoreCase(Workbook.COMMENT)?42:book.headers.get(i).startsWith("Data/hora")?30:22).append("\" customWidth=\"1\"/>");
            s.append("</cols><sheetData><row r=\"1\">");for(int i=0;i<book.headers.size();i++)cellXml(s,columnName(i)+"1",Cell.text(book.headers.get(i)),2);s.append("</row>");
            int row=2;for(Map<String,Cell> values:book.rows.values()){s.append("<row r=\"").append(row).append("\">");for(int i=0;i<book.headers.size();i++){Cell c=values.getOrDefault(book.headers.get(i),Cell.text(""));cellXml(s,columnName(i)+row,c,book.headers.get(i).equalsIgnoreCase(Workbook.COMMENT)?3:c.date?1:0);}s.append("</row>");row++;}
            s.append("</sheetData><autoFilter ref=\"A1:").append(columnName(book.headers.size()-1)).append(Math.max(1,row-1)).append("\"/></worksheet>");entry(zip,"xl/worksheets/sheet1.xml",s.toString());
        }return out.toByteArray();
    }
    private static void cellXml(StringBuilder s,String ref,Cell cell,int style){s.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"");if(cell.number&&!cell.value.isEmpty()){s.append("><v>").append(escape(cell.value)).append("</v></c>");}else{s.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(escape(cell.value)).append("</t></is></c>");}}
}
