package org.acme

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * 見積もり情報を表現するクラス
 * RabbitMQで送受信されるメッセージの形式を定義
 */
data class Quote(
    val id: UUID,
    val requestId: UUID,
    val product: String,
    val price: BigDecimal,
    val timestamp: Instant
) {
    companion object {
        /**
         * 見積もり依頼から見積もりを作成するファクトリメソッド
         */
        fun fromRequest(request: QuoteRequest, price: BigDecimal): Quote {
            return Quote(
                id = UUID.randomUUID(),  // 新しい見積もりIDを生成
                requestId = request.id,    // 元の依頼IDを保持
                product = request.product, // 商品名をコピー
                price = price,              // 計算された価格
                timestamp = Instant.now()   // 現在時刻
            )
        }
    }
}
