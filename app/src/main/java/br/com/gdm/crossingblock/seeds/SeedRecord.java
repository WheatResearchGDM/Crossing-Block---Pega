package br.com.gdm.crossingblock.seeds;

public final class SeedRecord {
    public final java.util.Map<String,Workbook.Cell> attributes;
    public final String qr, collectedAt, updatedAt;
    public final int seeds;
    public SeedRecord(String qr, int seeds, String collectedAt, String updatedAt) {
        this(qr,seeds,collectedAt,updatedAt,java.util.Collections.emptyMap());
    }
    public SeedRecord(String qr, int seeds, String collectedAt, String updatedAt,java.util.Map<String,Workbook.Cell> attributes) {
        if (qr == null || qr.trim().isEmpty()) throw new IllegalArgumentException("QR Code vazio.");
        if (qr.length() > 2048) throw new IllegalArgumentException("QR Code muito longo.");
        if (seeds < 0 || seeds > 99) throw new IllegalArgumentException("Quantidade inválida.");
        this.qr = qr.trim(); this.seeds = seeds;
        this.collectedAt = collectedAt; this.updatedAt = updatedAt;
        this.attributes=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(attributes));
    }
}
