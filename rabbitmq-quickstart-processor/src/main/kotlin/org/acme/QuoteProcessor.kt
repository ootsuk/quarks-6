package org.acme

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import io.vertx.core.json.JsonObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random
import java.util.UUID

/**
 * Processorアプリケーションのメインクラス
 * RabbitMQから見積もり依頼を受信し、価格を計算して結果を送信する
 */
@ApplicationScoped
class QuoteProcessor {
    
    private val random = Random()
    
    // 見積もり結果を送信するEmitter
    @Inject
    @Channel("quotes")
    lateinit var quoteEmitter: Emitter<Quote>
    
    /**
     * RabbitMQから見積もり依頼を受信し、価格を計算して結果を送信する
     * 
     * @param jsonMessage 受信したJSONメッセージ
     */
    @Incoming("quote-requests")
    fun processQuoteRequest(jsonMessage: JsonObject) {
        Log.info("JSONメッセージを受信: $jsonMessage")
        
        try {
            // JSONからQuoteRequestオブジェクトを作成
            val request = QuoteRequest(
                id = UUID.fromString(jsonMessage.getString("id")),
                product = jsonMessage.getString("product")
            )
            
            Log.info("見積もり依頼を処理: $request")
            
            // ランダムな価格を生成
            val price = generateRandomPrice(request.product)
            
            // 見積もり結果を作成
            val quote = Quote.fromRequest(request, price)
            
            Log.info("見積もり結果を生成: $quote")
            
            // RabbitMQに見積もり結果を送信
            quoteEmitter.send(quote)
            
            Log.info("見積もり結果をRabbitMQに送信: $quote")
            
        } catch (e: Exception) {
            Log.error("見積もり依頼の処理中にエラーが発生: ${e.message}", e)
        }
    }
    
    /**
     * 商品名に基づいてランダムな価格を生成する
     * 実際のアプリケーションでは、データベースや外部APIから価格を取得する
     * 
     * @param product 商品名
     * @return 生成された価格
     */
    private fun generateRandomPrice(product: String): BigDecimal {
        // 商品名の文字数に基づいて基本価格を決定
        val basePrice = product.length * 100
        
        // ランダムな変動を追加（±50%）
        val variation = 0.5 + random.nextDouble() // 0.5 から 1.5 の範囲
        val finalPrice = basePrice * variation
        
        // BigDecimalに変換して小数点以下2桁で丸める
        return BigDecimal.valueOf(finalPrice)
            .setScale(2, RoundingMode.HALF_UP)
    }
}
