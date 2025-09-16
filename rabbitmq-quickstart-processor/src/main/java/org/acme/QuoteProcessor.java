package org.acme;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;

/**
 * Processorアプリケーションのメインクラス
 * RabbitMQから見積もり依頼を受信し、価格を計算して結果を送信する
 */
@ApplicationScoped
public class QuoteProcessor {
    
    private static final Random RANDOM = new Random();
    
    // 見積もり結果を送信するEmitter
    @Inject
    @Channel("quotes")
    Emitter<Quote> quoteEmitter;
    
    /**
     * RabbitMQから見積もり依頼を受信し、価格を計算して結果を送信する
     * 
     * @param jsonMessage 受信したJSONメッセージ
     */
    @Incoming("quote-requests")
    public void processQuoteRequest(JsonObject jsonMessage) {
        Log.info("JSONメッセージを受信: " + jsonMessage);
        
        try {
            // JSONからQuoteRequestオブジェクトを作成
            QuoteRequest request = new QuoteRequest(
                UUID.fromString(jsonMessage.getString("id")),
                jsonMessage.getString("product")
            );
            
            Log.info("見積もり依頼を処理: " + request);
            
            // ランダムな価格を生成
            BigDecimal price = generateRandomPrice(request.getProduct());
            
            // 見積もり結果を作成
            Quote quote = Quote.fromRequest(request, price);
            
            Log.info("見積もり結果を生成: " + quote);
            
            // RabbitMQに見積もり結果を送信
            quoteEmitter.send(quote);
            
            Log.info("見積もり結果をRabbitMQに送信: " + quote);
            
        } catch (Exception e) {
            Log.error("見積もり依頼の処理中にエラーが発生: " + e.getMessage(), e);
        }
    }
    
    /**
     * 商品名に基づいてランダムな価格を生成する
     * 実際のアプリケーションでは、データベースや外部APIから価格を取得する
     * 
     * @param product 商品名
     * @return 生成された価格
     */
    private BigDecimal generateRandomPrice(String product) {
        // 商品名の文字数に基づいて基本価格を決定
        int basePrice = product.length() * 100;
        
        // ランダムな変動を追加（±50%）
        double variation = 0.5 + RANDOM.nextDouble(); // 0.5 から 1.5 の範囲
        double finalPrice = basePrice * variation;
        
        // BigDecimalに変換して小数点以下2桁で丸める
        return BigDecimal.valueOf(finalPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
