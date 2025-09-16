package org.acme;

import java.util.UUID;

/**
 * 見積もり依頼を表現するクラス
 * ユーザーが送信する見積もり依頼の形式を定義
 */
public class QuoteRequest {
    
    // 依頼ID（一意の識別子）
    private UUID id;
    
    // 商品名
    private String product;
    
    // デフォルトコンストラクタ（JSONシリアライゼーション用）
    public QuoteRequest() {}
    
    // コンストラクタ
    public QuoteRequest(UUID id, String product) {
        this.id = id;
        this.product = product;
    }
    
    // 新しい見積もり依頼を作成するファクトリメソッド
    public static QuoteRequest create(String product) {
        return new QuoteRequest(UUID.randomUUID(), product);
    }
    
    // Getter/Setter
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getProduct() {
        return product;
    }
    
    public void setProduct(String product) {
        this.product = product;
    }
    
    @Override
    public String toString() {
        return "QuoteRequest{" +
                "id=" + id +
                ", product='" + product + '\'' +
                '}';
    }
}
