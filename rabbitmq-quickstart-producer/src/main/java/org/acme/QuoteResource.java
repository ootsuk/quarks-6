package org.acme;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Producerアプリケーションのメインクラス
 * 見積もり依頼を受け取り、RabbitMQに送信する
 */
@Path("/quotes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuoteResource {
    
    private static final Logger LOG = Logger.getLogger(QuoteResource.class);
    
    // 見積もり依頼の状態を管理するマップ
    // Key: 依頼ID, Value: 見積もり依頼オブジェクト
    private final Map<UUID, QuoteRequest> requests = new ConcurrentHashMap<>();
    
    // RabbitMQへのメッセージ送信用Emitter
    @Inject
    @Channel("quote-requests")
    Emitter<QuoteRequest> quoteRequestEmitter;
    
    // QuoteProcessorを注入
    @Inject
    QuoteProcessor quoteProcessor;
    
    /**
     * 新しい見積もり依頼を作成し、RabbitMQに送信する
     * 
     * @param request 見積もり依頼（商品名のみ含む）
     * @return 作成された見積もり依頼のID
     */
    @POST
    @Path("/request")
    public Map<String, String> requestQuote(QuoteRequest request) {
        LOG.info("新しい見積もり依頼を受信: " + request.getProduct());
        
        // 新しい依頼IDを生成
        UUID requestId = UUID.randomUUID();
        QuoteRequest quoteRequest = new QuoteRequest(requestId, request.getProduct());
        
        // 依頼をマップに保存（後で結果を受け取る時に使用）
        requests.put(requestId, quoteRequest);
        
        // RabbitMQに依頼を送信
        quoteRequestEmitter.send(quoteRequest);
        
        LOG.info("見積もり依頼をRabbitMQに送信: " + quoteRequest);
        
        // クライアントに依頼IDを返す
        return Map.of("requestId", requestId.toString());
    }
    
    /**
     * 指定された依頼IDの見積もり依頼を取得する
     * 
     * @param requestId 依頼ID
     * @return 見積もり依頼オブジェクト
     */
    @GET
    @Path("/request/{requestId}")
    public QuoteRequest getRequest(@PathParam("requestId") String requestId) {
        UUID id = UUID.fromString(requestId);
        QuoteRequest request = requests.get(id);
        
        if (request == null) {
            throw new NotFoundException("見積もり依頼が見つかりません: " + requestId);
        }
        
        return request;
    }
    
    /**
     * 指定された依頼IDの見積もり結果を取得する
     * 
     * @param requestId 依頼ID
     * @return 見積もり結果
     */
    @GET
    @Path("/result/{requestId}")
    public Quote getQuoteResult(@PathParam("requestId") String requestId) {
        UUID id = UUID.fromString(requestId);
        Quote quote = quoteProcessor.getQuote(id);
        
        if (quote == null) {
            throw new NotFoundException("見積もり結果が見つかりません: " + requestId);
        }
        
        return quote;
    }
    
    /**
     * すべての見積もり依頼を取得する（デバッグ用）
     * 
     * @return すべての見積もり依頼のマップ
     */
    @GET
    @Path("/requests")
    public Map<UUID, QuoteRequest> getAllRequests() {
        return requests;
    }
    
    /**
     * すべての見積もり結果を取得する（デバッグ用）
     * 
     * @return すべての見積もり結果のマップ
     */
    @GET
    @Path("/results")
    public Map<UUID, Quote> getAllQuoteResults() {
        return quoteProcessor.getAllQuotes();
    }
}
