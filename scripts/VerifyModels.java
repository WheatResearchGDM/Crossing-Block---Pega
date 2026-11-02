import br.com.gdm.crossingblock.seeds.*;
import java.nio.file.*;
import java.util.*;

/** Read-only verification of the user's two models plus an export in an ignored folder. */
public class VerifyModels {
    public static void main(String[] args)throws Exception {
        Workbook input=ExcelFile.read(Files.newInputStream(Path.of(args[0])));
        Workbook template=ExcelFile.read(Files.newInputStream(Path.of(args[1])));
        if(input.rows.size()!=2299||template.rows.size()!=2299)throw new AssertionError("Unexpected source row counts");
        if(!Arrays.equals(input.parents("303307"),new String[]{"10","6"}))throw new AssertionError("Inverse parents");
        if(!Arrays.equals(input.parents("303283"),new String[]{"3","4"}))throw new AssertionError("Direct parents");
        if(!input.get("303283","Emasculation date").equals("22/10/2026"))throw new AssertionError("Date decoding: "+input.get("303283","Emasculation date"));
        SeedRecord known=new SeedRecord("303283",0,"2026-11-02T02:14:54Z","2026-11-02T02:14:54Z",input.rows.get("303283"));
        SeedRecord unknown=new SeedRecord("000999999",99,"2026-11-03T03:00:00Z","2026-11-03T03:00:00Z");
        Workbook out=Workbook.export(input,null,Arrays.asList(known,unknown));
        byte[] bytes=ExcelFile.write(out);Files.write(Path.of(args[2]),bytes);
        Workbook check=ExcelFile.read(new java.io.ByteArrayInputStream(bytes));
        if(check.rows.size()!=2300||!check.get("303283","Pega").equals("0")||!check.get("000999999","Pega").equals("99"))throw new AssertionError("Output reconciliation");
        if(!check.get("303284","Pega").isEmpty())throw new AssertionError("Uncollected row became zero");
        if(!check.get("000999999","Female origin").isEmpty())throw new AssertionError("Unknown QR has invented parents");
        System.out.println("PASS: 2,299 source rows, direct/inverse, dates, typed 0/99, unknown QR, 2,300 exported rows.");
    }
}
