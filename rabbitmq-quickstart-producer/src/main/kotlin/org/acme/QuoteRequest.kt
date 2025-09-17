package org.acme

import java.util.UUID

/**
 * 見積もり依頼を表現するクラス
 * ユーザーが送信する見積もり依頼の形式を定義
 */
data class QuoteRequest(
    val id: UUID,
    val product: String
) {
    companion object {
        /**
         * 新しい見積もり依頼を作成するファクトリメソッド
         */
        fun create(product: String): QuoteRequest {
            return QuoteRequest(UUID.randomUUID(), product)
        }
    }
}
