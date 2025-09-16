package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 見積もり結果を受信し、管理するクラス
 * RabbitMQから見積もり結果を受け取り、管理する
 */
@ApplicationScoped
public class QuoteProcessor {
    
    private static final Logger LOG = Logger.getLogger(QuoteProcessor.class);
    
    // 見積もり結果を管理するマップ
    // Key: 依頼ID, Value: 見積もり結果
    private final Map<UUID, Quote> quotes = new ConcurrentHashMap<>();
    
    /**
     * RabbitMQから見積もり結果を受信する
     * 
     * @param jsonMessage 受信したJSONメッセージ
     */
    @Incoming("quotes")
    public void processQuote(JsonObject jsonMessage) {
        LOG.info("JSONメッセージを受信: " + jsonMessage);
        
        try {
            // JSONからQuoteオブジェクトを作成
            Quote quote = new Quote(
                UUID.fromString(jsonMessage.getString("id")),
                UUID.fromString(jsonMessage.getString("requestId")),
                jsonMessage.getString("product"),
                new BigDecimal(jsonMessage.getString("price")),
                Instant.parse(jsonMessage.getString("timestamp"))
            );
            
            LOG.info("見積もり結果を受信: " + quote);
            
            // 見積もり結果をマップに保存
            quotes.put(quote.getRequestId(), quote);
            
            LOG.info("見積もり結果を保存: " + quote);
            
        } catch (Exception e) {
            LOG.error("見積もり結果の処理中にエラーが発生: " + e.getMessage(), e);
        }
    }
    
    /**
     * 指定された依頼IDの見積もり結果を取得する
     * 
     * @param requestId 依頼ID
     * @return 見積もり結果
     */
    public Quote getQuote(UUID requestId) {
        return quotes.get(requestId);
    }
    
    /**
     * すべての見積もり結果を取得する（デバッグ用）
     * 
     * @return すべての見積もり結果のマップ
     */
    public Map<UUID, Quote> getAllQuotes() {
        return quotes;
    }
}
