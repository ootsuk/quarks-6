package org.acme;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 見積もり情報を表現するクラス
 * RabbitMQで送受信されるメッセージの形式を定義
 */
public class Quote {
    
    // 見積もりID（一意の識別子）
    private UUID id;
    
    // 見積もり依頼のID（どの依頼に対する見積もりか）
    private UUID requestId;
    
    // 商品名
    private String product;
    
    // 価格
    private BigDecimal price;
    
    // 見積もり作成日時
    private Instant timestamp;
    
    // デフォルトコンストラクタ（JSONシリアライゼーション用）
    public Quote() {}
    
    // コンストラクタ
    public Quote(UUID id, UUID requestId, String product, BigDecimal price, Instant timestamp) {
        this.id = id;
        this.requestId = requestId;
        this.product = product;
        this.price = price;
        this.timestamp = timestamp;
    }
    
    // 見積もり依頼から見積もりを作成するファクトリメソッド
    public static Quote fromRequest(QuoteRequest request, BigDecimal price) {
        return new Quote(
            UUID.randomUUID(),  // 新しい見積もりIDを生成
            request.getId(),    // 元の依頼IDを保持
            request.getProduct(), // 商品名をコピー
            price,              // 計算された価格
            Instant.now()       // 現在時刻
        );
    }
    
    // Getter/Setter
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getRequestId() {
        return requestId;
    }
    
    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
    
    public String getProduct() {
        return product;
    }
    
    public void setProduct(String product) {
        this.product = product;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Quote{" +
                "id=" + id +
                ", requestId=" + requestId +
                ", product='" + product + '\'' +
                ", price=" + price +
                ", timestamp=" + timestamp +
                '}';
    }
}
