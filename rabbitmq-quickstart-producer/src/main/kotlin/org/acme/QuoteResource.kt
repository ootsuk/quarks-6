package org.acme

import io.smallrye.reactive.messaging.annotations.Broadcast
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Producerアプリケーションのメインクラス
 * 見積もり依頼を受け取り、RabbitMQに送信する
 */
@Path("/quotes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class QuoteResource {
    
    private val LOG = Logger.getLogger(QuoteResource::class.java)
    
    // 見積もり依頼の状態を管理するマップ
    // Key: 依頼ID, Value: 見積もり依頼オブジェクト
    private val requests = ConcurrentHashMap<UUID, QuoteRequest>()
    
    // RabbitMQへのメッセージ送信用Emitter
    @Inject
    @Channel("quote-requests")
    lateinit var quoteRequestEmitter: Emitter<QuoteRequest>
    
    // QuoteProcessorを注入
    @Inject
    lateinit var quoteProcessor: QuoteProcessor
    
    /**
     * 新しい見積もり依頼を作成し、RabbitMQに送信する
     * 
     * @param request 見積もり依頼（商品名のみ含む）
     * @return 作成された見積もり依頼のID
     */
    @POST
    @Path("/request")
    fun requestQuote(request: QuoteRequest): Map<String, String> {
        LOG.info("新しい見積もり依頼を受信: ${request.product}")
        
        // 新しい依頼IDを生成
        val requestId = UUID.randomUUID()
        val quoteRequest = QuoteRequest(requestId, request.product)
        
        // 依頼をマップに保存（後で結果を受け取る時に使用）
        requests[requestId] = quoteRequest
        
        // RabbitMQに依頼を送信
        quoteRequestEmitter.send(quoteRequest)
        
        LOG.info("見積もり依頼をRabbitMQに送信: $quoteRequest")
        
        // クライアントに依頼IDを返す
        return mapOf("requestId" to requestId.toString())
    }
    
    /**
     * 指定された依頼IDの見積もり依頼を取得する
     * 
     * @param requestId 依頼ID
     * @return 見積もり依頼オブジェクト
     */
    @GET
    @Path("/request/{requestId}")
    fun getRequest(@PathParam("requestId") requestId: String): QuoteRequest {
        val id = UUID.fromString(requestId)
        val request = requests[id]
        
        if (request == null) {
            throw NotFoundException("見積もり依頼が見つかりません: $requestId")
        }
        
        return request
    }
    
    /**
     * 指定された依頼IDの見積もり結果を取得する
     * 
     * @param requestId 依頼ID
     * @return 見積もり結果
     */
    @GET
    @Path("/result/{requestId}")
    fun getQuoteResult(@PathParam("requestId") requestId: String): Quote {
        val id = UUID.fromString(requestId)
        val quote = quoteProcessor.getQuote(id)
        
        if (quote == null) {
            throw NotFoundException("見積もり結果が見つかりません: $requestId")
        }
        
        return quote
    }
    
    /**
     * すべての見積もり依頼を取得する（デバッグ用）
     * 
     * @return すべての見積もり依頼のマップ
     */
    @GET
    @Path("/requests")
    fun getAllRequests(): Map<UUID, QuoteRequest> {
        return requests
    }
    
    /**
     * すべての見積もり結果を取得する（デバッグ用）
     * 
     * @return すべての見積もり結果のマップ
     */
    @GET
    @Path("/results")
    fun getAllQuoteResults(): Map<UUID, Quote> {
        return quoteProcessor.getAllQuotes()
    }
}
