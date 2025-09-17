package org.acme

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import io.vertx.core.json.JsonObject
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 見積もり結果を受信し、管理するクラス
 * RabbitMQから見積もり結果を受け取り、管理する
 */
@ApplicationScoped
class QuoteProcessor {
    
    private val LOG = Logger.getLogger(QuoteProcessor::class.java)
    
    // 見積もり結果を管理するマップ
    // Key: 依頼ID, Value: 見積もり結果
    private val quotes = ConcurrentHashMap<UUID, Quote>()
    
    /**
     * RabbitMQから見積もり結果を受信する
     * 
     * @param jsonMessage 受信したJSONメッセージ
     */
    @Incoming("quotes")
    fun processQuote(jsonMessage: JsonObject) {
        LOG.info("JSONメッセージを受信: $jsonMessage")
        
        try {
            // JSONからQuoteオブジェクトを作成
            val quote = Quote(
                id = UUID.fromString(jsonMessage.getString("id")),
                requestId = UUID.fromString(jsonMessage.getString("requestId")),
                product = jsonMessage.getString("product"),
                price = BigDecimal(jsonMessage.getString("price")),
                timestamp = Instant.parse(jsonMessage.getString("timestamp"))
            )
            
            LOG.info("見積もり結果を受信: $quote")
            
            // 見積もり結果をマップに保存
            quotes[quote.requestId] = quote
            
            LOG.info("見積もり結果を保存: $quote")
            
        } catch (e: Exception) {
            LOG.error("見積もり結果の処理中にエラーが発生: ${e.message}", e)
        }
    }
    
    /**
     * 指定された依頼IDの見積もり結果を取得する
     * 
     * @param requestId 依頼ID
     * @return 見積もり結果
     */
    fun getQuote(requestId: UUID): Quote? {
        return quotes[requestId]
    }
    
    /**
     * すべての見積もり結果を取得する（デバッグ用）
     * 
     * @return すべての見積もり結果のマップ
     */
    fun getAllQuotes(): Map<UUID, Quote> {
        return quotes
    }
}
