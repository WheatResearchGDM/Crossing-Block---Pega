package br.com.gdm.crossingblock.seeds;
import java.time.*;
import java.util.*;
public final class Summary {
    public final int todayCount,totalCount;
    public final double todayMean,totalMean;
    public Summary(List<SeedRecord> records,LocalDate today){
        int count=0,sum=0,total=0;
        for(SeedRecord r:records){total+=r.seeds;if(Instant.parse(r.collectedAt).atZone(Workbook.ZONE).toLocalDate().equals(today)){count++;sum+=r.seeds;}}
        todayCount=count;totalCount=records.size();todayMean=count==0?Double.NaN:(double)sum/count;totalMean=records.isEmpty()?Double.NaN:(double)total/records.size();
    }
}
